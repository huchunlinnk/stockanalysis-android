package com.example.stockanalysis.data.analysis

import com.example.stockanalysis.data.local.AnalysisResultDao
import com.example.stockanalysis.data.local.KLineDataDao
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.Decision
import com.example.stockanalysis.data.model.KLineData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 历史信号对比服务
 * 记录、追踪和验证分析信号的准确性
 */
@Singleton
class HistoryComparisonService @Inject constructor(
    private val analysisResultDao: AnalysisResultDao,
    private val kLineDataDao: KLineDataDao
) {
    companion object {
        const val DEFAULT_EVAL_WINDOW_DAYS = 5      // 默认评估窗口天数
        const val NEUTRAL_BAND_PCT = 2.0            // 中性区间百分比
        const val STOP_LOSS_PERCENT = -7.0          // 止损百分比
        const val TAKE_PROFIT_PERCENT = 15.0        // 止盈百分比
        
        // 决策关键词映射（用于推断方向）
        private val BULLISH_KEYWORDS = setOf(
            "买入", "加仓", "强烈买入", "增持", "建仓",
            "strong buy", "buy", "add", "accumulate"
        )
        private val BEARISH_KEYWORDS = setOf(
            "卖出", "减仓", "强烈卖出", "清仓", "减持",
            "strong sell", "sell", "reduce", "exit"
        )
        private val HOLD_KEYWORDS = setOf(
            "持有", "hold", "keeping"
        )
        private val WAIT_KEYWORDS = setOf(
            "观望", "等待", "wait", "neutral"
        )
    }

    /**
     * 记录信号
     * @param result 分析结果
     * @return 信号记录
     */
    suspend fun recordSignal(result: AnalysisResult): SignalRecord = withContext(Dispatchers.IO) {
        val direction = inferDirectionFromDecision(result.decision)
        val targetPrice = result.actionPlan?.targetPrice
        val stopLoss = result.actionPlan?.stopLossPrice
        
        SignalRecord(
            id = result.id,
            stockSymbol = result.stockSymbol,
            stockName = result.stockName,
            signalDate = result.analysisTime,
            signalType = result.decision.name,
            direction = direction,
            confidence = result.confidence,
            score = result.score,
            predictedPrice = targetPrice,
            stopLossPrice = stopLoss,
            summary = result.summary,
            isValidated = false
        )
    }

    /**
     * 验证单个信号的准确性
     * @param record 信号记录
     * @param evalDays 评估天数
     * @return 对比结果
     */
    suspend fun validateSignal(
        record: SignalRecord,
        evalDays: Int = DEFAULT_EVAL_WINDOW_DAYS
    ): ComparisonResult? = withContext(Dispatchers.IO) {
        // 获取信号日期的价格
        val priceAtSignal = getPriceAtDate(record.stockSymbol, record.signalDate)
            ?: return@withContext null
        
        // 获取未来N天的K线数据
        val forwardBars = getForwardBars(record.stockSymbol, record.signalDate, evalDays)
        if (forwardBars.size < evalDays) {
            return@withContext null // 数据不足
        }
        
        // 计算价格变化
        val endPrice = forwardBars.last().close
        val priceChange = ((endPrice - priceAtSignal) / priceAtSignal) * 100
        
        // 判断实际方向
        val actualDirection = when {
            priceChange >= NEUTRAL_BAND_PCT -> Direction.UP
            priceChange <= -NEUTRAL_BAND_PCT -> Direction.DOWN
            else -> Direction.FLAT
        }
        
        // 判断是否预测正确
        val isCorrect = when (record.direction) {
            Direction.UP -> actualDirection == Direction.UP
            Direction.DOWN -> actualDirection == Direction.DOWN
            Direction.FLAT -> actualDirection == Direction.FLAT
            Direction.NOT_DOWN -> actualDirection != Direction.DOWN
        }
        
        // 计算结果
        val outcome = when {
            isCorrect && abs(priceChange) >= NEUTRAL_BAND_PCT -> Outcome.WIN
            !isCorrect && abs(priceChange) >= NEUTRAL_BAND_PCT -> Outcome.LOSS
            else -> Outcome.NEUTRAL
        }
        
        // 止损止盈分析
        val stopLoss = record.stopLossPrice
        val takeProfit = record.predictedPrice
        val (hitStopLoss, hitTakeProfit, firstHit, exitPrice, exitReason) = evaluateTargets(
            record.direction, stopLoss, takeProfit, forwardBars, endPrice
        )
        
        ComparisonResult(
            signalId = record.id,
            stockSymbol = record.stockSymbol,
            signalDate = record.signalDate,
            signalType = record.signalType,
            predictedDirection = record.direction,
            actualDirection = actualDirection,
            isCorrect = isCorrect,
            outcome = outcome,
            priceAtSignal = priceAtSignal,
            actualPrice = endPrice,
            predictedPrice = record.predictedPrice,
            priceChangePercent = priceChange,
            evalWindowDays = evalDays,
            hitStopLoss = hitStopLoss,
            hitTakeProfit = hitTakeProfit,
            firstHit = firstHit,
            exitPrice = exitPrice,
            exitReason = exitReason,
            validationDate = Date()
        )
    }

    /**
     * 批量验证信号
     * @param symbol 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 信号准确率统计
     */
    suspend fun validateSignals(
        symbol: String,
        startDate: Date? = null,
        endDate: Date? = null
    ): SignalAccuracy = withContext(Dispatchers.IO) {
        // 获取历史分析记录
        val results = if (startDate != null && endDate != null) {
            analysisResultDao.getResultsByTimeRange(startDate, endDate)
                .filter { it.stockSymbol == symbol }
        } else {
            analysisResultDao.getAnalysisHistory(symbol)
        }
        
        if (results.isEmpty()) {
            return@withContext SignalAccuracy.empty(symbol)
        }
        
        // 验证每个信号
        val comparisons = results.mapNotNull { result ->
            val record = recordSignal(result)
            validateSignal(record)
        }
        
        calculateAccuracy(symbol, comparisons)
    }

    /**
     * 计算信号准确率统计
     */
    private fun calculateAccuracy(symbol: String, comparisons: List<ComparisonResult>): SignalAccuracy {
        if (comparisons.isEmpty()) {
            return SignalAccuracy.empty(symbol)
        }
        
        val total = comparisons.size
        val completed = comparisons.filter { it.outcome != Outcome.PENDING }
        
        // 计算各类型信号数量
        val buySignals = completed.filter { 
            it.signalType in listOf(Decision.BUY.name, Decision.STRONG_BUY.name) 
        }
        val sellSignals = completed.filter { 
            it.signalType in listOf(Decision.SELL.name, Decision.STRONG_SELL.name) 
        }
        val holdSignals = completed.filter { it.signalType == Decision.HOLD.name }
        
        // 计算胜率
        val winCount = completed.count { it.outcome == Outcome.WIN }
        val lossCount = completed.count { it.outcome == Outcome.LOSS }
        val neutralCount = completed.count { it.outcome == Outcome.NEUTRAL }
        
        val winRate = if ((winCount + lossCount) > 0) {
            (winCount.toDouble() / (winCount + lossCount)) * 100
        } else 0.0
        
        // 计算方向准确率
        val directionCorrectCount = completed.count { it.isCorrect }
        val directionAccuracy = if (completed.isNotEmpty()) {
            (directionCorrectCount.toDouble() / completed.size) * 100
        } else 0.0
        
        // 计算平均收益率
        val avgReturn = completed.map { it.priceChangePercent }.average()
        
        // 计算止损止盈触发率
        val withTargets = completed.filter { it.hitStopLoss != null || it.hitTakeProfit != null }
        val stopLossRate = if (withTargets.isNotEmpty()) {
            (withTargets.count { it.hitStopLoss == true }.toDouble() / withTargets.size) * 100
        } else null
        val takeProfitRate = if (withTargets.isNotEmpty()) {
            (withTargets.count { it.hitTakeProfit == true }.toDouble() / withTargets.size) * 100
        } else null
        
        return SignalAccuracy(
            stockSymbol = symbol,
            totalSignals = total,
            completedSignals = completed.size,
            winCount = winCount,
            lossCount = lossCount,
            neutralCount = neutralCount,
            winRate = winRate,
            directionAccuracy = directionAccuracy,
            avgReturn = avgReturn,
            buySignalAccuracy = calculateTypeAccuracy(buySignals),
            sellSignalAccuracy = calculateTypeAccuracy(sellSignals),
            holdSignalAccuracy = calculateTypeAccuracy(holdSignals),
            stopLossTriggerRate = stopLossRate,
            takeProfitTriggerRate = takeProfitRate,
            lastUpdated = Date()
        )
    }
    
    private fun calculateTypeAccuracy(signals: List<ComparisonResult>): Double? {
        if (signals.isEmpty()) return null
        val correct = signals.count { it.isCorrect }
        return (correct.toDouble() / signals.size) * 100
    }

    /**
     * 生成信号准确性报告
     * @param symbol 股票代码
     * @param days 天数范围
     * @return 报告
     */
    suspend fun generateReport(
        symbol: String,
        days: Int = 30
    ): SignalReport = withContext(Dispatchers.IO) {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.time
        
        val accuracy = validateSignals(symbol, startDate, endDate)
        val recentComparisons = getRecentComparisons(symbol, days)
        
        // 按信号类型分组统计
        val bySignalType = recentComparisons.groupBy { it.signalType }
            .mapValues { (_, list) ->
                SignalTypeStats(
                    total = list.size,
                    wins = list.count { it.outcome == Outcome.WIN },
                    losses = list.count { it.outcome == Outcome.LOSS },
                    accuracy = if (list.isNotEmpty()) {
                        (list.count { it.isCorrect }.toDouble() / list.size) * 100
                    } else 0.0
                )
            }
        
        // 趋势分析（最近10个信号的准确率趋势）
        val trend = calculateAccuracyTrend(recentComparisons, windowSize = 10)
        
        SignalReport(
            stockSymbol = symbol,
            periodDays = days,
            accuracy = accuracy,
            bySignalType = bySignalType,
            trend = trend,
            recommendations = generateRecommendations(accuracy, bySignalType),
            generatedAt = Date()
        )
    }

    /**
     * 获取最近对比结果
     */
    private suspend fun getRecentComparisons(symbol: String, days: Int): List<ComparisonResult> {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.time
        
        val results = analysisResultDao.getResultsByTimeRange(startDate, endDate)
            .filter { it.stockSymbol == symbol }
        
        return results.mapNotNull { result ->
            val record = recordSignal(result)
            validateSignal(record)
        }
    }

    /**
     * 计算准确率趋势
     */
    private fun calculateAccuracyTrend(
        comparisons: List<ComparisonResult>,
        windowSize: Int
    ): List<AccuracyTrendPoint> {
        if (comparisons.size < windowSize) return emptyList()
        
        val sorted = comparisons.sortedBy { it.signalDate }
        val trend = mutableListOf<AccuracyTrendPoint>()
        
        for (i in 0..sorted.size - windowSize) {
            val window = sorted.subList(i, i + windowSize)
            val correct = window.count { it.isCorrect }
            val accuracy = (correct.toDouble() / windowSize) * 100
            
            trend.add(AccuracyTrendPoint(
                date = window.last().signalDate,
                accuracy = accuracy,
                windowSize = windowSize
            ))
        }
        
        return trend
    }

    /**
     * 生成建议
     */
    private fun generateRecommendations(
        accuracy: SignalAccuracy,
        bySignalType: Map<String, SignalTypeStats>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 基于整体准确率
        when {
            accuracy.winRate >= 70 -> recommendations.add("该股票信号准确率较高，可参考交易")
            accuracy.winRate >= 50 -> recommendations.add("该股票信号准确率一般，建议结合其他指标")
            else -> recommendations.add("该股票信号准确率较低，建议谨慎参考")
        }
        
        // 基于信号类型分析
        bySignalType.forEach { (type, stats) ->
            when {
                stats.accuracy >= 75 -> recommendations.add("$type 信号准确率优秀 (${String.format("%.1f", stats.accuracy)}%)")
                stats.accuracy < 40 -> recommendations.add("$type 信号准确率较低，建议忽略")
            }
        }
        
        // 基于止损止盈触发率
        accuracy.stopLossTriggerRate?.let { rate ->
            if (rate > 50) {
                recommendations.add("止损触发率较高(${(rate).toInt()}%)，建议设置更宽松的止损")
            }
        }
        
        return recommendations
    }

    /**
     * 推断决策方向
     */
    private fun inferDirectionFromDecision(decision: Decision): Direction {
        return when (decision) {
            Decision.STRONG_BUY, Decision.BUY -> Direction.UP
            Decision.STRONG_SELL, Decision.SELL -> Direction.DOWN
            Decision.HOLD -> Direction.FLAT
        }
    }

    /**
     * 从文本推断方向
     */
    fun inferDirectionFromText(text: String?): Direction {
        if (text.isNullOrBlank()) return Direction.FLAT
        val normalized = text.lowercase()
        
        return when {
            BULLISH_KEYWORDS.any { normalized.contains(it) } -> Direction.UP
            BEARISH_KEYWORDS.any { normalized.contains(it) } -> Direction.DOWN
            HOLD_KEYWORDS.any { normalized.contains(it) } -> Direction.NOT_DOWN
            WAIT_KEYWORDS.any { normalized.contains(it) } -> Direction.FLAT
            else -> Direction.FLAT
        }
    }

    /**
     * 评估止损止盈
     */
    private fun evaluateTargets(
        direction: Direction,
        stopLoss: Double?,
        takeProfit: Double?,
        bars: List<KLineData>,
        endClose: Double
    ): TargetEvaluation {
        if (direction != Direction.UP || (stopLoss == null && takeProfit == null)) {
            return TargetEvaluation(
                hitStopLoss = null,
                hitTakeProfit = null,
                firstHit = FirstHit.NOT_APPLICABLE,
                exitPrice = endClose,
                exitReason = "窗口结束"
            )
        }
        
        var hitSL: Boolean? = null
        var hitTP: Boolean? = null
        var firstHit = FirstHit.NEITHER
        var exitPrice = endClose
        var exitReason = "窗口结束"
        
        for (bar in bars) {
            val slHit = stopLoss?.let { bar.low <= it } ?: false
            val tpHit = takeProfit?.let { bar.high >= it } ?: false
            
            if (slHit) hitSL = true
            if (tpHit) hitTP = true
            
            when {
                slHit && tpHit -> {
                    firstHit = FirstHit.AMBIGUOUS
                    exitPrice = stopLoss ?: 0.0
                    exitReason = "ambiguous_stop_loss"
                    return TargetEvaluation(hitSL, hitTP, firstHit, exitPrice, exitReason)
                }
                slHit -> {
                    firstHit = FirstHit.STOP_LOSS
                    exitPrice = stopLoss ?: 0.0
                    exitReason = "stop_loss"
                    return TargetEvaluation(hitSL, hitTP, firstHit, exitPrice, exitReason)
                }
                tpHit -> {
                    firstHit = FirstHit.TAKE_PROFIT
                    exitPrice = takeProfit ?: 0.0
                    exitReason = "take_profit"
                    return TargetEvaluation(hitSL, hitTP, firstHit, exitPrice, exitReason)
                }
            }
        }
        
        return TargetEvaluation(hitSL, hitTP, firstHit, exitPrice, exitReason)
    }

    /**
     * 获取指定日期的价格
     */
    private suspend fun getPriceAtDate(symbol: String, date: Date): Double? {
        return kLineDataDao.getKLineByDate(symbol, date)?.close
    }

    /**
     * 获取未来N天的K线数据
     */
    private suspend fun getForwardBars(
        symbol: String,
        fromDate: Date,
        days: Int
    ): List<KLineData> {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = fromDate
        calendar.add(java.util.Calendar.DAY_OF_YEAR, days)
        val endDate = calendar.time
        
        return kLineDataDao.getKLineDataRange(symbol, fromDate, endDate)
            .filter { it.timestamp.after(fromDate) }
            .sortedBy { it.timestamp }
    }

    /**
     * 对比预测与实际（流式API）
     */
    fun comparePredictedVsActual(
        symbol: String,
        days: Int = 30
    ): Flow<List<ComparisonResult>> = flow {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.time
        
        val results = analysisResultDao.getResultsByTimeRange(startDate, endDate)
            .filter { it.stockSymbol == symbol }
        
        val comparisons = results.mapNotNull { result ->
            val record = recordSignal(result)
            validateSignal(record)
        }
        
        emit(comparisons)
    }.flowOn(Dispatchers.IO)
}

