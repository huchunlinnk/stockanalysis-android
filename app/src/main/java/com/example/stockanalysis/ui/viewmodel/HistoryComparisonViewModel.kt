package com.example.stockanalysis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.analysis.*
import com.example.stockanalysis.data.local.StockDao
import com.example.stockanalysis.data.model.Stock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 历史信号对比 ViewModel
 */
@HiltViewModel
class HistoryComparisonViewModel @Inject constructor(
    private val historyComparisonService: HistoryComparisonService,
    private val stockDao: StockDao
) : ViewModel() {

    // UI 状态
    private val _uiState = MutableStateFlow<HistoryComparisonUiState>(HistoryComparisonUiState.Loading)
    val uiState: StateFlow<HistoryComparisonUiState> = _uiState.asStateFlow()

    // 当前选中的股票
    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    // 所有股票列表
    private val _stockList = MutableStateFlow<List<Stock>>(emptyList())
    val stockList: StateFlow<List<Stock>> = _stockList.asStateFlow()

    // 信号准确率统计
    private val _signalAccuracy = MutableStateFlow<SignalAccuracy?>(null)
    val signalAccuracy: StateFlow<SignalAccuracy?> = _signalAccuracy.asStateFlow()

    // 对比结果列表
    private val _comparisonResults = MutableStateFlow<List<ComparisonResult>>(emptyList())
    val comparisonResults: StateFlow<List<ComparisonResult>> = _comparisonResults.asStateFlow()

    // 信号报告
    private val _signalReport = MutableStateFlow<SignalReport?>(null)
    val signalReport: StateFlow<SignalReport?> = _signalReport.asStateFlow()

    // 筛选条件
    private val _filterType = MutableStateFlow(SignalFilterType.ALL)
    val filterType: StateFlow<SignalFilterType> = _filterType.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.DATE_DESC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    // 时间范围
    private val _timeRange = MutableStateFlow(TimeRange.LAST_30_DAYS)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    init {
        loadStockList()
    }

    /**
     * 加载股票列表
     */
    fun loadStockList() {
        viewModelScope.launch {
            try {
                val stocks = stockDao.getAllStocksSync()
                _stockList.value = stocks
                
                // 默认选择第一只股票
                if (stocks.isNotEmpty() && _selectedStock.value == null) {
                    selectStock(stocks.first())
                }
            } catch (e: Exception) {
                _uiState.value = HistoryComparisonUiState.Error("加载股票列表失败: ${e.message}")
            }
        }
    }

    /**
     * 选择股票
     */
    fun selectStock(stock: Stock) {
        _selectedStock.value = stock
        loadSignalData(stock.symbol)
    }

    /**
     * 加载信号数据
     */
    fun loadSignalData(symbol: String) {
        _uiState.value = HistoryComparisonUiState.Loading
        
        viewModelScope.launch {
            try {
                // 计算日期范围
                val (startDate, endDate) = calculateDateRange(_timeRange.value)
                
                // 加载准确率统计
                val accuracy = historyComparisonService.validateSignals(
                    symbol = symbol,
                    startDate = startDate,
                    endDate = endDate
                )
                _signalAccuracy.value = accuracy
                
                // 加载对比结果
                historyComparisonService.comparePredictedVsActual(
                    symbol = symbol,
                    days = _timeRange.value.days
                ).collect { comparisons ->
                    _comparisonResults.value = applyFilterAndSort(comparisons)
                }
                
                _uiState.value = HistoryComparisonUiState.Success
            } catch (e: Exception) {
                _uiState.value = HistoryComparisonUiState.Error("加载数据失败: ${e.message}")
            }
        }
    }

    /**
     * 生成信号报告
     */
    fun generateReport() {
        val symbol = _selectedStock.value?.symbol ?: return
        
        viewModelScope.launch {
            try {
                val report = historyComparisonService.generateReport(
                    symbol = symbol,
                    days = _timeRange.value.days
                )
                _signalReport.value = report
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        _selectedStock.value?.let { loadSignalData(it.symbol) }
    }

    /**
     * 设置筛选类型
     */
    fun setFilterType(filterType: SignalFilterType) {
        _filterType.value = filterType
        applyCurrentFilter()
    }

    /**
     * 设置排序类型
     */
    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
        applyCurrentFilter()
    }

    /**
     * 设置时间范围
     */
    fun setTimeRange(timeRange: TimeRange) {
        _timeRange.value = timeRange
        refresh()
    }

    /**
     * 应用筛选和排序
     */
    private fun applyFilterAndSort(results: List<ComparisonResult>): List<ComparisonResult> {
        val filtered = when (_filterType.value) {
            SignalFilterType.ALL -> results
            SignalFilterType.CORRECT -> results.filter { it.isCorrect }
            SignalFilterType.INCORRECT -> results.filter { !it.isCorrect }
            SignalFilterType.WIN -> results.filter { it.outcome == Outcome.WIN }
            SignalFilterType.LOSS -> results.filter { it.outcome == Outcome.LOSS }
            SignalFilterType.BUY_SIGNALS -> results.filter { 
                it.signalType.contains("BUY") 
            }
            SignalFilterType.SELL_SIGNALS -> results.filter { 
                it.signalType.contains("SELL") 
            }
        }
        
        return when (_sortType.value) {
            SortType.DATE_DESC -> filtered.sortedByDescending { it.signalDate }
            SortType.DATE_ASC -> filtered.sortedBy { it.signalDate }
            SortType.RETURN_DESC -> filtered.sortedByDescending { it.priceChangePercent }
            SortType.RETURN_ASC -> filtered.sortedBy { it.priceChangePercent }
            SortType.SCORE_DESC -> filtered.sortedByDescending { it.isCorrect }
        }
    }

    private fun applyCurrentFilter() {
        _comparisonResults.value = applyFilterAndSort(_comparisonResults.value)
    }

    /**
     * 计算日期范围
     */
    private fun calculateDateRange(range: TimeRange): Pair<Date?, Date?> {
        val calendar = java.util.Calendar.getInstance()
        val endDate = calendar.time
        
        val startDate = when (range) {
            TimeRange.LAST_7_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
                calendar.time
            }
            TimeRange.LAST_30_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -30)
                calendar.time
            }
            TimeRange.LAST_90_DAYS -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -90)
                calendar.time
            }
            TimeRange.ALL -> null
        }
        
        return startDate to endDate
    }

    /**
     * 获取筛选后的结果
     */
    fun getFilteredResults(): List<ComparisonResult> {
        return _comparisonResults.value
    }

    /**
     * 获取统计数据
     */
    fun getStatistics(): ComparisonStatistics {
        val results = _comparisonResults.value
        val accuracy = _signalAccuracy.value
        
        return ComparisonStatistics(
            totalSignals = results.size,
            correctSignals = results.count { it.isCorrect },
            incorrectSignals = results.count { !it.isCorrect },
            winCount = results.count { it.outcome == Outcome.WIN },
            lossCount = results.count { it.outcome == Outcome.LOSS },
            neutralCount = results.count { it.outcome == Outcome.NEUTRAL },
            avgReturn = if (results.isNotEmpty()) results.map { it.priceChangePercent }.average() else 0.0,
            maxReturn = results.maxOfOrNull { it.priceChangePercent } ?: 0.0,
            minReturn = results.minOfOrNull { it.priceChangePercent } ?: 0.0,
            overallAccuracy = accuracy?.winRate ?: 0.0,
            directionAccuracy = accuracy?.directionAccuracy ?: 0.0
        )
    }
}

