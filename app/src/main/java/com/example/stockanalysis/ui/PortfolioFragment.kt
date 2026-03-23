package com.example.stockanalysis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.databinding.FragmentPortfolioBinding
import com.example.stockanalysis.ui.adapter.PortfolioHoldingAdapter
import com.example.stockanalysis.ui.dialog.AddTransactionDialog
import com.example.stockanalysis.ui.dialog.TradeDialog
import com.example.stockanalysis.ui.viewmodel.PortfolioViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class PortfolioFragment : Fragment() {

    private var _binding: FragmentPortfolioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PortfolioViewModel by viewModels()
    private lateinit var holdingAdapter: PortfolioHoldingAdapter

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val percentFormat = NumberFormat.getPercentInstance(Locale.CHINA).apply {
        maximumFractionDigits = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPortfolioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        holdingAdapter = PortfolioHoldingAdapter(
            onItemClick = { holding ->
                // 跳转到股票详情
                // TODO: Navigate to stock detail
            },
            onTradeClick = { holding ->
                showTradeDialog(holding)
            }
        )

        binding.recyclerViewHoldings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = holdingAdapter
        }
    }

    private fun setupObservers() {
        // 观察持仓列表
        viewModel.allHoldings.observe(viewLifecycleOwner) { holdings ->
            holdingAdapter.submitList(holdings)
            binding.emptyView.visibility = if (holdings.isEmpty()) View.VISIBLE else View.GONE
        }

        // 观察汇总数据
        viewModel.portfolioSummary.observe(viewLifecycleOwner) { summary ->
            updateSummary(summary)
        }

        // 观察风险指标
        viewModel.riskMetrics.observe(viewLifecycleOwner) { metrics ->
            updateRiskMetrics(metrics)
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }

        // 观察错误
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // 观察交易结果
        viewModel.buyResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Snackbar.make(binding.root, "买入成功", Snackbar.LENGTH_SHORT).show()
            }.onFailure {
                Snackbar.make(binding.root, "买入失败: ${it.message}", Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.sellResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Snackbar.make(binding.root, "卖出成功", Snackbar.LENGTH_SHORT).show()
            }.onFailure {
                Snackbar.make(binding.root, "卖出失败: ${it.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshHoldingsValue()
        }

        binding.fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun updateSummary(summary: com.example.stockanalysis.data.portfolio.PortfolioSummary) {
        binding.apply {
            tvTotalAssets.text = currencyFormat.format(summary.totalAssets)
            tvMarketValue.text = currencyFormat.format(summary.totalMarketValue)
            tvCashBalance.text = currencyFormat.format(summary.cashBalance)

            tvTotalCost.text = currencyFormat.format(summary.totalCost)
            tvProfitLoss.text = currencyFormat.format(summary.totalProfitLoss)
            tvProfitLossPercent.text = String.format("%.2f%%", summary.totalProfitLossPercent)

            // 设置颜色
            val profitColor = if (summary.totalProfitLoss >= 0) {
                android.graphics.Color.RED
            } else {
                android.graphics.Color.GREEN
            }
            tvProfitLoss.setTextColor(profitColor)
            tvProfitLossPercent.setTextColor(profitColor)

            tvHoldingCount.text = summary.holdingCount.toString()
            tvStockCount.text = summary.stockCount.toString()
        }
    }

    private fun updateRiskMetrics(metrics: com.example.stockanalysis.data.portfolio.RiskMetrics) {
        binding.apply {
            metrics.winRate?.let {
                tvWinRate.text = String.format("%.2f%%", it)
            } ?: run {
                tvWinRate.text = "N/A"
            }

            metrics.profitFactor?.let {
                tvProfitFactor.text = String.format("%.2f", it)
            } ?: run {
                tvProfitFactor.text = "N/A"
            }

            metrics.maxDrawdown?.let {
                tvMaxDrawdown.text = String.format("%.2f%%", it)
            } ?: run {
                tvMaxDrawdown.text = "N/A"
            }
        }
    }

    private fun showAddTransactionDialog() {
        // TODO: Show dialog to add buy/sell transaction
        val dialog = AddTransactionDialog.newInstance()
        dialog.setOnTransactionListener { type, symbol, name, quantity, price, commission ->
            if (type == "BUY") {
                viewModel.buyStock(symbol, name, quantity, price, commission)
            } else {
                viewModel.sellStock(symbol, quantity, price, commission)
            }
        }
        dialog.show(childFragmentManager, "AddTransactionDialog")
    }

    private fun showTradeDialog(holding: com.example.stockanalysis.data.portfolio.PortfolioHolding) {
        // TODO: Show dialog to trade this holding
        val dialog = TradeDialog.newInstance(holding)
        dialog.setOnTradeListener { type, quantity, price, commission ->
            if (type == "SELL") {
                viewModel.sellStock(holding.stockSymbol, quantity, price, commission)
            }
        }
        dialog.show(childFragmentManager, "TradeDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
