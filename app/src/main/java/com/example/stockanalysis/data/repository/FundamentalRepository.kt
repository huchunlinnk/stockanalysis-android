package com.example.stockanalysis.data.repository

import android.util.Log
import com.example.stockanalysis.data.datasource.FundamentalDataSource
import com.example.stockanalysis.data.local.FundamentalDao
import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基本面数据仓库接口
 */
interface FundamentalRepository {
    
    /**
     * 获取股票基本面数据（优先从缓存获取，过期则刷新）
     */
    suspend fun getFundamentalData(symbol: String, name: String = "", forceRefresh: Boolean = false): Result<FundamentalData>
    
    /**
     * 获取基本面数据流
     */
    fun getFundamentalDataFlow(symbol: String, name: String = ""): Flow<Result<FundamentalData>>
    
    /**
     * 获取股票完整基本面分析结果
     */
    suspend fun getFundamentalAnalysis(symbol: String, name: String = "", currentPrice: Double? = null): Result<FundamentalAnalysisResult>
    
    /**
     * 批量获取基本面数据
     */
    suspend fun getBatchFundamentalData(symbols: List<Pair<String, String>>): Result<List<FundamentalData>>
    
    /**
     * 刷新指定股票的基本面数据
     */
    suspend fun refreshFundamentalData(symbol: String, name: String = ""): Result<FundamentalData>
    
    /**
     * 获取财务指标
     */
    suspend fun getFinancialIndicators(symbol: String): FinancialIndicators
    
    /**
     * 获取成长性指标
     */
    suspend fun getGrowthMetrics(symbol: String): GrowthMetrics
    
    /**
     * 获取分红信息
     */
    suspend fun getDividendInfo(symbol: String): DividendInfo
    
    /**
     * 获取机构持仓
     */
    suspend fun getInstitutionalHolding(symbol: String): InstitutionalHolding
    
    /**
     * 获取估值指标
     */
    suspend fun getValuationMetrics(symbol: String): ValuationMetrics
    
    /**
     * 清空缓存
     */
    suspend fun clearCache()
    
    /**
     * 删除指定股票的缓存
     */
    suspend fun clearCacheForSymbol(symbol: String)
    
    /**
     * 检查缓存是否有效
     */
    suspend fun isCacheValid(symbol: String): Boolean
    
    /**
     * 获取缓存的所有基本面数据
     */
    suspend fun getCachedFundamentalData(): List<FundamentalData>
}

/**
 * 基本面数据仓库实现
 * 
 * 功能：
 * 1. 管理基本面数据的本地缓存和远程获取
 * 2. 实现数据更新策略（缓存1天）
 * 3. 提供完整的基本面分析计算
 */
