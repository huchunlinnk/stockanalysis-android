package com.example.stockanalysis.data.agent

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent 配置
 * 管理 Agent 参数和编排策略
 */
data class AgentConfiguration(
    // 启用的 Agent 类型
    val enabledAgents: Set<AgentType> = AgentType.values().toSet(),
    
    // Agent 权重配置（用于加权决策）
    val agentWeights: Map<AgentType, Double> = mapOf(
        AgentType.TECHNICAL to 0.35,
        AgentType.FUNDAMENTAL to 0.25,
        AgentType.NEWS to 0.20,
        AgentType.RISK to 0.20,
        AgentType.DECISION to 0.0  // 决策 Agent 不参与加权
    ),
    
    // 执行模式
    val executionMode: ExecutionMode = ExecutionMode.HIERARCHICAL,
    
    // 超时配置（毫秒）
    val agentTimeoutMs: Long = 30000L,
    val totalTimeoutMs: Long = 120000L,
    
    // 是否允许降级执行
    val allowDegradedExecution: Boolean = true,
    
    // 最低需要的 Agent 完成数
    val minRequiredAgents: Int = 2,
    
    // 是否启用投票模式
    val enableVoting: Boolean = false,
    
    // 投票阈值（超过此值才通过）
    val votingThreshold: Double = 0.6,
    
    // 是否启用 Agent 间通信
    val enableInterAgentMessaging: Boolean = true,
    
    // 是否自动降级高风险信号
    val autoDowngradeRiskySignals: Boolean = true,
    
    // LLM 配置
    val llmTemperature: Double = 0.3,
    val llmMaxTokens: Int = 2000,
    
    // 调试模式
    val debugMode: Boolean = false
) {
    /**
     * 检查 Agent 是否启用
     */
    fun isAgentEnabled(agentType: AgentType): Boolean {
        return enabledAgents.contains(agentType)
    }
    
    /**
     * 获取 Agent 权重
     */
    fun getAgentWeight(agentType: AgentType): Double {
        return agentWeights[agentType] ?: 0.0
    }
    
    /**
     * 设置 Agent 启用状态
     */
    fun setAgentEnabled(agentType: AgentType, enabled: Boolean): AgentConfiguration {
        val newEnabledAgents = if (enabled) {
            enabledAgents + agentType
        } else {
            enabledAgents - agentType
        }
        return copy(enabledAgents = newEnabledAgents)
    }
    
    /**
     * 设置 Agent 权重
     */
    fun setAgentWeight(agentType: AgentType, weight: Double): AgentConfiguration {
        val newWeights = agentWeights.toMutableMap()
        newWeights[agentType] = weight.coerceIn(0.0, 1.0)
        return copy(agentWeights = newWeights)
    }
    
    /**
     * 获取启用的分析 Agent（不包括决策 Agent）
     */
    fun getEnabledAnalysisAgents(): List<AgentType> {
        return enabledAgents.filter { it != AgentType.DECISION }.sortedBy { it.priority() }
    }
    
    /**
     * 验证配置有效性
     */
    fun validate(): Boolean {
        // 至少需要一个分析 Agent 启用
        if (getEnabledAnalysisAgents().isEmpty()) {
            return false
        }
        // 如果需要决策，决策 Agent 必须启用
        if (!enabledAgents.contains(AgentType.DECISION)) {
            return false
        }
        return true
    }
    
    companion object {
        /**
         * 快速模式配置
         * 仅使用技术面分析
         */
        fun quickMode(): AgentConfiguration = AgentConfiguration(
            enabledAgents = setOf(AgentType.TECHNICAL, AgentType.DECISION),
            executionMode = ExecutionMode.SEQUENTIAL,
            agentWeights = mapOf(
                AgentType.TECHNICAL to 1.0,
                AgentType.FUNDAMENTAL to 0.0,
                AgentType.NEWS to 0.0,
                AgentType.RISK to 0.0,
                AgentType.DECISION to 0.0
            ),
            agentTimeoutMs = 20000L,
            totalTimeoutMs = 60000L
        )
        
        /**
         * 标准模式配置
         * 技术+舆情+决策
         */
        fun standardMode(): AgentConfiguration = AgentConfiguration(
            enabledAgents = setOf(AgentType.TECHNICAL, AgentType.NEWS, AgentType.DECISION),
            executionMode = ExecutionMode.PARALLEL,
            agentWeights = mapOf(
                AgentType.TECHNICAL to 0.60,
                AgentType.FUNDAMENTAL to 0.0,
                AgentType.NEWS to 0.40,
                AgentType.RISK to 0.0,
                AgentType.DECISION to 0.0
            ),
            agentTimeoutMs = 25000L,
            totalTimeoutMs = 90000L
        )
        
        /**
         * 完整模式配置
         * 所有 Agent 参与
         */
        fun fullMode(): AgentConfiguration = AgentConfiguration(
            enabledAgents = AgentType.values().toSet(),
            executionMode = ExecutionMode.HIERARCHICAL,
            agentWeights = mapOf(
                AgentType.TECHNICAL to 0.35,
                AgentType.FUNDAMENTAL to 0.25,
                AgentType.NEWS to 0.20,
                AgentType.RISK to 0.20,
                AgentType.DECISION to 0.0
            ),
            agentTimeoutMs = 30000L,
            totalTimeoutMs = 120000L
        )
        
        /**
         * 投票模式配置
         */
        fun votingMode(): AgentConfiguration = AgentConfiguration(
            enabledAgents = setOf(AgentType.TECHNICAL, AgentType.FUNDAMENTAL, AgentType.NEWS),
            executionMode = ExecutionMode.VOTING,
            enableVoting = true,
            votingThreshold = 0.6,
            agentTimeoutMs = 25000L,
            totalTimeoutMs = 90000L
        )
    }
}

