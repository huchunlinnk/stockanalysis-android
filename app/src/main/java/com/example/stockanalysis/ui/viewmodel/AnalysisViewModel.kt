package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.model.Stock
import com.example.stockanalysis.data.repository.AnalysisRepository
import com.example.stockanalysis.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 分析ViewModel
 * 管理AI分析的状态和流程
 */
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _watchlist = MutableStateFlow<List<Stock>>(emptyList())
    val watchlist: StateFlow<List<Stock>> = _watchlist

    private val _selectedStocks = MutableStateFlow<List<Stock>>(emptyList())
    val selectedStocks: StateFlow<List<Stock>> = _selectedStocks

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent

    /**
     * 加载自选列表
     */
    fun loadWatchlist() {
        viewModelScope.launch {
            val stocks = stockRepository.getAllStocksSync()
            _watchlist.value = stocks
        }
    }

    /**
     * 添加选中股票
     */
    fun addSelectedStock(stock: Stock) {
        if (!_selectedStocks.value.contains(stock)) {
            _selectedStocks.value = _selectedStocks.value + stock
        }
    }

    /**
     * 移除选中股票
     */
    fun removeSelectedStock(stock: Stock) {
        _selectedStocks.value = _selectedStocks.value - stock
    }

    /**
     * 清空选中列表
     */
    fun clearSelection() {
        _selectedStocks.value = emptyList()
    }

    /**
     * 开始分析
     */
    fun startAnalysis() {
        val stocks = _selectedStocks.value
        if (stocks.isEmpty()) {
            _errorMessage.value = "请选择至少一只股票"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            
            if (stocks.size == 1) {
                // 单股票分析
                analyzeSingleStock(stocks.first())
            } else {
                // 批量分析
                analyzeMultipleStocks(stocks)
            }
        }
    }

    private suspend fun analyzeSingleStock(stock: Stock) {
        analysisRepository.analyzeStock(stock.symbol, stock.name).collect { state ->
            when (state) {
                is com.example.stockanalysis.data.repository.AnalysisState.Loading -> {
                    _analysisState.value = AnalysisUiState.Analyzing(1, 1, stock.name)
                }
                is com.example.stockanalysis.data.repository.AnalysisState.Progress -> {
                    _analysisState.value = AnalysisUiState.Analyzing(1, 1, stock.name)
                }
                is com.example.stockanalysis.data.repository.AnalysisState.AgentResult -> {
                    // Agent 分析结果更新，继续等待最终结果
                }
                is com.example.stockanalysis.data.repository.AnalysisState.Success -> {
                    _isLoading.value = false
                    _analysisState.value = AnalysisUiState.Completed
                    _navigationEvent.emit(NavigationEvent.ToResult(state.result.id))
                }
                is com.example.stockanalysis.data.repository.AnalysisState.Error -> {
                    _isLoading.value = false
                    _analysisState.value = AnalysisUiState.Error(state.message)
                }
            }
        }
    }

    private suspend fun analyzeMultipleStocks(stocks: List<Stock>) {
        analysisRepository.analyzeStocks(stocks).collect { state ->
            when (state) {
                is com.example.stockanalysis.data.repository.BatchAnalysisState.Loading -> {
                    _analysisState.value = AnalysisUiState.Analyzing(0, stocks.size, "准备中...")
                }
                is com.example.stockanalysis.data.repository.BatchAnalysisState.Progress -> {
                    _analysisState.value = AnalysisUiState.Analyzing(
                        state.current, 
                        state.total, 
                        state.currentStock
                    )
                }
                is com.example.stockanalysis.data.repository.BatchAnalysisState.Success -> {
                    _isLoading.value = false
                    _analysisState.value = AnalysisUiState.Completed
                    _navigationEvent.emit(NavigationEvent.ToBatchResult(state.results.size))
                }
                is com.example.stockanalysis.data.repository.BatchAnalysisState.Error -> {
                    _isLoading.value = false
                    _analysisState.value = AnalysisUiState.Error(state.message)
                }
            }
        }
    }

    sealed class AnalysisUiState {
        object Idle : AnalysisUiState()
        data class Analyzing(val progress: Int, val total: Int, val currentStock: String) : AnalysisUiState()
        object Completed : AnalysisUiState()
        data class Error(val message: String) : AnalysisUiState()
    }

    sealed class NavigationEvent {
        data class ToResult(val analysisId: String) : NavigationEvent()
        data class ToBatchResult(val count: Int) : NavigationEvent()
    }
}
