package com.example.stockanalysis.data.agent

import android.util.Log
import com.example.stockanalysis.data.agent.tools.*
import com.example.stockanalysis.data.api.LLMApiService
import com.example.stockanalysis.data.llm.LLMService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 新版 Agent 编排器
 * 
 * 基于多 Agent 专业架构，支持：
 * 1. 顺序执行模式 (Sequential)
 * 2. 并行执行模式 (Parallel)
 * 3. 投票模式 (Voting)
 * 4. 分层决策模式 (Hierarchical)
 * 
 * 这是原 AgentOrchestrator 的升级版，提供更灵活的 Agent 编排能力。
 */
@Singleton
class AgentOrchestrator @Inject constructor(
    private val llmService: LLMService,
    private val llmApiService: LLMApiService,
    private val pipeline: AgentPipeline,
    private val configManager: AgentConfigurationManager,
    private val quoteTool: GetRealtimeQuoteTool,
    private val klineTool: GetKLineDataTool,
    private val indicatorsTool: GetTechnicalIndicatorsTool,
    private val trendTool: GetTrendAnalysisTool,
    private val newsTool: SearchNewsTool
) {
    companion object {
        private const val TAG = "AgentOrchestrator"
        const val DEFAULT_TIMEOUT_MS = 120000L
    }
    
    private val toolRegistry: AgentToolRegistry = AgentToolRegistry()
    
    init {
        // 注册所有可用工具
        toolRegistry.register(quoteTool)
        toolRegistry.register(klineTool)
        toolRegistry.register(indicatorsTool)
        toolRegistry.register(trendTool)
        toolRegistry.register(newsTool)
    }
    
    /**
     * 执行股票分析（新版多 Agent 模式）
     * 
     * @param stockSymbol 股票代码
     * @param stockName 股票名称
     * @param customConfig 自定义配置（可选）
     * @return Agent 事件流
     */
    fun analyzeStock(
        stockSymbol: String,
        stockName: String,
        customConfig: AgentConfiguration? = null
    ): Flow<MultiAgentEvent> = flow {
        val config = customConfig ?: configManager.loadConfiguration()
        
        // 创建分析上下文
        val context = AgentContext(
            stockSymbol = stockSymbol,
            stockName = stockName,
            query = "分析股票 $stockName ($stockSymbol)",
            config = config
        )
        
        emit(MultiAgentEvent.AnalysisStarted(stockSymbol, stockName, config.executionMode))
        
        try {
            // 执行流水线
            pipeline.execute(context).collect { event ->
                when (event) {
                    is PipelineEvent.Started -> {
                        emit(MultiAgentEvent.Progress(
                            stage = "初始化",
                            message = "开始${config.executionMode.displayName()}",
                            progress = 0.0f
                        ))
                    }
                    is PipelineEvent.LayerStarted -> {
                        emit(MultiAgentEvent.Progress(
                            stage = "第${event.layer}层",
                            message = event.description,
                            progress = (event.layer - 1) * 0.33f
                        ))
                    }
                    is PipelineEvent.AgentStarted -> {
                        emit(MultiAgentEvent.AgentStarted(
                            agentType = event.agentType,
                            message = "${event.agentType.displayName()}开始分析"
                        ))
                    }
                    is PipelineEvent.AgentCompleted -> {
                        emit(MultiAgentEvent.AgentCompleted(
                            agentType = event.agentType,
                            result = event.result,
                            opinion = event.result.opinion
                        ))
                    }
                    is PipelineEvent.VoteCompleted -> {
                        emit(MultiAgentEvent.VoteResult(
                            signal = event.voteResult.signal,
                            confidence = event.voteResult.confidence,
                            reasoning = event.voteResult.reasoning,
                            votes = event.voteResult.votes,
                            agreementRate = event.voteResult.agreementRate
                        ))
                    }
                    is PipelineEvent.Completed -> {
                        val finalDecision = context.getOpinion(AgentType.DECISION)
                        emit(MultiAgentEvent.AnalysisCompleted(
                            durationMs = event.durationMs,
                            finalDecision = finalDecision,
                            allOpinions = context.opinions.toList()
                        ))
                    }
                    is PipelineEvent.Error -> {
                        emit(MultiAgentEvent.Error(event.message, event.isRecoverable))
                    }
                    else -> { /* 忽略其他事件 */ }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "分析过程发生错误", e)
            emit(MultiAgentEvent.Error(e.message ?: "未知错误", false))
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    
    /**
     * 快速分析（仅技术面）
     */
    fun analyzeStockQuick(
        stockSymbol: String,
        stockName: String
    ): Flow<MultiAgentEvent> = analyzeStock(
        stockSymbol = stockSymbol,
        stockName = stockName,
        customConfig = AgentConfiguration.quickMode()
    )
    
    /**
     * 标准分析（技术面+舆情）
     */
    fun analyzeStockStandard(
        stockSymbol: String,
        stockName: String
    ): Flow<MultiAgentEvent> = analyzeStock(
        stockSymbol = stockSymbol,
        stockName = stockName,
        customConfig = AgentConfiguration.standardMode()
    )
    
    /**
     * 完整分析（所有 Agent）
     */
    fun analyzeStockFull(
        stockSymbol: String,
        stockName: String
    ): Flow<MultiAgentEvent> = analyzeStock(
        stockSymbol = stockSymbol,
        stockName = stockName,
        customConfig = AgentConfiguration.fullMode()
    )
    
    /**
     * 投票分析模式
     */
    fun analyzeStockVoting(
        stockSymbol: String,
        stockName: String
    ): Flow<MultiAgentEvent> = analyzeStock(
        stockSymbol = stockSymbol,
        stockName = stockName,
        customConfig = AgentConfiguration.votingMode()
    )
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfiguration(): AgentConfiguration {
        return configManager.loadConfiguration()
    }
    
    /**
     * 更新配置
     */
    fun updateConfiguration(config: AgentConfiguration) {
        configManager.saveConfiguration(config)
    }
    
    /**
     * 应用预设模式
     */
    fun applyPresetMode(mode: PresetMode) {
        configManager.applyPresetMode(mode)
    }
    
    /**
     * 获取可用预设模式
     */
    fun getAvailableModes(): List<PresetModeInfo> {
        return listOf(
            PresetModeInfo(
                mode = PresetMode.QUICK,
                name = "快速模式",
                description = "仅技术面分析，约30秒完成",
                agentCount = 2,
                features = listOf("技术指标", "趋势判断")
            ),
            PresetModeInfo(
                mode = PresetMode.STANDARD,
                name = "标准模式",
                description = "技术+舆情分析，约60秒完成",
                agentCount = 3,
                features = listOf("技术指标", "趋势判断", "新闻舆情", "市场情绪")
            ),
            PresetModeInfo(
                mode = PresetMode.FULL,
                name = "完整模式",
                description = "全维度分析，约2分钟完成",
                agentCount = 5,
                features = listOf("技术指标", "基本面", "新闻舆情", "风险评估", "综合决策")
            ),
            PresetModeInfo(
                mode = PresetMode.VOTING,
                name = "投票模式",
                description = "多Agent投票决策，约90秒完成",
                agentCount = 4,
                features = listOf("集体决策", "意见一致性", "投票统计")
            )
        )
    }
    
    /**
     * 设置 Agent 启用状态
     */
    fun setAgentEnabled(agentType: AgentType, enabled: Boolean) {
        val config = configManager.loadConfiguration()
        configManager.saveConfiguration(config.setAgentEnabled(agentType, enabled))
    }
    
    /**
     * 设置 Agent 权重
     */
    fun setAgentWeight(agentType: AgentType, weight: Double) {
        val config = configManager.loadConfiguration()
        configManager.saveConfiguration(config.setAgentWeight(agentType, weight))
    }
    
    /**
     * 设置执行模式
     */
    fun setExecutionMode(mode: ExecutionMode) {
        val config = configManager.loadConfiguration()
        configManager.saveConfiguration(config.copy(executionMode = mode))
    }
    
    /**
     * 获取所有可用 Agent 类型
     */
    fun getAvailableAgents(): List<AgentInfo> {
        val config = configManager.loadConfiguration()
        return AgentType.values().map { agentType ->
            AgentInfo(
                type = agentType,
                name = agentType.displayName(),
                description = agentType.description(),
                isEnabled = config.isAgentEnabled(agentType),
                weight = config.getAgentWeight(agentType),
                canParallelExecute = agentType.canParallelExecute()
            )
        }
    }
    
    /**
     * 获取 Agent 执行统计
     */
    fun getAgentStatistics(): AgentStatistics {
        // 这里可以实现统计功能
        return AgentStatistics(
            totalAnalyses = 0,
            averageDurationMs = 0,
            successRate = 0.0f
        )
    }
    
    // ========== 兼容旧版接口 ==========
    
    /**
     * 兼容旧版分析接口
     * 
     * @param strategyId 策略ID（现仅用于兼容，实际使用多Agent模式）
     * @param stockSymbol 股票代码
     * @param stockName 股票名称
     */
    @Deprecated("使用 analyzeStock 替代", ReplaceWith("analyzeStock(stockSymbol, stockName)"))
    fun analyze(
        strategyId: String,
        stockSymbol: String,
        stockName: String
    ): Flow<AgentEvent> = flow {
        // 将新的事件映射到旧的事件格式
        analyzeStock(stockSymbol, stockName).collect { event ->
            when (event) {
                is MultiAgentEvent.AnalysisStarted -> {
                    emit(AgentEvent.Progress(1, 4, "开始分析: $stockName"))
                }
                is MultiAgentEvent.AgentStarted -> {
                    emit(AgentEvent.Progress(2, 4, "${event.agentType.displayName()}分析中..."))
                }
                is MultiAgentEvent.AgentCompleted -> {
                    emit(AgentEvent.Progress(3, 4, "${event.agentType.displayName()}完成"))
                }
                is MultiAgentEvent.AnalysisCompleted -> {
                    val decision = event.finalDecision
                    val content = buildString {
                        appendLine("## 分析结果")
                        appendLine()
                        appendLine("**决策**: ${decision?.signal?.name ?: "未知"}")
                        appendLine()
                        appendLine("**置信度**: ${String.format("%.0f%%", (decision?.confidence ?: 0f) * 100)}")
                        appendLine()
                        appendLine("**分析理由**:")
                        appendLine(decision?.reasoning ?: "暂无")
                        appendLine()
                        appendLine("**各 Agent 意见**:")
                        event.allOpinions.filter { it.agentType != AgentType.DECISION }.forEach { opinion ->
                            appendLine("- ${opinion.agentType.displayName()}: ${opinion.signal.name}")
                        }
                    }
                    emit(AgentEvent.Complete(
                        content = content,
                        summary = decision?.reasoning ?: "分析完成"
                    ))
                }
                is MultiAgentEvent.Error -> {
                    emit(AgentEvent.Error(event.message))
                }
                else -> { /* 忽略其他事件 */ }
            }
        }
    }
    
    @Deprecated("使用 getAvailableModes 替代")
    fun getAllStrategies(): List<StrategyConfig> {
        return BuiltInStrategyLoader().loadAllStrategies()
    }
    
    @Deprecated("使用 getAvailableModes 替代")
    fun getStrategy(id: String): StrategyConfig? {
        return BuiltInStrategyLoader().loadStrategy(id)
    }
}

/**
 * 多 Agent 分析事件
 */
sealed class MultiAgentEvent {
    /**
     * 分析开始
     */
    data class AnalysisStarted(
        val stockSymbol: String,
        val stockName: String,
        val mode: ExecutionMode
    ) : MultiAgentEvent()
    
    /**
     * 进度更新
     */
    data class Progress(
        val stage: String,
        val message: String,
        val progress: Float  // 0.0 - 1.0
    ) : MultiAgentEvent()
    
    /**
     * Agent 开始
     */
    data class AgentStarted(
        val agentType: AgentType,
        val message: String
    ) : MultiAgentEvent()
    
    /**
     * Agent 进度
     */
    data class AgentProgress(
        val agentType: AgentType,
        val step: Int,
        val totalSteps: Int,
        val message: String
    ) : MultiAgentEvent()
    
    /**
     * Agent 完成
     */
    data class AgentCompleted(
        val agentType: AgentType,
        val result: AgentResult,
        val opinion: AgentOpinion?
    ) : MultiAgentEvent()
    
    /**
     * 投票结果
     */
    data class VoteResult(
        val signal: Signal,
        val confidence: Double,
        val reasoning: String,
        val votes: Map<String, Int>,
        val agreementRate: Double
    ) : MultiAgentEvent()
    
    /**
     * 分析完成
     */
    data class AnalysisCompleted(
        val durationMs: Long,
        val finalDecision: AgentOpinion?,
        val allOpinions: List<AgentOpinion>
    ) : MultiAgentEvent()
    
    /**
     * 错误
     */
    data class Error(
        val message: String,
        val isRecoverable: Boolean
    ) : MultiAgentEvent()
}

/**
 * Agent 信息
 */
data class AgentInfo(
    val type: AgentType,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val weight: Double,
    val canParallelExecute: Boolean
)

/**
 * 预设模式信息
 */
data class PresetModeInfo(
    val mode: PresetMode,
    val name: String,
    val description: String,
    val agentCount: Int,
    val features: List<String>
)

/**
 * Agent 统计
 */
data class AgentStatistics(
    val totalAnalyses: Int,
    val averageDurationMs: Long,
    val successRate: Float
)
