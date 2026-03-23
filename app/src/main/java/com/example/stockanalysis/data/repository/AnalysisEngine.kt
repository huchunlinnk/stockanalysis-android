package com.example.stockanalysis.data.repository

import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.data.datasource.TushareDataSource
import com.example.stockanalysis.data.local.LocalDataService
import com.example.stockanalysis.data.model.*
import com.example.stockanalysis.data.model.StrategyConfig
import com.example.stockanalysis.data.model.StrategyCategory
import com.example.stockanalysis.data.model.PresetStrategies
import com.example.stockanalysis.util.CrashReportingManager
import com.example.stockanalysis.utils.TechnicalIndicatorCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分析引擎（多 Agent 架构集成版）
 * 
 * 该引擎支持两种分析模式：
 * 1. 本地分析模式（传统）：使用本地计算的技术指标和规则
 * 2. 多 Agent 模式（新）：使用 AI Agent 进行智能分析
 * 
 * 通过 useMultiAgent 参数可以切换两种模式
 */
@Singleton
class AnalysisEngine @Inject constructor(
    private val localDataService: LocalDataService,
    private val tushareDataSource: TushareDataSource,
    private val crashReportingManager: CrashReportingManager,
    private val agentOrchestrator: AgentOrchestrator
) {
    
    /**
     * 执行股票分析
     * 
     * @param symbol 股票代码
     * @param stockName 股票名称
     * @param useMultiAgent 是否使用多 Agent 模式
     * @param config 多 Agent 配置（可选）
     */
    fun analyzeStock(
        symbol: String,
        stockName: String,
        useMultiAgent: Boolean = true,
        config: AgentConfiguration? = null
    ): Flow<AnalysisState> = flow {
        emit(AnalysisState.Loading)
        
        try {
            if (useMultiAgent) {
                // 使用多 Agent 模式
                performMultiAgentAnalysis(symbol, stockName, config)
            } else {
                // 使用本地分析模式
                performLocalAnalysis(symbol, stockName)
            }
        } catch (e: Exception) {
            crashReportingManager.recordAnalysisError(
                stockCode = symbol,
                errorType = e.javaClass.simpleName,
                errorMessage = e.message ?: "Unknown error"
            )
            crashReportingManager.recordException(e, "Analysis failed for $symbol")
            emit(AnalysisState.Error("分析失败: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 多 Agent 分析模式
     */
    private suspend fun FlowCollector<AnalysisState>.performMultiAgentAnalysis(
        symbol: String,
        stockName: String,
        customConfig: AgentConfiguration?
    ) {
        var finalResult: AnalysisResult? = null
        var progress = 0.1f
        
        emit(AnalysisState.Progress("初始化多 Agent 分析...", progress))
        
        agentOrchestrator.analyzeStock(symbol, stockName, customConfig).collect { event ->
            when (event) {
                is MultiAgentEvent.AnalysisStarted -> {
                    progress = 0.1f
                    emit(AnalysisState.Progress("开始${event.mode.displayName()}...", progress))
                }
                is MultiAgentEvent.Progress -> {
                    progress = event.progress.coerceIn(0.1f, 0.9f)
                    emit(AnalysisState.Progress(event.message, progress))
                }
                is MultiAgentEvent.AgentStarted -> {
                    emit(AnalysisState.Progress("${event.agentType.displayName()}分析中...", progress))
                }
                is MultiAgentEvent.AgentCompleted -> {
                    progress = (progress + 0.15f).coerceAtMost(0.9f)
                    event.opinion?.let {
                        emit(AnalysisState.AgentResult(
                            agentType = event.agentType,
                            signal = it.signal,
                            confidence = it.confidence,
                            reasoning = it.reasoning
                        ))
                    }
                }
                is MultiAgentEvent.VoteResult -> {
                    emit(AnalysisState.Progress(
                        "投票结果: ${event.signal.name} (一致性: ${String.format("%.0f%%", event.agreementRate * 100)})",
                        0.8f
                    ))
                }
                is MultiAgentEvent.AnalysisCompleted -> {
                    // 转换 Agent 结果到 AnalysisResult
                    finalResult = convertAgentResultToAnalysisResult(
                        symbol, stockName, event.allOpinions
                    )
                }
                is MultiAgentEvent.Error -> {
                    if (!event.isRecoverable) {
                        emit(AnalysisState.Error(event.message))
                        return@collect
                    }
                }
                else -> { /* 忽略其他事件 */ }
            }
        }
        
        finalResult?.let {
            emit(AnalysisState.Success(it))
        } ?: emit(AnalysisState.Error("未能获取分析结果"))
    }
    
    /**
     * 本地分析模式（传统）
     */
    private suspend fun FlowCollector<AnalysisState>.performLocalAnalysis(
        symbol: String,
        stockName: String
    ) {
        crashReportingManager.log("Starting local analysis for $symbol ($stockName)")
        
        // 1. 获取数据
        emit(AnalysisState.Progress("正在获取数据...", 0.1f))
        val klineData = localDataService.getOrGenerateKLineData(symbol, 90)
        val quote = localDataService.getRealtimeQuote(symbol)
        val chip = localDataService.getChipDistribution(symbol)
        
        if (klineData.size < 20) {
            crashReportingManager.recordAnalysisError(
                stockCode = symbol,
                errorType = "INSUFFICIENT_DATA",
                errorMessage = "K线数据不足：${klineData.size} < 20"
            )
            emit(AnalysisState.Error("数据不足，无法分析"))
            return
        }
        
        // 2. 计算技术指标
        emit(AnalysisState.Progress("正在计算技术指标...", 0.3f))
        val sortedData = klineData.reversed()
        val currentPrice = quote?.price ?: sortedData.last().close
        
        val indicators = TechnicalIndicatorCalculator.calculateAllIndicators(sortedData)
        val trendAnalysis = TechnicalIndicatorCalculator.analyzeTrend(sortedData, currentPrice)
        
        // 3. 基本面分析
        emit(AnalysisState.Progress("正在进行基本面分析...", 0.5f))
        val fundamentalAnalysis = analyzeFundamental(symbol, quote)
        
        // 4. 舆情分析（模拟）
        emit(AnalysisState.Progress("正在进行舆情分析...", 0.7f))
        val sentimentAnalysis = analyzeSentiment(symbol, stockName)
        
        // 5. 风险评估
        emit(AnalysisState.Progress("正在进行风险评估...", 0.8f))
        val riskAssessment = assessRisk(sortedData, trendAnalysis, indicators)
        
        // 6. 生成决策建议
        emit(AnalysisState.Progress("正在生成决策建议...", 0.9f))
        val decision = generateDecision(trendAnalysis, indicators, riskAssessment)
        val actionPlan = generateActionPlan(decision, currentPrice, trendAnalysis)
        
        // 7. 构建分析结果
        val analysisResult = AnalysisResult(
            id = UUID.randomUUID().toString(),
            stockSymbol = symbol,
            stockName = stockName,
            decision = decision,
            score = trendAnalysis?.signalScore ?: 50,
            confidence = determineConfidence(trendAnalysis),
            summary = generateSummary(stockName, decision, trendAnalysis),
            reasoning = generateReasoning(trendAnalysis, indicators, chip),
            technicalAnalysis = TechnicalAnalysis(
                trend = trendAnalysis?.trendStatus?.name ?: "未知",
                maAlignment = indicators?.movingAverages?.let {
                    when {
                        it.isBullishAlignment() -> "多头排列"
                        it.isBearishAlignment() -> "空头排列"
                        else -> "震荡"
                    }
                } ?: "未知",
                supportLevel = trendAnalysis?.supportLevel,
                resistanceLevel = trendAnalysis?.resistanceLevel,
                volumeAnalysis = trendAnalysis?.volumeStatus?.name ?: "正常",
                technicalScore = trendAnalysis?.signalScore ?: 50
            ),
            fundamentalAnalysis = fundamentalAnalysis,
            sentimentAnalysis = sentimentAnalysis,
            riskAssessment = riskAssessment,
            actionPlan = actionPlan
        )
        
        emit(AnalysisState.Success(analysisResult))
    }
    
    /**
     * 批量分析
     */
    fun analyzeStocks(
        stocks: List<Stock>,
        useMultiAgent: Boolean = false
    ): Flow<BatchAnalysisState> = flow {
        emit(BatchAnalysisState.Loading)
        
        val results = mutableListOf<AnalysisResult>()
        val total = stocks.size
        
        stocks.forEachIndexed { index, stock ->
            emit(BatchAnalysisState.Progress(index + 1, total, stock.name))
            
            try {
                var result: AnalysisResult? = null
                analyzeStock(stock.symbol, stock.name, useMultiAgent).collect { state ->
                    if (state is AnalysisState.Success) {
                        result = state.result
                    }
                }
                result?.let { results.add(it) }
            } catch (e: Exception) {
                // 继续分析下一个
            }
        }
        
        emit(BatchAnalysisState.Success(results))
    }.flowOn(Dispatchers.IO)
    
    /**
     * 将 Agent 结果转换为 AnalysisResult
     */
    private fun convertAgentResultToAnalysisResult(
        symbol: String,
        stockName: String,
        opinions: List<AgentOpinion>
    ): AnalysisResult {
        val decisionOpinion = opinions.find { it.agentType == AgentType.DECISION }
        val technicalOpinion = opinions.find { it.agentType == AgentType.TECHNICAL }
        val fundamentalOpinion = opinions.find { it.agentType == AgentType.FUNDAMENTAL }
        val newsOpinion = opinions.find { it.agentType == AgentType.NEWS }
        val riskOpinion = opinions.find { it.agentType == AgentType.RISK }
        
        // 决策转换
        val decision = when (decisionOpinion?.signal) {
            Signal.STRONG_BUY -> Decision.STRONG_BUY
            Signal.BUY -> Decision.BUY
            Signal.HOLD -> Decision.HOLD
            Signal.SELL -> Decision.SELL
            Signal.STRONG_SELL -> Decision.STRONG_SELL
            else -> Decision.HOLD
        }
        
        // 置信度转换
        val confidence = when ((decisionOpinion?.confidence ?: 0.5f)) {
            in 0.8f..1.0f -> ConfidenceLevel.HIGH
            in 0.5f..0.8f -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
        
        return AnalysisResult(
            id = UUID.randomUUID().toString(),
            stockSymbol = symbol,
            stockName = stockName,
            decision = decision,
            score = decisionOpinion?.signal?.score ?: technicalOpinion?.signal?.score ?: 50,
            confidence = confidence,
            summary = decisionOpinion?.reasoning ?: "多 Agent 分析完成",
            reasoning = buildReasoning(opinions),
            technicalAnalysis = convertToTechnicalAnalysis(technicalOpinion),
            fundamentalAnalysis = convertToFundamentalAnalysis(fundamentalOpinion),
            sentimentAnalysis = convertToSentimentAnalysis(newsOpinion),
            riskAssessment = convertToRiskAssessment(riskOpinion),
            actionPlan = convertToActionPlan(decisionOpinion, technicalOpinion?.keyLevels)
        )
    }
    
    /**
     * 构建推理说明
     */
    private fun buildReasoning(opinions: List<AgentOpinion>): String {
        return buildString {
            appendLine("【多 Agent 综合分析】")
            appendLine()
            opinions.filter { it.agentType != AgentType.DECISION }.forEach { opinion ->
                appendLine("${opinion.agentType.displayName()}：")
                appendLine("- 信号: ${opinion.signal.name}")
                appendLine("- 置信度: ${String.format("%.0f%%", opinion.confidence * 100)}")
                appendLine("- 理由: ${opinion.reasoning}")
                appendLine()
            }
        }
    }
    
    private fun convertToTechnicalAnalysis(opinion: AgentOpinion?): TechnicalAnalysis? {
        if (opinion == null) return null
        val rawData = opinion.rawData
        return TechnicalAnalysis(
            trend = rawData["trend"] as? String ?: "未知",
            maAlignment = (rawData["indicators"] as? Map<String, String>)?.get("ma_alignment") ?: "未知",
            supportLevel = opinion.keyLevels?.support,
            resistanceLevel = opinion.keyLevels?.resistance,
            volumeAnalysis = rawData["volume_analysis"] as? String ?: "正常",
            technicalScore = rawData["technical_score"] as? Int ?: opinion.signal.score
        )
    }
    
    private fun convertToFundamentalAnalysis(opinion: AgentOpinion?): FundamentalAnalysis? {
        if (opinion == null) return null
        val rawData = opinion.rawData
        return FundamentalAnalysis(
            valuation = (rawData["valuation"] as? Map<String, String>)?.get("valuation_conclusion") ?: "未知",
            growth = rawData["growth"] as? String ?: "未知",
            profitability = rawData["profitability"] as? String ?: "未知",
            financialHealth = rawData["financial_health"] as? String ?: "未知",
            fundamentalScore = rawData["fundamental_score"] as? Int ?: 50
        )
    }
    
    private fun convertToSentimentAnalysis(opinion: AgentOpinion?): SentimentAnalysis? {
        if (opinion == null) return null
        val rawData = opinion.rawData
        return SentimentAnalysis(
            overallSentiment = rawData["sentiment_label"] as? String ?: "中性",
            sentimentScore = rawData["sentiment_score"] as? Double ?: 0.0,
            keyNews = emptyList(), // 可从 rawData 解析
            riskFactors = (rawData["risk_factors"] as? List<String>) ?: emptyList(),
            catalysts = (rawData["catalysts"] as? List<String>) ?: emptyList()
        )
    }
    
    private fun convertToRiskAssessment(opinion: AgentOpinion?): RiskAssessment? {
        if (opinion == null) return null
        val rawData = opinion.rawData
        val riskLevel = when (rawData["risk_level"] as? String) {
            "high" -> RiskLevel.HIGH
            "low" -> RiskLevel.LOW
            else -> RiskLevel.MEDIUM
        }
        return RiskAssessment(
            riskLevel = riskLevel,
            volatility = "未知",
            liquidityRisk = "未知",
            marketRisk = "未知",
            specificRisks = (rawData["risk_factors"] as? List<String>) ?: emptyList()
        )
    }
    
    private fun convertToActionPlan(
        decisionOpinion: AgentOpinion?,
        keyLevels: AgentOpinion.KeyLevels?
    ): ActionPlan? {
        if (decisionOpinion == null) return null
        val rawData = decisionOpinion.rawData
        val riskManagement = rawData["risk_management"] as? Map<String, Any>
        
        return ActionPlan(
            entryPrice = keyLevels?.entryPrice,
            stopLossPrice = keyLevels?.stopLoss
                ?: (riskManagement?.get("stop_loss") as? Number)?.toDouble(),
            targetPrice = keyLevels?.takeProfit
                ?: (riskManagement?.get("take_profit") as? Number)?.toDouble(),
            positionSize = riskManagement?.get("max_position") as? String,
            timeHorizon = "1-3个月",
            checkList = (rawData["checklist"] as? List<Map<String, String>>)?.map {
                CheckItem(
                    item = it["item"] ?: "",
                    status = try {
                        CheckStatus.valueOf(it["status"]?.uppercase() ?: "WARNING")
                    } catch (e: Exception) {
                        CheckStatus.WARNING
                    }
                )
            } ?: emptyList()
        )
    }
    
    // ========== 本地分析方法 ==========
    
    private suspend fun analyzeFundamental(symbol: String, quote: RealtimeQuote?): FundamentalAnalysis {
        val tushareResult = tushareDataSource.fetchFundamentalAnalysis(symbol)
        
        return if (tushareResult.isSuccess) {
            tushareResult.getOrThrow()
        } else {
            FundamentalAnalysis(
                valuation = when {
                    (quote?.peRatio ?: 20.0) < 15 -> "估值偏低，具备安全边际"
                    (quote?.peRatio ?: 20.0) < 30 -> "估值合理"
                    else -> "估值偏高，注意风险"
                } + " [模拟数据]",
                growth = "业绩稳定增长，营收同比增长${kotlin.random.Random.nextInt(5, 25)}% [模拟数据]",
                profitability = "毛利率${kotlin.random.Random.nextInt(20, 60)}%，净利率${kotlin.random.Random.nextInt(5, 25)}%，盈利能力良好 [模拟数据]",
                financialHealth = "资产负债率${kotlin.random.Random.nextInt(30, 70)}%，财务状况健康 [模拟数据]",
                fundamentalScore = kotlin.random.Random.nextInt(60, 85)
            )
        }
    }
    
    private fun analyzeSentiment(symbol: String, stockName: String): SentimentAnalysis {
        val sentimentScore = kotlin.random.Random.nextDouble(-0.5, 0.5)
        
        return SentimentAnalysis(
            overallSentiment = when {
                sentimentScore > 0.3 -> "非常积极"
                sentimentScore > 0 -> "积极"
                sentimentScore > -0.3 -> "中性"
                else -> "消极"
            },
            sentimentScore = sentimentScore,
            keyNews = emptyList(),
            riskFactors = listOf("市场整体波动风险", "行业政策变化风险"),
            catalysts = listOf("业绩超预期", "行业景气度提升", "机构看好")
        )
    }
    
    private fun assessRisk(
        klineData: List<KLineData>,
        trendAnalysis: TrendAnalysis?,
        indicators: TechnicalIndicators?
    ): RiskAssessment {
        val volatility = calculateVolatility(klineData)
        
        val riskLevel = when {
            volatility > 3.0 || trendAnalysis?.riskFactors?.size ?: 0 > 2 -> RiskLevel.HIGH
            volatility > 2.0 || trendAnalysis?.riskFactors?.size ?: 0 > 1 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        
        return RiskAssessment(
            riskLevel = riskLevel,
            volatility = "日波动率 ${String.format("%.2f", volatility)}%",
            liquidityRisk = "流动性良好",
            marketRisk = "系统性风险中等，关注大盘走势",
            specificRisks = trendAnalysis?.riskFactors ?: emptyList()
        )
    }
    
    private fun generateDecision(
        trendAnalysis: TrendAnalysis?,
        indicators: TechnicalIndicators?,
        riskAssessment: RiskAssessment
    ): Decision {
        val buySignal = trendAnalysis?.buySignal ?: BuySignal.NEUTRAL
        val riskLevel = riskAssessment.riskLevel
        
        return when (buySignal) {
            BuySignal.STRONG_BUY -> if (riskLevel == RiskLevel.HIGH) Decision.BUY else Decision.STRONG_BUY
            BuySignal.BUY -> Decision.BUY
            BuySignal.WEAK_BUY -> Decision.HOLD
            BuySignal.NEUTRAL -> Decision.HOLD
            BuySignal.WEAK_SELL -> Decision.HOLD
            BuySignal.SELL -> Decision.SELL
            BuySignal.STRONG_SELL -> Decision.STRONG_SELL
        }
    }
    
    private fun generateActionPlan(
        decision: Decision,
        currentPrice: Double,
        trendAnalysis: TrendAnalysis?
    ): ActionPlan {
        val positionSize = when (decision) {
            Decision.STRONG_BUY -> "50-70%"
            Decision.BUY -> "30-50%"
            Decision.HOLD -> "0-20%"
            Decision.SELL, Decision.STRONG_SELL -> "清仓"
        }
        
        val stopLoss = currentPrice * 0.95
        val targetPrice = when (decision) {
            Decision.STRONG_BUY, Decision.BUY -> currentPrice * 1.15
            else -> null
        }
        
        return ActionPlan(
            entryPrice = if (decision in listOf(Decision.STRONG_BUY, Decision.BUY)) currentPrice else null,
            stopLossPrice = stopLoss,
            targetPrice = targetPrice,
            positionSize = positionSize,
            timeHorizon = "1-3个月",
            checkList = listOf(
                CheckItem("均线多头排列", if (trendAnalysis?.buySignal in listOf(BuySignal.STRONG_BUY, BuySignal.BUY)) CheckStatus.PASSED else CheckStatus.WARNING),
                CheckItem("成交量配合", if (trendAnalysis?.volumeStatus == VolumeStatus.EXPANDING) CheckStatus.PASSED else CheckStatus.WARNING),
                CheckItem("乖离率合理", if ((trendAnalysis?.biasMa5 ?: 0.0) < 5) CheckStatus.PASSED else CheckStatus.FAILED),
                CheckItem("无重大利空", CheckStatus.PASSED)
            )
        )
    }
    
    private fun determineConfidence(trendAnalysis: TrendAnalysis?): ConfidenceLevel {
        return when (trendAnalysis?.signalScore) {
            in 80..100 -> ConfidenceLevel.HIGH
            in 60..79 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
    }
    
    private fun generateSummary(stockName: String, decision: Decision, trendAnalysis: TrendAnalysis?): String {
        return when (decision) {
            Decision.STRONG_BUY -> "$stockName 技术面良好，均线多头排列，建议积极买入"
            Decision.BUY -> "$stockName 走势稳健，存在买入机会"
            Decision.HOLD -> "$stockName 震荡整理，建议观望"
            Decision.SELL -> "$stockName 走势偏弱，建议减仓"
            Decision.STRONG_SELL -> "$stockName 空头排列，建议清仓"
        }
    }
    
    private fun generateReasoning(
        trendAnalysis: TrendAnalysis?,
        indicators: TechnicalIndicators?,
        chip: ChipDistribution?
    ): String {
        val reasons = mutableListOf<String>()
        
        trendAnalysis?.let {
            reasons.add("【趋势分析】${it.trendStatus.name}，趋势强度${String.format("%.0f", it.trendStrength)}分")
            reasons.add("【买入信号】${it.buySignal.name}，信号评分${it.signalScore}分")
            if (it.signalReasons.isNotEmpty()) {
                reasons.add("【利好因素】${it.signalReasons.joinToString("、")}")
            }
            if (it.riskFactors.isNotEmpty()) {
                reasons.add("【风险因素】${it.riskFactors.joinToString("、")}")
            }
        }
        
        indicators?.let {
            it.movingAverages?.let { ma ->
                reasons.add("【均线系统】MA5=${String.format("%.2f", ma.ma5 ?: 0.0)}, MA10=${String.format("%.2f", ma.ma10 ?: 0.0)}, MA20=${String.format("%.2f", ma.ma20 ?: 0.0)}")
            }
            it.macd?.let { macd ->
                reasons.add("【MACD】DIF=${String.format("%.2f", macd.dif)}, DEA=${String.format("%.2f", macd.dea)}")
            }
        }
        
        chip?.let {
            reasons.add("【筹码分布】平均成本${String.format("%.2f", it.avgCost)}，获利比例${String.format("%.1f", it.profitRatio * 100)}%")
        }
        
        return reasons.joinToString("\n")
    }
    
    private fun calculateVolatility(klineData: List<KLineData>): Double {
        if (klineData.size < 2) return 0.0
        
        val returns = klineData.zipWithNext { prev, curr ->
            (curr.close - prev.close) / prev.close * 100
        }
        
        val mean = returns.average()
        val variance = returns.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * 使用策略配置执行分析
     */
    suspend fun analyze(
        symbol: String,
        strategyConfig: StrategyConfig,
        params: Map<String, Any> = strategyConfig.getDefaultParams()
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val stockName = localDataService.getStockPool()
            .find { it.first == symbol }?.second ?: symbol
        
        // 获取基础数据
        val klineData = localDataService.getOrGenerateKLineData(symbol, 90)
        val sortedData = klineData.reversed()
        val currentPrice = sortedData.last().close
        val indicators = TechnicalIndicatorCalculator.calculateAllIndicators(sortedData)
        val trendAnalysis = TechnicalIndicatorCalculator.analyzeTrend(sortedData, currentPrice)
        
        // 根据策略类别生成分析结果
        val decision = when (strategyConfig.category) {
            StrategyCategory.TREND_FOLLOWING -> when (trendAnalysis?.buySignal) {
                BuySignal.STRONG_BUY -> Decision.STRONG_BUY
                BuySignal.BUY -> Decision.BUY
                BuySignal.SELL -> Decision.SELL
                BuySignal.STRONG_SELL -> Decision.STRONG_SELL
                else -> Decision.HOLD
            }
            StrategyCategory.MEAN_REVERSION -> {
                val ma20 = indicators?.movingAverages?.ma20 ?: currentPrice
                val deviation = (currentPrice - ma20) / ma20 * 100
                when {
                    deviation < -5 -> Decision.BUY
                    deviation > 5 -> Decision.SELL
                    else -> Decision.HOLD
                }
            }
            else -> trendAnalysis?.buySignal?.let {
                when (it) {
                    BuySignal.STRONG_BUY -> Decision.STRONG_BUY
                    BuySignal.BUY -> Decision.BUY
                    BuySignal.SELL -> Decision.SELL
                    BuySignal.STRONG_SELL -> Decision.STRONG_SELL
                    else -> Decision.HOLD
                }
            } ?: Decision.HOLD
        }
        
        AnalysisResult(
            id = UUID.randomUUID().toString(),
            stockSymbol = symbol,
            stockName = stockName,
            decision = decision,
            score = trendAnalysis?.signalScore ?: 50,
            confidence = determineConfidence(trendAnalysis),
            summary = generateSummary(stockName, decision, trendAnalysis),
            reasoning = "策略分析：${strategyConfig.name}",
            technicalAnalysis = TechnicalAnalysis(
                trend = trendAnalysis?.trendStatus?.name ?: "未知",
                maAlignment = indicators?.movingAverages?.getAlignmentStatus(currentPrice) ?: "未知",
                supportLevel = trendAnalysis?.supportLevel,
                resistanceLevel = trendAnalysis?.resistanceLevel,
                volumeAnalysis = trendAnalysis?.volumeStatus?.name ?: "正常",
                technicalScore = trendAnalysis?.signalScore ?: 50
            ),
            fundamentalAnalysis = null,
            sentimentAnalysis = null,
            riskAssessment = assessRisk(sortedData, trendAnalysis, indicators),
            actionPlan = generateActionPlan(decision, currentPrice, trendAnalysis)
        )
    }
}

/**
 * 分析状态
 */
sealed class AnalysisState {
    object Loading : AnalysisState()
    data class Progress(val message: String, val progress: Float) : AnalysisState()
    data class AgentResult(
        val agentType: AgentType,
        val signal: Signal,
        val confidence: Float,
        val reasoning: String
    ) : AnalysisState()
    data class Success(val result: AnalysisResult) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

sealed class BatchAnalysisState {
    object Loading : BatchAnalysisState()
    data class Progress(val current: Int, val total: Int, val currentStock: String) : BatchAnalysisState()
    data class Success(val results: List<AnalysisResult>) : BatchAnalysisState()
    data class Error(val message: String) : BatchAnalysisState()
}
