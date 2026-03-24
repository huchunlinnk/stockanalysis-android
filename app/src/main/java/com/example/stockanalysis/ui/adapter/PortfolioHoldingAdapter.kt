package com.example.stockanalysis.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.portfolio.PortfolioHolding
import com.example.stockanalysis.databinding.ItemPortfolioHoldingBinding
import java.text.NumberFormat
import java.util.Locale

class PortfolioHoldingAdapter(
    private val onItemClick: (PortfolioHolding) -> Unit,
    private val onTradeClick: (PortfolioHolding) -> Unit
) : ListAdapter<PortfolioHolding, PortfolioHoldingAdapter.HoldingViewHolder>(HoldingDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val percentFormat = NumberFormat.getPercentInstance(Locale.CHINA).apply {
        maximumFractionDigits = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HoldingViewHolder {
        val binding = ItemPortfolioHoldingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HoldingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HoldingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HoldingViewHolder(
        private val binding: ItemPortfolioHoldingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(holding: PortfolioHolding) {
            binding.apply {
                // 基本信息
                tvStockSymbol.text = holding.stockSymbol
                tvStockName.text = holding.stockName

                // 持仓数量
                tvQuantity.text = "${holding.totalQuantity}股"
                tvAvailableQuantity.text = "可用: ${holding.availableQuantity}"

                // 成本和价格
                tvAverageCost.text = "成本: ${String.format("%.2f", holding.averageCost)}"
                tvCurrentPrice.text = "现价: ${String.format("%.2f", holding.currentPrice)}"

                // 市值
                tvMarketValue.text = currencyFormat.format(holding.marketValue)

                // 盈亏
                tvProfitLoss.text = currencyFormat.format(holding.profitLoss)
                tvProfitLossPercent.text = String.format("%.2f%%", holding.profitLossPercent)

                // 设置盈亏颜色
                val profitColor = if (holding.profitLoss >= 0) Color.RED else Color.GREEN
                tvProfitLoss.setTextColor(profitColor)
                tvProfitLossPercent.setTextColor(profitColor)

                // 点击事件
                root.setOnClickListener {
                    onItemClick(holding)
                }

                btnTrade.setOnClickListener {
                    onTradeClick(holding)
                }
            }
        }
    }

    private class HoldingDiffCallback : DiffUtil.ItemCallback<PortfolioHolding>() {
        override fun areItemsTheSame(oldItem: PortfolioHolding, newItem: PortfolioHolding): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PortfolioHolding, newItem: PortfolioHolding): Boolean {
            return oldItem == newItem
        }
    }
}