/**
 * Agent 配置管理器
 * 负责配置的持久化和加载
 */
@Singleton
class AgentConfigurationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREF_NAME = "agent_configuration"
        private const val KEY_ENABLED_AGENTS = "enabled_agents"
        private const val KEY_EXECUTION_MODE = "execution_mode"
        private const val KEY_AGENT_TIMEOUT = "agent_timeout"
        private const val KEY_TOTAL_TIMEOUT = "total_timeout"
        private const val KEY_ALLOW_DEGRADED = "allow_degraded"
        private const val KEY_ENABLE_VOTING = "enable_voting"
        private const val KEY_VOTING_THRESHOLD = "voting_threshold"
        private const val KEY_DEBUG_MODE = "debug_mode"
        private const val KEY_PRESET_MODE = "preset_mode"
    }
    
    /**
     * 加载配置
     */
    fun loadConfiguration(): AgentConfiguration {
        val enabledAgentsStr = prefs.getString(KEY_ENABLED_AGENTS, null)
        val enabledAgents = enabledAgentsStr?.split(",")?.mapNotNull { 
            try {
                AgentType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }?.toSet() ?: AgentType.values().toSet()
        
        val executionMode = try {
            ExecutionMode.valueOf(prefs.getString(KEY_EXECUTION_MODE, ExecutionMode.HIERARCHICAL.name)!!)
        } catch (e: Exception) {
            ExecutionMode.HIERARCHICAL
        }
        
        return AgentConfiguration(
            enabledAgents = enabledAgents,
            executionMode = executionMode,
            agentTimeoutMs = prefs.getLong(KEY_AGENT_TIMEOUT, 30000L),
            totalTimeoutMs = prefs.getLong(KEY_TOTAL_TIMEOUT, 120000L),
            allowDegradedExecution = prefs.getBoolean(KEY_ALLOW_DEGRADED, true),
            enableVoting = prefs.getBoolean(KEY_ENABLE_VOTING, false),
            votingThreshold = prefs.getFloat(KEY_VOTING_THRESHOLD, 0.6f).toDouble(),
            debugMode = prefs.getBoolean(KEY_DEBUG_MODE, false)
        )
    }
    
    /**
     * 保存配置
     */
    fun saveConfiguration(config: AgentConfiguration) {
        prefs.edit().apply {
            putString(KEY_ENABLED_AGENTS, config.enabledAgents.joinToString(",") { it.name })
            putString(KEY_EXECUTION_MODE, config.executionMode.name)
            putLong(KEY_AGENT_TIMEOUT, config.agentTimeoutMs)
            putLong(KEY_TOTAL_TIMEOUT, config.totalTimeoutMs)
            putBoolean(KEY_ALLOW_DEGRADED, config.allowDegradedExecution)
            putBoolean(KEY_ENABLE_VOTING, config.enableVoting)
            putFloat(KEY_VOTING_THRESHOLD, config.votingThreshold.toFloat())
            putBoolean(KEY_DEBUG_MODE, config.debugMode)
            apply()
        }
    }
    
    /**
     * 应用预设模式
     */
    fun applyPresetMode(mode: PresetMode) {
        val config = when (mode) {
            PresetMode.QUICK -> AgentConfiguration.quickMode()
            PresetMode.STANDARD -> AgentConfiguration.standardMode()
            PresetMode.FULL -> AgentConfiguration.fullMode()
            PresetMode.VOTING -> AgentConfiguration.votingMode()
        }
        saveConfiguration(config)
        prefs.edit().putString(KEY_PRESET_MODE, mode.name).apply()
    }
    
    /**
     * 获取当前预设模式
     */
    fun getCurrentPresetMode(): PresetMode {
        return try {
            PresetMode.valueOf(prefs.getString(KEY_PRESET_MODE, PresetMode.FULL.name)!!)
        } catch (e: Exception) {
            PresetMode.FULL
        }
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        saveConfiguration(AgentConfiguration())
    }
}

/**
 * 预设模式
 */
enum class PresetMode {
    QUICK,      // 快速模式
    STANDARD,   // 标准模式
    FULL,       // 完整模式
    VOTING;     // 投票模式
    
    fun displayName(): String = when (this) {
        QUICK -> "快速模式"
        STANDARD -> "标准模式"
        FULL -> "完整模式"
        VOTING -> "投票模式"
    }
    
    fun description(): String = when (this) {
        QUICK -> "仅技术分析，最快完成"
        STANDARD -> "技术+舆情，平衡速度和质量"
        FULL -> "全维度分析，最全面"
        VOTING -> "多Agent投票，集体决策"
    }
}
