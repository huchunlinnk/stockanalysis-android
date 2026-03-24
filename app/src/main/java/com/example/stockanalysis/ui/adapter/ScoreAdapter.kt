package com.example.stockanalysis.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.databinding.ItemScoreBinding

/**
 * 评分展示适配器
 * 用于展示基本面各维度评分
 */
class ScoreAdapter : ListAdapter<ScoreAdapter.ScoreItem, ScoreAdapter.ScoreViewHolder>(ScoreDiffCallback()) {

    data class ScoreItem(
        val name: String,
        val score: Int,
        val conclusion: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val binding = ItemScoreBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScoreViewHolder(
        private val binding: ItemScoreBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScoreItem) {
            binding.tvScoreName.text = item.name
            binding.tvScoreValue.text = item.score.toString()
            binding.progressBar.progress = item.score

            // 根据评分设置颜色
            val color = when {
                item.score >= 80 -> android.graphics.Color.parseColor("#4CAF50") // 绿色
                item.score >= 60 -> android.graphics.Color.parseColor("#FF9800") // 橙色
                else -> android.graphics.Color.parseColor("#F44336") // 红色
            }
            binding.tvScoreValue.setTextColor(color)
        }
    }

    private class ScoreDiffCallback : DiffUtil.ItemCallback<ScoreItem>() {
        override fun areItemsTheSame(oldItem: ScoreItem, newItem: ScoreItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: ScoreItem, newItem: ScoreItem): Boolean {
            return oldItem == newItem
        }
    }
}
