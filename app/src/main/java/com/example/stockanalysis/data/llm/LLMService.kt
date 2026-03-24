package com.example.stockanalysis.data.llm

import android.util.Log
import com.example.stockanalysis.data.api.ChatCompletionRequest
import com.example.stockanalysis.data.api.ChatCompletionResponse
import com.example.stockanalysis.data.api.LLMApiService
import com.example.stockanalysis.data.api.Message
import com.example.stockanalysis.data.local.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LLM 服务 - 多模型支持
 *
 * 功能：
 * 1. 统一的 LLM 调用接口
 * 2. 自动识别提供商
 * 3. 错误处理和重试
 * 4. 日志记录
 *
 * 支持的提供商：
 * - OpenAI (GPT-4, GPT-3.5)
 * - Anthropic (Claude)
 * - Google (Gemini)
 * - Deepseek (Deepseek-V3)
 * - 阿里云 (Qwen)
 * - Ollama (本地模型)
 */
@Singleton
class LLMService @Inject constructor(
    private val llmApiService: LLMApiService,
    private val preferencesManager: PreferencesManager
) {

    companion object {
        const val TAG = "LLMService"
        const val DEFAULT_TEMPERATURE = 0.7
        const val DEFAULT_MAX_TOKENS = 2000
    }

    /**
     * 获取当前配置的提供商
     */
    fun getCurrentProvider(): LLMProvider {
        val baseUrl = preferencesManager.getLLMBaseUrl()
        val model = preferencesManager.getLLMModel()

        return if (baseUrl.isNotEmpty()) {
            LLMProvider.fromBaseUrl(baseUrl)
        } else {
            LLMProvider.fromModelName(model)
        }
    }

    /**
     * 检查配置是否完整
     */
    fun isConfigured(): Boolean {
        val apiKey = preferencesManager.getLLMApiKey()
        val model = preferencesManager.getLLMModel()
        val provider = getCurrentProvider()

        // Ollama 不需要 API Key
        if (provider == LLMProvider.OLLAMA) {
            return model.isNotEmpty()
        }

        return apiKey.isNotEmpty() && model.isNotEmpty()
    }

    /**
     * 聊天完成 - 主要接口
     *
     * @param messages 消息列表
     * @param temperature 温度参数（0-2，越高越随机）
     * @param maxTokens 最大输出 token 数
     * @return 模型响应文本，失败返回 null
     */
    suspend fun chatCompletion(
        messages: List<Message>,
        temperature: Double = DEFAULT_TEMPERATURE,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(
                    Exception("LLM 未配置，请在设置中配置 API Key 和模型")
                )
            }

            val model = preferencesManager.getLLMModel()
            val provider = getCurrentProvider()

            Log.d(TAG, "调用 LLM: provider=$provider, model=$model, messages=${messages.size}")

            val request = ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens,
                stream = false
            )

            val response = llmApiService.chatCompletion(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.choices.isNotEmpty()) {
                    val content = body.choices[0].message.content
                    Log.d(TAG, "LLM 响应成功: ${content.take(100)}...")
                    Result.success(content)
                } else {
                    val error = "LLM 响应为空"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "LLM API 请求失败: ${response.code()} - ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "LLM 调用异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 简化接口 - 单条消息
     */
    suspend fun ask(
        prompt: String,
        systemPrompt: String? = null
    ): Result<String> {
        val messages = mutableListOf<Message>()

        if (systemPrompt != null) {
            messages.add(Message(role = "system", content = systemPrompt))
        }

        messages.add(Message(role = "user", content = prompt))

        return chatCompletion(messages)
    }

    /**
     * 测试连接
     */
    suspend fun testConnection(): Result<String> {
        return try {
            val response = llmApiService.listModels()

            if (response.isSuccessful) {
                val models = response.body()?.data?.map { it.id } ?: emptyList()
                Log.d(TAG, "可用模型: $models")
                Result.success("连接成功，可用模型: ${models.take(3).joinToString(", ")}")
            } else {
                val error = "连接失败: ${response.code()} - ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "测试连接异常: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取提供商特定的配置建议
     */
    fun getProviderConfigHint(provider: LLMProvider): String {
        return when (provider) {
            LLMProvider.OPENAI -> "需要 OpenAI API Key (https://platform.openai.com/api-keys)"
            LLMProvider.ANTHROPIC -> "需要 Anthropic API Key (https://console.anthropic.com/)"
            LLMProvider.GEMINI -> "需要 Google AI Studio API Key (https://aistudio.google.com/apikey)"
            LLMProvider.DEEPSEEK -> "需要 Deepseek API Key (https://platform.deepseek.com/)"
            LLMProvider.QWEN -> "需要阿里云 API Key (https://dashscope.console.aliyun.com/)"
            LLMProvider.KIMI -> "需要 Moonshot API Key (https://platform.moonshot.cn/)"
            LLMProvider.SPARK -> "需要讯飞星火 API Key (https://xinghuo.xfyun.cn/)"
            LLMProvider.ZHIPU -> "需要智谱 API Key (https://open.bigmodel.cn/)"
            LLMProvider.OLLAMA -> "需要本地运行 Ollama 服务 (http://localhost:11434)"
            LLMProvider.CUSTOM -> "请配置自定义 API 端点"
        }
    }

    /**
     * 股票分析专用接口
     *
     * @param stockName 股票名称
     * @param stockCode 股票代码
     * @param technicalSummary 技术分析摘要
     * @param fundamentalSummary 基本面摘要
     * @return AI 分析结果
     */
    suspend fun analyzeStock(
        stockName: String,
        stockCode: String,
        technicalSummary: String,
        fundamentalSummary: String
    ): Result<String> {
        val systemPrompt = """
你是一位专业的股票分析师，擅长结合技术分析和基本面分析给出投资建议。

请基于以下信息分析股票并给出建议：
1. 综合技术面和基本面
2. 评估风险和机会
3. 给出具体的操作建议

请用简洁、专业的语言回答，分为以下几个部分：
- 综合评价（1-2句）
- 关键亮点（2-3点）
- 风险提示（1-2点）
- 操作建议（具体的买入/持有/卖出建议）
        """.trimIndent()

        val userPrompt = """
股票名称：$stockName ($stockCode)

技术分析：
$technicalSummary

基本面分析：
$fundamentalSummary

请给出你的分析和建议。
        """.trimIndent()

        return ask(userPrompt, systemPrompt)
    }

    /**
     * 新闻舆情分析专用接口
     */
    suspend fun analyzeSentiment(
        stockName: String,
        newsItems: List<String>
    ): Result<String> {
        val systemPrompt = """
你是一位专业的市场舆情分析师，擅长从新闻中提取市场情绪和投资机会。

请分析以下新闻对股票的影响：
1. 判断整体舆情（积极/中性/消极）
2. 提取关键信息和潜在催化剂
3. 评估对股价的短期和中期影响
        """.trimIndent()

        val userPrompt = """
股票名称：$stockName

相关新闻：
${newsItems.joinToString("\n\n") { "- $it" }}

请分析这些新闻对股票的影响。
        """.trimIndent()

        return ask(userPrompt, systemPrompt)
    }
}
