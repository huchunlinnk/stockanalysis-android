package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.backtest.BacktestEngine
import com.example.stockanalysis.data.backtest.BacktestResult
import com.example.stockanalysis.data.backtest.BacktestSignal
import com.example.stockanalysis.data.backtest.BacktestDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BacktestViewModel @Inject constructor(
    private val backtestEngine: BacktestEngine,
    private val backtestDao: BacktestDao
) : ViewModel() {

    private val _backtestResult = MutableLiveData<BacktestResult?>()
    val backtestResult: LiveData<BacktestResult?> = _backtestResult

    private val _backtestSignals = MutableLiveData<List<BacktestSignal>>()
    val backtestSignals: LiveData<List<BacktestSignal>> = _backtestSignals

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val allResults = backtestDao.getAllResults()

    fun runBacktest(
        symbol: String,
        strategyId: String? = null,
        startDate: Date? = null,
        endDate: Date? = null
    ) {
        viewModelScope.launch {
            try {
                _isRunning.value = true
                _progress.value = 0
                _error.value = null

                // 执行回测
                val result = backtestEngine.runBacktest(
                    symbol = symbol,
                    strategyId = strategyId,
                    startDate = startDate,
                    endDate = endDate
                )

                result.onSuccess { backtestResult ->
                    _backtestResult.value = backtestResult
                    _progress.value = 100

                    // 加载详细信号
                    loadSignals(backtestResult.id)
                }.onFailure { e ->
                    _error.value = "回测失败: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "执行回测出错: ${e.message}"
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun loadBacktestResult(resultId: String) {
        viewModelScope.launch {
            try {
                // 由于没有getBacktestResultById方法,直接从LiveData中查找
                _error.value = "此功能暂不可用"
            } catch (e: Exception) {
                _error.value = "加载回测结果失败: ${e.message}"
            }
        }
    }

    private fun loadSignals(backtestResultId: String) {
        viewModelScope.launch {
            try {
                val signals = backtestDao.getSignalsByResultId(backtestResultId)
                _backtestSignals.value = signals
            } catch (e: Exception) {
                _error.value = "加载信号详情失败: ${e.message}"
            }
        }
    }

    fun deleteBacktestResult(resultId: String) {
        viewModelScope.launch {
            try {
                backtestDao.deleteResultById(resultId)
                _backtestResult.value = null
                _backtestSignals.value = emptyList()
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
