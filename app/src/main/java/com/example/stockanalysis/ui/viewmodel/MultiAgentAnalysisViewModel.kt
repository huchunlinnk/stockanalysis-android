package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.repository.AnalysisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 多 Agent 分析 ViewModel
 * 管理多 Agent 分析流程和可视化展示
 */
@HiltViewModel
class MultiAgentAnalysisViewModel @Inject constructor(
    private val analysisEngine: AnalysisEngine,
    private val agentOrchestrator: AgentOrchestrator,
    private val configManager: AgentConfigurationManager
) : ViewModel() {

    // 分析状态
    private val _analysisState = MutableStateFlow<MultiAgentAnalysisUiState>(MultiAgentAnalysisUiState.Idle)
    val analysisState: StateFlow<MultiAgentAnalysisUiState> = _analysisState.asStateFlow()

    // Agent 执行状态
    private val _agentStates = MutableStateFlow<Map<AgentType, com.example.stockanalysis.data.agent.AgentExecutionState>>(emptyMap())
    val agentStates: StateFlow<Map<AgentType, com.example.stockanalysis.data.agent.AgentExecutionState>> = _agentStates.asStateFlow()

    // 分析结果
    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    // 所有 Agent 意见
    private val _allOpinions = MutableStateFlow<List<AgentOpinion>>(emptyList())
    val allOpinions: StateFlow<List<AgentOpinion>> = _allOpinions.asStateFlow()

    // 投票结果
    private val _voteResult = MutableStateFlow<VoteResultDisplay?>(null)
    val voteResult: StateFlow<VoteResultDisplay?> = _voteResult.asStateFlow()

    // 执行模式
    private val _executionMode = MutableStateFlow(ExecutionMode.HIERARCHICAL)
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    // 当前进度
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 执行统计
    private val _executionStats = MutableStateFlow<ExecutionStats?>(null)
    val executionStats: StateFlow<ExecutionStats?> = _executionStats.asStateFlow()

    // 可用配置
    private val _availableModes = MutableStateFlow(agentOrchestrator.getAvailableModes())
    val availableModes: StateFlow<List<PresetModeInfo>> = _availableModes.asStateFlow()

    private val _availableAgents = MutableStateFlow(agentOrchestrator.getAvailableAgents())
    val availableAgents: StateFlow<List<AgentInfo>> = _availableAgents.asStateFlow()

    init {
        loadConfiguration()
    }

    /**
     * 加载配置
     */
    private fun loadConfiguration() {
        val config = configManager.loadConfiguration()
        _executionMode.value = config.executionMode
    }

    /**
     * 开始多 Agent 分析
     */
    fun startAnalysis(stockSymbol: String, stockName: String, customConfig: AgentConfiguration? = null) {
        viewModelScope.launch {
            _analysisState.value = MultiAgentAnalysisUiState.Analyzing
            _agentStates.value = emptyMap()
            _allOpinions.value = emptyList()
            _voteResult.value = null
            _progress.value = 0f
            _errorMessage.value = null
            _executionStats.value = null

            val startTime = System.currentTimeMillis()
            var completedAgents = 0
            var failedAgents = 0

            analysisEngine.analyzeStock(
                symbol = stockSymbol,
                stockName = stockName,
                useMultiAgent = true,
                config = customConfig
            ).collect { state ->
                when (state) {
                    is com.example.stockanalysis.data.repository.AnalysisState.Loading -> {
                        _analysisState.value = MultiAgentAnalysisUiState.Analyzing
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Progress -> {
                        _progress.value = state.progress
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.AgentResult -> {
                        // 更新 Agent 状态
                        updateAgentState(state.agentType, state)
                        _allOpinions.value = _allOpinions.value + AgentOpinion(
                            agentType = state.agentType,
                            agentName = state.agentType.displayName(),
                            signal = state.signal,
                            confidence = state.confidence,
                            reasoning = state.reasoning
                        )
                        completedAgents++
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Success -> {
                        val duration = System.currentTimeMillis() - startTime
                        _analysisResult.value = state.result
                        _executionStats.value = ExecutionStats(
                            totalDurationMs = duration,
                            completedAgents = completedAgents,
                            failedAgents = failedAgents
                        )
                        _analysisState.value = MultiAgentAnalysisUiState.Completed
                        _progress.value = 1f
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Error -> {
                        _errorMessage.value = state.message
                        _analysisState.value = MultiAgentAnalysisUiState.Error(state.message)
                    }
                }
            }
        }
    }

    /**
     * 更新 Agent 执行状态
     */
    private fun updateAgentState(agentType: AgentType, result: com.example.stockanalysis.data.repository.AnalysisState.AgentResult) {
        val currentStates = _agentStates.value.toMutableMap()
        currentStates[agentType] = com.example.stockanalysis.data.agent.AgentExecutionState(
            agentType = agentType,
            status = AgentStatus.COMPLETED,
            signal = result.signal,
            confidence = result.confidence
        )
        _agentStates.value = currentStates
    }

    /**
     * 设置执行模式
     */
    fun setExecutionMode(mode: ExecutionMode) {
        _executionMode.value = mode
        agentOrchestrator.setExecutionMode(mode)
    }

    /**
     * 应用预设模式
     */
    fun applyPresetMode(mode: PresetMode) {
        agentOrchestrator.applyPresetMode(mode)
        loadConfiguration()
    }

    /**
     * 设置 Agent 启用状态
     */
    fun setAgentEnabled(agentType: AgentType, enabled: Boolean) {
        agentOrchestrator.setAgentEnabled(agentType, enabled)
        _availableAgents.value = agentOrchestrator.getAvailableAgents()
    }

    /**
     * 设置 Agent 权重
     */
    fun setAgentWeight(agentType: AgentType, weight: Double) {
        agentOrchestrator.setAgentWeight(agentType, weight)
        _availableAgents.value = agentOrchestrator.getAvailableAgents()
    }

    /**
     * 获取当前配置
     */
    fun getCurrentConfiguration(): AgentConfiguration {
        return agentOrchestrator.getCurrentConfiguration()
    }

    /**
     * 刷新配置
     */
    fun refreshConfiguration() {
        loadConfiguration()
        _availableAgents.value = agentOrchestrator.getAvailableAgents()
    }

    /**
     * 重置所有状态
     */
    fun reset() {
        _analysisState.value = MultiAgentAnalysisUiState.Idle
        _agentStates.value = emptyMap()
        _analysisResult.value = null
        _allOpinions.value = emptyList()
        _voteResult.value = null
        _progress.value = 0f
        _errorMessage.value = null
        _executionStats.value = null
    }

    /**
     * 获取 Agent 信号颜色
     */
    fun getSignalColor(signal: Signal): Long {
        return when (signal) {
            Signal.STRONG_BUY -> 0xFF4CAF50L  // 绿色
            Signal.BUY -> 0xFF8BC34AL         // 浅绿
            Signal.HOLD -> 0xFFFF9800L        // 橙色
            Signal.SELL -> 0xFFFF5722L        // 红色
            Signal.STRONG_SELL -> 0xFFD32F2FL // 深红
        }
    }

    /**
     * 获取 Agent 状态颜色
     */
    fun getAgentStatusColor(status: AgentStatus): Long {
        return when (status) {
            AgentStatus.PENDING -> 0xFF9E9E9EL   // 灰色
            AgentStatus.RUNNING -> 0xFF2196F3L  // 蓝色
            AgentStatus.COMPLETED -> 0xFF4CAF50L // 绿色
            AgentStatus.FAILED -> 0xFFF44336L    // 红色
            AgentStatus.SKIPPED -> 0xFF9E9E9EL   // 灰色
        }
    }
}

/**
 * 多 Agent 分析 UI 状态
 */
sealed class MultiAgentAnalysisUiState {
    object Idle : MultiAgentAnalysisUiState()
    object Analyzing : MultiAgentAnalysisUiState()
    object Completed : MultiAgentAnalysisUiState()
    data class Error(val message: String) : MultiAgentAnalysisUiState()
}



/**
 * 投票结果展示
 */
data class VoteResultDisplay(
    val signal: Signal,
    val confidence: Double,
    val reasoning: String,
    val votes: Map<String, Int>,
    val agreementRate: Double,
    val weightedScore: Int
)

/**
 * 执行统计
 */
data class ExecutionStats(
    val totalDurationMs: Long,
    val completedAgents: Int,
    val failedAgents: Int
) {
    val durationSeconds: Double get() = totalDurationMs / 1000.0
    val successRate: Float get() = 
        if (completedAgents + failedAgents > 0) {
            completedAgents.toFloat() / (completedAgents + failedAgents)
        } else 0f
}
