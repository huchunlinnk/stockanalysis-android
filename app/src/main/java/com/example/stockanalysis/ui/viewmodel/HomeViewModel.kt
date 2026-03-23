package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.datasource.DataSourceManager
import com.example.stockanalysis.data.local.AnalysisResultDao
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.MarketIndex
import com.example.stockanalysis.data.model.MarketOverview
import com.example.stockanalysis.data.model.MarketType
import com.example.stockanalysis.data.model.MarketStats
import com.example.stockanalysis.data.model.SectorPerformance
import com.example.stockanalysis.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val dataSourceManager: DataSourceManager,
    private val analysisResultDao: AnalysisResultDao
) : ViewModel() {

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val searchResults: StateFlow<List<Pair<String, String>>> = _searchResults

    // 市场指数列表
    private val _marketIndices = MutableStateFlow<List<MarketIndex>>(emptyList())
    val marketIndices: StateFlow<List<MarketIndex>> = _marketIndices

    // 市场统计
    private val _marketStats = MutableStateFlow<MarketStats?>(null)
    val marketStats: StateFlow<MarketStats?> = _marketStats

    // 板块表现
    private val _sectorPerformance = MutableStateFlow<SectorPerformance?>(null)
    val sectorPerformance: StateFlow<SectorPerformance?> = _sectorPerformance

    // 当前选中的市场类型
    private val _selectedMarketType = MutableStateFlow<MarketType?>(null) // null表示全部市场
    val selectedMarketType: StateFlow<MarketType?> = _selectedMarketType

    // 过滤后的市场指数
    private val _filteredIndices = MutableStateFlow<List<MarketIndex>>(emptyList())
    val filteredIndices: StateFlow<List<MarketIndex>> = _filteredIndices

    // 最近的分析报告
    private val _recentReports = MutableStateFlow<List<AnalysisResult>>(emptyList())
    val recentReports: StateFlow<List<AnalysisResult>> = _recentReports

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    
    // 数据更新时间
    private val _lastUpdateTime = MutableStateFlow<Long?>(null)
    val lastUpdateTime: StateFlow<Long?> = _lastUpdateTime

    init {
        setupSearchDebounce()
        loadMarketIndices()
        setupMarketFilter()
        loadRecentReports()
    }

    /**
     * 设置市场过滤
     */
    private fun setupMarketFilter() {
        viewModelScope.launch {
            _marketIndices.collect { indices ->
                filterMarketIndices(indices)
            }
        }
    }

    /**
     * 过滤市场指数
     */
    private fun filterMarketIndices(indices: List<MarketIndex>) {
        val selectedType = _selectedMarketType.value
        _filteredIndices.value = if (selectedType == null) {
            indices
        } else {
            indices.filter { it.marketType == selectedType }
        }
    }

    /**
     * 切换市场类型
     */
    fun selectMarketType(marketType: MarketType?) {
        _selectedMarketType.value = marketType
        filterMarketIndices(_marketIndices.value)
    }

    /**
     * 设置搜索防抖
     */
    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        _searchQuery
            .debounce(300) // 300ms 防抖
            .filter { it.length >= 1 } // 至少1个字符才搜索（支持拼音首字母）
            .onEach { query ->
                performSearch(query)
            }
            .launchIn(viewModelScope)
    }

    /**
     * 更新搜索查询
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
        }
    }

    /**
     * 执行搜索
     */
    fun searchStocks(query: String) {
        if (query.length < 1) {
            _errorMessage.value = "请输入至少1个字符进行搜索"
            return
        }
        viewModelScope.launch {
            performSearch(query)
        }
    }

    /**
     * 执行实际搜索操作
     */
    private suspend fun performSearch(query: String) {
        _isLoading.value = true
        try {
            // 使用 DataSourceManager 进行搜索（带故障切换）
            val result = dataSourceManager.searchStocks(query)
            result.fold(
                onSuccess = { stocks ->
                    _searchResults.value = stocks
                    if (stocks.isEmpty()) {
                        _errorMessage.value = "未找到匹配的股票，尝试输入拼音如 'gzmt' 或 '茅台'"
                    }
                },
                onFailure = { error ->
                    _errorMessage.value = "搜索失败: ${error.message}"
                    _searchResults.value = emptyList()
                }
            )
        } catch (e: Exception) {
            _errorMessage.value = "搜索出错: ${e.message}"
            _searchResults.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * 加载市场指数数据（直接使用 DataSourceManager）
     */
    fun loadMarketIndices() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // 直接使用 DataSourceManager 获取市场概览
                val result = dataSourceManager.fetchMarketOverview()
                
                result.fold(
                    onSuccess = { overview ->
                        updateMarketData(overview)
                        _lastUpdateTime.value = System.currentTimeMillis()
                    },
                    onFailure = { error ->
                        handleMarketDataError(error)
                    }
                )
            } catch (e: Exception) {
                handleMarketDataError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新市场数据
     */
    private fun updateMarketData(overview: MarketOverview) {
        _marketIndices.value = overview.indices
        _marketStats.value = overview.stats
        _sectorPerformance.value = overview.sectorPerformance
    }
    
    /**
     * 处理市场数据获取错误
     */
    private fun handleMarketDataError(error: Throwable) {
        _errorMessage.value = "加载市场数据失败: ${error.message}。请检查网络连接后重试。"
        // 不清空已有数据，保留上一次成功加载的数据
        if (_marketIndices.value.isEmpty()) {
            // 如果是首次加载且失败，显示空状态提示
            _marketIndices.value = emptyList()
        }
    }

    /**
     * 刷新所有市场数据（下拉刷新）
     */
    fun refreshMarketData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // 刷新数据源健康状态
                dataSourceManager.refreshDataSourceHealth()
                
                // 获取A股市场概览
                val aShareResult = dataSourceManager.fetchMarketOverview()
                
                aShareResult.fold(
                    onSuccess = { overview ->
                        updateMarketData(overview)
                        _lastUpdateTime.value = System.currentTimeMillis()
                    },
                    onFailure = { error ->
                        _errorMessage.value = "刷新失败: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "刷新出错: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 清除搜索结果
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _searchQuery.value = ""
    }

    /**
     * 加载最近的分析报告
     */
    fun loadRecentReports(limit: Int = 5) {
        viewModelScope.launch {
            try {
                val reports = analysisResultDao.getRecentResults(limit)
                _recentReports.value = reports
            } catch (e: Exception) {
                _errorMessage.value = "加载报告失败: ${e.message}"
                _recentReports.value = emptyList()
            }
        }
    }

    /**
     * 删除分析报告
     */
    fun deleteReport(result: AnalysisResult) {
        viewModelScope.launch {
            try {
                analysisResultDao.deleteResult(result)
                // 重新加载报告列表
                loadRecentReports()
            } catch (e: Exception) {
                _errorMessage.value = "删除报告失败: ${e.message}"
            }
        }
    }
    
    /**
     * 获取数据源健康状态
     */
    fun getDataSourceStatus(): Map<String, Boolean> {
        return dataSourceManager.getDataSourceStatus()
    }
}
