package com.example.stockanalysis.data.agent

import kotlinx.coroutines.flow.Flow

/**
 * Agent 接口定义
 * 所有专业 Agent 必须实现此接口
 */
interface Agent {
    /**
     * Agent 类型
     */
    val agentType: AgentType
    
    /**
     * Agent 名称
     */
    val agentName: String
    
    /**
     * Agent 描述
     */
    val description: String
    
    /**
     * 是否启用
     */
    var isEnabled: Boolean
    
    /**
     * 最大执行步数
     */
    val maxSteps: Int
    
    /**
     * 执行分析
     * 
     * @param context 分析上下文
     * @return 分析结果流，包含进度更新和最终结果
     */
    fun analyze(context: AgentContext): Flow<AgentResultEvent>
    
    /**
     * 同步执行分析（简化接口）
     * 
     * @param context 分析上下文
     * @return 分析结果
     */
    suspend fun analyzeSync(context: AgentContext): AgentResult
    
    /**
     * 获取系统提示词
     * 
     * @param context 分析上下文
     * @return 系统提示词
     */
    fun getSystemPrompt(context: AgentContext): String
    
    /**
     * 获取用户提示词
     * 
     * @param context 分析上下文
     * @return 用户提示词
     */
    fun getUserPrompt(context: AgentContext): String
    
    /**
     * 后处理 LLM 响应
     * 
     * @param context 分析上下文
     * @param rawResponse 原始 LLM 响应
     * @return 解析后的 Agent 意见
     */
    suspend fun postProcess(context: AgentContext, rawResponse: String): AgentOpinion?
    
    /**
     * 检查是否需要执行
     * 
     * @param context 分析上下文
     * @return 是否需要执行
     */
    fun shouldExecute(context: AgentContext): Boolean = true
    
    /**
     * 获取依赖的 Agent 类型列表
     * 这些 Agent 必须先于当前 Agent 执行
     */
    fun getDependencies(): List<AgentType> = emptyList()
    
    /**
     * 获取可用的工具列表
     */
    fun getAvailableTools(): List<String>
}

/**
 * Agent 结果事件
 */
sealed class AgentResultEvent {
    /**
     * 进度更新
     */
    data class Progress(
        val step: Int,
        val totalSteps: Int,
        val message: String,
        val progress: Float = step.toFloat() / totalSteps.toFloat()
    ) : AgentResultEvent()
    
    /**
     * 工具调用
     */
    data class ToolCall(
        val toolName: String,
        val params: Map<String, Any>
    ) : AgentResultEvent()
    
    /**
     * 工具调用结果
     */
    data class ToolResult(
        val toolName: String,
        val success: Boolean,
        val result: String
    ) : AgentResultEvent()
    
    /**
     * LLM 思考过程
     */
    data class Thinking(
        val content: String
    ) : AgentResultEvent()
    
    /**
     * 分析完成
     */
    data class Complete(
        val result: AgentResult
    ) : AgentResultEvent()
    
    /**
     * 发生错误
     */
    data class Error(
        val error: String,
        val isRecoverable: Boolean = true
    ) : AgentResultEvent()
}

/**
 * Agent 回调接口
 * 用于外部监听 Agent 执行过程
 */
interface AgentCallback {
    /**
     * Agent 开始执行
     */
    fun onAgentStart(agentType: AgentType, context: AgentContext)
    
    /**
     * Agent 进度更新
     */
    fun onAgentProgress(agentType: AgentType, progress: Float, message: String)
    
    /**
     * Agent 调用工具
     */
    fun onToolCall(agentType: AgentType, toolName: String, params: Map<String, Any>)
    
    /**
     * Agent 获得工具结果
     */
    fun onToolResult(agentType: AgentType, toolName: String, success: Boolean, result: String)
    
    /**
     * Agent 完成执行
     */
    fun onAgentComplete(agentType: AgentType, result: AgentResult)
    
    /**
     * Agent 执行失败
     */
    fun onAgentError(agentType: AgentType, error: String)
}

/**
 * 空实现的 AgentCallback
 */
open class EmptyAgentCallback : AgentCallback {
    override fun onAgentStart(agentType: AgentType, context: AgentContext) {}
    override fun onAgentProgress(agentType: AgentType, progress: Float, message: String) {}
    override fun onToolCall(agentType: AgentType, toolName: String, params: Map<String, Any>) {}
    override fun onToolResult(agentType: AgentType, toolName: String, success: Boolean, result: String) {}
    override fun onAgentComplete(agentType: AgentType, result: AgentResult) {}
    override fun onAgentError(agentType: AgentType, error: String) {}
}