@Singleton
class FundamentalRepositoryImpl @Inject constructor(
    private val fundamentalDao: FundamentalDao,
    private val fundamentalDataSource: FundamentalDataSource
) : FundamentalRepository {
    
    companion object {
        const val TAG = "FundamentalRepository"
        const val CACHE_DAYS = 1  // 缓存有效期（天）
    }
    
    /**
     * 获取股票基本面数据
     * 
     * 策略：
     * 1. 如果强制刷新，直接从网络获取
     * 2. 如果本地有有效缓存，直接返回
     * 3. 否则从网络获取并缓存
     */
    override suspend fun getFundamentalData(
        symbol: String, 
        name: String,
        forceRefresh: Boolean
    ): Result<FundamentalData> = withContext(Dispatchers.IO) {
        try {
            // 检查是否需要强制刷新
            if (!forceRefresh) {
                // 检查本地缓存
                val cachedData = fundamentalDao.getFundamentalDataBySymbol(symbol)
                
                if (cachedData != null && !cachedData.isExpired(CACHE_DAYS)) {
                    Log.d(TAG, "使用缓存的基本面数据: $symbol")
                    return@withContext Result.success(cachedData)
                }
            }
            
            // 从网络获取
            Log.d(TAG, "从网络获取基本面数据: $symbol")
            val result = fundamentalDataSource.getFundamentalBundle(symbol, name)
            
            result.fold(
                onSuccess = { data ->
                    // 保存到本地
                    fundamentalDao.insertFundamentalData(data)
                    Log.d(TAG, "基本面数据已缓存: $symbol")
                    Result.success(data)
                },
                onFailure = { error ->
                    // 网络失败但本地有缓存，返回缓存数据
                    val cachedData = fundamentalDao.getFundamentalDataBySymbol(symbol)
                    if (cachedData != null) {
                        Log.w(TAG, "网络请求失败，使用过期缓存: $symbol")
                        Result.success(cachedData.copy(isCacheValid = false))
                    } else {
                        Result.failure(error)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取基本面数据异常: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取基本面数据流
     */
    override fun getFundamentalDataFlow(symbol: String, name: String): Flow<Result<FundamentalData>> = flow {
        // 先尝试从缓存获取
        val cachedData = fundamentalDao.getFundamentalDataBySymbol(symbol)
        
        if (cachedData != null && !cachedData.isExpired(CACHE_DAYS)) {
            emit(Result.success(cachedData))
            return@flow
        }
        
        // 缓存无效或不存在，从网络获取
        emit(Result.failure<FundamentalData>(Exception("Loading from network...")))
        
        val result = fundamentalDataSource.getFundamentalBundle(symbol, name)
        
        result.fold(
            onSuccess = { data ->
                fundamentalDao.insertFundamentalData(data)
                emit(Result.success(data))
            },
            onFailure = { error ->
                if (cachedData != null) {
                    emit(Result.success(cachedData.copy(isCacheValid = false)))
                } else {
                    emit(Result.failure<FundamentalData>(error))
                }
            }
        )
    }.flowOn(Dispatchers.IO)
    
    /**
     * 获取完整的基本面分析结果
     */
    override suspend fun getFundamentalAnalysis(
        symbol: String,
        name: String,
        currentPrice: Double?
    ): Result<FundamentalAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            // 获取基本面数据
            val fundamentalResult = getFundamentalData(symbol, name)
            
            fundamentalResult.fold(
                onSuccess = { data ->
                    val result = calculateFundamentalAnalysis(data, currentPrice)
                    Result.success(result)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "基本面分析失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 批量获取基本面数据
     */
    override suspend fun getBatchFundamentalData(
        symbols: List<Pair<String, String>>
    ): Result<List<FundamentalData>> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<FundamentalData>()
            
            symbols.forEach { (symbol, name) ->
                getFundamentalData(symbol, name).getOrNull()?.let {
                    results.add(it)
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 强制刷新基本面数据
     */
    override suspend fun refreshFundamentalData(symbol: String, name: String): Result<FundamentalData> {
        return getFundamentalData(symbol, name, forceRefresh = true)
    }
    
    /**
     * 获取财务指标
     */
    override suspend fun getFinancialIndicators(symbol: String): FinancialIndicators = withContext(Dispatchers.IO) {
        // 先从缓存获取
        val cachedData = fundamentalDao.getFundamentalDataBySymbol(symbol)
        cachedData?.financialIndicatorsJson?.let { json ->
            return@withContext fundamentalDataSource.deserializeFinancialIndicators(json)
        }
        
        // 从网络获取
        val result = fundamentalDataSource.fetchFinancialIndicators(symbol)
        result.getOrDefault(FinancialIndicators())
    }
    
    /**
     * 获取成长性指标
     */
    override suspend fun getGrowthMetrics(symbol: String): GrowthMetrics = withContext(Dispatchers.IO) {
        val cachedData = fundamentalDao.getFundamentalDataBySymbol(symbol)
        cachedData?.growthMetricsJson?.let { json ->
            return@withContext fundamentalDataSource.deserializeGrowthMetrics(json)
        }
        
        val result = fundamentalDataSource.fetchGrowthMetrics(symbol)
        result.getOrDefault(GrowthMetrics())
    }
    
    /**
     * 获取分红信息
     */
    override suspend fun getDividendInfo(symbol: String): DividendInfo = withContext(Dispatchers.IO) {
        val cachedData = fundamentalDao.getFundamentalDataBySymbol(symbol)
        cachedData?.dividendInfoJson?.let { json ->
            return@withContext fundamentalDataSource.deserializeDividendInfo(json)
        }
        
        val result = fundamentalDataSource.fetchDividendInfo(symbol)
        result.getOrDefault(DividendInfo(symbol = symbol))
    }
    
    /**
     * 获取机构持仓
     */
    override suspend fun getInstitutionalHolding(symbol: String): InstitutionalHolding = withContext(Dispatchers.IO) {
        val cachedData = fundamentalDao.getFundamentalDataBySymbol(symbol)
        cachedData?.institutionalHoldingJson?.let { json ->
            return@withContext fundamentalDataSource.deserializeInstitutionalHolding(json)
        }
        
        val result = fundamentalDataSource.fetchInstitutionalHolding(symbol)
        result.getOrDefault(InstitutionalHolding(symbol = symbol))
    }
    
    /**
     * 获取估值指标
     */
    override suspend fun getValuationMetrics(symbol: String): ValuationMetrics = withContext(Dispatchers.IO) {
        val result = fundamentalDataSource.fetchValuationMetrics(symbol)
        result.getOrDefault(ValuationMetrics(symbol = symbol))
    }
    
    /**
     * 清空所有缓存
     */
    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            fundamentalDao.deleteAll()
            Log.d(TAG, "基本面数据缓存已清空")
        }
    }
    
    /**
     * 删除指定股票的缓存
     */
    override suspend fun clearCacheForSymbol(symbol: String) {
        withContext(Dispatchers.IO) {
            fundamentalDao.deleteBySymbol(symbol)
            Log.d(TAG, "已删除 $symbol 的基本面数据缓存")
        }
    }
    
    /**
     * 检查缓存是否有效
     */
    override suspend fun isCacheValid(symbol: String): Boolean = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -CACHE_DAYS)
        fundamentalDao.isCacheValid(symbol, calendar.time)
    }
    
    /**
     * 获取所有缓存的基本面数据
     */
    override suspend fun getCachedFundamentalData(): List<FundamentalData> = withContext(Dispatchers.IO) {
        fundamentalDao.getAllFundamentalDataSync()
    }
    
    // ============ 私有辅助方法 ============
    
    /**
     * 计算基本面分析结果
     */
    private fun calculateFundamentalAnalysis(
        data: FundamentalData,
        currentPrice: Double?
    ): FundamentalAnalysisResult {
        // 解析JSON数据
        val financialIndicators = data.financialIndicatorsJson?.let {
            fundamentalDataSource.deserializeFinancialIndicators(it)
        } ?: FinancialIndicators()
        
        val growthMetrics = data.growthMetricsJson?.let {
            fundamentalDataSource.deserializeGrowthMetrics(it)
        } ?: GrowthMetrics()
        
        val dividendInfo = data.dividendInfoJson?.let {
            fundamentalDataSource.deserializeDividendInfo(it)
        } ?: DividendInfo(symbol = data.symbol)
        
        val institutionalHolding = data.institutionalHoldingJson?.let {
            fundamentalDataSource.deserializeInstitutionalHolding(it)
        } ?: InstitutionalHolding(symbol = data.symbol)
        
        // 计算各项评分
        val valuationScore = calculateValuationScore(data.peRatio, data.pbRatio)
        val profitabilityScore = calculateProfitabilityScore(financialIndicators)
        val growthScore = growthMetrics.getGrowthScore()
        val financialHealthScore = calculateFinancialHealthScore(financialIndicators)
        val dividendScore = calculateDividendScore(dividendInfo, currentPrice)
        val institutionScore = calculateInstitutionScore(institutionalHolding)
        
        // 计算综合评分
        val overallScore = (
            valuationScore * 0.15 +
            profitabilityScore * 0.25 +
            growthScore * 0.25 +
            financialHealthScore * 0.15 +
            dividendScore * 0.10 +
            institutionScore * 0.10
        ).toInt()
        
        // 分析结论
        val valuationConclusion = when {
            valuationScore >= 80 -> "估值处于历史低位，具备安全边际"
            valuationScore >= 60 -> "估值合理，接近历史平均水平"
            valuationScore >= 40 -> "估值偏高，需要警惕"
            else -> "估值过高，注意风险"
        }
        
        val profitabilityConclusion = when {
            profitabilityScore >= 80 -> "盈利能力优秀，ROE和毛利率表现突出"
            profitabilityScore >= 60 -> "盈利能力良好，各项指标健康"
            profitabilityScore >= 40 -> "盈利能力一般，需关注改善空间"
            else -> "盈利能力较弱，存在经营风险"
        }
        
        val growthConclusion = growthMetrics.getGrowthRating()
        
        val financialHealthConclusion = financialIndicators.getFinancialHealthRating()
        
        val dividendConclusion = dividendInfo.getDividendRating()
        
        val institutionConclusion = when {
            institutionScore >= 80 -> "机构高度认可，持续增持"
            institutionScore >= 60 -> "机构较为关注，持仓稳定"
            institutionScore >= 40 -> "机构关注度一般"
            else -> "机构关注度较低"
        }
        
        // 风险因素
        val riskFactors = mutableListOf<String>()
        if ((data.peRatio ?: 50.0) > 50) riskFactors.add("估值偏高")
        if ((financialIndicators.debtToEquity ?: 50.0) > 70) riskFactors.add("负债率较高")
        if ((growthMetrics.revenueGrowthYoY ?: 0.0) < 0) riskFactors.add("营收下滑")
        if ((growthMetrics.netProfitGrowthYoY ?: 0.0) < 0) riskFactors.add("利润下滑")
        if ((financialIndicators.roe ?: 10.0) < 5) riskFactors.add("盈利能力较弱")
        
        // 投资建议
        val investmentAdvice = when {
            overallScore >= 80 && valuationScore >= 60 -> "强烈建议买入：基本面优秀，估值合理"
            overallScore >= 70 && valuationScore >= 50 -> "建议买入：基本面良好，有成长空间"
            overallScore >= 60 -> "可适当配置：基本面尚可，关注后续发展"
            overallScore >= 40 -> "谨慎观望：存在一定风险，需进一步观察"
            else -> "建议回避：基本面较弱，风险较高"
        }
        
        return FundamentalAnalysisResult(
            symbol = data.symbol,
            name = data.name,
            updateTime = data.updateTime,
            overallScore = overallScore,
            valuationScore = valuationScore,
            profitabilityScore = profitabilityScore,
            growthScore = growthScore,
            financialHealthScore = financialHealthScore,
            dividendScore = dividendScore,
            institutionScore = institutionScore,
            valuationConclusion = valuationConclusion,
            profitabilityConclusion = profitabilityConclusion,
            growthConclusion = growthConclusion,
            financialHealthConclusion = financialHealthConclusion,
            dividendConclusion = dividendConclusion,
            institutionConclusion = institutionConclusion,
            riskFactors = riskFactors,
            investmentAdvice = investmentAdvice
        )
    }
    
    /**
     * 计算估值评分
     */
    private fun calculateValuationScore(pe: Double?, pb: Double?): Int {
        var score = 50
        
        // PE评分
        score += when {
            (pe ?: 20.0) < 10 -> 30
            (pe ?: 20.0) < 15 -> 20
            (pe ?: 20.0) < 25 -> 10
            (pe ?: 20.0) < 40 -> 0
            else -> -10
        }
        
        // PB评分
        score += when {
            (pb ?: 2.0) < 1 -> 20
            (pb ?: 2.0) < 2 -> 10
            (pb ?: 2.0) < 4 -> 0
            else -> -10
        }
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * 计算盈利能力评分
     */
    private fun calculateProfitabilityScore(indicators: FinancialIndicators): Int {
        var score = 50
        
        // ROE评分
        score += when {
            (indicators.roe ?: 10.0) > 20 -> 20
            (indicators.roe ?: 10.0) > 15 -> 15
            (indicators.roe ?: 10.0) > 10 -> 10
            (indicators.roe ?: 10.0) > 5 -> 0
            else -> -10
        }
        
        // 毛利率评分
        score += when {
            (indicators.grossMargin ?: 30.0) > 40 -> 15
            (indicators.grossMargin ?: 30.0) > 30 -> 10
            (indicators.grossMargin ?: 30.0) > 20 -> 5
            else -> 0
        }
        
        // 净利率评分
        score += when {
            (indicators.netMargin ?: 10.0) > 20 -> 15
            (indicators.netMargin ?: 10.0) > 10 -> 10
            (indicators.netMargin ?: 10.0) > 5 -> 5
            else -> 0
        }
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * 计算财务健康度评分
     */
    private fun calculateFinancialHealthScore(indicators: FinancialIndicators): Int {
        var score = 50
        
        // 资产负债率评分
        score += when {
            (indicators.debtToEquity ?: 50.0) < 30 -> 25
            (indicators.debtToEquity ?: 50.0) < 50 -> 15
            (indicators.debtToEquity ?: 50.0) < 70 -> 5
            (indicators.debtToEquity ?: 50.0) < 85 -> -5
            else -> -15
        }
        
        // 流动比率评分
        score += when {
            (indicators.currentRatio ?: 1.5) > 2 -> 15
            (indicators.currentRatio ?: 1.5) > 1.5 -> 10
            (indicators.currentRatio ?: 1.5) > 1 -> 5
            else -> 0
        }
        
        // 现金流评分
        if ((indicators.operatingCashFlow ?: 0.0) > 0) score += 10
        if ((indicators.freeCashFlow ?: 0.0) > 0) score += 10
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * 计算分红评分
     */
    private fun calculateDividendScore(dividendInfo: DividendInfo, currentPrice: Double?): Int {
        var score = 50
        
        // 股息率
        val dividendYield = dividendInfo.dividendYield 
            ?: (dividendInfo.ttmDividendPerShare?.let { it * 100 / (currentPrice ?: 10.0) })
        
        score += when {
            (dividendYield ?: 0.0) > 5 -> 30
            (dividendYield ?: 0.0) > 3 -> 20
            (dividendYield ?: 0.0) > 2 -> 10
            (dividendYield ?: 0.0) > 0 -> 5
            else -> 0
        }
        
        // 分红持续性
        score += when {
            dividendInfo.consecutiveYears >= 10 -> 20
            dividendInfo.consecutiveYears >= 5 -> 15
            dividendInfo.consecutiveYears >= 3 -> 10
            dividendInfo.consecutiveYears >= 1 -> 5
            else -> 0
        }
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * 计算机构关注度评分
     */
    private fun calculateInstitutionScore(holding: InstitutionalHolding): Int {
        var score = 50
        
        // 机构数量评分
        score += when {
            (holding.institutionCount ?: 0) > 200 -> 25
            (holding.institutionCount ?: 0) > 100 -> 20
            (holding.institutionCount ?: 0) > 50 -> 15
            (holding.institutionCount ?: 0) > 20 -> 10
            (holding.institutionCount ?: 0) > 5 -> 5
            else -> 0
        }
        
        // 持仓比例评分
        score += when {
            (holding.holdingRatio ?: 0.0) > 50 -> 25
            (holding.holdingRatio ?: 0.0) > 30 -> 20
            (holding.holdingRatio ?: 0.0) > 15 -> 15
            (holding.holdingRatio ?: 0.0) > 5 -> 10
            else -> 0
        }
        
        return score.coerceIn(0, 100)
    }
}
