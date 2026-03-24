package com.example.stockanalysis.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.model.RealtimeQuote
import com.example.stockanalysis.data.model.Stock
import com.example.stockanalysis.databinding.ItemStockBinding

class StockAdapter(
    private val onItemClick: (Stock) -> Unit,
    private val onDeleteClick: (Stock) -> Unit
) : ListAdapter<Stock, StockAdapter.StockViewHolder>(StockDiffCallback()) {

    // 行情数据映射表：股票代码 -> 实时行情
    private var quotes: Map<String, RealtimeQuote> = emptyMap()

    /**
     * 更新行情数据
     */
    fun submitQuotes(newQuotes: Map<String, RealtimeQuote>) {
        quotes = newQuotes
        // 通知数据变化以刷新显示
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = getItem(position)
        val quote = quotes[stock.symbol]
        holder.bind(stock, quote)
    }

    inner class StockViewHolder(
        private val binding: ItemStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: Stock, quote: RealtimeQuote?) {
            // 显示股票名称和代码
            binding.tvStockName.text = "${stock.name}\n${stock.symbol}"
            
            if (quote != null) {
                // 显示实时价格和涨跌幅
                binding.tvPrice.text = quote.getPriceText()
                binding.tvChange.text = quote.getChangePercentText()
                
                // 根据涨跌设置颜色（A股：红涨绿跌）
                val color = when {
                    quote.changePercent > 0 -> Color.parseColor("#FF4444") // 红色 - 涨
                    quote.changePercent < 0 -> Color.parseColor("#00AA00") // 绿色 - 跌
                    else -> Color.GRAY // 灰色 - 平
                }
                binding.tvPrice.setTextColor(color)
                binding.tvChange.setTextColor(color)
            } else {
                // 无行情数据时显示占位符
                binding.tvPrice.text = "--"
                binding.tvChange.text = "--"
                binding.tvPrice.setTextColor(Color.GRAY)
                binding.tvChange.setTextColor(Color.GRAY)
            }
            
            binding.root.setOnClickListener {
                onItemClick(stock)
            }
            
            binding.root.setOnLongClickListener {
                onDeleteClick(stock)
                true
            }
        }
    }

    class StockDiffCallback : DiffUtil.ItemCallback<Stock>() {
        override fun areItemsTheSame(oldItem: Stock, newItem: Stock): Boolean {
            return oldItem.symbol == newItem.symbol
        }

        override fun areContentsTheSame(oldItem: Stock, newItem: Stock): Boolean {
            return oldItem == newItem
        }
    }
}