/**
 * 方向枚举
 */
enum class Direction {
    UP,         // 上涨
    DOWN,       // 下跌
    FLAT,       // 震荡
    NOT_DOWN    // 不下跌（持有）
}

/**
 * 结果枚举
 */
enum class Outcome {
    WIN,        // 赢
    LOSS,       // 输
    NEUTRAL,    // 中性
    PENDING     // 待验证
}

/**
 * 首次触发类型
 */
enum class FirstHit {
    STOP_LOSS,      // 止损
    TAKE_PROFIT,    // 止盈
    AMBIGUOUS,      // 同时触发（模糊）
    NEITHER,        // 都未触发
    NOT_APPLICABLE  // 不适用
}

/**
 * 信号记录
 */
data class SignalRecord(
    val id: String,
    val stockSymbol: String,
    val stockName: String,
    val signalDate: Date,
    val signalType: String,
    val direction: Direction,
    val confidence: com.example.stockanalysis.data.model.ConfidenceLevel,
    val score: Int,
    val predictedPrice: Double?,
    val stopLossPrice: Double?,
    val summary: String,
    val isValidated: Boolean = false
)

/**
 * 对比结果
 */
data class ComparisonResult(
    val signalId: String,
    val stockSymbol: String,
    val signalDate: Date,
    val signalType: String,
    val predictedDirection: Direction,
    val actualDirection: Direction,
    val isCorrect: Boolean,
    val outcome: Outcome,
    val priceAtSignal: Double,
    val actualPrice: Double,
    val predictedPrice: Double?,
    val priceChangePercent: Double,
    val evalWindowDays: Int,
    val hitStopLoss: Boolean?,
    val hitTakeProfit: Boolean?,
    val firstHit: FirstHit,
    val exitPrice: Double?,
    val exitReason: String,
    val validationDate: Date
)

