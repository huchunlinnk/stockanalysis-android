package com.example.stockanalysis.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.Decision
import com.example.stockanalysis.databinding.ItemReportSummaryBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 报告摘要适配器
 * 用于在首页展示最近的分析报告摘要
 */
class ReportSummaryAdapter(
    private val onItemClick: (AnalysisResult) -> Unit,
    private val onDeleteClick: (AnalysisResult) -> Unit
) : ListAdapter<AnalysisResult, ReportSummaryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemReportSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        fun bind(result: AnalysisResult) {
            // 股票信息
            binding.tvStockName.text = result.stockName
            binding.tvStockSymbol.text = result.stockSymbol

            // 分析时间
            binding.tvAnalysisTime.text = dateFormat.format(result.analysisTime)

            // 决策建议和评分
            binding.tvDecision.text = getDecisionText(result.decision)
            binding.tvScore.text = "${result.score}分"

            // 根据决策类型设置颜色
            val decisionColor = when (result.decision) {
                Decision.STRONG_BUY -> Color.parseColor("#D32F2F") // 深红色
                Decision.BUY -> Color.parseColor("#F57C00") // 橙色
                Decision.HOLD -> Color.parseColor("#757575") // 灰色
                Decision.SELL -> Color.parseColor("#388E3C") // 绿色
                Decision.STRONG_SELL -> Color.parseColor("#1976D2") // 蓝色
            }
            binding.tvDecision.setTextColor(decisionColor)

            // 摘要内容
            binding.tvSummary.text = result.summary

            // 技术分析摘要
            if (result.technicalAnalysis != null) {
                binding.tvTechnicalSummary.visibility = View.VISIBLE
                binding.tvTechnicalSummary.text = "技术: ${result.technicalAnalysis.trend}"
            } else {
                binding.tvTechnicalSummary.visibility = View.GONE
            }

            // 基本面摘要
            if (result.fundamentalAnalysis != null) {
                binding.tvFundamentalSummary.visibility = View.VISIBLE
                binding.tvFundamentalSummary.text = "基本面: ${result.fundamentalAnalysis.valuation}"
            } else {
                binding.tvFundamentalSummary.visibility = View.GONE
            }

            // 新闻摘要
            if (result.sentimentAnalysis != null && result.sentimentAnalysis.keyNews.isNotEmpty()) {
                binding.tvNewsSummary.visibility = View.VISIBLE
                val newsCount = result.sentimentAnalysis.keyNews.size
                binding.tvNewsSummary.text = "新闻: ${result.sentimentAnalysis.overallSentiment} ($newsCount 条)"
            } else {
                binding.tvNewsSummary.visibility = View.GONE
            }

            // 置信度指示器
            binding.tvConfidence.text = when (result.confidence) {
                com.example.stockanalysis.data.model.ConfidenceLevel.HIGH -> "高"
                com.example.stockanalysis.data.model.ConfidenceLevel.MEDIUM -> "中"
                com.example.stockanalysis.data.model.ConfidenceLevel.LOW -> "低"
            }

            val confidenceColor = when (result.confidence) {
                com.example.stockanalysis.data.model.ConfidenceLevel.HIGH -> Color.parseColor("#4CAF50")
                com.example.stockanalysis.data.model.ConfidenceLevel.MEDIUM -> Color.parseColor("#FFA726")
                com.example.stockanalysis.data.model.ConfidenceLevel.LOW -> Color.parseColor("#EF5350")
            }
            binding.tvConfidence.setTextColor(confidenceColor)

            // 点击查看详情
            binding.root.setOnClickListener {
                onItemClick(result)
            }

            // 删除按钮
            binding.btnDelete.setOnClickListener {
                onDeleteClick(result)
            }
        }

        private fun getDecisionText(decision: Decision): String {
            return when (decision) {
                Decision.STRONG_BUY -> "强烈买入"
                Decision.BUY -> "买入"
                Decision.HOLD -> "持有"
                Decision.SELL -> "卖出"
                Decision.STRONG_SELL -> "强烈卖出"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AnalysisResult>() {
        override fun areItemsTheSame(oldItem: AnalysisResult, newItem: AnalysisResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AnalysisResult, newItem: AnalysisResult): Boolean {
            return oldItem == newItem
        }
    }
}
