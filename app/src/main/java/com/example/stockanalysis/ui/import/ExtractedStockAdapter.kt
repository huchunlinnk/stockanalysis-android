package com.example.stockanalysis.ui.import

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.import.ExtractedStock
import com.example.stockanalysis.databinding.ItemExtractedStockBinding

/**
 * 提取股票列表适配器
 */
class ExtractedStockAdapter : ListAdapter<ExtractedStock, ExtractedStockAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExtractedStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemExtractedStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: ExtractedStock) {
            binding.tvStockCode.text = stock.code
            binding.tvStockName.text = stock.name.ifBlank { "未知" }

            // 设置置信度颜色
            binding.tvConfidence.text = when (stock.confidence) {
                "high" -> "高"
                "medium" -> "中"
                "low" -> "低"
                else -> "未知"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ExtractedStock>() {
        override fun areItemsTheSame(oldItem: ExtractedStock, newItem: ExtractedStock): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: ExtractedStock, newItem: ExtractedStock): Boolean {
            return oldItem == newItem
        }
    }
}
