package com.example.stockanalysis.data.datasource

import android.util.Log
import com.example.stockanalysis.data.model.ChipDistribution
import com.example.stockanalysis.data.model.KLineData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 筹码分布数据源接口
 */
interface ChipDistributionDataSource {
    /**
     * 获取筹码分布数据
     * @param symbol 股票代码
     * @param days 计算周期（默认90天）
     * @return 筹码分布数据
     */
    suspend fun fetchChipDistribution(symbol: String, days: Int = 90): Result<ChipDistribution>
    
    /**
     * 获取筹码分布历史（用于分析筹码变化趋势）
     * @param symbol 股票代码
     * @param historyDays 历史天数
     * @return 历史筹码分布列表
     */
    suspend fun fetchChipDistributionHistory(
        symbol: String, 
        historyDays: Int = 30
    ): Result<List<ChipDistribution>>
}

/**
 * 东方财富筹码分布数据源
 * 通过历史K线数据计算筹码分布
 */
@Singleton
class EFinanceChipDistributionDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val eFinanceDataSource: EFinanceDataSource
) : ChipDistributionDataSource {
    
    companion object {
        const val TAG = "ChipDistDataSource"
        // 东方财富获取筹码数据的API
        const val CHIP_DATA_URL = "https://push2.eastmoney.com/api/qt/stock/get"
    }
    
    override suspend fun fetchChipDistribution(symbol: String, days: Int): Result<ChipDistribution> = 
        withContext(Dispatchers.IO) {
            try {
                // 1. 获取K线数据
                val klineResult = eFinanceDataSource.fetchKLineData(symbol, days)
                
                klineResult.fold(
                    onSuccess = { klines ->
                        if (klines.size < 30) {
                            return@withContext Result.failure(
                                DataSourceException.ParseException("Insufficient K-line data")
                            )
                        }
                        
                        // 2. 计算筹码分布
                        val chipDistribution = calculateChipDistribution(symbol, klines)
                        Result.success(chipDistribution)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch chip distribution for $symbol", e)
                Result.failure(DataSourceException.NetworkException("Failed to fetch chip distribution", e))
            }
        }
    
    override suspend fun fetchChipDistributionHistory(
        symbol: String,
        historyDays: Int
    ): Result<List<ChipDistribution>> = withContext(Dispatchers.IO) {
        try {
            val history = mutableListOf<ChipDistribution>()
            val calendar = Calendar.getInstance()
            
            // 获取最近N天的筹码分布
            for (i in 0 until historyDays step 5) { // 每5天取一个点
                calendar.add(Calendar.DAY_OF_YEAR, -5)
                val endDate = calendar.time
                
                // 获取到该日期为止的K线数据
                val klineResult = eFinanceDataSource.fetchKLineData(symbol, 90)
                
                klineResult.getOrNull()?.let { klines ->
                    val filteredKlines = klines.filter { it.timestamp <= endDate }
                    if (filteredKlines.size >= 30) {
                        val chipDist = calculateChipDistribution(symbol, filteredKlines)
                        history.add(chipDist)
                    }
                }
            }
            
            Result.success(history)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch chip distribution history for $symbol", e)
            Result.failure(DataSourceException.NetworkException("Failed to fetch history", e))
        }
    }
    
    /**
     * 计算筹码分布
     * 基于成交量加权平均成本算法
     */
    private fun calculateChipDistribution(symbol: String, klines: List<KLineData>): ChipDistribution {
        // 1. 计算平均成本（成交量加权）
        var totalVolume = 0L
        var totalCost = 0.0
        
        klines.forEach { kline ->
            // 使用典型价格（高+低+收）/ 3 作为当日成交价格代表
            val typicalPrice = (kline.high + kline.low + kline.close) / 3.0
            totalCost += typicalPrice * kline.volume
            totalVolume += kline.volume
        }
        
        val avgCost = if (totalVolume > 0) totalCost / totalVolume else klines.last().close
        
        // 2. 计算各价格区间的筹码分布
        val priceRanges = createPriceRanges(klines)
        val chipDistribution = mutableMapOf<Double, Long>() // 价格区间中心 -> 筹码量
        
        klines.forEach { kline ->
            val typicalPrice = (kline.high + kline.low + kline.close) / 3.0
            val rangeCenter = findNearestRangeCenter(typicalPrice, priceRanges)
            chipDistribution[rangeCenter] = chipDistribution.getOrDefault(rangeCenter, 0L) + kline.volume
        }
        
        // 3. 计算获利比例
        val currentPrice = klines.last().close
        var profitVolume = 0L
        chipDistribution.forEach { (price, volume) ->
            if (price < currentPrice) {
                profitVolume += volume
            }
        }
        val profitRatio = if (totalVolume > 0) profitVolume.toDouble() / totalVolume else 0.5
        
        // 4. 计算筹码集中度
        val sortedPrices = chipDistribution.keys.sorted()
        val totalChip = chipDistribution.values.sum()
        
        val concentration90 = calculateConcentration(chipDistribution, sortedPrices, totalChip, 0.90)
        val concentration70 = calculateConcentration(chipDistribution, sortedPrices, totalChip, 0.70)
        
        // 5. 找到筹码峰值价格
        val peakPrice = chipDistribution.maxByOrNull { it.value }?.key
        
        // 6. 计算支撑和阻力位
        val (supportLevels, resistanceLevels) = calculateSupportResistance(
            klines, avgCost, currentPrice, chipDistribution
        )
        
        return ChipDistribution(
            symbol = symbol,
            avgCost = avgCost,
            profitRatio = profitRatio,
            concentration90 = concentration90,
            concentration70 = concentration70,
            peakPrice = peakPrice,
            supportLevels = supportLevels,
            resistanceLevels = resistanceLevels
        )
    }
    
    /**
     * 创建价格区间
     */
    private fun createPriceRanges(klines: List<KLineData>): List<Double> {
        val minPrice = klines.minOf { it.low }
        val maxPrice = klines.maxOf { it.high }
        val range = maxPrice - minPrice
        
        // 创建20个价格区间
        val intervals = 20
        val step = range / intervals
        
        return (0..intervals).map { minPrice + it * step }
    }
    
    /**
     * 找到最近的价格区间中心
     */
    private fun findNearestRangeCenter(price: Double, ranges: List<Double>): Double {
        return ranges.minByOrNull { abs(it - price) } ?: price
    }
    
    /**
     * 计算筹码集中度
     * @param percentage 需要覆盖的筹码比例（如0.90表示90%）
     * @return 集中度比例（0-1，越小越集中）
     */
    private fun calculateConcentration(
        chipDistribution: Map<Double, Long>,
        sortedPrices: List<Double>,
        totalChip: Long,
        percentage: Double
    ): Double {
        if (totalChip == 0L || sortedPrices.isEmpty()) return 1.0
        
        val targetChip = totalChip * percentage
        var currentChip = 0L
        var minPrice = sortedPrices.first()
        var maxPrice = sortedPrices.first()
        
        // 从平均筹码位置开始向外扩展
        val centerIndex = sortedPrices.size / 2
        var leftIndex = centerIndex
        var rightIndex = centerIndex
        
        while (currentChip < targetChip && (leftIndex > 0 || rightIndex < sortedPrices.size - 1)) {
            if (leftIndex > 0) {
                leftIndex--
                val price = sortedPrices[leftIndex]
                currentChip += chipDistribution[price] ?: 0
                minPrice = price
            }
            if (rightIndex < sortedPrices.size - 1 && currentChip < targetChip) {
                rightIndex++
                val price = sortedPrices[rightIndex]
                currentChip += chipDistribution[price] ?: 0
                maxPrice = price
            }
        }
        
        val priceRange = sortedPrices.last() - sortedPrices.first()
        return if (priceRange > 0) {
            (maxPrice - minPrice) / priceRange
        } else {
            1.0
        }
    }
    
    /**
     * 计算支撑和阻力位
     */
    private fun calculateSupportResistance(
        klines: List<KLineData>,
        avgCost: Double,
        currentPrice: Double,
        chipDistribution: Map<Double, Long>
    ): Pair<List<Double>, List<Double>> {
        val supportLevels = mutableListOf<Double>()
        val resistanceLevels = mutableListOf<Double>()
        
        // 1. 基于平均成本的支撑/阻力
        if (currentPrice > avgCost) {
            supportLevels.add(avgCost)
        } else {
            resistanceLevels.add(avgCost)
        }
        
        // 2. 基于筹码密集区
        val sortedByChip = chipDistribution.entries.sortedByDescending { it.value }
        val denseAreas = sortedByChip.take(3).map { it.key }
        
        denseAreas.forEach { price ->
            when {
                price < currentPrice * 0.98 -> supportLevels.add(price)
                price > currentPrice * 1.02 -> resistanceLevels.add(price)
            }
        }
        
        // 3. 基于近期高低点
        val recentKlines = klines.takeLast(20)
        val recentLow = recentKlines.minOf { it.low }
        val recentHigh = recentKlines.maxOf { it.high }
        
        if (recentLow < currentPrice * 0.95) {
            supportLevels.add(recentLow)
        }
        if (recentHigh > currentPrice * 1.05) {
            resistanceLevels.add(recentHigh)
        }
        
        // 4. 添加基于平均成本的斐波那契回撤位
        val fibRetracement38 = currentPrice - (currentPrice - avgCost) * 0.382
        val fibRetracement50 = (currentPrice + avgCost) / 2
        val fibRetracement62 = currentPrice - (currentPrice - avgCost) * 0.618
        
        if (fibRetracement38 < currentPrice) supportLevels.add(fibRetracement38)
        if (fibRetracement50 < currentPrice) supportLevels.add(fibRetracement50)
        if (fibRetracement62 < currentPrice) supportLevels.add(fibRetracement62)
        
        // 去重并排序
        return Pair(
            supportLevels.distinct().sortedDescending().take(3),
            resistanceLevels.distinct().sorted().take(3)
        )
    }
}

/**
 * 本地筹码分布数据源（基于已有K线数据计算）
 */
@Singleton
class LocalChipDistributionDataSource @Inject constructor(
    private val localDataSource: LocalDataSource
) : ChipDistributionDataSource {
    
    companion object {
        const val TAG = "LocalChipDistDS"
    }
    
    override suspend fun fetchChipDistribution(symbol: String, days: Int): Result<ChipDistribution> =
        withContext(Dispatchers.IO) {
            try {
                // 获取本地K线数据
                val klineResult = localDataSource.fetchKLineData(symbol, days)
                
                klineResult.fold(
                    onSuccess = { klines ->
                        if (klines.isEmpty()) {
                            return@withContext Result.failure(
                                DataSourceException.ParseException("No K-line data available")
                            )
                        }
                        
                        val chipDist = calculateFromKlines(symbol, klines)
                        Result.success(chipDist)
                    },
                    onFailure = { error ->
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Result.failure(DataSourceException.NetworkException("Failed to calculate chip distribution", e))
            }
        }
    
    override suspend fun fetchChipDistributionHistory(
        symbol: String,
        historyDays: Int
    ): Result<List<ChipDistribution>> {
        // 本地数据源不支持历史筹码分布
        return Result.success(emptyList())
    }
    
    /**
     * 从K线数据计算筹码分布
     */
    private fun calculateFromKlines(symbol: String, klines: List<KLineData>): ChipDistribution {
        val calculator = ChipDistributionCalculator()
        return calculator.calculate(symbol, klines)
    }
}

/**
 * 筹码分布计算器
 */
class ChipDistributionCalculator {
    
    /**
     * 计算筹码分布
     */
    fun calculate(symbol: String, klines: List<KLineData>): ChipDistribution {
        val validKlines = klines.sortedBy { it.timestamp }
        
        // 成交量加权平均成本
        var totalVolume = 0L
        var weightedCost = 0.0
        
        validKlines.forEach { kline ->
            val typicalPrice = (kline.high + kline.low + kline.close) / 3.0
            weightedCost += typicalPrice * kline.volume
            totalVolume += kline.volume
        }
        
        val avgCost = if (totalVolume > 0) weightedCost / totalVolume else validKlines.last().close
        
        // 计算价格分布
        val priceDistribution = mutableMapOf<Double, Long>()
        val minPrice = validKlines.minOf { it.low }
        val maxPrice = validKlines.maxOf { it.high }
        val binSize = (maxPrice - minPrice) / 20.0
        
        validKlines.forEach { kline ->
            val typicalPrice = (kline.high + kline.low + kline.close) / 3.0
            val binCenter = minPrice + ((typicalPrice - minPrice) / binSize).toInt() * binSize + binSize / 2
            priceDistribution[binCenter] = priceDistribution.getOrDefault(binCenter, 0L) + kline.volume
        }
        
        // 计算获利比例
        val currentPrice = validKlines.last().close
        val profitVolume = priceDistribution.filter { it.key < currentPrice }.values.sum()
        val profitRatio = if (totalVolume > 0) profitVolume.toDouble() / totalVolume else 0.5
        
        // 计算集中度
        val concentration90 = calculateConcentrationLevel(priceDistribution, 0.90)
        val concentration70 = calculateConcentrationLevel(priceDistribution, 0.70)
        
        // 峰值价格
        val peakPrice = priceDistribution.maxByOrNull { it.value }?.key
        
        // 支撑阻力位
        val (supports, resistances) = calculateLevels(validKlines, avgCost, currentPrice, priceDistribution)
        
        return ChipDistribution(
            symbol = symbol,
            avgCost = avgCost,
            profitRatio = profitRatio,
            concentration90 = concentration90,
            concentration70 = concentration70,
            peakPrice = peakPrice,
            supportLevels = supports,
            resistanceLevels = resistances
        )
    }
    
    /**
     * 计算集中度水平
     */
    private fun calculateConcentrationLevel(
        distribution: Map<Double, Long>,
        ratio: Double
    ): Double {
        if (distribution.isEmpty()) return 1.0
        
        val total = distribution.values.sum()
        val target = total * ratio
        val sorted = distribution.entries.sortedBy { it.key }
        
        var accumulated = 0L
        var minPrice = sorted.first().key
        var maxPrice = sorted.last().key
        
        // 从筹码最多的位置开始
        val maxEntry = sorted.maxByOrNull { it.value } ?: return 1.0
        val centerIndex = sorted.indexOf(maxEntry)
        
        var left = centerIndex
        var right = centerIndex
        accumulated += maxEntry.value
        
        while (accumulated < target && (left > 0 || right < sorted.size - 1)) {
            if (left > 0) {
                left--
                accumulated += sorted[left].value
                minPrice = sorted[left].key
            }
            if (accumulated < target && right < sorted.size - 1) {
                right++
                accumulated += sorted[right].value
                maxPrice = sorted[right].key
            }
        }
        
        val priceRange = sorted.last().key - sorted.first().key
        return if (priceRange > 0) (maxPrice - minPrice) / priceRange else 1.0
    }
    
    /**
     * 计算支撑阻力位
     */
    private fun calculateLevels(
        klines: List<KLineData>,
        avgCost: Double,
        currentPrice: Double,
        distribution: Map<Double, Long>
    ): Pair<List<Double>, List<Double>> {
        val supports = mutableListOf<Double>()
        val resistances = mutableListOf<Double>()
        
        // 添加平均成本作为支撑或阻力
        if (avgCost < currentPrice) {
            supports.add(avgCost)
        } else {
            resistances.add(avgCost)
        }
        
        // 筹码密集区
        val topChips = distribution.entries.sortedByDescending { it.value }.take(5)
        topChips.forEach { (price, _) ->
            when {
                price < currentPrice * 0.97 -> supports.add(price)
                price > currentPrice * 1.03 -> resistances.add(price)
            }
        }
        
        // 近期高低点
        val recent = klines.takeLast(20)
        val low = recent.minOf { it.low }
        val high = recent.maxOf { it.high }
        
        supports.add(low)
        resistances.add(high)
        
        return Pair(
            supports.distinct().sortedDescending().take(3),
            resistances.distinct().sorted().take(3)
        )
    }
}