/**
 * 信号准确率统计
 */
data class SignalAccuracy(
    val stockSymbol: String,
    val totalSignals: Int,
    val completedSignals: Int,
    val winCount: Int,
    val lossCount: Int,
    val neutralCount: Int,
    val winRate: Double,
    val directionAccuracy: Double,
    val avgReturn: Double,
    val buySignalAccuracy: Double?,
    val sellSignalAccuracy: Double?,
    val holdSignalAccuracy: Double?,
    val stopLossTriggerRate: Double?,
    val takeProfitTriggerRate: Double?,
    val lastUpdated: Date
) {
    companion object {
        fun empty(symbol: String) = SignalAccuracy(
            stockSymbol = symbol,
            totalSignals = 0,
            completedSignals = 0,
            winCount = 0,
            lossCount = 0,
            neutralCount = 0,
            winRate = 0.0,
            directionAccuracy = 0.0,
            avgReturn = 0.0,
            buySignalAccuracy = null,
            sellSignalAccuracy = null,
            holdSignalAccuracy = null,
            stopLossTriggerRate = null,
            takeProfitTriggerRate = null,
            lastUpdated = Date()
        )
    }
}

/**
 * 目标评估结果
 */
private data class TargetEvaluation(
    val hitStopLoss: Boolean?,
    val hitTakeProfit: Boolean?,
    val firstHit: FirstHit,
    val exitPrice: Double?,
    val exitReason: String
)

/**
 * 信号类型统计
 */
data class SignalTypeStats(
    val total: Int,
    val wins: Int,
    val losses: Int,
    val accuracy: Double
)

/**
 * 准确率趋势点
 */
data class AccuracyTrendPoint(
    val date: Date,
    val accuracy: Double,
    val windowSize: Int
)

/**
 * 信号报告
 */
data class SignalReport(
    val stockSymbol: String,
    val periodDays: Int,
    val accuracy: SignalAccuracy,
    val bySignalType: Map<String, SignalTypeStats>,
    val trend: List<AccuracyTrendPoint>,
    val recommendations: List<String>,
    val generatedAt: Date
)
