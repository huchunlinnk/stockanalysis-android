package com.example.stockanalysis.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.databinding.ItemAnalysisResultBinding

class AnalysisResultAdapter(
    private val onItemClick: (AnalysisResult) -> Unit,
    private val onDeleteClick: (AnalysisResult) -> Unit
) : ListAdapter<AnalysisResult, AnalysisResultAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalysisResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAnalysisResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: AnalysisResult) {
            binding.tvStockName.text = "${result.stockName} (${result.stockSymbol})"
            binding.tvRecommendation.text = result.decision.name
            binding.tvScore.text = "${result.score}分"

            // 检测数据来源并显示标识
            val isMockData = result.fundamentalAnalysis?.valuation?.contains("[模拟数据]") == true ||
                             result.fundamentalAnalysis?.growth?.contains("[模拟数据]") == true

            if (isMockData) {
                binding.tvDataSource.visibility = View.VISIBLE
                binding.tvDataSource.text = "⚠️ 基本面数据为模拟数据（未配置 Tushare Token）"
                binding.tvDataSource.setTextColor(Color.parseColor("#FFA500")) // Orange
            } else if (result.fundamentalAnalysis != null) {
                binding.tvDataSource.visibility = View.VISIBLE
                binding.tvDataSource.text = "✅ 基本面数据来自 Tushare Pro"
                binding.tvDataSource.setTextColor(Color.parseColor("#4CAF50")) // Green
            } else {
                binding.tvDataSource.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(result)
            }

            binding.root.setOnLongClickListener {
                onDeleteClick(result)
                true
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
