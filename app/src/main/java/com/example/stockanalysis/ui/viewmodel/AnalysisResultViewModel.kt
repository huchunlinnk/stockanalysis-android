package com.example.stockanalysis.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.FundamentalAnalysisResult
import com.example.stockanalysis.data.model.FundamentalData
import com.example.stockanalysis.data.model.ValuationMetrics
import com.example.stockanalysis.data.repository.AnalysisRepository
import com.example.stockanalysis.data.repository.FundamentalRepository
import com.example.stockanalysis.util.MarkdownExporter
import com.example.stockanalysis.util.ReportImageGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 分析结果ViewModel
 */
@HiltViewModel
class AnalysisResultViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val fundamentalRepository: FundamentalRepository
) : ViewModel() {

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult
    
    // 基本面分析结果
    private val _fundamentalAnalysis = MutableStateFlow<FundamentalAnalysisResult?>(null)
    val fundamentalAnalysis: StateFlow<FundamentalAnalysisResult?> = _fundamentalAnalysis
    
    // 是否正在加载基本面数据
    private val _isLoadingFundamental = MutableStateFlow(false)
    val isLoadingFundamental: StateFlow<Boolean> = _isLoadingFundamental

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    // 基本面数据错误信息
    private val _fundamentalError = MutableStateFlow<String?>(null)
    val fundamentalError: StateFlow<String?> = _fundamentalError

    private val _shareEvent = MutableSharedFlow<ShareEvent>()
    val shareEvent: SharedFlow<ShareEvent> = _shareEvent
    
    // 分享图片状态
    private val _shareImageState = MutableStateFlow<ShareImageState>(ShareImageState.Idle)
    val shareImageState: StateFlow<ShareImageState> = _shareImageState

    // Markdown 导出器
    private val markdownExporter = MarkdownExporter()

    /**
     * 加载分析结果
     */
    fun loadAnalysisResult(analysisId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = analysisRepository.getResultById(analysisId)
            if (result != null) {
                _analysisResult.value = result
                // 加载基本面数据
                loadFundamentalAnalysis(result.stockSymbol, result.stockName)
            } else {
                _errorMessage.value = "分析结果不存在"
            }
            _isLoading.value = false
        }
    }
    
    /**
     * 加载基本面分析数据
     */
    private fun loadFundamentalAnalysis(symbol: String, name: String) {
        viewModelScope.launch {
            _isLoadingFundamental.value = true
            _fundamentalError.value = null
            
            val currentPrice = _analysisResult.value?.actionPlan?.entryPrice
            
            fundamentalRepository.getFundamentalAnalysis(symbol, name, currentPrice)
                .fold(
                    onSuccess = { analysis ->
                        _fundamentalAnalysis.value = analysis
                    },
                    onFailure = { error ->
                        _fundamentalError.value = "基本面数据加载失败: ${error.message}"
                    }
                )
            
            _isLoadingFundamental.value = false
        }
    }
    
    /**
     * 刷新基本面数据
     */
    fun refreshFundamentalData() {
        _analysisResult.value?.let { result ->
            viewModelScope.launch {
                _isLoadingFundamental.value = true
                _fundamentalError.value = null
                
                val currentPrice = result.actionPlan?.entryPrice
                
                fundamentalRepository.refreshFundamentalData(result.stockSymbol, result.stockName)
                    .fold(
                        onSuccess = { data ->
                            // 重新计算分析结果
                            val analysis = fundamentalRepository.getFundamentalAnalysis(
                                result.stockSymbol, 
                                result.stockName,
                                currentPrice
                            )
                            analysis.fold(
                                onSuccess = { _fundamentalAnalysis.value = it },
                                onFailure = { _fundamentalError.value = "分析失败: ${it.message}" }
                            )
                        },
                        onFailure = { error ->
                            _fundamentalError.value = "刷新失败: ${error.message}"
                        }
                    )
                
                _isLoadingFundamental.value = false
            }
        }
    }

    /**
     * 开始新分析
     */
    fun startNewAnalysis(symbol: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            analysisRepository.analyzeStock(symbol, name).collect { state ->
                when (state) {
                    is com.example.stockanalysis.data.repository.AnalysisState.Loading -> {
                        _isLoading.value = true
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Progress -> {
                        // 显示进度
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.AgentResult -> {
                        // Agent 分析结果更新
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Success -> {
                        _isLoading.value = false
                        _analysisResult.value = state.result
                    }
                    is com.example.stockanalysis.data.repository.AnalysisState.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = state.message
                    }
                }
            }
        }
    }

    /**
     * 分享分析结果（文本形式）
     */
    fun shareResult() {
        viewModelScope.launch {
            _analysisResult.value?.let { result ->
                val content = buildShareContent(result)
                _shareEvent.emit(ShareEvent.Success(content))
            }
        }
    }
    
    /**
     * 生成分享图片
     * @param callback 回调函数，返回生成的 Bitmap（在主线程回调）
     */
    fun generateShareImage(callback: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            _shareImageState.value = ShareImageState.Generating
            
            val result = withContext(Dispatchers.Default) {
                try {
                    _analysisResult.value?.let { analysisResult ->
                        val generator = ReportImageGenerator(
                            com.example.stockanalysis.StockAnalysisApplication.getContext()
                        )
                        generator.generateReportImage(analysisResult)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            _shareImageState.value = if (result != null) {
                ShareImageState.Success(result)
            } else {
                ShareImageState.Error("生成图片失败")
            }
            
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }
    
    /**
     * 生成简洁版分享图片
     */
    fun generateCompactShareImage(callback: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            _shareImageState.value = ShareImageState.Generating
            
            val result = withContext(Dispatchers.Default) {
                try {
                    _analysisResult.value?.let { analysisResult ->
                        val generator = ReportImageGenerator(
                            com.example.stockanalysis.StockAnalysisApplication.getContext()
                        )
                        generator.generateCompactReportImage(analysisResult)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            _shareImageState.value = if (result != null) {
                ShareImageState.Success(result)
            } else {
                ShareImageState.Error("生成图片失败")
            }
            
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    /**
     * 刷新分析
     */
    fun refreshAnalysis() {
        _analysisResult.value?.let { result ->
            startNewAnalysis(result.stockSymbol, result.stockName)
        }
    }
    
    /**
     * 获取股票基本面数据
     * @param symbol 股票代码
     * @return 基本面数据
     */
    suspend fun getFundamentalData(symbol: String): FundamentalData? {
        return withContext(Dispatchers.IO) {
            fundamentalRepository.getFundamentalData(symbol).getOrNull()
        }
    }
    
    /**
     * 获取估值指标
     * @param symbol 股票代码
     * @return 估值指标
     */
    suspend fun getValuationMetrics(symbol: String): ValuationMetrics {
        return withContext(Dispatchers.IO) {
            fundamentalRepository.getValuationMetrics(symbol)
        }
    }
    
    /**
     * 获取财务指标
     * @param symbol 股票代码
     * @return 财务指标
     */
    suspend fun getFinancialIndicators(symbol: String): com.example.stockanalysis.data.model.FinancialIndicators {
        return withContext(Dispatchers.IO) {
            fundamentalRepository.getFinancialIndicators(symbol)
        }
    }

    /**
     * 删除分析结果
     */
    fun deleteAnalysisResult() {
        viewModelScope.launch {
            _analysisResult.value?.let { result ->
                analysisRepository.deleteResult(result.id)
                _analysisResult.value = null
            }
        }
    }

    /**
     * 导出为Markdown格式
     */
    fun exportToMarkdown(): String? {
        return _analysisResult.value?.let { result ->
            markdownExporter.exportToMarkdown(result)
        }
    }

    /**
     * 导出为简洁Markdown格式
     */
    fun exportToCompactMarkdown(): String? {
        return _analysisResult.value?.let { result ->
            markdownExporter.exportCompactMarkdown(result)
        }
    }

    private fun buildShareContent(result: AnalysisResult): String {
        return """
            📊 ${result.stockName}(${result.stockSymbol}) 分析报告
            
            ⏰ 分析时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(result.analysisTime)}
            
            🎯 决策建议: ${getDecisionText(result.decision)}
            
            📈 综合评分: ${result.score}/100
            
            📝 分析摘要:
            ${result.summary}
            
            ⚠️ 免责声明: 本分析仅供参考，不构成投资建议
        """.trimIndent()
    }
    
    private fun getDecisionText(decision: com.example.stockanalysis.data.model.Decision): String {
        return when (decision) {
            com.example.stockanalysis.data.model.Decision.STRONG_BUY -> "强烈买入 ⭐⭐⭐"
            com.example.stockanalysis.data.model.Decision.BUY -> "买入 ⭐⭐"
            com.example.stockanalysis.data.model.Decision.HOLD -> "持有观望 ➖"
            com.example.stockanalysis.data.model.Decision.SELL -> "卖出 📉"
            com.example.stockanalysis.data.model.Decision.STRONG_SELL -> "强烈卖出 📉📉"
        }
    }

    sealed class ShareEvent {
        data class Success(val content: String) : ShareEvent()
    }
    
    /**
     * 分享图片状态
     */
    sealed class ShareImageState {
        object Idle : ShareImageState()
        object Generating : ShareImageState()
        data class Success(val bitmap: Bitmap) : ShareImageState()
        data class Error(val message: String) : ShareImageState()
    }
}
