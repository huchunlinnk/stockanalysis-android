package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.model.Stock
import com.example.stockanalysis.data.model.RealtimeQuote
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
 * 自选ViewModel
 * 管理自选列表的数据和操作
 */
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _watchlist = MutableStateFlow<List<Stock>>(emptyList())
    val watchlist: StateFlow<List<Stock>> = _watchlist

    private val _quotes = MutableStateFlow<Map<String, RealtimeQuote>>(emptyMap())
    val quotes: StateFlow<Map<String, RealtimeQuote>> = _quotes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _analysisEvent = MutableSharedFlow<AnalysisEvent>()
    val analysisEvent: SharedFlow<AnalysisEvent> = _analysisEvent

    init {
        loadWatchlist()
    }

    /**
     * 加载自选列表
     */
    fun loadWatchlist() {
        viewModelScope.launch {
            stockRepository.getAllStocks().observeForever { stocks ->
                _watchlist.value = stocks
                refreshQuotes()
            }
        }
    }

    /**
     * 刷新行情数据
     */
    fun refreshQuotes() {
        viewModelScope.launch {
            val symbols = _watchlist.value.map { it.symbol }
            if (symbols.isEmpty()) return@launch

            _isLoading.value = true
            val result = stockRepository.getQuotes(symbols)
            result.onSuccess { quoteList ->
                val quoteMap = quoteList.associateBy { it.symbol }
                _quotes.value = quoteMap
            }.onFailure { error ->
                _errorMessage.value = "刷新失败: ${error.message}"
            }
            _isLoading.value = false
        }
    }

    /**
     * 添加股票到自选
     */
    fun addStock(stock: Stock) {
        viewModelScope.launch {
            val result = stockRepository.addStock(stock.symbol, stock.name)
            result.onFailure { error ->
                _errorMessage.value = "添加失败: ${error.message}"
            }
        }
    }

    /**
     * 从自选移除股票
     */
    fun removeStock(symbol: String) {
        viewModelScope.launch {
            val result = stockRepository.removeStock(symbol)
            result.onFailure { error ->
                _errorMessage.value = "删除失败: ${error.message}"
            }
        }
    }

    /**
     * 分析股票
     */
    fun analyzeStock(stock: Stock) {
        viewModelScope.launch {
            _isLoading.value = true
            analysisRepository.analyzeStock(stock.symbol, stock.name).collect { state ->
                when (state) {
                    is com.example.stockanalysis.data.repository.AnalysisState.Loading,
                    is com.example.stockanalysis.data.repository.AnalysisState.Progress -> {
                        // 保持加载状态
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.AgentResult -> {
                        // Agent 分析结果更新，继续等待最终结果
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Success -> {
                        _isLoading.value = false
                        _analysisEvent.emit(AnalysisEvent.Success(state.result.id))
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Error -> {
                        _isLoading.value = false
                        _analysisEvent.emit(AnalysisEvent.Error(state.message))
                    }
                }
            }
        }
    }

    sealed class AnalysisEvent {
        data class Success(val analysisId: String) : AnalysisEvent()
        data class Error(val message: String) : AnalysisEvent()
    }
}
