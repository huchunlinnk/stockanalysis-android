package com.example.stockanalysis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.databinding.FragmentBacktestBinding
import com.example.stockanalysis.ui.adapter.BacktestResultAdapter
import com.example.stockanalysis.ui.dialog.BacktestConfigDialog
import com.example.stockanalysis.ui.viewmodel.BacktestViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class BacktestFragment : Fragment() {

    private var _binding: FragmentBacktestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BacktestViewModel by viewModels()
    private lateinit var resultAdapter: BacktestResultAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBacktestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        resultAdapter = BacktestResultAdapter(
            onItemClick = { result ->
                viewModel.loadBacktestResult(result.id)
                showResultDetail()
            },
            onDeleteClick = { result ->
                showDeleteConfirmation(result.id)
            }
        )

        binding.recyclerViewResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = resultAdapter
        }
    }

    private fun setupObservers() {
        // 观察回测结果列表
        viewModel.allResults.observe(viewLifecycleOwner) { results ->
            resultAdapter.submitList(results)
            binding.emptyView.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }

        // 观察当前回测结果
        viewModel.backtestResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                updateResultSummary(it)
            }
        }

        // 观察回测信号
        viewModel.backtestSignals.observe(viewLifecycleOwner) { signals ->
            // TODO: Update signals list if showing detail
        }

        // 观察运行状态
        viewModel.isRunning.observe(viewLifecycleOwner) { isRunning ->
            binding.progressBar.visibility = if (isRunning) View.VISIBLE else View.GONE
            binding.fabRunBacktest.isEnabled = !isRunning
        }

        // 观察进度
        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            binding.progressBar.progress = progress
        }

        // 观察错误
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupListeners() {
        binding.fabRunBacktest.setOnClickListener {
            showBacktestConfigDialog()
        }
    }

    private fun updateResultSummary(result: com.example.stockanalysis.data.backtest.BacktestResult) {
        binding.apply {
            cardSummary.visibility = View.VISIBLE

            tvSymbol.text = "${result.stockName} (${result.stockSymbol})"
            tvStrategy.text = "策略: ${result.strategyName}"
            tvPeriod.text = "${dateFormat.format(result.startDate)} 至 ${dateFormat.format(result.endDate)}"

            // 准确率
            tvAccuracy.text = String.format("%.2f%%", result.accuracy)
            tvTotalSignals.text = "${result.totalSignals}个"
            tvCorrectSignals.text = "${result.correctSignals}个"

            // 收益统计
            tvTotalReturn.text = String.format("%.2f%%", result.totalReturn)
            tvMaxDrawdown.text = String.format("%.2f%%", result.maxDrawdown)
            tvSharpeRatio.text = String.format("%.2f", result.sharpeRatio)

            // 胜率统计
            tvWinRate.text = String.format("%.2f%%", result.winRate)
            tvProfitFactor.text = String.format("%.2f", result.profitFactor)
            tvTradeCount.text = "${result.tradeCount}次"
            tvWinCount.text = "${result.winCount}次"
            tvLossCount.text = "${result.lossCount}次"
        }
    }

    private fun showResultDetail() {
        // TODO: Show detail fragment or activity
        binding.cardSummary.visibility = View.VISIBLE
    }

    private fun showBacktestConfigDialog() {
        val dialog = BacktestConfigDialog.newInstance()
        dialog.setOnConfigListener { symbol, strategyId, startDate, endDate ->
            viewModel.runBacktest(symbol, strategyId, startDate, endDate)
        }
        dialog.show(childFragmentManager, "BacktestConfigDialog")
    }

    private fun showDeleteConfirmation(resultId: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除回测结果")
            .setMessage("确定要删除这个回测结果吗?")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteBacktestResult(resultId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
