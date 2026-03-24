package com.example.stockanalysis.data.agent

import android.util.Log
import com.example.stockanalysis.data.llm.LLMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Date
import kotlin.system.measureTimeMillis

/**
 * Agent 基类
 * 提供所有专业 Agent 的通用功能实现
 */
abstract class BaseAgent(
    protected val llmService: LLMService,
    protected val toolRegistry: AgentToolRegistry
) : Agent {
    
    companion object {
        private const val TAG = "BaseAgent"
    }
    
    override var isEnabled: Boolean = true
    
    /**
     * 执行分析流程
     */
    final override fun analyze(context: AgentContext): Flow<AgentResultEvent> = flow {
        if (!isEnabled) {
            emit(AgentResultEvent.Complete(
                AgentResult(
                    agentType = agentType,
                    status = AgentStatus.SKIPPED,
                    error = "Agent 已禁用"
                )
            ))
            return@flow
        }
        
        if (!shouldExecute(context)) {
            emit(AgentResultEvent.Complete(
                AgentResult(
                    agentType = agentType,
                    status = AgentStatus.SKIPPED,
                    error = "条件不满足，跳过执行"
                )
            ))
            return@flow
        }
        
        val startTime = System.currentTimeMillis()
        var opinion: AgentOpinion? = null
        var error: String? = null
        val toolCalls = mutableListOf<AgentResult.ToolCallInfo>()
        var tokensUsed = 0
        
        try {
            // 1. 预取数据
            emit(AgentResultEvent.Progress(1, 3, "正在收集数据..."))
            prefetchData(context)
            
            // 2. 构建提示词
            emit(AgentResultEvent.Progress(2, 3, "正在分析..."))
            val systemPrompt = getSystemPrompt(context)
            val userPrompt = getUserPrompt(context)
            
            // 3. 调用 LLM
            emit(AgentResultEvent.Thinking("正在生成分析..."))
            val response = callLLM(systemPrompt, userPrompt, context)
            
            // 4. 解析结果
            emit(AgentResultEvent.Progress(3, 3, "正在整理结果..."))
            opinion = postProcess(context, response)
            
            if (opinion != null) {
                context.addOpinion(opinion)
                emit(AgentResultEvent.Complete(
                    AgentResult(
                        agentType = agentType,
                        status = AgentStatus.COMPLETED,
                        opinion = opinion,
                        durationMs = System.currentTimeMillis() - startTime,
                        tokensUsed = tokensUsed,
                        toolCalls = toolCalls
                    )
                ))
            } else {
                error = "无法解析 LLM 响应"
                emit(AgentResultEvent.Complete(
                    AgentResult(
                        agentType = agentType,
                        status = AgentStatus.FAILED,
                        error = error,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                ))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[${agentType.name}] 分析失败", e)
            error = e.message ?: "未知错误"
            emit(AgentResultEvent.Error(error, false))
            emit(AgentResultEvent.Complete(
                AgentResult(
                    agentType = agentType,
                    status = AgentStatus.FAILED,
                    error = error,
                    durationMs = System.currentTimeMillis() - startTime
                )
            ))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 同步执行分析
     */
    final override suspend fun analyzeSync(context: AgentContext): AgentResult = withContext(Dispatchers.IO) {
        var result: AgentResult? = null
        analyze(context).collect { event ->
            if (event is AgentResultEvent.Complete) {
                result = event.result
            }
        }
        result ?: AgentResult(
            agentType = agentType,
            status = AgentStatus.FAILED,
            error = "未收到完成事件"
        )
    }
    
    /**
     * 预取数据
     * 子类可以重写此方法在执行前预取所需数据
     */
    protected open suspend fun prefetchData(context: AgentContext) {
        // 默认空实现，子类可重写
    }
    
    /**
     * 调用 LLM
     */
    protected open suspend fun callLLM(
        systemPrompt: String,
        userPrompt: String,
        context: AgentContext
    ): String {
        val messages = listOf(
            com.example.stockanalysis.data.api.Message(
                role = "system",
                content = systemPrompt
            ),
            com.example.stockanalysis.data.api.Message(
                role = "user",
                content = userPrompt
            )
        )
        
        val result = llmService.chatCompletion(
            messages = messages,
            temperature = 0.3,
            maxTokens = 2000
        )
        
        return result.getOrElse { throw it }
    }
    
    /**
     * 执行工具
     */
    protected suspend fun executeTool(
        toolName: String,
        params: Map<String, Any>,
        context: AgentContext
    ): Result<String> {
        val tool = toolRegistry.getTool(toolName)
            ?: return Result.failure(IllegalArgumentException("工具不存在: $toolName"))
        
        return tool.execute(params)
    }
    
    /**
     * 从 JSON 提取字符串值
     */
    protected fun extractString(json: JSONObject, key: String, default: String = ""): String {
        return try {
            json.optString(key, default)
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * 从 JSON 提取双精度值
     */
    protected fun extractDouble(json: JSONObject, key: String, default: Double = 0.0): Double {
        return try {
            json.optDouble(key, default)
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * 从 JSON 提取整数值
     */
    protected fun extractInt(json: JSONObject, key: String, default: Int = 0): Int {
        return try {
            json.optInt(key, default)
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * 从 JSON 提取布尔值
     */
    protected fun extractBoolean(json: JSONObject, key: String, default: Boolean = false): Boolean {
        return try {
            json.optBoolean(key, default)
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * 从 JSON 提取 JSONObject
     */
    protected fun extractJSONObject(json: JSONObject, key: String): JSONObject? {
        return try {
            json.optJSONObject(key)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 解析信号字符串
     */
    protected fun parseSignal(signalStr: String): Signal {
        return when (signalStr.lowercase()) {
            "strong_buy", "强烈买入" -> Signal.STRONG_BUY
            "buy", "买入" -> Signal.BUY
            "hold", "观望", "持有" -> Signal.HOLD
            "sell", "卖出" -> Signal.SELL
            "strong_sell", "强烈卖出" -> Signal.STRONG_SELL
            else -> Signal.HOLD
        }
    }
    
    /**
     * 验证置信度值
     */
    protected fun validateConfidence(value: Float): Float {
        return value.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * 构建基础系统提示词
     */
    protected fun buildBaseSystemPrompt(): String {
        return """你是一个专业的股票分析 AI Agent。

分析原则：
1. 基于数据和事实进行分析
2. 明确区分事实和观点
3. 提供可量化的结论
4. 客观评估风险
5. 避免模棱两可的建议

输出要求：
1. 必须使用 JSON 格式输出
2. 信号必须是以下之一：strong_buy, buy, hold, sell, strong_sell
3. 置信度必须是 0.0 到 1.0 之间的数值
4. 推理过程必须简洁明了

当前 Agent：$agentName
描述：$description"""
    }
}

/**
 * Agent 执行异常
 */
class AgentExecutionException(
    message: String,
    val agentType: AgentType,
    val isRecoverable: Boolean = true
) : Exception(message)

/**
 * Agent 配置异常
 */
class AgentConfigurationException(
    message: String
) : Exception(message)
