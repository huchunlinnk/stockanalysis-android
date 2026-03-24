package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.portfolio.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _portfolioSummary = MutableLiveData<PortfolioSummary>()
    val portfolioSummary: LiveData<PortfolioSummary> = _portfolioSummary

    private val _riskMetrics = MutableLiveData<RiskMetrics>()
    val riskMetrics: LiveData<RiskMetrics> = _riskMetrics

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _buyResult = MutableLiveData<Result<PortfolioTransaction>>()
    val buyResult: LiveData<Result<PortfolioTransaction>> = _buyResult

    private val _sellResult = MutableLiveData<Result<PortfolioTransaction>>()
    val sellResult: LiveData<Result<PortfolioTransaction>> = _sellResult

    // LiveData for real-time updates
    val allHoldings = portfolioRepository.getAllHoldings()
    val allTransactions = portfolioRepository.getAllTransactions()
    val allCashFlows = portfolioRepository.getAllCashFlows()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // 刷新持仓市值
                portfolioRepository.refreshHoldingsValue()

                // 加载汇总数据
                val summary = portfolioRepository.getPortfolioSummary()
                _portfolioSummary.value = summary

                // 加载风险指标
                val metrics = portfolioRepository.calculateRiskMetrics()
                _riskMetrics.value = metrics

            } catch (e: Exception) {
                _error.value = "加载数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshHoldingsValue() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                portfolioRepository.refreshHoldingsValue()
                loadData()
            } catch (e: Exception) {
                _error.value = "刷新失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun buyStock(
        symbol: String,
        name: String,
        quantity: Int,
        price: Double,
        commission: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = portfolioRepository.buyStock(
                    symbol = symbol,
                    name = name,
                    quantity = quantity,
                    price = price,
                    commission = commission
                )
                _buyResult.value = result
                if (result.isSuccess) {
                    loadData()
                }
            } catch (e: Exception) {
                _buyResult.value = Result.failure(e)
                _error.value = "买入失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sellStock(
        symbol: String,
        quantity: Int,
        price: Double,
        commission: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = portfolioRepository.sellStock(
                    symbol = symbol,
                    quantity = quantity,
                    price = price,
                    commission = commission
                )
                _sellResult.value = result
                if (result.isSuccess) {
                    loadData()
                }
            } catch (e: Exception) {
                _sellResult.value = Result.failure(e)
                _error.value = "卖出失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createDailySnapshot() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                portfolioRepository.createDailySnapshot()
            } catch (e: Exception) {
                _error.value = "创建快照失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
