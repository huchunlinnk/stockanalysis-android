package com.example.stockanalysis.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.R
import com.example.stockanalysis.data.model.Stock
import com.example.stockanalysis.databinding.ItemStockSelectionBinding

/**
 * 股票选择适配器
 * 用于分析页面显示自选股列表并支持多选
 */
class StockSelectionAdapter(
    private val onSelectionChanged: (Stock, Boolean) -> Unit
) : ListAdapter<Stock, StockSelectionAdapter.StockSelectionViewHolder>(StockDiffCallback()) {

    // 保存选中状态的集合
    private val selectedStocks = mutableSetOf<Stock>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockSelectionViewHolder {
        val binding = ItemStockSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StockSelectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockSelectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 设置已选中的股票列表
     */
    fun setSelectedStocks(stocks: List<Stock>) {
        selectedStocks.clear()
        selectedStocks.addAll(stocks)
        notifyDataSetChanged()
    }

    /**
     * 获取当前选中的股票列表
     */
    fun getSelectedStocks(): List<Stock> = selectedStocks.toList()

    /**
     * 清除所有选择
     */
    fun clearSelection() {
        selectedStocks.clear()
        notifyDataSetChanged()
    }

    /**
     * 切换选择状态
     */
    private fun toggleSelection(stock: Stock) {
        val isSelected = if (selectedStocks.contains(stock)) {
            selectedStocks.remove(stock)
            false
        } else {
            selectedStocks.add(stock)
            true
        }
        onSelectionChanged(stock, isSelected)
        notifyItemChanged(currentList.indexOf(stock))
    }

    inner class StockSelectionViewHolder(
        private val binding: ItemStockSelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: Stock) {
            binding.tvStockName.text = stock.name
            binding.tvStockSymbol.text = stock.symbol
            
            // 根据市场类型显示不同图标或标签
            binding.tvMarketType.text = when (stock.market) {
                com.example.stockanalysis.data.model.MarketType.A_SHARE -> "A股"
                com.example.stockanalysis.data.model.MarketType.HK -> "港股"
                com.example.stockanalysis.data.model.MarketType.US -> "美股"
            }

            // 检查是否被选中
            val isSelected = selectedStocks.contains(stock)
            updateSelectionState(isSelected)

            // 点击切换选择状态
            binding.root.setOnClickListener {
                toggleSelection(stock)
            }

            // 点击选择框也切换状态
            binding.checkboxSelection.setOnClickListener {
                toggleSelection(stock)
            }
        }

        private fun updateSelectionState(isSelected: Boolean) {
            binding.checkboxSelection.isChecked = isSelected
            
            if (isSelected) {
                // 选中状态：显示高亮背景
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.selected_item_background)
                )
                binding.ivSelectedIndicator.visibility = View.VISIBLE
            } else {
                // 未选中状态：默认背景
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.white)
                )
                binding.ivSelectedIndicator.visibility = View.GONE
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
