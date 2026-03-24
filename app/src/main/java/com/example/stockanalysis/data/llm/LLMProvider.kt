package com.example.stockanalysis.data.llm

/**
 * LLM 提供商枚举
 *
 * 支持的模型提供商：
 * - OpenAI (GPT-4, GPT-3.5)
 * - Anthropic (Claude)
 * - Google (Gemini)
 * - Deepseek (Deepseek-V3)
 * - 阿里云 (Qwen)
 * - Moonshot (Kimi)
 * - 讯飞星火 (Spark)
 * - 智谱GLM (Zhipu)
 * - Ollama (本地模型)
 */
enum class LLMProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val requiresApiKey: Boolean
) {
    OPENAI(
        displayName = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/",
        requiresApiKey = true
    ),

    ANTHROPIC(
        displayName = "Anthropic (Claude)",
        defaultBaseUrl = "https://api.anthropic.com/",
        requiresApiKey = true
    ),

    GEMINI(
        displayName = "Google (Gemini)",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/",
        requiresApiKey = true
    ),

    DEEPSEEK(
        displayName = "Deepseek",
        defaultBaseUrl = "https://api.deepseek.com/",
        requiresApiKey = true
    ),

    QWEN(
        displayName = "阿里云 (Qwen)",
        defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/",
        requiresApiKey = true
    ),

    KIMI(
        displayName = "Moonshot (Kimi)",
        defaultBaseUrl = "https://api.moonshot.cn/",
        requiresApiKey = true
    ),

    SPARK(
        displayName = "讯飞星火",
        defaultBaseUrl = "https://spark-api.xf-yun.com/",
        requiresApiKey = true
    ),

    ZHIPU(
        displayName = "智谱GLM",
        defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4/",
        requiresApiKey = true
    ),

    OLLAMA(
        displayName = "Ollama (本地)",
        defaultBaseUrl = "http://localhost:11434/",
        requiresApiKey = false
    ),

    CUSTOM(
        displayName = "自定义",
        defaultBaseUrl = "",
        requiresApiKey = true
    );

    companion object {
        /**
         * 从 Base URL 推断提供商
         */
        fun fromBaseUrl(baseUrl: String): LLMProvider {
            return when {
                baseUrl.contains("openai.com") -> OPENAI
                baseUrl.contains("anthropic.com") -> ANTHROPIC
                baseUrl.contains("generativelanguage.googleapis.com") -> GEMINI
                baseUrl.contains("deepseek.com") -> DEEPSEEK
                baseUrl.contains("dashscope.aliyuncs.com") -> QWEN
                baseUrl.contains("moonshot.cn") -> KIMI
                baseUrl.contains("xf-yun.com") || baseUrl.contains("spark-api") -> SPARK
                baseUrl.contains("bigmodel.cn") -> ZHIPU
                baseUrl.contains("localhost:11434") || baseUrl.contains("ollama") -> OLLAMA
                else -> CUSTOM
            }
        }

        /**
         * 从模型名称推断提供商
         */
        fun fromModelName(model: String): LLMProvider {
            return when {
                model.startsWith("gpt-") -> OPENAI
                model.startsWith("claude-") -> ANTHROPIC
                model.startsWith("gemini-") -> GEMINI
                model.startsWith("deepseek-") -> DEEPSEEK
                model.startsWith("qwen-") -> QWEN
                model.startsWith("moonshot-") -> KIMI
                model.startsWith("general") || model.startsWith("spark-") -> SPARK
                model.startsWith("glm-") || model.startsWith("chatglm-") -> ZHIPU
                else -> CUSTOM
            }
        }
    }
}

/**
 * 推荐的模型配置
 */
data class RecommendedModel(
    val provider: LLMProvider,
    val modelName: String,
    val description: String,
    val contextWindow: Int,
    val costTier: CostTier
)

enum class CostTier {
    FREE,
    LOW,
    MEDIUM,
    HIGH
}

/**
 * 获取提供商的默认推荐模型
 */
