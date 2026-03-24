package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 添加股票ViewModel
 */
@HiltViewModel
class AddStockViewModel @Inject constructor(
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val searchResults: StateFlow<List<Pair<String, String>>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _addStockResult = MutableStateFlow<AddStockResult?>(null)
    val addStockResult: StateFlow<AddStockResult?> = _addStockResult

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * 搜索股票
     */
    fun searchStocks(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            stockRepository.searchStocks(query).collect { results ->
                _searchResults.value = results
                _isLoading.value = false
            }
        }
    }

    /**
     * 添加股票到自选
     */
    fun addStock(symbol: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = stockRepository.addStock(symbol, name)
            result.onSuccess {
                _addStockResult.value = AddStockResult.Success
            }.onFailure { error ->
                _addStockResult.value = AddStockResult.Error(error.message ?: "添加失败")
            }
            _isLoading.value = false
        }
    }

    /**
     * 清除添加结果
     */
    fun clearAddResult() {
        _addStockResult.value = null
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    sealed class AddStockResult {
        object Success : AddStockResult()
        data class Error(val message: String) : AddStockResult()
    }
}
