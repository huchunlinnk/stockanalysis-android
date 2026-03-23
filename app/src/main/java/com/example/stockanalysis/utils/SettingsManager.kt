package com.example.stockanalysis.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.stockanalysis.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置管理器
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // AI 配置
    var aiProvider: AIProvider
        get() = AIProvider.valueOf(prefs.getString(KEY_AI_PROVIDER, AIProvider.OPENAI.name)!!)
        set(value) = prefs.edit { putString(KEY_AI_PROVIDER, value.name) }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_KEY, value) }

    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_BASE_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_API_BASE_URL, value) }

    var modelName: String
        get() = prefs.getString(KEY_MODEL_NAME, "gpt-3.5-turbo") ?: "gpt-3.5-turbo"
        set(value) = prefs.edit { putString(KEY_MODEL_NAME, value) }

    // 通知设置
    var enableNotification: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_NOTIFICATION, true)
        set(value) = prefs.edit { putBoolean(KEY_ENABLE_NOTIFICATION, value) }

    var notificationTime: String
        get() = prefs.getString(KEY_NOTIFICATION_TIME, "18:00") ?: "18:00"
        set(value) = prefs.edit { putString(KEY_NOTIFICATION_TIME, value) }

    var enableAutoAnalysis: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_AUTO_ANALYSIS, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLE_AUTO_ANALYSIS, value) }

    // 数据源设置
    var dataSource: DataSource
        get() = DataSource.valueOf(prefs.getString(KEY_DATA_SOURCE, DataSource.AKSHARE.name)!!)
        set(value) = prefs.edit { putString(KEY_DATA_SOURCE, value.name) }

    var cacheDurationMinutes: Int
        get() = prefs.getInt(KEY_CACHE_DURATION, 5)
        set(value) = prefs.edit { putInt(KEY_CACHE_DURATION, value) }

    // 分析设置
    var reportLanguage: Language
        get() = Language.valueOf(prefs.getString(KEY_REPORT_LANGUAGE, Language.CHINESE.name)!!)
        set(value) = prefs.edit { putString(KEY_REPORT_LANGUAGE, value.name) }

    var biasThreshold: Double
        get() = prefs.getFloat(KEY_BIAS_THRESHOLD, 5.0f).toDouble()
        set(value) = prefs.edit { putFloat(KEY_BIAS_THRESHOLD, value.toFloat()) }

    // 显示设置
    var theme: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        set(value) = prefs.edit { putString(KEY_THEME, value.name) }

    var enableChipDistribution: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_CHIP, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLE_CHIP, value) }

    /**
     * 获取所有设置
     */
    fun getAllSettings(): AppSettings {
        return AppSettings(
            aiProvider = aiProvider,
            apiKey = apiKey,
            apiBaseUrl = apiBaseUrl,
            modelName = modelName,
            enableNotification = enableNotification,
            notificationTime = notificationTime,
            enableAutoAnalysis = enableAutoAnalysis,
            dataSource = dataSource,
            cacheDurationMinutes = cacheDurationMinutes,
            reportLanguage = reportLanguage,
            biasThreshold = biasThreshold,
            theme = theme,
            enableChipDistribution = enableChipDistribution
        )
    }

    /**
     * 保存所有设置
     */
    fun saveSettings(settings: AppSettings) {
        prefs.edit {
            putString(KEY_AI_PROVIDER, settings.aiProvider.name)
            putString(KEY_API_KEY, settings.apiKey)
            putString(KEY_API_BASE_URL, settings.apiBaseUrl)
            putString(KEY_MODEL_NAME, settings.modelName)
            putBoolean(KEY_ENABLE_NOTIFICATION, settings.enableNotification)
            putString(KEY_NOTIFICATION_TIME, settings.notificationTime)
            putBoolean(KEY_ENABLE_AUTO_ANALYSIS, settings.enableAutoAnalysis)
            putString(KEY_DATA_SOURCE, settings.dataSource.name)
            putInt(KEY_CACHE_DURATION, settings.cacheDurationMinutes)
            putString(KEY_REPORT_LANGUAGE, settings.reportLanguage.name)
            putFloat(KEY_BIAS_THRESHOLD, settings.biasThreshold.toFloat())
            putString(KEY_THEME, settings.theme.name)
            putBoolean(KEY_ENABLE_CHIP, settings.enableChipDistribution)
        }
    }

    /**
     * 清除所有设置
     */
    fun clearSettings() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "stock_analysis_settings"
        
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_ENABLE_NOTIFICATION = "enable_notification"
        private const val KEY_NOTIFICATION_TIME = "notification_time"
        private const val KEY_ENABLE_AUTO_ANALYSIS = "enable_auto_analysis"
        private const val KEY_DATA_SOURCE = "data_source"
        private const val KEY_CACHE_DURATION = "cache_duration"
        private const val KEY_REPORT_LANGUAGE = "report_language"
        private const val KEY_BIAS_THRESHOLD = "bias_threshold"
        private const val KEY_THEME = "theme"
        private const val KEY_ENABLE_CHIP = "enable_chip"
    }
}
