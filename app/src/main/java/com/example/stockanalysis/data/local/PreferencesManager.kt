package com.example.stockanalysis.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    init {
        // 首次启动时，从明文迁移到加密存储
        migrateToSecureStorage()
    }

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_ANALYSIS_NOTIFICATIONS = "analysis_notifications"
        private const val KEY_MARKET_NOTIFICATIONS = "market_notifications"
        private const val KEY_DAILY_REPORT = "daily_report"

        // LLM API配置
        private const val KEY_LLM_API_KEY = "llm_api_key"
        private const val KEY_LLM_BACKUP_API_KEYS = "llm_backup_api_keys"
        private const val KEY_LLM_BASE_URL = "llm_base_url"
        private const val KEY_LLM_MODEL = "llm_model"
        private const val KEY_LLM_FALLBACK_MODEL = "llm_fallback_model"
        private const val KEY_LLM_PROVIDER = "llm_provider"
        private const val KEY_LLM_TEMPERATURE = "llm_temperature"
        private const val KEY_LLM_TIMEOUT = "llm_timeout"

        // Tushare API配置
        private const val KEY_TUSHARE_TOKEN = "tushare_token"

        // 数据源配置
        private const val KEY_DATA_SOURCES_CONFIG = "data_sources_config"
        private const val KEY_DATA_SOURCE_FALLBACK_ENABLED = "data_source_fallback_enabled"

        // 首次使用标记
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_API_CONFIGURED = "api_configured"

        // 免责声明接受标记
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"

        // Webhook 配置
        private const val KEY_WEBHOOK_WECHAT = "webhook_wechat"
        private const val KEY_WEBHOOK_FEISHU = "webhook_feishu"
        private const val KEY_WEBHOOK_DINGTALK = "webhook_dingtalk"
        private const val KEY_PUSHPLUS_TOKEN = "pushplus_token"
        private const val KEY_SERVERCHAN_KEY = "serverchan_key"
    }

    // ==================== 通知设置 ====================

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setAnalysisNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ANALYSIS_NOTIFICATIONS, enabled) }
    }

    fun isAnalysisNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_ANALYSIS_NOTIFICATIONS, true)
    }

    fun setMarketNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MARKET_NOTIFICATIONS, enabled) }
    }

    fun isMarketNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_MARKET_NOTIFICATIONS, false)
    }

    fun setDailyReportEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DAILY_REPORT, enabled) }
    }

    fun isDailyReportEnabled(): Boolean {
        return prefs.getBoolean(KEY_DAILY_REPORT, false)
    }

    // ==================== LLM API 配置 ====================

    fun setLLMApiKey(apiKey: String) {
        securePreferencesManager.setLLMApiKey(apiKey)
        prefs.edit {
            putBoolean(KEY_API_CONFIGURED, apiKey.isNotEmpty())
        }
    }

    fun getLLMApiKey(): String {
        return securePreferencesManager.getLLMApiKey()
    }

    /**
     * 设置备用 API Keys（用于负载均衡）
     */
    fun setLLMBackupApiKeys(apiKeys: List<String>) {
        val json = apiKeys.joinToString(",")
        securePreferencesManager.setString(KEY_LLM_BACKUP_API_KEYS, json)
    }

    /**
     * 获取备用 API Keys
     */
    fun getLLMBackupApiKeys(): List<String> {
        val json = securePreferencesManager.getString(KEY_LLM_BACKUP_API_KEYS, "")
        return if (json.isEmpty()) {
            emptyList()
        } else {
            json.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    /**
     * 获取所有 API Keys（主 Key + 备用 Keys）
     */
    fun getLLMAllApiKeys(): List<String> {
        val primaryKey = getLLMApiKey()
        val backupKeys = getLLMBackupApiKeys()
        return if (primaryKey.isNotEmpty()) {
            listOf(primaryKey) + backupKeys
        } else {
            backupKeys
        }
    }

    fun setLLMBaseUrl(baseUrl: String) {
        prefs.edit { putString(KEY_LLM_BASE_URL, baseUrl) }
    }

    fun getLLMBaseUrl(): String {
        return prefs.getString(KEY_LLM_BASE_URL, "https://api.openai.com/") ?: "https://api.openai.com/"
    }

    fun setLLMModel(model: String) {
        prefs.edit { putString(KEY_LLM_MODEL, model) }
    }

    fun getLLMModel(): String {
        // 根据提供商返回默认模型
        val provider = getLLMProvider()
        val defaultModel = when (provider) {
            "DEEPSEEK" -> "deepseek-chat"
            "QWEN" -> "qwen-plus"
            "GEMINI" -> "gemini-2.0-flash-exp"
            "ANTHROPIC" -> "claude-3-5-sonnet-20241022"
            "OPENAI" -> "gpt-4o-mini"
            "OLLAMA" -> "llama2"
            else -> "gpt-4o-mini"
        }
        return prefs.getString(KEY_LLM_MODEL, defaultModel) ?: defaultModel
    }

    /**
     * 设置 Fallback 模型
     */
    fun setLLMFallbackModel(model: String) {
        prefs.edit { putString(KEY_LLM_FALLBACK_MODEL, model) }
    }

    /**
     * 获取 Fallback 模型
     */
    fun getLLMFallbackModel(): String {
        return prefs.getString(KEY_LLM_FALLBACK_MODEL, "") ?: ""
    }

    /**
     * 获取所有模型（主模型 + Fallback 模型）
     */
    fun getLLMAllModels(): List<String> {
        val primaryModel = getLLMModel()
        val fallbackModel = getLLMFallbackModel()
        return if (fallbackModel.isNotEmpty()) {
            listOf(primaryModel, fallbackModel)
        } else {
            listOf(primaryModel)
        }
    }

    fun setLLMProvider(provider: String) {
        prefs.edit { putString(KEY_LLM_PROVIDER, provider) }
    }

    fun getLLMProvider(): String {
        return prefs.getString(KEY_LLM_PROVIDER, "DEEPSEEK") ?: "DEEPSEEK"
    }

    /**
     * 设置温度参数
     */
    fun setLLMTemperature(temperature: Float) {
        prefs.edit { putFloat(KEY_LLM_TEMPERATURE, temperature) }
    }

    /**
     * 获取温度参数
     */
    fun getLLMTemperature(): Float {
        return prefs.getFloat(KEY_LLM_TEMPERATURE, 0.7f)
    }

    /**
     * 设置超时时间（秒）
     */
    fun setLLMTimeout(timeout: Int) {
        prefs.edit { putInt(KEY_LLM_TIMEOUT, timeout) }
    }

    /**
     * 获取超时时间（秒）
     */
    fun getLLMTimeout(): Int {
        return prefs.getInt(KEY_LLM_TIMEOUT, 60)
    }

    // ==================== Tushare API 配置 ====================

    fun setTushareToken(token: String) {
        securePreferencesManager.setTushareToken(token)
    }

    fun getTushareToken(): String {
        return securePreferencesManager.getTushareToken()
    }

    // ==================== 首次使用检测 ====================

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit { putBoolean(KEY_FIRST_LAUNCH, false) }
    }

    fun isApiConfigured(): Boolean {
        return prefs.getBoolean(KEY_API_CONFIGURED, false)
    }

    // ==================== 免责声明 ====================

    fun isDisclaimerAccepted(): Boolean {
        return prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
    }

    fun setDisclaimerAccepted(accepted: Boolean) {
        prefs.edit { putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted) }
    }

    // ==================== Webhook 配置 ====================

    fun setWechatWebhook(url: String) {
        securePreferencesManager.setString(KEY_WEBHOOK_WECHAT, url)
    }

    fun getWechatWebhook(): String {
        return securePreferencesManager.getString(KEY_WEBHOOK_WECHAT, "")
    }

    fun setFeishuWebhook(url: String) {
        securePreferencesManager.setString(KEY_WEBHOOK_FEISHU, url)
    }

    fun getFeishuWebhook(): String {
        return securePreferencesManager.getString(KEY_WEBHOOK_FEISHU, "")
    }

    fun setDingtalkWebhook(url: String) {
        securePreferencesManager.setString(KEY_WEBHOOK_DINGTALK, url)
    }

    fun getDingtalkWebhook(): String {
        return securePreferencesManager.getString(KEY_WEBHOOK_DINGTALK, "")
    }

    fun setPushPlusToken(token: String) {
        securePreferencesManager.setString(KEY_PUSHPLUS_TOKEN, token)
    }

    fun getPushPlusToken(): String {
        return securePreferencesManager.getString(KEY_PUSHPLUS_TOKEN, "")
    }

    fun setServerChanKey(key: String) {
        securePreferencesManager.setString(KEY_SERVERCHAN_KEY, key)
    }

    fun getServerChanKey(): String {
        return securePreferencesManager.getString(KEY_SERVERCHAN_KEY, "")
    }

    // ==================== 数据源配置 ====================

    fun saveDataSourceConfigs(configs: List<com.example.stockanalysis.data.model.DataSourceConfig>) {
        val json = com.example.stockanalysis.data.model.DataSourceConfig.toJson(configs)
        prefs.edit { putString(KEY_DATA_SOURCES_CONFIG, json) }
    }

    fun getDataSourceConfigs(): List<com.example.stockanalysis.data.model.DataSourceConfig> {
        val json = prefs.getString(KEY_DATA_SOURCES_CONFIG, null)
        return if (json != null) {
            com.example.stockanalysis.data.model.DataSourceConfig.fromJson(json)
        } else {
            com.example.stockanalysis.data.model.DataSourceConfig.getAvailableDataSources()
        }
    }

    fun saveDataSourceConfig(config: com.example.stockanalysis.data.model.DataSourceConfig) {
        val configs = getDataSourceConfigs().toMutableList()
        val index = configs.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            configs[index] = config
        } else {
            configs.add(config)
        }
        saveDataSourceConfigs(configs)
    }

    fun getDataSourceConfig(id: String): com.example.stockanalysis.data.model.DataSourceConfig? {
        return getDataSourceConfigs().firstOrNull { it.id == id }
    }

    fun setDataSourceEnabled(id: String, enabled: Boolean) {
        val config = getDataSourceConfig(id)
        if (config != null) {
            config.enabled = enabled
            saveDataSourceConfig(config)
        }
    }

    fun setDataSourceFallbackEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DATA_SOURCE_FALLBACK_ENABLED, enabled) }
    }

    fun isDataSourceFallbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_DATA_SOURCE_FALLBACK_ENABLED, true)
    }

    // ==================== 通用方法 ====================

    fun setCustomValue(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    fun getCustomValue(key: String): String? {
        return prefs.getString(key, null)
    }

    // ==================== 清除所有配置 ====================

    fun clearAll() {
        prefs.edit { clear() }
        securePreferencesManager.clearAll()
    }

    // ==================== 迁移到加密存储 ====================

    private fun migrateToSecureStorage() {
        val migrated = prefs.getBoolean("migrated_to_secure", false)
        if (!migrated) {
            val oldApiKey = prefs.getString(KEY_LLM_API_KEY, "") ?: ""
            val oldToken = prefs.getString(KEY_TUSHARE_TOKEN, "") ?: ""

            if (oldApiKey.isNotEmpty() || oldToken.isNotEmpty()) {
                securePreferencesManager.migrateFromPlainText(oldApiKey, oldToken)

                // 清除明文存储
                prefs.edit {
                    remove(KEY_LLM_API_KEY)
                    remove(KEY_TUSHARE_TOKEN)
                    putBoolean("migrated_to_secure", true)
                }
            } else {
                // 没有需要迁移的数据，直接标记为已迁移
                prefs.edit { putBoolean("migrated_to_secure", true) }
            }
        }
    }
}
