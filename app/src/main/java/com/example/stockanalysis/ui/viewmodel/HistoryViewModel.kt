package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.Decision
import com.example.stockanalysis.data.repository.AnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 历史ViewModel
 * 管理分析历史的数据和筛选
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _allResults = MutableStateFlow<List<AnalysisResult>>(emptyList())
    
    private val _filteredResults = MutableStateFlow<List<AnalysisResult>>(emptyList())
    val filteredResults: StateFlow<List<AnalysisResult>> = _filteredResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _statistics = MutableStateFlow(HistoryStatistics())
    val statistics: StateFlow<HistoryStatistics> = _statistics

    private val _currentFilter = MutableStateFlow(FilterType.ALL)

    init {
        loadHistory()
    }

    /**
     * 加载历史记录
     */
    fun loadHistory() {
        viewModelScope.launch {
            analysisRepository.getAllResults().collect { results ->
                _allResults.value = results
                applyFilter()
                updateStatistics(results)
            }
        }
    }

    /**
     * 刷新历史记录
     */
    fun refreshHistory() {
        loadHistory()
    }

    /**
     * 设置筛选条件
     */
    fun setFilter(filterType: FilterType) {
        _currentFilter.value = filterType
        applyFilter()
    }

    /**
     * 应用筛选
     */
    private fun applyFilter() {
        val results = _allResults.value
        val filtered = when (_currentFilter.value) {
            FilterType.ALL -> results
            FilterType.BUY -> results.filter { 
                it.decision == Decision.BUY || it.decision == Decision.STRONG_BUY 
            }
            FilterType.SELL -> results.filter { 
                it.decision == Decision.SELL || it.decision == Decision.STRONG_SELL 
            }
            FilterType.HOLD -> results.filter { it.decision == Decision.HOLD }
        }
        _filteredResults.value = filtered
    }

    /**
     * 更新统计信息
     */
    private fun updateStatistics(results: List<AnalysisResult>) {
        val buyCount = results.count { 
            it.decision == Decision.BUY || it.decision == Decision.STRONG_BUY 
        }
        val sellCount = results.count { 
            it.decision == Decision.SELL || it.decision == Decision.STRONG_SELL 
        }
        val holdCount = results.count { it.decision == Decision.HOLD }
        val avgScore = if (results.isNotEmpty()) {
            results.map { it.score }.average()
        } else 0.0

        _statistics.value = HistoryStatistics(
            totalCount = results.size,
            buyCount = buyCount,
            sellCount = sellCount,
            holdCount = holdCount,
            averageScore = avgScore
        )
    }

    /**
     * 删除单条记录
     */
    fun deleteResult(id: String) {
        viewModelScope.launch {
            try {
                analysisRepository.deleteResult(id)
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 清空历史记录
     */
    fun clearHistory() {
        viewModelScope.launch {
            try {
                analysisRepository.clearAllHistory()
            } catch (e: Exception) {
                _errorMessage.value = "清空失败: ${e.message}"
            }
        }
    }

    enum class FilterType {
        ALL, BUY, SELL, HOLD
    }

    data class HistoryStatistics(
        val totalCount: Int = 0,
        val buyCount: Int = 0,
        val sellCount: Int = 0,
        val holdCount: Int = 0,
        val averageScore: Double = 0.0
    )
}