fun LLMProvider.getDefaultModel(): String {
    return when (this) {
        LLMProvider.OPENAI -> "gpt-4o"
        LLMProvider.ANTHROPIC -> "claude-3-5-sonnet-20241022"
        LLMProvider.GEMINI -> "gemini-2.0-flash-exp"
        LLMProvider.DEEPSEEK -> "deepseek-chat"
        LLMProvider.QWEN -> "qwen-max"
        LLMProvider.KIMI -> "moonshot-v1-8k"
        LLMProvider.SPARK -> "generalv3.5"
        LLMProvider.ZHIPU -> "glm-4"
        LLMProvider.OLLAMA -> "llama2"
        LLMProvider.CUSTOM -> ""
    }
}

/**
 * 获取提供商的模型列表
 */
fun LLMProvider.getDefaultModels(): List<String> {
    return when (this) {
        LLMProvider.DEEPSEEK -> listOf("deepseek-chat", "deepseek-reasoner")
        LLMProvider.QWEN -> listOf("qwen-max", "qwen-plus", "qwen-turbo", "qwen-long")
        LLMProvider.KIMI -> listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
        LLMProvider.SPARK -> listOf("generalv3.5", "generalv3", "pro-128k")
        LLMProvider.ZHIPU -> listOf("glm-4", "glm-4-plus", "glm-4-air", "glm-4-flash")
        LLMProvider.GEMINI -> listOf("gemini-2.0-flash-exp", "gemini-1.5-pro", "gemini-1.5-flash")
        LLMProvider.OPENAI -> listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo")
        LLMProvider.ANTHROPIC -> listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")
        LLMProvider.OLLAMA -> listOf("llama2", "mistral", "qwen:7b", "gemma")
        LLMProvider.CUSTOM -> listOf("custom-model")
    }
}

/**
 * 获取推荐的模型列表
 */
