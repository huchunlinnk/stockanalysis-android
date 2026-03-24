package com.example.stockanalysis.data.agent

/**
 * Agent 类型枚举
 * 定义系统中所有可用的专业 Agent 类型
 */
enum class AgentType {
    /**
     * 技术面分析 Agent
     * 负责：均线、MACD、KDJ、布林带、趋势分析、支撑阻力位
     */
    TECHNICAL,
    
    /**
     * 基本面分析 Agent
     * 负责：估值分析、成长性、盈利能力、财务健康度
     */
    FUNDAMENTAL,
    
    /**
     * 新闻情报分析 Agent
     * 负责：新闻搜索、舆情分析、市场情绪、催化剂识别
     */
    NEWS,
    
    /**
     * 风险评估 Agent
     * 负责：风险识别、风险等级评估、风险因素监控
     */
    RISK,
    
    /**
     * 决策建议 Agent
     * 负责：综合所有 Agent 意见、生成最终决策、行动计划
     */
    DECISION;
    
    /**
     * 获取显示名称
     */
    fun displayName(): String = when (this) {
        TECHNICAL -> "技术面分析"
        FUNDAMENTAL -> "基本面分析"
        NEWS -> "舆情分析"
        RISK -> "风险评估"
        DECISION -> "决策建议"
    }
    
    /**
     * 获取描述
     */
    fun description(): String = when (this) {
        TECHNICAL -> "分析技术指标、趋势状态和关键价格水平"
        FUNDAMENTAL -> "评估公司估值、财务状况和成长潜力"
        NEWS -> "收集新闻情报，分析市场情绪和催化剂"
        RISK -> "识别投资风险，评估风险等级和影响"
        DECISION -> "综合所有分析结果，生成最终投资建议"
    }
    
    /**
     * 获取优先级（数值越小优先级越高）
     */
    fun priority(): Int = when (this) {
        TECHNICAL -> 1
        FUNDAMENTAL -> 2
        NEWS -> 3
        RISK -> 4
        DECISION -> 5
    }
    
    /**
     * 是否可以并行执行
     */
    fun canParallelExecute(): Boolean = this != DECISION
    
    companion object {
        /**
         * 获取所有可并行执行的 Agent 类型
         */
        fun parallelAgents(): List<AgentType> = values().filter { it.canParallelExecute() }
        
        /**
         * 按优先级排序
         */
        fun sortedByPriority(): List<AgentType> = values().sortedBy { it.priority() }
    }
}

/**
 * Agent 执行模式
 */
enum class ExecutionMode {
    /**
     * 顺序执行模式
     * 按优先级顺序依次执行每个 Agent
     */
    SEQUENTIAL,
    
    /**
     * 并行执行模式
     * 可同时并行执行的 Agent 同时运行
     */
    PARALLEL,
    
    /**
     * 投票模式
     * 多个 Agent 独立分析，通过投票机制决定最终结果
     */
    VOTING,
    
    /**
     * 分层决策模式
     * 第一层：技术+基本面并行
     * 第二层：新闻+风险并行
     * 第三层：决策 Agent 综合
     */
    HIERARCHICAL;
    
    fun displayName(): String = when (this) {
        SEQUENTIAL -> "顺序执行"
        PARALLEL -> "并行执行"
        VOTING -> "投票决策"
        HIERARCHICAL -> "分层决策"
    }
}

/**
 * Agent 状态
 */
enum class AgentStatus {
    PENDING,    // 等待执行
    RUNNING,    // 执行中
    COMPLETED,  // 已完成
    FAILED,     // 失败
    SKIPPED;    // 已跳过
    
    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED || this == SKIPPED
}

/**
 * 交易信号
 */
enum class Signal(val value: String, val score: Int) {
    STRONG_BUY("strong_buy", 90),
    BUY("buy", 75),
    HOLD("hold", 50),
    SELL("sell", 25),
    STRONG_SELL("strong_sell", 10);
    
    companion object {
        fun fromString(value: String): Signal {
            return values().find { it.value == value.lowercase() } ?: HOLD
        }
        
        fun fromScore(score: Int): Signal {
            return when (score) {
                in 80..100 -> STRONG_BUY
                in 60..79 -> BUY
                in 40..59 -> HOLD
                in 20..39 -> SELL
                else -> STRONG_SELL
            }
        }
    }
}
