package com.example.stockanalysis.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

/**
 * LLM API 服务
 * 支持 OpenAI、Gemini、Claude 等兼容接口
 */
interface LLMApiService {
    
    /**
     * OpenAI 兼容接口 - Chat Completions
     */
    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body request: ChatCompletionRequest): Response<ChatCompletionResponse>
    
    /**
     * 测试 API 连接
     */
    @GET("v1/models")
    suspend fun listModels(): Response<ModelsResponse>
}

/**
 * 聊天完成请求
 */
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 2000,
    val stream: Boolean = false
)

/**
 * 消息
 */
data class Message(
    val role: String,  // system, user, assistant
    val content: String
)

/**
 * 聊天完成响应
 */
data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: Message,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

/**
 * 模型列表响应
 */
data class ModelsResponse(
    val data: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    val created: Long,
    val ownedBy: String
)
