package com.example.stockanalysis.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.backtest.BacktestResult
import com.example.stockanalysis.databinding.ItemBacktestResultBinding
import java.text.SimpleDateFormat
import java.util.Locale

class BacktestResultAdapter(
    private val onItemClick: (BacktestResult) -> Unit,
    private val onDeleteClick: (BacktestResult) -> Unit
) : ListAdapter<BacktestResult, BacktestResultAdapter.ResultViewHolder>(ResultDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemBacktestResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ResultViewHolder(
        private val binding: ItemBacktestResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: BacktestResult) {
            binding.apply {
                // 基本信息
                tvStockSymbol.text = result.stockSymbol
                tvStockName.text = result.stockName
                tvStrategy.text = result.strategyName
                tvPeriod.text = "${dateFormat.format(result.startDate)} - ${dateFormat.format(result.endDate)}"

                // 统计数据
                tvAccuracy.text = String.format("准确率: %.2f%%", result.accuracy)
                tvTotalReturn.text = String.format("收益率: %.2f%%", result.totalReturn)
                tvWinRate.text = String.format("胜率: %.2f%%", result.winRate)
                tvSignalCount.text = "${result.totalSignals}个信号"

                // 设置收益率颜色
                val returnColor = if (result.totalReturn >= 0) {
                    android.graphics.Color.RED
                } else {
                    android.graphics.Color.GREEN
                }
                tvTotalReturn.setTextColor(returnColor)

                // 点击事件
                root.setOnClickListener {
                    onItemClick(result)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(result)
                }
            }
        }
    }

    private class ResultDiffCallback : DiffUtil.ItemCallback<BacktestResult>() {
        override fun areItemsTheSame(oldItem: BacktestResult, newItem: BacktestResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BacktestResult, newItem: BacktestResult): Boolean {
            return oldItem == newItem
        }
    }
}
