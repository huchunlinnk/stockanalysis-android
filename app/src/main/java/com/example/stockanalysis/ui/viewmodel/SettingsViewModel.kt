package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.model.AIProvider
import com.example.stockanalysis.data.model.AppSettings
import com.example.stockanalysis.utils.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _settings = MutableStateFlow(settingsManager.getAllSettings())
    val settings: StateFlow<AppSettings> = _settings

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    /**
     * 更新AI提供商
     */
    fun updateAIProvider(provider: AIProvider) {
        _settings.value = _settings.value.copy(aiProvider = provider)
    }

    /**
     * 更新API Key
     */
    fun updateApiKey(apiKey: String) {
        _settings.value = _settings.value.copy(apiKey = apiKey)
    }

    /**
     * 更新API Base URL
     */
    fun updateApiBaseUrl(url: String) {
        _settings.value = _settings.value.copy(apiBaseUrl = url)
    }

    /**
     * 更新模型名称
     */
    fun updateModelName(model: String) {
        _settings.value = _settings.value.copy(modelName = model)
    }

    /**
     * 更新通知设置
     */
    fun updateNotification(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableNotification = enabled)
    }

    /**
     * 更新通知时间
     */
    fun updateNotificationTime(time: String) {
        _settings.value = _settings.value.copy(notificationTime = time)
    }

    /**
     * 更新自动分析
     */
    fun updateAutoAnalysis(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableAutoAnalysis = enabled)
    }

    /**
     * 更新报告语言
     */
    fun updateLanguage(language: com.example.stockanalysis.data.model.Language) {
        _settings.value = _settings.value.copy(reportLanguage = language)
    }

    /**
     * 保存设置
     */
    fun saveSettings() {
        viewModelScope.launch {
            settingsManager.saveSettings(_settings.value)
            _saveSuccess.value = true
        }
    }

    /**
     * 重置保存状态
     */
    fun resetSaveStatus() {
        _saveSuccess.value = false
    }

    /**
     * 清除所有数据
     */
    fun clearAllData() {
        viewModelScope.launch {
            settingsManager.clearSettings()
            _settings.value = AppSettings()
        }
    }

    /**
     * 测试API连接
     */
    suspend fun testApiConnection(): Boolean {
        val currentSettings = _settings.value
        return currentSettings.apiKey.isNotEmpty()
    }
}
