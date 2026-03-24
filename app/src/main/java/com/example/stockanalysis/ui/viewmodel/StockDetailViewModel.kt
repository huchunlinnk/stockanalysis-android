package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.model.*
import com.example.stockanalysis.data.repository.AnalysisRepository
import com.example.stockanalysis.data.repository.StockRepository
import com.example.stockanalysis.data.repository.FundamentalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 股票详情ViewModel
 */
@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val analysisRepository: AnalysisRepository,
    private val fundamentalRepository: FundamentalRepository
) : ViewModel() {

    private val _stockQuote = MutableStateFlow<RealtimeQuote?>(null)
    val stockQuote: StateFlow<RealtimeQuote?> = _stockQuote

    private val _technicalIndicators = MutableStateFlow<TechnicalIndicators?>(null)
    val technicalIndicators: StateFlow<TechnicalIndicators?> = _technicalIndicators

    // 基本面数据
    private val _fundamentalData = MutableStateFlow<FundamentalData?>(null)
    val fundamentalData: StateFlow<FundamentalData?> = _fundamentalData

    private val _fundamentalAnalysis = MutableStateFlow<FundamentalAnalysisResult?>(null)
    val fundamentalAnalysis: StateFlow<FundamentalAnalysisResult?> = _fundamentalAnalysis

    private val _financialIndicators = MutableStateFlow<FinancialIndicators?>(null)
    val financialIndicators: StateFlow<FinancialIndicators?> = _financialIndicators

    private val _growthMetrics = MutableStateFlow<GrowthMetrics?>(null)
    val growthMetrics: StateFlow<GrowthMetrics?> = _growthMetrics

    private val _dividendInfo = MutableStateFlow<DividendInfo?>(null)
    val dividendInfo: StateFlow<DividendInfo?> = _dividendInfo

    private val _institutionalHolding = MutableStateFlow<InstitutionalHolding?>(null)
    val institutionalHolding: StateFlow<InstitutionalHolding?> = _institutionalHolding

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingFundamental = MutableStateFlow(false)
    val isLoadingFundamental: StateFlow<Boolean> = _isLoadingFundamental

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _analysisEvent = MutableSharedFlow<AnalysisEvent>()
    val analysisEvent: SharedFlow<AnalysisEvent> = _analysisEvent

    private var currentSymbol: String = ""
    private var currentName: String = ""

    /**
     * 加载股票详情
     */
    fun loadStockDetail(symbol: String, name: String) {
        currentSymbol = symbol
        currentName = name
        refreshData()
        loadFundamentalData()
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        if (currentSymbol.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true

            // 加载行情
            val quoteResult = stockRepository.getQuote(currentSymbol)
            quoteResult.onSuccess { quote ->
                _stockQuote.value = quote
            }.onFailure { error ->
                _errorMessage.value = "获取行情失败: ${error.message}"
            }

            // 加载技术指标
            val techResult = stockRepository.getTechnicalIndicators(currentSymbol)
            techResult.onSuccess { indicators ->
                _technicalIndicators.value = indicators
            }

            _isLoading.value = false
        }
    }

    /**
     * 加载基本面数据
     */
    fun loadFundamentalData(forceRefresh: Boolean = false) {
        if (currentSymbol.isEmpty()) return

        viewModelScope.launch {
            _isLoadingFundamental.value = true

            try {
                // 获取基本面数据
                val fundamentalResult = fundamentalRepository.getFundamentalData(
                    currentSymbol,
                    currentName,
                    forceRefresh
                )

                fundamentalResult.onSuccess { data ->
                    _fundamentalData.value = data

                    // 获取当前价格
                    val currentPrice = _stockQuote.value?.price

                    // 获取基本面分析结果
                    val analysisResult = fundamentalRepository.getFundamentalAnalysis(
                        currentSymbol,
                        currentName,
                        currentPrice
                    )

                    analysisResult.onSuccess { analysis ->
                        _fundamentalAnalysis.value = analysis
                    }

                    // 加载详细指标
                    loadDetailedMetrics()
                }.onFailure { error ->
                    _errorMessage.value = "获取基本面数据失败: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载基本面数据异常: ${e.message}"
            } finally {
                _isLoadingFundamental.value = false
            }
        }
    }

    /**
     * 加载详细指标
     */
    private fun loadDetailedMetrics() {
        viewModelScope.launch {
            try {
                _financialIndicators.value = fundamentalRepository.getFinancialIndicators(currentSymbol)
                _growthMetrics.value = fundamentalRepository.getGrowthMetrics(currentSymbol)
                _dividendInfo.value = fundamentalRepository.getDividendInfo(currentSymbol)
                _institutionalHolding.value = fundamentalRepository.getInstitutionalHolding(currentSymbol)
            } catch (e: Exception) {
                // 静默失败，不影响主流程
            }
        }
    }

    /**
     * 开始分析
     */
    fun startAnalysis() {
        if (currentSymbol.isEmpty() || currentName.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            analysisRepository.analyzeStock(currentSymbol, currentName).collect { state ->
                when (state) {
                    is com.example.stockanalysis.data.repository.AnalysisState.Success -> {
                        _isLoading.value = false
                        _analysisEvent.emit(AnalysisEvent.NavigateToResult(state.result.id))
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = state.message
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 添加到自选
     */
    fun addToWatchlist() {
        if (currentSymbol.isEmpty() || currentName.isEmpty()) return

        viewModelScope.launch {
            val result = stockRepository.addStock(currentSymbol, currentName)
            result.onSuccess {
                _errorMessage.value = "已添加到自选"
            }.onFailure { error ->
                _errorMessage.value = "添加失败: ${error.message}"
            }
        }
    }

    sealed class AnalysisEvent {
        data class NavigateToResult(val analysisId: String) : AnalysisEvent()
    }
}
