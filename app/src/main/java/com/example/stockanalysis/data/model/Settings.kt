package com.example.stockanalysis.data.model

/**
 * 应用设置
 */
data class AppSettings(
    // AI配置
    val aiProvider: AIProvider = AIProvider.OPENAI,
    val apiKey: String = "",
    val apiBaseUrl: String = "",
    val modelName: String = "gpt-3.5-turbo",
    
    // 通知设置
    val enableNotification: Boolean = true,
    val notificationTime: String = "18:00",
    val enableAutoAnalysis: Boolean = false,
    
    // 数据源设置
    val dataSource: DataSource = DataSource.AKSHARE,
    val cacheDurationMinutes: Int = 5,
    
    // 分析设置
    val reportLanguage: Language = Language.CHINESE,
    val analysisDelaySeconds: Int = 1,
    val maxConcurrentAnalysis: Int = 3,
    val biasThreshold: Double = 5.0,
    
    // 显示设置
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val enableChipDistribution: Boolean = false
)

/**
 * AI提供商
 */
enum class AIProvider {
    OPENAI,
    GEMINI,
    ANTHROPIC,
    DEEPSEEK,
    CUSTOM
}

/**
 * 数据源
 */
enum class DataSource {
    AKSHARE,
    TUSHARE,
    YFINANCE,
    SINA
}

/**
 * 语言
 */
enum class Language {
    CHINESE,
    ENGLISH
}

/**
 * 主题模式
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