fun getRecommendedModels(): List<RecommendedModel> {
    return listOf(
        // OpenAI
        RecommendedModel(
            provider = LLMProvider.OPENAI,
            modelName = "gpt-4o",
            description = "最新的 GPT-4 优化版本，性能强大",
            contextWindow = 128000,
            costTier = CostTier.HIGH
        ),
        RecommendedModel(
            provider = LLMProvider.OPENAI,
            modelName = "gpt-4o-mini",
            description = "轻量级版本，性价比高",
            contextWindow = 128000,
            costTier = CostTier.LOW
        ),
        RecommendedModel(
            provider = LLMProvider.OPENAI,
            modelName = "gpt-3.5-turbo",
            description = "经典模型，速度快",
            contextWindow = 16385,
            costTier = CostTier.LOW
        ),

        // Anthropic
        RecommendedModel(
            provider = LLMProvider.ANTHROPIC,
            modelName = "claude-3-5-sonnet-20241022",
            description = "Claude 3.5 Sonnet，平衡性能和成本",
            contextWindow = 200000,
            costTier = CostTier.MEDIUM
        ),
        RecommendedModel(
            provider = LLMProvider.ANTHROPIC,
            modelName = "claude-3-5-haiku-20241022",
            description = "Claude 3.5 Haiku，速度最快",
            contextWindow = 200000,
            costTier = CostTier.LOW
        ),

        // Deepseek
        RecommendedModel(
            provider = LLMProvider.DEEPSEEK,
            modelName = "deepseek-chat",
            description = "Deepseek V3，性价比极高",
            contextWindow = 64000,
            costTier = CostTier.LOW
        ),
        RecommendedModel(
            provider = LLMProvider.DEEPSEEK,
            modelName = "deepseek-reasoner",
            description = "Deepseek R1，推理能力强",
            contextWindow = 64000,
            costTier = CostTier.LOW
        ),

        // Qwen
        RecommendedModel(
            provider = LLMProvider.QWEN,
            modelName = "qwen-max",
            description = "通义千问旗舰版",
            contextWindow = 32000,
            costTier = CostTier.MEDIUM
        ),
        RecommendedModel(
            provider = LLMProvider.QWEN,
            modelName = "qwen-plus",
            description = "通义千问标准版",
            contextWindow = 32000,
            costTier = CostTier.LOW
        ),
        RecommendedModel(
            provider = LLMProvider.QWEN,
            modelName = "qwen-turbo",
            description = "通义千问极速版",
            contextWindow = 32000,
            costTier = CostTier.LOW
        ),

        // Kimi
        RecommendedModel(
            provider = LLMProvider.KIMI,
            modelName = "moonshot-v1-8k",
            description = "Kimi 8K上下文版本",
            contextWindow = 8000,
            costTier = CostTier.LOW
        ),
        RecommendedModel(
            provider = LLMProvider.KIMI,
            modelName = "moonshot-v1-32k",
            description = "Kimi 32K上下文版本",
            contextWindow = 32000,
            costTier = CostTier.MEDIUM
        ),
        RecommendedModel(
            provider = LLMProvider.KIMI,
            modelName = "moonshot-v1-128k",
            description = "Kimi 128K长文本版本",
            contextWindow = 128000,
            costTier = CostTier.MEDIUM
        ),

        // 讯飞星火
        RecommendedModel(
            provider = LLMProvider.SPARK,
            modelName = "generalv3.5",
            description = "讯飞星火V3.5，中文理解强",
            contextWindow = 8000,
            costTier = CostTier.MEDIUM
        ),
        RecommendedModel(
            provider = LLMProvider.SPARK,
            modelName = "generalv3",
            description = "讯飞星火V3.0",
            contextWindow = 8000,
            costTier = CostTier.LOW
        ),
        RecommendedModel(
            provider = LLMProvider.SPARK,
            modelName = "pro-128k",
            description = "讯飞星火Pro 128K",
            contextWindow = 128000,
            costTier = CostTier.MEDIUM
        ),

        // 智谱GLM
        RecommendedModel(
            provider = LLMProvider.ZHIPU,
            modelName = "glm-4",
            description = "智谱GLM-4旗舰模型",
            contextWindow = 128000,
            costTier = CostTier.MEDIUM
        ),
        RecommendedModel(
            provider = LLMProvider.ZHIPU,
            modelName = "glm-4-plus",
            description = "智谱GLM-4 Plus增强版",
            contextWindow = 128000,
            costTier = CostTier.MEDIUM
        ),
        RecommendedModel(
            provider = LLMProvider.ZHIPU,
            modelName = "glm-4-air",
            description = "智谱GLM-4 Air轻量版",
            contextWindow = 128000,
            costTier = CostTier.LOW
        ),
        RecommendedModel(
            provider = LLMProvider.ZHIPU,
            modelName = "glm-4-flash",
            description = "智谱GLM-4 Flash极速版",
            contextWindow = 128000,
            costTier = CostTier.FREE
        ),

        // Gemini
        RecommendedModel(
            provider = LLMProvider.GEMINI,
            modelName = "gemini-2.0-flash-exp",
            description = "Gemini 2.0 Flash，速度极快",
            contextWindow = 1000000,
            costTier = CostTier.FREE
        ),
        RecommendedModel(
            provider = LLMProvider.GEMINI,
            modelName = "gemini-1.5-pro",
            description = "Gemini 1.5 Pro，功能强大",
            contextWindow = 2000000,
            costTier = CostTier.MEDIUM
        ),
        RecommendedModel(
            provider = LLMProvider.GEMINI,
            modelName = "gemini-1.5-flash",
            description = "Gemini 1.5 Flash，轻量快速",
            contextWindow = 1000000,
            costTier = CostTier.FREE
        ),

        // Ollama
        RecommendedModel(
            provider = LLMProvider.OLLAMA,
            modelName = "llama2",
            description = "Llama 2 本地部署",
            contextWindow = 4096,
            costTier = CostTier.FREE
        ),
        RecommendedModel(
            provider = LLMProvider.OLLAMA,
            modelName = "mistral",
            description = "Mistral 本地部署",
            contextWindow = 8192,
            costTier = CostTier.FREE
        ),
        RecommendedModel(
            provider = LLMProvider.OLLAMA,
            modelName = "qwen:7b",
            description = "通义千问7B本地版",
            contextWindow = 8192,
            costTier = CostTier.FREE
        )
    )
}
