package com.example.stockanalysis.utils

import com.example.stockanalysis.data.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 技术指标计算器
 * 参照 daily_stock_analysis 的技术分析逻辑实现
 */
object TechnicalIndicatorCalculator {
    
    /**
     * 计算移动平均线
     */
    fun calculateMA(prices: List<Double>, period: Int): List<Double?> {
        if (prices.size < period) return List(prices.size) { null }
        
        val result = MutableList<Double?>(prices.size) { null }
        for (i in period - 1 until prices.size) {
            val sum = prices.subList(i - period + 1, i + 1).sum()
            result[i] = sum / period
        }
        return result
    }
    
    /**
     * 计算EMA（指数移动平均线）
     */
    fun calculateEMA(prices: List<Double>, period: Int): List<Double?> {
        if (prices.size < period) return List(prices.size) { null }
        
        val multiplier = 2.0 / (period + 1)
        val result = MutableList<Double?>(prices.size) { null }
        
        // 第一个EMA使用SMA
        var ema = prices.subList(0, period).average()
        result[period - 1] = ema
        
        for (i in period until prices.size) {
            ema = (prices[i] - ema) * multiplier + ema
            result[i] = ema
        }
        return result
    }
    
    /**
     * 计算MACD
     */
    fun calculateMACD(prices: List<Double>): List<MacdData?> {
        val ema12 = calculateEMA(prices, 12)
        val ema26 = calculateEMA(prices, 26)
        
        val difList = MutableList<Double?>(prices.size) { null }
        for (i in prices.indices) {
            if (ema12[i] != null && ema26[i] != null) {
                difList[i] = ema12[i]!! - ema26[i]!!
            }
        }
        
        // 计算DEA（DIF的9日EMA）
        val validDifs = difList.filterNotNull()
        val deaListFull = calculateEMA(validDifs, 9)
        
        val result = MutableList<MacdData?>(prices.size) { null }
        var deaIndex = 0
        for (i in prices.indices) {
            if (difList[i] != null && deaIndex < deaListFull.size && deaListFull[deaIndex] != null) {
                val dif = difList[i]!!
                val dea = deaListFull[deaIndex]!!
                val macd = (dif - dea) * 2
                result[i] = MacdData(dif, dea, macd)
                deaIndex++
            }
        }
        return result
    }
    
    /**
     * 计算KDJ
     */
    fun calculateKDJ(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        n: Int = 9,
        m1: Int = 3,
        m2: Int = 3
    ): List<KdjData?> {
        if (highs.size != lows.size || highs.size != closes.size || highs.size < n) {
            return List(highs.size) { null }
        }
        
        val size = highs.size
        val rsvList = MutableList<Double?>(size) { null }
        
        for (i in n - 1 until size) {
            val highestHigh = highs.subList(i - n + 1, i + 1).maxOrNull() ?: continue
            val lowestLow = lows.subList(i - n + 1, i + 1).minOrNull() ?: continue
            
            if (highestHigh != lowestLow) {
                rsvList[i] = (closes[i] - lowestLow) / (highestHigh - lowestLow) * 100
            }
        }
        
        val kList = MutableList<Double?>(size) { null }
        val dList = MutableList<Double?>(size) { null }
        val jList = MutableList<Double?>(size) { null }
        
        // 初始化K、D值
        var k = 50.0
        var d = 50.0
        
        for (i in 0 until size) {
            if (rsvList[i] != null) {
                k = (k * (m1 - 1) + rsvList[i]!!) / m1
                d = (d * (m2 - 1) + k) / m2
                val j = 3 * k - 2 * d
                
                kList[i] = k
                dList[i] = d
                jList[i] = j
            }
        }
        
        return List(size) { i ->
            if (kList[i] != null && dList[i] != null && jList[i] != null) {
                KdjData(kList[i]!!, dList[i]!!, jList[i]!!)
            } else null
        }
    }
    
