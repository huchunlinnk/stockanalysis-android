package com.example.stockanalysis.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.databinding.ItemMetricBinding

/**
 * 指标展示适配器
 * 用于展示财务指标、成长性指标、机构持仓等数据
 */
class MetricAdapter : ListAdapter<MetricAdapter.MetricItem, MetricAdapter.MetricViewHolder>(MetricDiffCallback()) {

    data class MetricItem(
        val name: String,
        val value: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetricViewHolder {
        val binding = ItemMetricBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MetricViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetricViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MetricViewHolder(
        private val binding: ItemMetricBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MetricItem) {
            binding.tvMetricName.text = item.name
            binding.tvMetricValue.text = item.value
        }
    }

    private class MetricDiffCallback : DiffUtil.ItemCallback<MetricItem>() {
        override fun areItemsTheSame(oldItem: MetricItem, newItem: MetricItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: MetricItem, newItem: MetricItem): Boolean {
            return oldItem == newItem
        }
    }
}
