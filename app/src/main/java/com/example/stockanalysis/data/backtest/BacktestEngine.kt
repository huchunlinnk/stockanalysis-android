package com.example.stockanalysis.data.backtest

import com.example.stockanalysis.data.analysis.ComparisonResult
import com.example.stockanalysis.data.analysis.HistoryComparisonService
import com.example.stockanalysis.data.analysis.Outcome
import com.example.stockanalysis.data.analysis.SignalAccuracy
import com.example.stockanalysis.data.local.AnalysisResultDao
import com.example.stockanalysis.data.model.Decision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 回测引擎
 * 在Android端本地运行，不依赖后端服务器
 */
@Singleton
class BacktestEngine @Inject constructor(
    private val analysisResultDao: AnalysisResultDao,
    private val backtestDao: BacktestDao,
    private val historyComparisonService: HistoryComparisonService? = null
) {
    
    companion object {
        const val DEFAULT_HOLDING_DAYS = 5  // 默认持有天数
        const val STOP_LOSS_PERCENT = -0.07  // 默认止损 -7%
        const val TAKE_PROFIT_PERCENT = 0.15  // 默认止盈 15%
    }
    
    /**
     * 执行回测
     * 
     * @param symbol 股票代码
     * @param strategyId 策略ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    suspend fun runBacktest(
        symbol: String,
        strategyId: String? = null,
        startDate: Date? = null,
        endDate: Date? = null
    ): Result<BacktestResult> = withContext(Dispatchers.IO) {
        try {
            // 1. 获取历史分析结果
            val history = analysisResultDao.getAnalysisHistory(symbol)
            
            if (history.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("没有该股票的历史分析数据")
                )
            }
            
            // 2. 过滤时间范围
            val filteredHistory = history.filter { result ->
                val afterStart = startDate?.let { result.analysisTime >= it } ?: true
                val beforeEnd = endDate?.let { result.analysisTime <= it } ?: true
                afterStart && beforeEnd
            }
            
            if (filteredHistory.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("指定时间范围内没有分析数据")
                )
            }
            
            // 3. 评估每个信号
            val evaluations = mutableListOf<SignalEvaluation>()
            
            filteredHistory.forEach { analysis ->
                // 获取分析后的实际走势
                val evaluation = evaluateSignal(
                    analysisId = analysis.id,
                    symbol = symbol,
                    signalDate = analysis.analysisTime,
                    decision = analysis.decision.name,
                    predictedDirection = decisionToDirection(analysis.decision),
                    priceAtSignal = getPriceAtDate(symbol, analysis.analysisTime)
                )
                
                if (evaluation != null) {
                    evaluations.add(evaluation)
                }
            }
            
            // 4. 计算统计数据
            val stats = calculateStats(evaluations)
            
            // 5. 构建回测结果
            val result = BacktestResult(
                stockSymbol = symbol,
                stockName = filteredHistory.firstOrNull()?.stockName ?: symbol,
                strategyId = strategyId ?: "comprehensive",
                strategyName = strategyId ?: "综合分析",
                startDate = filteredHistory.last().analysisTime,
                endDate = filteredHistory.first().analysisTime,
                totalSignals = evaluations.size,
                correctSignals = evaluations.count { it.isCorrect },
                accuracy = stats.accuracy,
                totalReturn = stats.totalReturn,
                maxDrawdown = stats.maxDrawdown,
                winRate = stats.winRate
            )
            
            // 6. 保存结果
            backtestDao.insertResult(result)
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 评估单个信号
     */
    private fun evaluateSignal(
        analysisId: String,
        symbol: String,
        signalDate: Date,
        decision: String,
        predictedDirection: String,
        priceAtSignal: Double?
    ): SignalEvaluation? {
        if (priceAtSignal == null || priceAtSignal <= 0) return null
        
        // 获取N天后的价格
        val priceAfterNDays = mutableMapOf<Int, Double>()
        val returnAfterNDays = mutableMapOf<Int, Double>()
        
        // 1天、3天、5天、10天后的价格
        val daysToCheck = listOf(1, 3, 5, 10)
        
        daysToCheck.forEach { days ->
            val futurePrice = getPriceAtNDaysLater(symbol, signalDate, days)
            if (futurePrice != null) {
                priceAfterNDays[days] = futurePrice
                returnAfterNDays[days] = ((futurePrice - priceAtSignal) / priceAtSignal)
            }
        }
        
        // 判断实际方向 (基于5天后的价格变化)
        val actualDirection = returnAfterNDays[DEFAULT_HOLDING_DAYS]?.let { ret ->
            when {
                ret > 0.02 -> "UP"      // 上涨超过2%
                ret < -0.02 -> "DOWN"   // 下跌超过2%
                else -> "FLAT"              // 震荡
            }
        } ?: "UNKNOWN"
        
        // 判断是否预测正确
        val isCorrect = when (predictedDirection) {
            "UP" -> actualDirection == "UP"
            "DOWN" -> actualDirection == "DOWN"
            else -> actualDirection == "FLAT"
        }
        
        return SignalEvaluation(
            analysisId = analysisId,
            signalDate = signalDate,
            decision = decision,
            predictedDirection = predictedDirection,
            actualDirection = actualDirection,
            isCorrect = isCorrect,
            priceAtSignal = priceAtSignal,
            priceAfterNDays = priceAfterNDays,
            returnAfterNDays = returnAfterNDays
        )
    }
    
    /**
     * 计算统计数据
     */
    private fun calculateStats(evaluations: List<SignalEvaluation>): BacktestStats {
        if (evaluations.isEmpty()) {
            return BacktestStats(0.0, 0.0, 0.0, 0.0)
        }
        
        // 准确率
        val correctCount = evaluations.count { it.isCorrect }
        val accuracy = (correctCount.toDouble() / evaluations.size) * 100
        
        // 胜率 (基于5天后收益)
        val winningTrades = evaluations.count { eval ->
            eval.returnAfterNDays[DEFAULT_HOLDING_DAYS]?.let { r -> r > 0 } ?: false 
        }
        val winRate = (winningTrades.toDouble() / evaluations.size) * 100
        
        // 累计收益 (假设每次投入等额资金)
        var totalReturn = 0.0
        var maxDrawdown = 0.0
        var peak = 0.0
        var cumulative = 0.0
        
        evaluations.forEach { eval ->
            val ret = eval.returnAfterNDays[DEFAULT_HOLDING_DAYS] ?: 0.0
            cumulative += ret
            
            if (cumulative > peak) {
                peak = cumulative
            }
            
            val drawdown = peak - cumulative
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown
            }
        }
        
        totalReturn = (cumulative / evaluations.size) * 100
        
        return BacktestStats(
            accuracy = accuracy,
            totalReturn = totalReturn,
            maxDrawdown = maxDrawdown * 100,
            winRate = winRate
        )
    }
    
    /**
     * 将决策转换为方向
     */
    private fun decisionToDirection(decision: Decision): String {
        return when (decision) {
            Decision.STRONG_BUY, Decision.BUY -> "UP"
            Decision.STRONG_SELL, Decision.SELL -> "DOWN"
            Decision.HOLD -> "FLAT"
        }
    }
    
    /**
     * 获取指定日期的价格
     */
    private fun getPriceAtDate(symbol: String, date: Date): Double? {
        // 从K线数据获取
        // 简化实现，实际应该从KLineDataDao获取
        return null
    }
    
    /**
     * 获取N天后的价格
     */
    private fun getPriceAtNDaysLater(
        symbol: String, 
        fromDate: Date, 
        days: Int
    ): Double? {
        // 从K线数据获取
        // 简化实现
        return null
    }
    
    /**
     * 对比分析 (比较不同策略的表现)
     */
    suspend fun compareStrategies(
        symbol: String,
        strategyIds: List<String>
    ): Result<Map<String, BacktestResult>> = withContext(Dispatchers.IO) {
        try {
            val results = mutableMapOf<String, BacktestResult>()
            
            strategyIds.forEach { strategyId ->
                val result = runBacktest(symbol, strategyId)
                result.onSuccess { 
                    results[strategyId] = it 
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取历史准确率趋势
     */
    suspend fun getAccuracyTrend(
        symbol: String,
        windowSize: Int = 10
    ): List<AccuracyPoint> = withContext(Dispatchers.IO) {
        val history = analysisResultDao.getAnalysisHistory(symbol)
        
        if (history.size < windowSize) {
            return@withContext emptyList()
        }
        
        val points = mutableListOf<AccuracyPoint>()
        
        for (i in 0..history.size - windowSize) {
            val window = history.subList(i, i + windowSize)
            
            // 计算窗口内的准确率
            var correct = 0
            window.forEach { analysis ->
                val direction = decisionToDirection(analysis.decision)
                // 简化计算
                if (direction == "UP") correct++
            }
            
            val accuracy = (correct.toDouble() / windowSize) * 100
            
            points.add(AccuracyPoint(
                date = window.first().analysisTime,
                accuracy = accuracy,
                windowSize = windowSize
            ))
        }
        
        points
    }
    
    /**
     * 清除旧回测数据
     */
    suspend fun cleanupOldData(daysToKeep: Int = 30) = withContext(Dispatchers.IO) {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysToKeep)
        backtestDao.deleteOldResults(calendar.time)
    }
    
    // ==================== 历史信号对比集成 ====================
    
    /**
     * 使用 HistoryComparisonService 验证信号
     * 集成更准确的价格对比和信号验证逻辑
     */
    suspend fun validateSignalsWithComparison(
        symbol: String,
        days: Int = DEFAULT_HOLDING_DAYS
    ): List<ComparisonResult>? = withContext(Dispatchers.IO) {
        historyComparisonService?.let { service ->
            val calendar = java.util.Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -30)
            val startDate = calendar.time
            
            val results = analysisResultDao.getResultsByTimeRange(symbol, startDate, endDate)
            
            results.mapNotNull { result ->
                val record = service.recordSignal(result)
                service.validateSignal(record, days)
            }
        }
    }
    
    /**
     * 获取信号准确率统计
     */
    suspend fun getSignalAccuracy(symbol: String): SignalAccuracy? = withContext(Dispatchers.IO) {
        historyComparisonService?.validateSignals(symbol)
    }
    
    /**
     * 对比回测结果与历史信号验证
     */
    suspend fun compareBacktestWithSignalValidation(
        symbol: String
    ): BacktestValidationComparison? = withContext(Dispatchers.IO) {
        val backtestResult = backtestDao.getResultsBySymbol(symbol).firstOrNull()
        val signalAccuracy = historyComparisonService?.validateSignals(symbol)
        
        if (backtestResult != null && signalAccuracy != null) {
            BacktestValidationComparison(
                stockSymbol = symbol,
                backtestAccuracy = backtestResult.accuracy,
                backtestWinRate = backtestResult.winRate,
                signalWinRate = signalAccuracy.winRate,
                signalDirectionAccuracy = signalAccuracy.directionAccuracy,
                backtestTotalReturn = backtestResult.totalReturn,
                signalAvgReturn = signalAccuracy.avgReturn,
                consistency = calculateConsistency(
                    backtestResult.winRate,
                    signalAccuracy.winRate
                )
            )
        } else null
    }
    
    /**
     * 流式获取对比结果
     */
    fun comparePredictedVsActualFlow(
        symbol: String,
        days: Int = 30
    ): Flow<List<ComparisonResult>> = flow {
        historyComparisonService?.let { service ->
            service.comparePredictedVsActual(symbol, days).collect { comparisons ->
                emit(comparisons)
            }
        } ?: emit(emptyList())
    }.flowOn(Dispatchers.IO)
    
    /**
     * 生成增强版回测报告
     * 结合回测引擎和历史信号对比
     */
    suspend fun generateEnhancedReport(symbol: String): EnhancedBacktestReport? = withContext(Dispatchers.IO) {
        val backtestResults = backtestDao.getResultsBySymbol(symbol)
        val signalAccuracy = historyComparisonService?.validateSignals(symbol)
        val history = analysisResultDao.getAnalysisHistory(symbol)
        
        if (backtestResults.isEmpty() || signalAccuracy == null) {
            return@withContext null
        }
        
        val latestBacktest = backtestResults.first()
        
        EnhancedBacktestReport(
            stockSymbol = symbol,
            backtestResult = latestBacktest,
            signalAccuracy = signalAccuracy,
            totalAnalysisCount = history.size,
            buySignals = history.count { it.decision == Decision.BUY || it.decision == Decision.STRONG_BUY },
            sellSignals = history.count { it.decision == Decision.SELL || it.decision == Decision.STRONG_SELL },
            holdSignals = history.count { it.decision == Decision.HOLD },
            recommendation = generateRecommendation(signalAccuracy, latestBacktest),
            generatedAt = Date()
        )
    }
    
    /**
     * 计算一致性
     */
    private fun calculateConsistency(backtestRate: Double, signalRate: Double): Double {
        val diff = kotlin.math.abs(backtestRate - signalRate)
        return when {
            diff < 5 -> 100.0
            diff < 10 -> 80.0
            diff < 20 -> 60.0
            else -> 40.0
        }
    }
    
    /**
     * 生成建议
     */
    private fun generateRecommendation(
        accuracy: SignalAccuracy,
        backtest: BacktestResult
    ): String {
        return when {
            accuracy.winRate >= 70 && backtest.winRate >= 60 ->
                "信号质量优秀，建议参考进行交易"
            accuracy.winRate >= 50 && backtest.winRate >= 50 ->
                "信号质量一般，建议结合其他指标"
            else ->
                "信号质量较差，建议谨慎参考"
        }
    }
}

/**
 * 回测与信号验证对比
 */
data class BacktestValidationComparison(
    val stockSymbol: String,
    val backtestAccuracy: Double,
    val backtestWinRate: Double,
    val signalWinRate: Double,
    val signalDirectionAccuracy: Double,
    val backtestTotalReturn: Double,
    val signalAvgReturn: Double,
    val consistency: Double  // 一致性评分 0-100
)

/**
 * 增强版回测报告
 */
data class EnhancedBacktestReport(
    val stockSymbol: String,
    val backtestResult: BacktestResult,
    val signalAccuracy: SignalAccuracy,
    val totalAnalysisCount: Int,
    val buySignals: Int,
    val sellSignals: Int,
    val holdSignals: Int,
    val recommendation: String,
    val generatedAt: Date
)

/**
 * 回测统计数据
 */
private data class BacktestStats(
    val accuracy: Double,
    val totalReturn: Double,
    val maxDrawdown: Double,
    val winRate: Double
)

/**
 * 准确率数据点
 */
data class AccuracyPoint(
    val date: Date,
    val accuracy: Double,
    val windowSize: Int
)
