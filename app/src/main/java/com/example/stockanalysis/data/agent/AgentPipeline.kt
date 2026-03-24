package com.example.stockanalysis.data.agent

import android.util.Log
import com.example.stockanalysis.data.agent.agents.*
import com.example.stockanalysis.data.llm.LLMService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent 流水线
 * 协调多个 Agent 的执行，支持顺序、并行、投票和分层模式
 */
@Singleton
class AgentPipeline @Inject constructor(
    private val llmService: LLMService,
    private val toolRegistry: AgentToolRegistry,
    private val configManager: AgentConfigurationManager
) {
    companion object {
        private const val TAG = "AgentPipeline"
    }
    
    // Agent 缓存
    private val agentCache = ConcurrentHashMap<AgentType, Agent>()
    
    /**
     * 执行分析流水线
     * 
     * @param context 分析上下文
     * @return 流水线结果流
     */
    fun execute(context: AgentContext): Flow<PipelineEvent> = flow {
        val config = context.config
        val startTime = System.currentTimeMillis()
        
        emit(PipelineEvent.Started(context.stockSymbol, config.executionMode))
        
        try {
            when (config.executionMode) {
                ExecutionMode.SEQUENTIAL -> {
                    executeSequential(context, this)
                }
                ExecutionMode.PARALLEL -> {
                    executeParallel(context, this)
                }
                ExecutionMode.VOTING -> {
                    executeVoting(context, this)
                }
                ExecutionMode.HIERARCHICAL -> {
                    executeHierarchical(context, this)
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            emit(PipelineEvent.Completed(duration, context))
            
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "流水线执行超时")
            emit(PipelineEvent.Error("分析超时，请稍后重试", true))
        } catch (e: Exception) {
            Log.e(TAG, "流水线执行失败", e)
            emit(PipelineEvent.Error(e.message ?: "未知错误", false))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 顺序执行模式
     */
    private suspend fun executeSequential(
        context: AgentContext,
        emitter: FlowCollector<PipelineEvent>
    ) {
        val enabledAgents = context.config.getEnabledAnalysisAgents() + AgentType.DECISION
        
        for ((index, agentType) in enabledAgents.withIndex()) {
            val agent = getOrCreateAgent(agentType)
            if (!agent.isEnabled) continue
            
            emitter.emit(PipelineEvent.AgentStarted(agentType, index + 1, enabledAgents.size))
            
            val result = withTimeoutOrNull(context.config.agentTimeoutMs) {
                agent.analyzeSync(context)
            } ?: AgentResult(
                agentType = agentType,
                status = AgentStatus.FAILED,
                error = "执行超时"
            )
            
            emitter.emit(PipelineEvent.AgentCompleted(agentType, result))
            
            // 非关键 Agent 失败可以继续
            if (!result.isSuccess && agentType in listOf(AgentType.FUNDAMENTAL, AgentType.NEWS)) {
                Log.w(TAG, "${agentType.name} 失败，继续执行")
            }
        }
    }
    
    /**
     * 并行执行模式
     */
    private suspend fun executeParallel(
        context: AgentContext,
        emitter: FlowCollector<PipelineEvent>
    ) = coroutineScope {
        val analysisAgents = context.config.getEnabledAnalysisAgents()
            .filter { it.canParallelExecute() }
        
        // 第一阶段：并行执行所有分析 Agent
        val deferredResults = analysisAgents.map { agentType ->
            async {
                val agent = getOrCreateAgent(agentType)
                if (!agent.isEnabled) {
                    return@async agentType to AgentResult(
                        agentType = agentType,
                        status = AgentStatus.SKIPPED
                    )
                }
                
                emitter.emit(PipelineEvent.AgentStarted(agentType, 1, analysisAgents.size + 1))
                
                val result = withTimeoutOrNull(context.config.agentTimeoutMs) {
                    agent.analyzeSync(context)
                } ?: AgentResult(
                    agentType = agentType,
                    status = AgentStatus.FAILED,
                    error = "执行超时"
                )
                
                emitter.emit(PipelineEvent.AgentCompleted(agentType, result))
                agentType to result
            }
        }
        
        // 等待所有分析 Agent 完成
        val completedCount = deferredResults.awaitAll().count { it.second.isSuccess }
        
        // 检查是否满足最小要求
        if (completedCount < context.config.minRequiredAgents && !context.config.allowDegradedExecution) {
            throw AgentExecutionException(
                "完成的 Agent 数量不足: $completedCount < ${context.config.minRequiredAgents}",
                AgentType.DECISION,
                false
            )
        }
        
        // 第二阶段：执行决策 Agent
        val decisionAgent = getOrCreateAgent(AgentType.DECISION)
        emitter.emit(PipelineEvent.AgentStarted(AgentType.DECISION, analysisAgents.size + 1, analysisAgents.size + 1))
        
        val decisionResult = withTimeoutOrNull(context.config.agentTimeoutMs) {
            decisionAgent.analyzeSync(context)
        } ?: generateFallbackDecision(context)
        
        emitter.emit(PipelineEvent.AgentCompleted(AgentType.DECISION, decisionResult))
    }
    
    /**
     * 投票执行模式
     */
    private suspend fun executeVoting(
        context: AgentContext,
        emitter: FlowCollector<PipelineEvent>
    ) = coroutineScope {
        val votingAgents = context.config.getEnabledAnalysisAgents()
        
        // 并行执行所有投票 Agent
        val results = votingAgents.map { agentType ->
            async {
                val agent = getOrCreateAgent(agentType)
                emitter.emit(PipelineEvent.AgentStarted(agentType, 1, votingAgents.size))
                
                val result = withTimeoutOrNull(context.config.agentTimeoutMs) {
                    agent.analyzeSync(context)
                } ?: AgentResult(
                    agentType = agentType,
                    status = AgentStatus.FAILED,
                    error = "执行超时"
                )
                
                emitter.emit(PipelineEvent.AgentCompleted(agentType, result))
                result
            }
        }.awaitAll()
        
        // 计算投票结果
        val voteResult = calculateVoteResult(results.filter { it.isSuccess }, context.config)
        context.setData("vote_result", voteResult)
        
        emitter.emit(PipelineEvent.VoteCompleted(voteResult))
        
        // 生成投票决策意见
        val votingOpinion = AgentOpinion(
            agentType = AgentType.DECISION,
            agentName = "投票决策",
            signal = voteResult.signal,
            confidence = voteResult.confidence.toFloat(),
            reasoning = voteResult.reasoning,
            rawData = mapOf(
                "voting_result" to true,
                "votes" to voteResult.votes,
                "agreement_rate" to voteResult.agreementRate
            )
        )
        context.addOpinion(votingOpinion)
        context.setData("final_decision", votingOpinion)
        
        emitter.emit(PipelineEvent.AgentCompleted(AgentType.DECISION, AgentResult(
            agentType = AgentType.DECISION,
            status = AgentStatus.COMPLETED,
            opinion = votingOpinion
        )))
    }
    
    /**
     * 分层执行模式
     * 第一层：技术面+基本面并行
     * 第二层：新闻+风险并行（依赖第一层）
     * 第三层：决策（依赖所有）
     */
    private suspend fun executeHierarchical(
        context: AgentContext,
        emitter: FlowCollector<PipelineEvent>
    ) {
        val config = context.config
        
        // 第一层：核心分析（技术和基本面）
        val layer1Agents = listOf(AgentType.TECHNICAL, AgentType.FUNDAMENTAL)
            .filter { config.isAgentEnabled(it) }
        
        if (layer1Agents.isNotEmpty()) {
            emitter.emit(PipelineEvent.LayerStarted(1, "核心分析"))
            
            val layer1Results = coroutineScope {
                layer1Agents.map { agentType ->
                    async {
                        executeAgent(agentType, context, emitter)
                    }
                }.awaitAll()
            }
            
            emitter.emit(PipelineEvent.LayerCompleted(1, layer1Results.count { it.isSuccess }))
        }
        
        // 第二层：辅助分析（新闻和风险）
        val layer2Agents = listOf(AgentType.NEWS, AgentType.RISK)
            .filter { config.isAgentEnabled(it) }
        
        if (layer2Agents.isNotEmpty()) {
            emitter.emit(PipelineEvent.LayerStarted(2, "辅助分析"))
            
            val layer2Results = coroutineScope {
                layer2Agents.map { agentType ->
                    async {
                        executeAgent(agentType, context, emitter)
                    }
                }.awaitAll()
            }
            
            emitter.emit(PipelineEvent.LayerCompleted(2, layer2Results.count { it.isSuccess }))
        }
        
        // 第三层：决策
        if (config.isAgentEnabled(AgentType.DECISION)) {
            emitter.emit(PipelineEvent.LayerStarted(3, "综合决策"))
            
            val decisionAgent = getOrCreateAgent(AgentType.DECISION)
            val decisionResult = withTimeoutOrNull(config.agentTimeoutMs) {
                decisionAgent.analyzeSync(context)
            } ?: generateFallbackDecision(context)
            
            emitter.emit(PipelineEvent.AgentCompleted(AgentType.DECISION, decisionResult))
            emitter.emit(PipelineEvent.LayerCompleted(3, if (decisionResult.isSuccess) 1 else 0))
        }
    }
    
    /**
     * 执行单个 Agent
     */
    private suspend fun executeAgent(
        agentType: AgentType,
        context: AgentContext,
        emitter: FlowCollector<PipelineEvent>
    ): AgentResult {
        val agent = getOrCreateAgent(agentType)
        if (!agent.isEnabled) {
            return AgentResult(agentType = agentType, status = AgentStatus.SKIPPED)
        }
        
        emitter.emit(PipelineEvent.AgentStarted(agentType, 0, 0))
        
        return withTimeoutOrNull(context.config.agentTimeoutMs) {
            agent.analyzeSync(context)
        } ?: AgentResult(
            agentType = agentType,
            status = AgentStatus.FAILED,
            error = "执行超时"
        ).also {
            emitter.emit(PipelineEvent.AgentCompleted(agentType, it))
        }
    }
    
    /**
     * 计算投票结果
     */
    private fun calculateVoteResult(
        results: List<AgentResult>,
        config: AgentConfiguration
    ): VoteResult {
        if (results.isEmpty()) {
            return VoteResult(
                signal = Signal.HOLD,
                confidence = 0.0,
                reasoning = "无有效投票",
                votes = emptyMap(),
                agreementRate = 0.0
            )
        }
        
        // 统计各信号的票数
        val voteCounts = mutableMapOf<Signal, Int>()
        var totalWeight = 0.0
        var weightedSum = 0.0
        
        results.forEach { result ->
            result.opinion?.let { opinion ->
                val weight = config.getAgentWeight(opinion.agentType)
                voteCounts[opinion.signal] = (voteCounts[opinion.signal] ?: 0) + 1
                totalWeight += weight
                weightedSum += opinion.signal.score * weight * opinion.confidence
            }
        }
        
        // 找出得票最多的信号
        val winner = voteCounts.maxByOrNull { it.value }?.key ?: Signal.HOLD
        val maxVotes = voteCounts[winner] ?: 0
        val agreementRate = maxVotes.toDouble() / results.size
        
        // 计算加权平均分
        val weightedScore = if (totalWeight > 0) weightedSum / totalWeight else 50.0
        val confidence = (agreementRate * (results.size.coerceAtMost(4) / 4.0)).coerceIn(0.0, 1.0)
        
        val reasoning = buildString {
            appendLine("投票结果：${winner.value} (得票 ${maxVotes}/${results.size})")
            appendLine("意见一致性：${String.format("%.0f%%", agreementRate * 100)}")
            appendLine("加权评分：${String.format("%.0f", weightedScore)}")
            appendLine("各 Agent 意见：")
            results.forEach { result ->
                result.opinion?.let {
                    appendLine("- ${it.agentType.displayName()}: ${it.signal.value} (置信度: ${String.format("%.0f%%", it.confidence * 100)})")
                }
            }
        }
        
        return VoteResult(
            signal = winner,
            confidence = confidence,
            reasoning = reasoning,
            votes = voteCounts.mapKeys { it.key.value },
            agreementRate = agreementRate,
            weightedScore = weightedScore.toInt()
        )
    }
    
    /**
     * 生成降级决策
     */
    private fun generateFallbackDecision(context: AgentContext): AgentResult {
        val weightedScore = context.calculateWeightedScore()
        val signal = Signal.fromScore(weightedScore)
        
        val opinion = AgentOpinion(
            agentType = AgentType.DECISION,
            agentName = "降级决策",
            signal = signal,
            confidence = 0.5f,
            reasoning = "基于各 Agent 加权评分自动生成的决策（LLM决策失败后的降级方案）",
            rawData = mapOf("fallback" to true, "weighted_score" to weightedScore)
        )
        
        context.addOpinion(opinion)
        context.setData("final_decision", opinion)
        
        return AgentResult(
            agentType = AgentType.DECISION,
            status = AgentStatus.COMPLETED,
            opinion = opinion
        )
    }
    
    /**
     * 获取或创建 Agent 实例
     */
    private fun getOrCreateAgent(agentType: AgentType): Agent {
        return agentCache.getOrPut(agentType) {
            when (agentType) {
                AgentType.TECHNICAL -> TechnicalAnalysisAgent(llmService, toolRegistry)
                AgentType.FUNDAMENTAL -> FundamentalAnalysisAgent(llmService, toolRegistry)
                AgentType.NEWS -> NewsAnalysisAgent(llmService, toolRegistry)
                AgentType.RISK -> RiskAssessmentAgent(llmService, toolRegistry)
                AgentType.DECISION -> DecisionAgent(llmService, toolRegistry)
            }
        }
    }
    
    /**
     * 清除 Agent 缓存
     */
    fun clearCache() {
        agentCache.clear()
    }
}

/**
 * 投票结果
 */
data class VoteResult(
    val signal: Signal,
    val confidence: Double,
    val reasoning: String,
    val votes: Map<String, Int>,
    val agreementRate: Double,
    val weightedScore: Int = 50
)

/**
 * 流水线事件
 */
sealed class PipelineEvent {
    /**
     * 流水线开始
     */
    data class Started(
        val stockSymbol: String,
        val mode: ExecutionMode
    ) : PipelineEvent()
    
    /**
     * 层开始
     */
    data class LayerStarted(
        val layer: Int,
        val description: String
    ) : PipelineEvent()
    
    /**
     * 层完成
     */
    data class LayerCompleted(
        val layer: Int,
        val successCount: Int
    ) : PipelineEvent()
    
    /**
     * Agent 开始
     */
    data class AgentStarted(
        val agentType: AgentType,
        val currentStep: Int,
        val totalSteps: Int
    ) : PipelineEvent()
    
    /**
     * Agent 进度更新
     */
    data class AgentProgress(
        val agentType: AgentType,
        val progress: Float,
        val message: String
    ) : PipelineEvent()
    
    /**
     * Agent 完成
     */
    data class AgentCompleted(
        val agentType: AgentType,
        val result: AgentResult
    ) : PipelineEvent()
    
    /**
     * 投票完成
     */
    data class VoteCompleted(
        val voteResult: VoteResult
    ) : PipelineEvent()
    
    /**
     * 流水线完成
     */
    data class Completed(
        val durationMs: Long,
        val context: AgentContext
    ) : PipelineEvent()
    
    /**
     * 错误
     */
    data class Error(
        val message: String,
        val isRecoverable: Boolean
    ) : PipelineEvent()
}

/**
 * 流水线结果
 */
data class PipelineResult(
    val success: Boolean,
    val context: AgentContext,
    val durationMs: Long,
    val completedAgents: List<AgentType>,
    val failedAgents: List<AgentType>,
    val error: String? = null
)

/**
 * Agent 执行状态
 * 用于 UI 展示 Agent 的执行进度和结果
 */
data class AgentExecutionState(
    val agentType: AgentType,
    val status: AgentStatus,
    val signal: Signal? = null,
    val confidence: Float? = null,
    val error: String? = null,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
) {
    /**
     * 获取执行时长（毫秒）
     */
    fun getDuration(): Long {
        return (endTime ?: System.currentTimeMillis()) - startTime
    }
    
    /**
     * 是否已完成（成功或失败）
     */
    fun isCompleted(): Boolean = status.isTerminal()
    
    /**
     * 是否成功完成
     */
    fun isSuccess(): Boolean = status == AgentStatus.COMPLETED
    
    companion object {
        /**
         * 创建初始状态
         */
        fun initial(agentType: AgentType): AgentExecutionState {
            return AgentExecutionState(
                agentType = agentType,
                status = AgentStatus.PENDING
            )
        }
        
        /**
         * 创建运行中状态
         */
        fun running(agentType: AgentType): AgentExecutionState {
            return AgentExecutionState(
                agentType = agentType,
                status = AgentStatus.RUNNING
            )
        }
        
        /**
         * 创建完成状态
         */
        fun completed(
            agentType: AgentType,
            signal: Signal,
            confidence: Float,
            startTime: Long
        ): AgentExecutionState {
            return AgentExecutionState(
                agentType = agentType,
                status = AgentStatus.COMPLETED,
                signal = signal,
                confidence = confidence,
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        }
        
        /**
         * 创建失败状态
         */
        fun failed(agentType: AgentType, error: String, startTime: Long): AgentExecutionState {
            return AgentExecutionState(
                agentType = agentType,
                status = AgentStatus.FAILED,
                error = error,
                startTime = startTime,
                endTime = System.currentTimeMillis()
            )
        }
        
        /**
         * 创建跳过状态
         */
        fun skipped(agentType: AgentType): AgentExecutionState {
            return AgentExecutionState(
                agentType = agentType,
                status = AgentStatus.SKIPPED
            )
        }
    }
}
