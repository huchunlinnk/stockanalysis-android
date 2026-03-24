package com.example.stockanalysis.data.agent

import java.util.Date
import java.util.UUID

/**
 * Agent 间通信的消息格式
 * 用于 Agent 之间的数据传递和上下文共享
 */
sealed class AgentMessage {
    abstract val id: String
    abstract val timestamp: Date
    abstract val fromAgent: AgentType
    abstract val toAgent: AgentType?
    
    /**
     * 分析请求消息
     */
    data class AnalysisRequest(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Date = Date(),
        override val fromAgent: AgentType,
        override val toAgent: AgentType?,
        val stockSymbol: String,
        val stockName: String,
        val query: String,
        val context: Map<String, Any> = emptyMap()
    ) : AgentMessage()
    
    /**
     * 分析结果消息
     */
    data class AnalysisResult(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Date = Date(),
        override val fromAgent: AgentType,
        override val toAgent: AgentType?,
        val success: Boolean,
        val opinion: AgentOpinion?,
        val error: String? = null,
        val metadata: Map<String, Any> = emptyMap()
    ) : AgentMessage()
    
    /**
     * 数据共享消息
     */
    data class DataShare(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Date = Date(),
        override val fromAgent: AgentType,
        override val toAgent: AgentType?,
        val dataType: DataType,
        val data: Any,
        val description: String = ""
    ) : AgentMessage() {
        enum class DataType {
            QUOTE,          // 实时行情
            KLINE,          // K线数据
            INDICATORS,     // 技术指标
            TREND,          // 趋势分析
            NEWS,           // 新闻数据
            FUNDAMENTAL,    // 基本面数据
            CHIP,           // 筹码分布
            RISK_FLAGS,     // 风险标记
            OPINION         // 分析意见
        }
    }
    
    /**
     * 风险告警消息
     */
    data class RiskAlert(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Date = Date(),
        override val fromAgent: AgentType,
        override val toAgent: AgentType?,
        val severity: RiskSeverity,
        val category: String,
        val description: String,
        val impact: String = ""
    ) : AgentMessage() {
        enum class RiskSeverity {
            LOW,      // 低风险
            MEDIUM,   // 中风险
            HIGH;     // 高风险
            
            fun shouldOverrideDecision(): Boolean = this == HIGH
        }
    }
    
    /**
     * 协调消息
     */
    data class Coordination(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Date = Date(),
        override val fromAgent: AgentType,
        override val toAgent: AgentType?,
        val action: CoordinationAction,
        val payload: Map<String, Any> = emptyMap()
    ) : AgentMessage() {
        enum class CoordinationAction {
            START,      // 开始执行
            PAUSE,      // 暂停执行
            RESUME,     // 恢复执行
            CANCEL,     // 取消执行
            SYNC,       // 同步状态
            COMPLETE    // 完成执行
        }
    }
    
    /**
     * 进度更新消息
     */
    data class ProgressUpdate(
        override val id: String = UUID.randomUUID().toString(),
        override val timestamp: Date = Date(),
        override val fromAgent: AgentType,
        override val toAgent: AgentType?,
        val stage: String,
        val progress: Float,  // 0.0 - 1.0
        val message: String = ""
    ) : AgentMessage()
}

/**
 * Agent 意见/分析结果
 */
data class AgentOpinion(
    val agentType: AgentType,
    val agentName: String,
    val signal: Signal,
    val confidence: Float,  // 0.0 - 1.0
    val reasoning: String,
    val keyLevels: KeyLevels? = null,
    val rawData: Map<String, Any> = emptyMap(),
    val timestamp: Date = Date()
) {
    init {
        require(confidence in 0.0f..1.0f) { "Confidence must be between 0.0 and 1.0" }
    }
    
    /**
     * 关键价格水平
     */
    data class KeyLevels(
        val support: Double? = null,
        val resistance: Double? = null,
        val stopLoss: Double? = null,
        val takeProfit: Double? = null,
        val entryPrice: Double? = null
    )
    
    /**
     * 转换为评分 (0-100)
     */
    fun toScore(): Int {
        return (signal.score * confidence).toInt()
    }
    
    companion object {
        /**
         * 创建空意见（用于失败情况）
         */
        fun empty(agentType: AgentType): AgentOpinion {
            return AgentOpinion(
                agentType = agentType,
                agentName = agentType.displayName(),
                signal = Signal.HOLD,
                confidence = 0.0f,
                reasoning = "分析失败或无可用数据"
            )
        }
    }
}