    /**
     * 计算RSI
     */
    fun calculateRSI(prices: List<Double>, period: Int = 14): List<Double?> {
        if (prices.size < period + 1) return List(prices.size) { null }
        
        val result = MutableList<Double?>(prices.size) { null }
        var avgGain = 0.0
        var avgLoss = 0.0
        
        // 计算初始平均涨跌
        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) avgGain += change
            else avgLoss += -change
        }
        avgGain /= period
        avgLoss /= period
        
        result[period] = if (avgLoss == 0.0) 100.0 else 100 - (100 / (1 + avgGain / avgLoss))
        
        // 计算后续RSI
        for (i in period + 1 until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0
            
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            
            result[i] = if (avgLoss == 0.0) 100.0 else 100 - (100 / (1 + avgGain / avgLoss))
        }
        return result
    }
    
    /**
     * 计算布林带
     */
    fun calculateBOLL(prices: List<Double>, period: Int = 20, multiplier: Double = 2.0): List<BollData?> {
        if (prices.size < period) return List(prices.size) { null }
        
        val result = MutableList<BollData?>(prices.size) { null }
        
        for (i in period - 1 until prices.size) {
            val slice = prices.subList(i - period + 1, i + 1)
            val middle = slice.average()
            val variance = slice.map { (it - middle) * (it - middle) }.average()
            val stdDev = sqrt(variance)
            
            result[i] = BollData(
                upper = middle + multiplier * stdDev,
                middle = middle,
                lower = middle - multiplier * stdDev
            )
        }
        return result
    }
    
    /**
     * 计算乖离率
     */
    fun calculateBIAS(prices: List<Double>, period: Int): List<Double?> {
        val ma = calculateMA(prices, period)
        return prices.zip(ma).map { (price, maValue) ->
            if (maValue != null && maValue != 0.0) {
                (price - maValue) / maValue * 100
            } else null
        }
    }
    
    /**
     * 计算CCI（商品通道指数）
     */
    fun calculateCCI(
        highs: List<Double>,
        lows: List<Double>,
        closes: List<Double>,
        period: Int = 14
    ): List<Double?> {
        if (highs.size != lows.size || highs.size != closes.size || highs.size < period) {
            return List(highs.size) { null }
        }
        
        val tpList = closes.indices.map { i ->
            (highs[i] + lows[i] + closes[i]) / 3
        }
        
        val result = MutableList<Double?>(highs.size) { null }
        
        for (i in period - 1 until highs.size) {
            val slice = tpList.subList(i - period + 1, i + 1)
            val smaTp = slice.average()
            val meanDev = slice.map { kotlin.math.abs(it - smaTp) }.average()
            
            result[i] = if (meanDev != 0.0) (tpList[i] - smaTp) / (0.015 * meanDev) else null
        }
        return result
    }
    
    /**
     * 从K线数据计算所有技术指标
     */
    fun calculateAllIndicators(klineData: List<KLineData>): TechnicalIndicators? {
        if (klineData.size < 60) return null
        
        val closes = klineData.map { it.close }
        val highs = klineData.map { it.high }
        val lows = klineData.map { it.low }
        val volumes = klineData.map { it.volume.toDouble() }
        
        val ma5 = calculateMA(closes, 5).lastOrNull()
        val ma10 = calculateMA(closes, 10).lastOrNull()
        val ma20 = calculateMA(closes, 20).lastOrNull()
        val ma60 = calculateMA(closes, 60).lastOrNull()
        
        val maList = calculateMA(closes, 5)
        val bias24 = if (closes.size >= 24) {
            calculateBIAS(closes, 24).lastOrNull()
        } else null
        
        return TechnicalIndicators(
            symbol = klineData.first().symbol,
            timestamp = klineData.first().timestamp,
            movingAverages = MovingAverages(
                symbol = klineData.first().symbol,
                ma5 = ma5,
                ma10 = ma10,
                ma20 = ma20,
                ma60 = ma60
            ),
            macd = calculateMACD(closes).lastOrNull(),
            kdj = calculateKDJ(highs, lows, closes).lastOrNull(),
            rsi6 = calculateRSI(closes, 6).lastOrNull(),
            rsi12 = calculateRSI(closes, 12).lastOrNull(),
            rsi24 = calculateRSI(closes, 24).lastOrNull(),
            boll = calculateBOLL(closes).lastOrNull(),
            volumeMa5 = calculateMA(volumes, 5).lastOrNull(),
            volumeMa10 = calculateMA(volumes, 10).lastOrNull(),
            bias24 = bias24
        )
    }
    
    /**
     * 执行趋势分析
     * 参照 daily_stock_analysis 的 StockTrendAnalyzer
     */
    fun analyzeTrend(klineData: List<KLineData>, currentPrice: Double): TrendAnalysis? {
        if (klineData.size < 20) return null
        
        val closes = klineData.map { it.close }
        val volumes = klineData.map { it.volume.toDouble() }
        
        val ma5List = calculateMA(closes, 5)
        val ma10List = calculateMA(closes, 10)
        val ma20List = calculateMA(closes, 20)
        
        val ma5 = ma5List.lastOrNull() ?: return null
        val ma10 = ma10List.lastOrNull() ?: return null
        val ma20 = ma20List.lastOrNull() ?: return null
        
        // 判断均线排列
        val isBullishAlignment = ma5 > ma10 && ma10 > ma20
        val isBearishAlignment = ma5 < ma10 && ma10 < ma20
        
        // 计算乖离率
        val biasMa5 = (currentPrice - ma5) / ma5 * 100
        val biasMa10 = (currentPrice - ma10) / ma10 * 100
        
        // 分析成交量
        val recentVolume = volumes.takeLast(5).average()
        val prevVolume = volumes.subList(max(0, volumes.size - 10), volumes.size - 5).average()
        val volumeStatus = when {
            recentVolume > prevVolume * 1.5 -> VolumeStatus.EXPANDING
            recentVolume < prevVolume * 0.7 -> VolumeStatus.SHRINKING
            else -> VolumeStatus.NORMAL
        }
        
        // 计算支撑位和阻力位（简化版）
        val recentLows = klineData.takeLast(20).map { it.low }
        val recentHighs = klineData.takeLast(20).map { it.high }
        val supportLevel = recentLows.minOrNull()
        val resistanceLevel = recentHighs.maxOrNull()
        
        // 生成信号理由
        val signalReasons = mutableListOf<String>()
        val riskFactors = mutableListOf<String>()
        
        if (isBullishAlignment) {
            signalReasons.add("均线多头排列")
        } else if (isBearishAlignment) {
            riskFactors.add("均线空头排列")
        }
        
        if (biasMa5 > 5) {
            riskFactors.add("偏离MA5超过5%，不宜追高")
        } else if (biasMa5 < -3) {
            signalReasons.add("回踩MA5支撑")
        }
        
        when (volumeStatus) {
            VolumeStatus.EXPANDING -> signalReasons.add("成交量放大")
            VolumeStatus.SHRINKING -> signalReasons.add("缩量整理")
            else -> {}
        }
        
        // 计算趋势强度和买入信号
        val trendStatus = when {
            isBullishAlignment && currentPrice > ma5 -> TrendStatus.STRONG_UP
            isBullishAlignment -> TrendStatus.UP
            currentPrice > ma5 && ma5 > ma10 -> TrendStatus.WEAK_UP
            isBearishAlignment && currentPrice < ma5 -> TrendStatus.STRONG_DOWN
            isBearishAlignment -> TrendStatus.DOWN
            currentPrice < ma5 && ma5 < ma10 -> TrendStatus.WEAK_DOWN
            else -> TrendStatus.SIDEWAYS
        }
        
        val buySignal = when {
            isBullishAlignment && biasMa5 in -3.0..3.0 && volumeStatus == VolumeStatus.EXPANDING ->
                BuySignal.STRONG_BUY
            isBullishAlignment && biasMa5 in -5.0..5.0 ->
                BuySignal.BUY
            biasMa5 > 0 && volumeStatus == VolumeStatus.EXPANDING ->
                BuySignal.WEAK_BUY
            isBearishAlignment && biasMa5 < -5 ->
                BuySignal.STRONG_SELL
            isBearishAlignment ->
                BuySignal.SELL
            biasMa5 < 0 ->
                BuySignal.WEAK_SELL
            else ->
                BuySignal.NEUTRAL
        }
        
        val trendStrength = calculateTrendStrength(klineData, isBullishAlignment, isBearishAlignment)
        val signalScore = calculateSignalScore(buySignal, trendStrength, signalReasons.size, riskFactors.size)
        
        return TrendAnalysis(
            symbol = klineData.first().symbol,
            trendStatus = trendStatus,
            trendStrength = trendStrength,
            buySignal = buySignal,
            signalScore = signalScore,
            signalReasons = signalReasons,
            riskFactors = riskFactors,
            biasMa5 = biasMa5,
            biasMa10 = biasMa10,
            volumeStatus = volumeStatus,
            supportLevel = supportLevel,
            resistanceLevel = resistanceLevel
        )
    }
    
    private fun calculateTrendStrength(
        klineData: List<KLineData>,
        isBullishAlignment: Boolean,
        isBearishAlignment: Boolean
    ): Double {
        var strength = 50.0
        
        // 均线排列加分
        when {
            isBullishAlignment -> strength += 20
            isBearishAlignment -> strength -= 20
        }
        
        // 价格趋势分析
        val recentData = klineData.takeLast(10)
        val upDays = recentData.count { it.close > it.open }
        val downDays = recentData.count { it.close < it.open }
        
        strength += (upDays - downDays) * 3
        
        // 限制范围
        return strength.coerceIn(0.0, 100.0)
    }
    
    private fun calculateSignalScore(
        buySignal: BuySignal,
        trendStrength: Double,
        positiveFactors: Int,
        riskFactors: Int
    ): Int {
        var score = when (buySignal) {
            BuySignal.STRONG_BUY -> 85
            BuySignal.BUY -> 70
            BuySignal.WEAK_BUY -> 60
            BuySignal.NEUTRAL -> 50
            BuySignal.WEAK_SELL -> 40
            BuySignal.SELL -> 30
            BuySignal.STRONG_SELL -> 15
        }
        
        // 根据趋势强度调整
        score += (trendStrength - 50).toInt() / 5
        
        // 因子调整
        score += positiveFactors * 3
        score -= riskFactors * 5
        
        return score.coerceIn(0, 100)
    }
}
