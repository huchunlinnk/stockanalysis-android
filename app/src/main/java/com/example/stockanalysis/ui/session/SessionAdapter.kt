package com.example.stockanalysis.ui.session

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.model.ChatSession
import com.example.stockanalysis.data.model.SessionType
import com.example.stockanalysis.databinding.ItemChatSessionBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 会话列表适配器
 */
class SessionAdapter(
    private val onSessionClick: (ChatSession) -> Unit,
    private val onMoreClick: (ChatSession, View) -> Unit
) : ListAdapter<ChatSession, SessionAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemChatSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: ChatSession) {
            binding.tvTitle.text = session.title
            binding.tvTime.text = dateFormat.format(session.updatedAt)

            // 显示股票代码
            if (session.stockSymbol != null) {
                binding.tvStockSymbol.visibility = View.VISIBLE
                binding.tvStockSymbol.text = session.stockSymbol
            } else {
                binding.tvStockSymbol.visibility = View.GONE
            }

            // 显示会话类型
            binding.tvType.text = when (session.sessionType) {
                SessionType.GENERAL -> "普通对话"
                SessionType.STOCK_ANALYSIS -> "股票分析"
                SessionType.MULTI_AGENT -> "多Agent分析"
            }

            // 点击事件
            binding.root.setOnClickListener {
                onSessionClick(session)
            }

            binding.btnMore.setOnClickListener {
                onMoreClick(session, it)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ChatSession>() {
        override fun areItemsTheSame(oldItem: ChatSession, newItem: ChatSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatSession, newItem: ChatSession): Boolean {
            return oldItem == newItem
        }
    }
}
