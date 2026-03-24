package com.example.stockanalysis.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.stockanalysis.databinding.ActivityAnalysisResultBinding
import com.example.stockanalysis.ui.dialog.ExportDialog
import com.example.stockanalysis.ui.viewmodel.AnalysisResultViewModel
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.util.MarkdownExporter
import com.example.stockanalysis.util.ReportImageGenerator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class AnalysisResultActivity : AppCompatActivity(), ExportDialog.ExportListener {

    private lateinit var binding: ActivityAnalysisResultBinding
    private val viewModel: AnalysisResultViewModel by viewModels()

    private var generatedBitmap: Bitmap? = null
    private val markdownExporter = MarkdownExporter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val analysisId = intent.getStringExtra(EXTRA_ANALYSIS_ID)
        val stockSymbol = intent.getStringExtra(EXTRA_STOCK_SYMBOL)
        val stockName = intent.getStringExtra(EXTRA_STOCK_NAME)
        
        if (analysisId != null) {
            viewModel.loadAnalysisResult(analysisId)
        } else if (stockSymbol != null && stockName != null) {
            viewModel.startNewAnalysis(stockSymbol, stockName)
        } else {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupToolbar()
        setupButtons()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupButtons() {
        // 分享按钮 - 显示导出对话框
        binding.btnShare.setOnClickListener {
            showExportDialog()
        }

        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshAnalysis()
        }

        // 删除按钮
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        // 查看详细基本面分析按钮
        binding.btnViewFundamentalDetail.setOnClickListener {
            showFundamentalDetailDialog()
        }
    }
    
    /**
     * 显示基本面详细分析对话框
     */
    private fun showFundamentalDetailDialog() {
        val analysis = viewModel.fundamentalAnalysis.value
        if (analysis == null) {
            Toast.makeText(this, "基本面数据加载中，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }
        
        val message = buildString {
            appendLine("【综合评分】${analysis.overallScore}/100")
            appendLine()
            appendLine("【估值分析】评分: ${analysis.valuationScore}")
            appendLine(analysis.valuationConclusion)
            appendLine()
            appendLine("【盈利能力】评分: ${analysis.profitabilityScore}")
            appendLine(analysis.profitabilityConclusion)
            appendLine()
            appendLine("【成长性】评分: ${analysis.growthScore}")
            appendLine(analysis.growthConclusion)
            appendLine()
            appendLine("【财务健康】评分: ${analysis.financialHealthScore}")
            appendLine(analysis.financialHealthConclusion)
            appendLine()
            appendLine("【分红情况】评分: ${analysis.dividendScore}")
            appendLine(analysis.dividendConclusion)
            appendLine()
            appendLine("【机构关注】评分: ${analysis.institutionScore}")
            appendLine(analysis.institutionConclusion)
            appendLine()
            if (analysis.riskFactors.isNotEmpty()) {
                appendLine("【风险提示】")
                analysis.riskFactors.forEach { risk ->
                    appendLine("• $risk")
                }
                appendLine()
            }
            appendLine("【投资建议】")
            appendLine(analysis.investmentAdvice)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("${analysis.name}(${analysis.symbol}) 基本面分析")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setNeutralButton("刷新数据") { _, _ ->
                viewModel.refreshFundamentalData()
                Toast.makeText(this, "正在刷新基本面数据...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    /**
     * 显示导出对话框
     */
    private fun showExportDialog() {
        val dialog = ExportDialog.newInstance()
        dialog.show(supportFragmentManager, ExportDialog.TAG)
    }

    // ==================== ExportDialog.ExportListener 接口实现 ====================

    override fun onExportMarkdown() {
        viewModel.analysisResult.value?.let { result ->
            val markdown = markdownExporter.exportToMarkdown(result)
            saveAndShareFile(markdown, "markdown", "text/markdown", ".md")
        } ?: run {
            Toast.makeText(this, "暂无分析数据", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onExportImage() {
        shareAsImage()
    }

    override fun onExportText() {
        shareAsText()
    }

    override fun onShareToOthers() {
        showShareOptionsDialog()
    }

    override fun onSaveToLocal() {
        showSaveOptionsDialog()
    }

    /**
     * 显示分享选项对话框
     */
    private fun showShareOptionsDialog() {
        val options = arrayOf(
            "分享为Markdown",
            "分享为图片",
            "分享为文本"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("选择分享格式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onExportMarkdown()
                    1 -> shareAsImage()
                    2 -> shareAsText()
                }
            }
            .show()
    }

    /**
     * 显示保存选项对话框
     */
    private fun showSaveOptionsDialog() {
        val options = arrayOf(
            "保存为Markdown",
            "保存为图片",
            "保存为文本"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("选择保存格式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> saveMarkdownToLocal()
                    1 -> saveToGallery()
                    2 -> saveTextToLocal()
                }
            }
            .show()
    }

    /**
     * 保存Markdown到本地
     */
    private fun saveMarkdownToLocal() {
        viewModel.analysisResult.value?.let { result ->
            val markdown = markdownExporter.exportToMarkdown(result)
            try {
                val filename = "stock_report_${result.stockSymbol}_${System.currentTimeMillis()}.md"
                val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), filename)

                file.writeText(markdown)

                Toast.makeText(this, "Markdown已保存到: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 保存文本到本地
     */
    private fun saveTextToLocal() {
        viewModel.analysisResult.value?.let { result ->
            val content = buildShareContent(result)
            try {
                val filename = "stock_report_${result.stockSymbol}_${System.currentTimeMillis()}.txt"
                val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), filename)

                file.writeText(content)

                Toast.makeText(this, "文本已保存到: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 保存文件并分享
     */
    private fun saveAndShareFile(content: String, type: String, mimeType: String, extension: String) {
        viewModel.analysisResult.value?.let { result ->
            try {
                val filename = "stock_report_${result.stockSymbol}_${System.currentTimeMillis()}$extension"
                val file = File(cacheDir, filename)

                file.writeText(content)

                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    this.type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "股票分析报告 - ${result.stockName}(${result.stockSymbol})")
                    putExtra(Intent.EXTRA_TEXT, "来自 StockAnalysis 的专业股票分析报告")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "分享报告"))

            } catch (e: IOException) {
                Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 分享为图片
     */
    private fun shareAsImage() {
        viewModel.generateShareImage { bitmap ->
            if (bitmap == null) {
                Toast.makeText(this, "生成图片失败", Toast.LENGTH_SHORT).show()
                return@generateShareImage
            }
            
            generatedBitmap = bitmap
            
            try {
                // 保存到缓存目录
                val file = File(cacheDir, "share_report_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                // 获取 FileProvider URI
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                
                // 创建分享 Intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "股票分析报告")
                    putExtra(Intent.EXTRA_TEXT, "来自 StockAnalysis 的专业股票分析报告")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "分享报告"))
                
            } catch (e: IOException) {
                Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 分享为文本
     */
    private fun shareAsText() {
        viewModel.shareResult()
    }
    
    /**
     * 保存到相册
     */
    private fun saveToGallery() {
        viewModel.generateShareImage { bitmap ->
            if (bitmap == null) {
                Toast.makeText(this, "生成图片失败", Toast.LENGTH_SHORT).show()
                return@generateShareImage
            }
            
            generatedBitmap = bitmap
            
            try {
                // 保存到 Pictures 目录
                val filename = "stock_report_${System.currentTimeMillis()}.png"
                val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), filename)
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                // 通知媒体库
                val uri = Uri.fromFile(file)
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
                
                Toast.makeText(this, "已保存到: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                
            } catch (e: IOException) {
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除分析结果")
            .setMessage("确定要删除这条分析结果吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteAnalysisResult()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun observeViewModel() {
        // 观察分析结果
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.analysisResult.collect { result ->
                    result?.let {
                        binding.tvStockName.text = it.stockName
                        binding.tvStockSymbol.text = it.stockSymbol
                        binding.tvDecision.text = getDecisionText(it.decision)
                        binding.tvScore.text = "${it.score}"
                        binding.tvSummary.text = it.summary
                        binding.tvAnalysisTime.text = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()
                        ).format(it.analysisTime)
                        
                        // 根据决策设置颜色
                        val decisionColor = getDecisionColor(it.decision)
                        binding.cardDecision.setCardBackgroundColor(decisionColor)
                        
                        // 显示基本面分析摘要
                        displayFundamentalSummary(it.fundamentalAnalysis)
                    }
                }
            }
        }
        
        // 观察基本面分析结果
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fundamentalAnalysis.collect { analysis ->
                    analysis?.let {
                        displayFundamentalAnalysis(it)
                    }
                }
            }
        }
        
        // 观察基本面数据加载状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoadingFundamental.collect { isLoading ->
                    // TODO: 如需显示基本面数据加载指示器，请在布局中添加 progressBarFundamental 视图
                    // binding.progressBarFundamental?.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }
        
        // 观察基本面数据错误
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fundamentalError.collect { error ->
                    error?.let {
                        // 显示基本面数据错误但不影响整体显示
                        android.util.Log.w("AnalysisResult", "基本面数据错误: $it")
                    }
                }
            }
        }
        
        // 观察错误信息
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Toast.makeText(this@AnalysisResultActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // 观察加载状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.btnRefresh.isEnabled = !isLoading
                    binding.btnShare.isEnabled = !isLoading
                }
            }
        }
        
        // 观察分享事件
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shareEvent.collect { event ->
                    when (event) {
                        is AnalysisResultViewModel.ShareEvent.Success -> {
                            shareText(event.content)
                        }
                    }
                }
            }
        }
    }
    
    private fun shareText(content: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, "股票分析报告")
        }
        startActivity(Intent.createChooser(shareIntent, "分享报告"))
    }

    /**
     * 构建分享内容（纯文本格式）
     */
    private fun buildShareContent(result: AnalysisResult): String {
        return buildString {
            appendLine("📊 ${result.stockName}(${result.stockSymbol}) 分析报告")
            appendLine()
            appendLine("⏰ 分析时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(result.analysisTime)}")
            appendLine()
            appendLine("🎯 决策建议: ${getDecisionText(result.decision)}")
            appendLine()
            appendLine("📈 综合评分: ${result.score}/100")
            appendLine()
            appendLine("📝 分析摘要:")
            appendLine(result.summary)
            appendLine()

            // 技术面分析
            result.technicalAnalysis?.let { tech: com.example.stockanalysis.data.model.TechnicalAnalysis ->
                appendLine("📈 技术面分析:")
                appendLine("趋势: ${tech.trend}")
                appendLine("均线: ${tech.maAlignment}")
                tech.supportLevel?.let { level: Double -> appendLine("支撑位: ¥%.2f".format(level)) }
                tech.resistanceLevel?.let { level: Double -> appendLine("阻力位: ¥%.2f".format(level)) }
                appendLine()
            }

            // 基本面分析
            result.fundamentalAnalysis?.let { fund: com.example.stockanalysis.data.model.FundamentalAnalysis ->
                appendLine("📊 基本面分析:")
                appendLine("估值: ${fund.valuation}")
                fund.growth?.let { appendLine("成长性: $it") }
                appendLine()
            }

            // 风险评估
            result.riskAssessment?.let { risk: com.example.stockanalysis.data.model.RiskAssessment ->
                appendLine("⚠️ 风险评估: ${getRiskText(risk.riskLevel)}")
                if (risk.specificRisks.isNotEmpty()) {
                    appendLine("风险因素:")
                    risk.specificRisks.forEach { riskItem: String -> appendLine("  - $riskItem") }
                }
                appendLine()
            }

            // 操作建议
            result.actionPlan?.let { action: com.example.stockanalysis.data.model.ActionPlan ->
                appendLine("🎯 操作建议:")
                action.entryPrice?.let { price: Double -> appendLine("建议买入价: ¥%.2f".format(price)) }
                action.targetPrice?.let { price: Double -> appendLine("目标价位: ¥%.2f".format(price)) }
                action.stopLossPrice?.let { price: Double -> appendLine("止损价位: ¥%.2f".format(price)) }
                appendLine()
            }

            appendLine("⚠️ 免责声明: 本分析仅供参考，不构成投资建议")
            appendLine()
            appendLine("---")
            appendLine("来自 StockAnalysis AI 智能分析")
        }
    }

    private fun getRiskText(riskLevel: com.example.stockanalysis.data.model.RiskLevel): String {
        return when (riskLevel) {
            com.example.stockanalysis.data.model.RiskLevel.LOW -> "低风险 🟢"
            com.example.stockanalysis.data.model.RiskLevel.MEDIUM -> "中等风险 🟡"
            com.example.stockanalysis.data.model.RiskLevel.HIGH -> "高风险 🔴"
        }
    }
    
    private fun getDecisionText(decision: com.example.stockanalysis.data.model.Decision): String {
        return when (decision) {
            com.example.stockanalysis.data.model.Decision.STRONG_BUY -> "强烈买入"
            com.example.stockanalysis.data.model.Decision.BUY -> "买入"
            com.example.stockanalysis.data.model.Decision.HOLD -> "持有观望"
            com.example.stockanalysis.data.model.Decision.SELL -> "卖出"
            com.example.stockanalysis.data.model.Decision.STRONG_SELL -> "强烈卖出"
        }
    }
    
    private fun getDecisionColor(decision: com.example.stockanalysis.data.model.Decision): Int {
        val colorRes = when (decision) {
            com.example.stockanalysis.data.model.Decision.STRONG_BUY -> android.R.color.holo_green_dark
            com.example.stockanalysis.data.model.Decision.BUY -> android.R.color.holo_green_light
            com.example.stockanalysis.data.model.Decision.HOLD -> android.R.color.holo_orange_light
            com.example.stockanalysis.data.model.Decision.SELL -> android.R.color.holo_red_light
            com.example.stockanalysis.data.model.Decision.STRONG_SELL -> android.R.color.holo_red_dark
        }
        return getColor(colorRes)
    }
    
    /**
     * 显示基本面分析摘要（从分析结果中）
     * 显示PE、PB、ROE、毛利率等关键指标
     */
    private fun displayFundamentalSummary(fundamental: com.example.stockanalysis.data.model.FundamentalAnalysis?) {
        if (fundamental == null) {
            // 数据为空时隐藏卡片
            binding.cardFundamental.visibility = View.GONE
            return
        }
        
        // 从分析结果中提取关键指标信息并显示
        // 注意：这里使用 fundamentalAnalysis StateFlow 中的详细数据来填充
        binding.cardFundamental.visibility = View.VISIBLE
    }
    
    /**
     * 格式化数值显示
     */
    private fun formatValue(value: Double?, suffix: String = ""): String {
        if (value == null) return "--"
        return when {
            value == Double.POSITIVE_INFINITY -> "∞"
            value == Double.NEGATIVE_INFINITY -> "-∞"
            value.isNaN() -> "--"
            suffix == "%" -> String.format("%.2f%%", value)
            else -> String.format("%.2f", value)
        }
    }
    
    /**
     * 显示详细的基本面分析
     * 从 FundamentalAnalysisResult 中提取关键指标显示在卡片上
     */
    private fun displayFundamentalAnalysis(analysis: com.example.stockanalysis.data.model.FundamentalAnalysisResult) {
        // 显示基本面卡片
        binding.cardFundamental.visibility = View.VISIBLE
        
        // 显示基本面分析中的盈利和成长性评分
        binding.tvRoe.text = "${analysis.profitabilityScore}分"
        binding.tvGrossMargin.text = "${analysis.growthScore}分"
        
        // 根据评分设置颜色
        binding.tvRoe.setTextColor(getScoreColor(analysis.profitabilityScore))
        binding.tvGrossMargin.setTextColor(getScoreColor(analysis.growthScore))
        
        // 从 repository 获取原始基本面数据来显示具体指标（PE、PB）
        viewModel.analysisResult.value?.let { result ->
            lifecycleScope.launch {
                try {
                    // 尝试获取详细的估值指标
                    val valuationMetrics = viewModel.getValuationMetrics(result.stockSymbol)
                    binding.tvPeRatio.text = formatValue(valuationMetrics.peTtm)
                    binding.tvPbRatio.text = formatValue(valuationMetrics.pbRatio)
                } catch (e: Exception) {
                    // 如果获取失败，保持默认值
                    android.util.Log.w("AnalysisResult", "获取估值指标失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 根据评分获取颜色
     */
    private fun getScoreColor(score: Int): Int {
        return when {
            score >= 80 -> getColor(android.R.color.holo_green_dark)
            score >= 60 -> getColor(android.R.color.holo_green_light)
            score >= 40 -> getColor(android.R.color.holo_orange_light)
            else -> getColor(android.R.color.holo_red_light)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理生成的 Bitmap
        generatedBitmap?.recycle()
        generatedBitmap = null
    }
    
    companion object {
        const val EXTRA_ANALYSIS_ID = "analysis_id"
        const val EXTRA_STOCK_SYMBOL = "stock_symbol"
        const val EXTRA_STOCK_NAME = "stock_name"
        
        fun createIntent(
            context: android.content.Context,
            analysisId: String? = null,
            stockSymbol: String? = null,
            stockName: String? = null
        ): Intent {
            return Intent(context, AnalysisResultActivity::class.java).apply {
                analysisId?.let { putExtra(EXTRA_ANALYSIS_ID, it) }
                stockSymbol?.let { putExtra(EXTRA_STOCK_SYMBOL, it) }
                stockName?.let { putExtra(EXTRA_STOCK_NAME, it) }
            }
        }
    }
}
