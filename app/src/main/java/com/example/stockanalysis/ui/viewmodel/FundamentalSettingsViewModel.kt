package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.data.repository.FundamentalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 基本面设置ViewModel
 */
@HiltViewModel
class FundamentalSettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val fundamentalRepository: FundamentalRepository
) : ViewModel() {

    data class FundamentalSettings(
        val autoRefresh: Boolean = true,
        val cacheDays: Int = 1,
        val showFinancial: Boolean = true,
        val showGrowth: Boolean = true,
        val showDividend: Boolean = true,
        val showInstitution: Boolean = true,
        val includeFundamentalInAnalysis: Boolean = true
    )

    data class CacheInfo(
        val cacheCount: Int = 0,
        val cacheSize: Long = 0
    )

    private val _settings = MutableStateFlow(FundamentalSettings())
    val settings: StateFlow<FundamentalSettings> = _settings

    private val _cacheInfo = MutableStateFlow(CacheInfo())
    val cacheInfo: StateFlow<CacheInfo> = _cacheInfo

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    companion object {
        private const val PREF_AUTO_REFRESH = "fundamental_auto_refresh"
        private const val PREF_CACHE_DAYS = "fundamental_cache_days"
        private const val PREF_SHOW_FINANCIAL = "fundamental_show_financial"
        private const val PREF_SHOW_GROWTH = "fundamental_show_growth"
        private const val PREF_SHOW_DIVIDEND = "fundamental_show_dividend"
        private const val PREF_SHOW_INSTITUTION = "fundamental_show_institution"
        private const val PREF_INCLUDE_IN_ANALYSIS = "fundamental_include_in_analysis"
    }

    /**
     * 加载设置
     */
    fun loadSettings() {
        viewModelScope.launch {
            _settings.value = FundamentalSettings(
                autoRefresh = preferencesManager.getCustomValue(PREF_AUTO_REFRESH)?.toBoolean() ?: true,
                cacheDays = preferencesManager.getCustomValue(PREF_CACHE_DAYS)?.toIntOrNull() ?: 1,
                showFinancial = preferencesManager.getCustomValue(PREF_SHOW_FINANCIAL)?.toBoolean() ?: true,
                showGrowth = preferencesManager.getCustomValue(PREF_SHOW_GROWTH)?.toBoolean() ?: true,
                showDividend = preferencesManager.getCustomValue(PREF_SHOW_DIVIDEND)?.toBoolean() ?: true,
                showInstitution = preferencesManager.getCustomValue(PREF_SHOW_INSTITUTION)?.toBoolean() ?: true,
                includeFundamentalInAnalysis = preferencesManager.getCustomValue(PREF_INCLUDE_IN_ANALYSIS)?.toBoolean() ?: true
            )

            loadCacheInfo()
        }
    }

    /**
     * 设置自动刷新
     */
    fun setAutoRefresh(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCustomValue(PREF_AUTO_REFRESH, enabled.toString())
            _settings.value = _settings.value.copy(autoRefresh = enabled)
        }
    }

    /**
     * 设置缓存有效期
     */
    fun setCacheDays(days: Int) {
        viewModelScope.launch {
            preferencesManager.setCustomValue(PREF_CACHE_DAYS, days.toString())
            _settings.value = _settings.value.copy(cacheDays = days)
        }
    }

    /**
     * 设置显示财务指标
     */
    fun setShowFinancial(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCustomValue(PREF_SHOW_FINANCIAL, show.toString())
            _settings.value = _settings.value.copy(showFinancial = show)
        }
    }

    /**
     * 设置显示成长性
     */
    fun setShowGrowth(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCustomValue(PREF_SHOW_GROWTH, show.toString())
            _settings.value = _settings.value.copy(showGrowth = show)
        }
    }

    /**
     * 设置显示分红
     */
    fun setShowDividend(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCustomValue(PREF_SHOW_DIVIDEND, show.toString())
            _settings.value = _settings.value.copy(showDividend = show)
        }
    }

    /**
     * 设置显示机构持仓
     */
    fun setShowInstitution(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCustomValue(PREF_SHOW_INSTITUTION, show.toString())
            _settings.value = _settings.value.copy(showInstitution = show)
        }
    }

    /**
     * 设置分析时包含基本面
     */
    fun setIncludeFundamentalInAnalysis(include: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCustomValue(PREF_INCLUDE_IN_ANALYSIS, include.toString())
            _settings.value = _settings.value.copy(includeFundamentalInAnalysis = include)
        }
    }

    /**
     * 加载缓存信息
     */
    private fun loadCacheInfo() {
        viewModelScope.launch {
            try {
                val cachedData = fundamentalRepository.getCachedFundamentalData()
                val count = cachedData.size
                // 简单估算：每条数据约10KB
                val estimatedSize = count * 10L * 1024

                _cacheInfo.value = CacheInfo(
                    cacheCount = count,
                    cacheSize = estimatedSize
                )
            } catch (e: Exception) {
                _cacheInfo.value = CacheInfo()
            }
        }
    }

    /**
     * 清空缓存
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                fundamentalRepository.clearCache()
                _message.value = "缓存已清空"
                loadCacheInfo()
            } catch (e: Exception) {
                _message.value = "清空缓存失败: ${e.message}"
            }
        }
    }
}