/**
 * Agent 上下文
 * 共享状态和数据容器
 */
data class AgentContext(
    val stockSymbol: String,
    val stockName: String,
    val query: String = "",
    val executionMode: ExecutionMode = ExecutionMode.SEQUENTIAL,
    val sharedData: MutableMap<String, Any> = mutableMapOf(),
    val opinions: MutableList<AgentOpinion> = mutableListOf(),
    val riskAlerts: MutableList<AgentMessage.RiskAlert> = mutableListOf(),
    val messages: MutableList<AgentMessage> = mutableListOf(),
    val config: AgentConfiguration = AgentConfiguration(),
    val createdAt: Date = Date()
) {
    /**
     * 添加数据到共享上下文
     */
    fun setData(key: String, value: Any) {
        sharedData[key] = value
    }
    
    /**
     * 从共享上下文获取数据
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(key: String): T? {
        return sharedData[key] as? T
    }
    
    /**
     * 添加 Agent 意见
     */
    fun addOpinion(opinion: AgentOpinion) {
        opinions.removeAll { it.agentType == opinion.agentType }
        opinions.add(opinion)
    }
    
    /**
     * 获取指定类型的意见
     */
    fun getOpinion(agentType: AgentType): AgentOpinion? {
        return opinions.find { it.agentType == agentType }
    }
    
    /**
     * 添加风险告警
     */
    fun addRiskAlert(alert: AgentMessage.RiskAlert) {
        riskAlerts.add(alert)
    }
    
    /**
     * 是否有高风险告警
     */
    fun hasHighRiskAlert(): Boolean {
        return riskAlerts.any { it.severity == AgentMessage.RiskAlert.RiskSeverity.HIGH }
    }
    
    /**
     * 添加消息
     */
    fun addMessage(message: AgentMessage) {
        messages.add(message)
    }
    
    /**
     * 获取所有非决策 Agent 的意见
     */
    fun getAnalysisOpinions(): List<AgentOpinion> {
        return opinions.filter { it.agentType != AgentType.DECISION }
    }
    
    /**
     * 计算加权平均信号分数
     */
    fun calculateWeightedScore(): Int {
        val analysisOpinions = getAnalysisOpinions()
        if (analysisOpinions.isEmpty()) return 50
        
        val totalWeight = analysisOpinions.sumOf { getAgentWeight(it.agentType) }
        if (totalWeight == 0.0) return 50
        
        val weightedSum = analysisOpinions.sumOf {
            it.signal.score * getAgentWeight(it.agentType) * it.confidence.toDouble()
        }
        
        return (weightedSum / totalWeight).toInt().coerceIn(0, 100)
    }
    
    /**
     * 获取 Agent 权重
     */
    private fun getAgentWeight(agentType: AgentType): Double {
        return config.agentWeights[agentType] ?: when (agentType) {
            AgentType.TECHNICAL -> 0.35
            AgentType.FUNDAMENTAL -> 0.25
            AgentType.NEWS -> 0.20
            AgentType.RISK -> 0.20
            AgentType.DECISION -> 0.0
        }
    }
}

/**
 * Agent 执行结果
 */
data class AgentResult(
    val agentType: AgentType,
    val status: AgentStatus,
    val opinion: AgentOpinion? = null,
    val error: String? = null,
    val durationMs: Long = 0,
    val tokensUsed: Int = 0,
    val toolCalls: List<ToolCallInfo> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
) {
    val isSuccess: Boolean get() = status == AgentStatus.COMPLETED && opinion != null
    
    data class ToolCallInfo(
        val toolName: String,
        val params: Map<String, Any>,
        val result: String,
        val durationMs: Long
    )
}
