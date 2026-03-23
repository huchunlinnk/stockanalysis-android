package com.example.stockanalysis.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.R
import com.example.stockanalysis.data.analysis.ComparisonResult
import com.example.stockanalysis.data.analysis.Direction
import com.example.stockanalysis.data.analysis.FirstHit
import com.example.stockanalysis.data.analysis.Outcome
import com.example.stockanalysis.data.model.Stock
import com.example.stockanalysis.databinding.FragmentHistoryComparisonBinding
import com.example.stockanalysis.ui.viewmodel.*
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 历史信号对比 Fragment
 */
@AndroidEntryPoint
class HistoryComparisonFragment : Fragment() {

    private var _binding: FragmentHistoryComparisonBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryComparisonViewModel by viewModels()
    private lateinit var adapter: SignalComparisonAdapter

    private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryComparisonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCharts()
        setupSpinners()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SignalComparisonAdapter(
            onItemClick = { result ->
                showSignalDetail(result)
            }
        )
        
        binding.rvSignals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryComparisonFragment.adapter
        }
    }

    private fun setupCharts() {
        // 准确率饼图
        binding.pieChartAccuracy.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false)
            legend.orientation = Legend.LegendOrientation.VERTICAL
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
        }

        // 收益对比柱状图
        binding.barChartReturns.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setMaxVisibleValueCount(20)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = -20f
                axisMaximum = 20f
            }
            
            axisRight.isEnabled = false
        }

        // 准确率趋势折线图
        binding.lineChartTrend.apply {
            description.isEnabled = false
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 100f
                valueFormatter = PercentFormatter()
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun setupSpinners() {
        // 股票选择
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stockList.collect { stocks ->
                    setupStockSpinner(stocks)
                }
            }
        }

        // 时间范围选择
        val timeRanges = TimeRange.values().map { it.label }
        binding.spinnerTimeRange.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            timeRanges
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // 筛选类型
        val filters = SignalFilterType.values().map {
            when (it) {
                SignalFilterType.ALL -> "全部信号"
                SignalFilterType.CORRECT -> "预测正确"
                SignalFilterType.INCORRECT -> "预测错误"
                SignalFilterType.WIN -> "盈利信号"
                SignalFilterType.LOSS -> "亏损信号"
                SignalFilterType.BUY_SIGNALS -> "买入信号"
                SignalFilterType.SELL_SIGNALS -> "卖出信号"
            }
        }
        binding.spinnerFilter.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filters
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // 排序方式
        val sorts = SortType.values().map {
            when (it) {
                SortType.DATE_DESC -> "日期降序"
                SortType.DATE_ASC -> "日期升序"
                SortType.RETURN_DESC -> "收益降序"
                SortType.RETURN_ASC -> "收益升序"
                SortType.SCORE_DESC -> "评分降序"
            }
        }
        binding.spinnerSort.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sorts
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setupStockSpinner(stocks: List<Stock>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            stocks.map { "${it.name} (${it.symbol})" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerStock.adapter = adapter
    }

    private fun setupListeners() {
        // 股票选择
        binding.spinnerStock.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.stockList.value.getOrNull(position)?.let {
                    viewModel.selectStock(it)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 时间范围
        binding.spinnerTimeRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setTimeRange(TimeRange.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 筛选
        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setFilterType(SignalFilterType.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 排序
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setSortType(SortType.values()[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 刷新
        binding.btnRefresh.setOnClickListener {
            viewModel.refresh()
        }

        // 生成报告
        binding.btnGenerateReport.setOnClickListener {
            viewModel.generateReport()
        }
    }

    private fun observeViewModel() {
        // UI 状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HistoryComparisonUiState.Loading -> showLoading()
                        is HistoryComparisonUiState.Success -> showContent()
                        is HistoryComparisonUiState.Error -> showError(state.message)
                    }
                }
            }
        }

        // 对比结果
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.comparisonResults.collect { results ->
                    adapter.submitList(results)
                    updateStatistics(results)
                    updateCharts(results)
                }
            }
        }

        // 准确率统计
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signalAccuracy.collect { accuracy ->
                    accuracy?.let { updateAccuracyDisplay(it) }
                }
            }
        }

        // 信号报告
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signalReport.collect { report ->
                    report?.let { showReportDialog(it) }
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.contentContainer.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentContainer.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun updateStatistics(results: List<ComparisonResult>) {
        val stats = viewModel.getStatistics()
        
        binding.tvTotalSignals.text = "${stats.totalSignals}"
        binding.tvAccuracy.text = String.format("%.1f%%", stats.accuracyRate)
        binding.tvWinRate.text = String.format("%.1f%%", stats.winRate)
        binding.tvAvgReturn.text = String.format("%.2f%%", stats.avgReturn)
        
        // 设置颜色
        val positiveColor = ContextCompat.getColor(requireContext(), R.color.trend_up)
        val negativeColor = ContextCompat.getColor(requireContext(), R.color.trend_down)
        
        binding.tvAvgReturn.setTextColor(if (stats.avgReturn >= 0) positiveColor else negativeColor)
    }

    private fun updateCharts(results: List<ComparisonResult>) {
        updateAccuracyPieChart(results)
        updateReturnsBarChart(results)
        updateTrendLineChart(results)
    }

    private fun updateAccuracyPieChart(results: List<ComparisonResult>) {
        val correct = results.count { it.isCorrect }
        val incorrect = results.count { !it.isCorrect }
        
        if (correct + incorrect == 0) {
            binding.pieChartAccuracy.clear()
            return
        }
        
        val entries = listOf(
            PieEntry(correct.toFloat(), "正确"),
            PieEntry(incorrect.toFloat(), "错误")
        )
        
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.trend_up),
                ContextCompat.getColor(requireContext(), R.color.trend_down)
            )
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        
        binding.pieChartAccuracy.data = PieData(dataSet)
        binding.pieChartAccuracy.invalidate()
    }

    private fun updateReturnsBarChart(results: List<ComparisonResult>) {
        if (results.isEmpty()) {
            binding.barChartReturns.clear()
            return
        }
        
        val entries = results.take(20).mapIndexed { index, result ->
            BarEntry(index.toFloat(), result.priceChangePercent.toFloat())
        }
        
        val dataSet = BarDataSet(entries, "收益率(%)").apply {
            colors = results.take(20).map {
                if (it.priceChangePercent >= 0) {
                    ContextCompat.getColor(requireContext(), R.color.trend_up)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.trend_down)
                }
            }
            valueTextSize = 10f
        }
        
        binding.barChartReturns.data = BarData(dataSet)
        binding.barChartReturns.xAxis.valueFormatter = IndexAxisValueFormatter(
            results.take(20).map { dateFormat.format(it.signalDate) }
        )
        binding.barChartReturns.invalidate()
    }

    private fun updateTrendLineChart(results: List<ComparisonResult>) {
        if (results.size < 5) {
            binding.lineChartTrend.clear()
            return
        }
        
        // 计算滑动窗口准确率
        val windowSize = minOf(5, results.size / 2)
        val sorted = results.sortedBy { it.signalDate }
        
        val entries = mutableListOf<Entry>()
        for (i in 0..sorted.size - windowSize) {
            val window = sorted.subList(i, i + windowSize)
            val accuracy = window.count { it.isCorrect }.toFloat() / windowSize * 100
            entries.add(Entry(i.toFloat(), accuracy))
        }
        
        val dataSet = LineDataSet(entries, "准确率趋势(%)").apply {
            color = ContextCompat.getColor(requireContext(), R.color.purple_500)
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
        }
        
        binding.lineChartTrend.data = LineData(dataSet)
        binding.lineChartTrend.invalidate()
    }

    private fun updateAccuracyDisplay(accuracy: com.example.stockanalysis.data.analysis.SignalAccuracy) {
        binding.tvDirectionAccuracy.text = String.format("方向准确率: %.1f%%", accuracy.directionAccuracy)
        binding.tvBuyAccuracy.text = accuracy.buySignalAccuracy?.let {
            String.format("买入信号准确率: %.1f%%", it)
        } ?: "买入信号准确率: -"
        binding.tvSellAccuracy.text = accuracy.sellSignalAccuracy?.let {
            String.format("卖出信号准确率: %.1f%%", it)
        } ?: "卖出信号准确率: -"
    }

    private fun showSignalDetail(result: ComparisonResult) {
        val message = buildString {
            appendLine("股票: ${result.stockSymbol}")
            appendLine("日期: ${fullDateFormat.format(result.signalDate)}")
            appendLine("信号: ${getSignalDisplayName(result.signalType)}")
            appendLine("预测方向: ${getDirectionDisplay(result.predictedDirection)}")
            appendLine("实际方向: ${getDirectionDisplay(result.actualDirection)}")
            appendLine("预测结果: ${if (result.isCorrect) "✓ 正确" else "✗ 错误"}")
            appendLine("")
            appendLine("价格信息:")
            appendLine("  信号时: ¥${String.format("%.2f", result.priceAtSignal)}")
            appendLine("  实际: ¥${String.format("%.2f", result.actualPrice)}")
            result.predictedPrice?.let {
                appendLine("  预测目标: ¥${String.format("%.2f", it)}")
            }
            appendLine("  变化: ${String.format("%.2f", result.priceChangePercent)}%")
            appendLine("")
            appendLine("止损止盈:")
            appendLine("  止损触发: ${result.hitStopLoss?.let { if (it) "是" else "否" } ?: "-"}")
            appendLine("  止盈触发: ${result.hitTakeProfit?.let { if (it) "是" else "否" } ?: "-"}")
            appendLine("  首次触发: ${getFirstHitDisplay(result.firstHit)}")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("信号详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showReportDialog(report: com.example.stockanalysis.data.analysis.SignalReport) {
        val message = buildString {
            appendLine("股票: ${report.stockSymbol}")
            appendLine("统计周期: ${report.periodDays}天")
            appendLine("")
            appendLine("整体表现:")
            appendLine("  胜率: ${String.format("%.1f", report.accuracy.winRate)}%")
            appendLine("  方向准确率: ${String.format("%.1f", report.accuracy.directionAccuracy)}%")
            appendLine("  平均收益: ${String.format("%.2f", report.accuracy.avgReturn)}%")
            appendLine("")
            appendLine("建议:")
            report.recommendations.forEach { recommendation ->
                appendLine("  • $recommendation")
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("信号分析报告")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun getSignalDisplayName(signalType: String): String {
        return when (signalType) {
            "STRONG_BUY" -> "强烈买入"
            "BUY" -> "买入"
            "HOLD" -> "持有/观望"
            "SELL" -> "卖出"
            "STRONG_SELL" -> "强烈卖出"
            else -> signalType
        }
    }

    private fun getDirectionDisplay(direction: Direction): String {
        return when (direction) {
            Direction.UP -> "上涨 ↑"
            Direction.DOWN -> "下跌 ↓"
            Direction.FLAT -> "震荡 →"
            Direction.NOT_DOWN -> "不跌"
        }
    }

    private fun getFirstHitDisplay(firstHit: FirstHit): String {
        return when (firstHit) {
            FirstHit.STOP_LOSS -> "止损"
            FirstHit.TAKE_PROFIT -> "止盈"
            FirstHit.AMBIGUOUS -> "同时触发"
            FirstHit.NEITHER -> "都未触发"
            FirstHit.NOT_APPLICABLE -> "不适用"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * 信号对比列表 DiffCallback
 */
private class SignalComparisonDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<ComparisonResult>() {
    override fun areItemsTheSame(oldItem: ComparisonResult, newItem: ComparisonResult): Boolean = 
        oldItem.signalId == newItem.signalId
    override fun areContentsTheSame(oldItem: ComparisonResult, newItem: ComparisonResult): Boolean = 
        oldItem == newItem
}

/**
 * 信号对比列表适配器
 */
class SignalComparisonAdapter(
    private val onItemClick: (ComparisonResult) -> Unit
) : androidx.recyclerview.widget.ListAdapter<ComparisonResult, SignalComparisonAdapter.ViewHolder>(
    SignalComparisonDiffCallback()
) {
    private val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_signal_comparison, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val tvDate: android.widget.TextView = itemView.findViewById(R.id.tvDate)
        private val tvSignal: android.widget.TextView = itemView.findViewById(R.id.tvSignal)
        private val tvPredicted: android.widget.TextView = itemView.findViewById(R.id.tvPredicted)
        private val tvActual: android.widget.TextView = itemView.findViewById(R.id.tvActual)
        private val tvReturn: android.widget.TextView = itemView.findViewById(R.id.tvReturn)
        private val ivStatus: android.widget.ImageView = itemView.findViewById(R.id.ivStatus)

        fun bind(result: ComparisonResult) {
            tvDate.text = dateFormat.format(result.signalDate)
            tvSignal.text = getSignalDisplayName(result.signalType)
            tvPredicted.text = getDirectionDisplay(result.predictedDirection)
            tvActual.text = getDirectionDisplay(result.actualDirection)
            tvReturn.text = String.format("%.1f%%", result.priceChangePercent)
            
            // 设置颜色
            val context = itemView.context
            val colorRes = when {
                result.isCorrect && result.outcome == Outcome.WIN -> R.color.trend_up
                result.isCorrect && result.outcome == Outcome.LOSS -> R.color.trend_down
                result.isCorrect -> R.color.trend_up
                else -> R.color.trend_down
            }
            tvReturn.setTextColor(ContextCompat.getColor(context, colorRes))
            
            // 状态图标
            ivStatus.setImageResource(
                if (result.isCorrect) R.drawable.ic_check_circle else R.drawable.ic_close_circle
            )
            ivStatus.setColorFilter(ContextCompat.getColor(context, colorRes))
            
            itemView.setOnClickListener { onItemClick(result) }
        }

        private fun getSignalDisplayName(signalType: String): String {
            return when (signalType) {
                "STRONG_BUY" -> "强烈买入"
                "BUY" -> "买入"
                "HOLD" -> "持有"
                "SELL" -> "卖出"
                "STRONG_SELL" -> "强烈卖出"
                else -> signalType
            }
        }

        private fun getDirectionDisplay(direction: Direction): String {
            return when (direction) {
                Direction.UP -> "涨"
                Direction.DOWN -> "跌"
                Direction.FLAT -> "平"
                Direction.NOT_DOWN -> "不跌"
            }
        }
    }
}