/**
 * UI 状态
 */
sealed class HistoryComparisonUiState {
    object Loading : HistoryComparisonUiState()
    object Success : HistoryComparisonUiState()
    data class Error(val message: String) : HistoryComparisonUiState()
}

/**
 * 信号筛选类型
 */
enum class SignalFilterType {
    ALL,            // 全部
    CORRECT,        // 预测正确
    INCORRECT,      // 预测错误
    WIN,            // 盈利
    LOSS,           // 亏损
    BUY_SIGNALS,    // 买入信号
    SELL_SIGNALS    // 卖出信号
}

/**
 * 排序类型
 */
enum class SortType {
    DATE_DESC,      // 日期降序
    DATE_ASC,       // 日期升序
    RETURN_DESC,    // 收益降序
    RETURN_ASC,     // 收益升序
    SCORE_DESC      // 评分降序
}

/**
 * 时间范围
 */
enum class TimeRange(val days: Int, val label: String) {
    LAST_7_DAYS(7, "近7天"),
    LAST_30_DAYS(30, "近30天"),
    LAST_90_DAYS(90, "近90天"),
    ALL(365, "全部")
}

/**
 * 对比统计
 */
data class ComparisonStatistics(
    val totalSignals: Int,
    val correctSignals: Int,
    val incorrectSignals: Int,
    val winCount: Int,
    val lossCount: Int,
    val neutralCount: Int,
    val avgReturn: Double,
    val maxReturn: Double,
    val minReturn: Double,
    val overallAccuracy: Double,
    val directionAccuracy: Double
) {
    val winRate: Double
        get() = if ((winCount + lossCount) > 0) {
            (winCount.toDouble() / (winCount + lossCount)) * 100
        } else 0.0
    
    val accuracyRate: Double
        get() = if (totalSignals > 0) {
            (correctSignals.toDouble() / totalSignals) * 100
        } else 0.0
}
