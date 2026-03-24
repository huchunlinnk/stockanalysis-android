package com.example.stockanalysis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.R
import com.example.stockanalysis.data.model.MarketType
import com.example.stockanalysis.databinding.FragmentHomeBinding
import com.example.stockanalysis.ui.adapter.MarketIndexAdapter
import com.example.stockanalysis.ui.adapter.ReportSummaryAdapter
import com.example.stockanalysis.ui.adapter.SearchResultAdapter
import com.example.stockanalysis.ui.viewmodel.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var searchResultAdapter: SearchResultAdapter
    private lateinit var marketIndexAdapter: MarketIndexAdapter
    private lateinit var reportSummaryAdapter: ReportSummaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupViews()
        observeViewModel()
    }

    /**
     * 设置 RecyclerViews
     */
    private fun setupRecyclerViews() {
        // 搜索结果适配器
        searchResultAdapter = SearchResultAdapter { stockPair ->
            // 点击搜索结果，跳转到股票详情页面
            navigateToStockDetail(stockPair.first, stockPair.second)
        }

        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchResultAdapter
        }

        // 市场指数适配器
        marketIndexAdapter = MarketIndexAdapter()

        binding.rvIndices.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = marketIndexAdapter
        }

        // 报告摘要适配器
        reportSummaryAdapter = ReportSummaryAdapter(
            onItemClick = { result ->
                // 点击报告，跳转到分析结果详情页面
                navigateToAnalysisDetail(result)
            },
            onDeleteClick = { result ->
                // 删除报告前确认
                showDeleteConfirmDialog(result)
            }
        )

        binding.rvReportSummaries.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportSummaryAdapter
        }
    }

    /**
     * 设置视图
     */
    private fun setupViews() {
        // 设置市场切换Tab
        setupMarketTabs()

        // 搜索框输入监听（实时搜索）
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString() ?: ""
            viewModel.onSearchQueryChanged(query)

            // 如果输入为空，隐藏搜索结果
            if (query.isEmpty()) {
                binding.rvSearchResults.visibility = View.GONE
            }
        }
        
        // 搜索框键盘搜索按钮监听
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString()
                if (query.isNotEmpty()) {
                    viewModel.searchStocks(query)
                }
                true
            } else {
                false
            }
        }
        
        // 快速分析按钮
        binding.btnQuickAnalysis.setOnClickListener {
            val query = binding.etSearch.text.toString()
            if (query.isNotEmpty()) {
                viewModel.searchStocks(query)
            } else {
                Toast.makeText(requireContext(), "请输入股票代码或名称", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 自选股按钮 - 导航到自选股页面
        binding.btnWatchlist.setOnClickListener {
            navigateToWatchlist()
        }
        
        // 历史记录按钮 - 导航到历史记录页面
        binding.btnHistory.setOnClickListener {
            navigateToHistory()
        }

        // 查看全部报告按钮
        binding.tvViewAllReports.setOnClickListener {
            navigateToHistory()
        }
    }

    /**
     * 设置市场切换Tab
     */
    private fun setupMarketTabs() {
        // 添加Tab选项
        binding.tabMarket.addTab(binding.tabMarket.newTab().setText("全部"))
        binding.tabMarket.addTab(binding.tabMarket.newTab().setText("A股"))
        binding.tabMarket.addTab(binding.tabMarket.newTab().setText("港股"))
        binding.tabMarket.addTab(binding.tabMarket.newTab().setText("美股"))

        // Tab选择监听
        binding.tabMarket.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.selectMarketType(null) // 全部
                    1 -> viewModel.selectMarketType(MarketType.A_SHARE)
                    2 -> viewModel.selectMarketType(MarketType.HK)
                    3 -> viewModel.selectMarketType(MarketType.US)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    /**
     * 观察 ViewModel 数据
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察搜索结果
                launch {
                    viewModel.searchResults.collect { results ->
                        searchResultAdapter.submitList(results)

                        // 根据结果显示或隐藏 RecyclerView
                        binding.rvSearchResults.visibility =
                            if (results.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // 观察过滤后的市场指数
                launch {
                    viewModel.filteredIndices.collect { indices ->
                        marketIndexAdapter.submitList(indices)
                    }
                }

                // 观察加载状态
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                // 观察错误信息
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }

                // 观察最近的分析报告
                launch {
                    viewModel.recentReports.collect { reports ->
                        reportSummaryAdapter.submitList(reports)

                        // 根据报告数量显示/隐藏空状态提示
                        if (reports.isEmpty()) {
                            binding.rvReportSummaries.visibility = View.GONE
                            binding.tvEmptyReports.visibility = View.VISIBLE
                        } else {
                            binding.rvReportSummaries.visibility = View.VISIBLE
                            binding.tvEmptyReports.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    /**
     * 导航到自选股页面
     */
    private fun navigateToWatchlist() {
        try {
            findNavController().navigate(R.id.action_home_to_watchlist)
        } catch (e: Exception) {
            // 如果 action 不存在，使用直接导航
            try {
                findNavController().navigate(R.id.nav_watchlist)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导航到自选股页面失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 导航到历史记录页面
     */
    private fun navigateToHistory() {
        try {
            findNavController().navigate(R.id.action_home_to_history)
        } catch (e: Exception) {
            // 如果 action 不存在，使用直接导航
            try {
                findNavController().navigate(R.id.nav_history)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导航到历史记录页面失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 导航到股票详情页面
     */
    private fun navigateToStockDetail(symbol: String, name: String) {
        try {
            val bundle = bundleOf(
                "stock_symbol" to symbol,
                "stock_name" to name
            )
            findNavController().navigate(R.id.action_home_to_stock_detail, bundle)
        } catch (e: Exception) {
            // 如果 action 不存在，使用直接导航
            try {
                val bundle = bundleOf(
                    "stock_symbol" to symbol,
                    "stock_name" to name
                )
                findNavController().navigate(R.id.stockDetailActivity, bundle)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导航到股票详情失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 导航到分析结果详情页面
     */
    private fun navigateToAnalysisDetail(result: com.example.stockanalysis.data.model.AnalysisResult) {
        try {
            val bundle = bundleOf(
                "analysis_id" to result.id,
                "stock_symbol" to result.stockSymbol,
                "stock_name" to result.stockName
            )
            findNavController().navigate(R.id.action_home_to_stock_detail, bundle)
        } catch (e: Exception) {
            // 如果导航失败，尝试跳转到股票详情页
            try {
                navigateToStockDetail(result.stockSymbol, result.stockName)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "导航到分析详情失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(result: com.example.stockanalysis.data.model.AnalysisResult) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除报告")
            .setMessage("确定要删除 ${result.stockName} 的分析报告吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteReport(result)
                Toast.makeText(requireContext(), "已删除报告", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
