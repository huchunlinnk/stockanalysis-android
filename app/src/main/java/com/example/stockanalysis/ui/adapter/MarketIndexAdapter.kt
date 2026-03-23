package com.example.stockanalysis.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.model.MarketIndex
import com.example.stockanalysis.data.model.Trend
import com.example.stockanalysis.databinding.ItemMarketIndexBinding

class MarketIndexAdapter : ListAdapter<MarketIndex, MarketIndexAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMarketIndexBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMarketIndexBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(index: MarketIndex) {
            // 显示指数名称（可选择性显示市场标签）
            binding.tvIndexName.text = index.name

            // 显示当前点数
            binding.tvIndexValue.text = String.format("%.2f", index.currentValue)

            // 显示涨跌幅
            val changePercent = index.changePercent
            val changeText = String.format("%+.2f%%", changePercent)
            binding.tvIndexChange.text = changeText

            // 根据市场类型和涨跌设置颜色
            val color = when (index.getTrend()) {
                Trend.UP -> {
                    // 美股和港股涨绿跌红，A股涨红跌绿
                    if (index.marketType == com.example.stockanalysis.data.model.MarketType.US ||
                        index.marketType == com.example.stockanalysis.data.model.MarketType.HK) {
                        Color.parseColor("#52C41A")  // 绿色
                    } else {
                        Color.parseColor("#FF4D4F")  // 红色
                    }
                }
                Trend.DOWN -> {
                    if (index.marketType == com.example.stockanalysis.data.model.MarketType.US ||
                        index.marketType == com.example.stockanalysis.data.model.MarketType.HK) {
                        Color.parseColor("#FF4D4F")  // 红色
                    } else {
                        Color.parseColor("#52C41A")  // 绿色
                    }
                }
                Trend.FLAT -> Color.parseColor("#999999")    // 灰色
            }
            binding.tvIndexChange.setTextColor(color)
            binding.tvIndexValue.setTextColor(color)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MarketIndex>() {
        override fun areItemsTheSame(oldItem: MarketIndex, newItem: MarketIndex): Boolean {
            return oldItem.symbol == newItem.symbol
        }

        override fun areContentsTheSame(oldItem: MarketIndex, newItem: MarketIndex): Boolean {
            return oldItem == newItem
        }
    }
}
