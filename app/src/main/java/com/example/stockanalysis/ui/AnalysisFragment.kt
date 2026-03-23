package com.example.stockanalysis.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.data.model.Stock
import com.example.stockanalysis.databinding.FragmentAnalysisBinding
import com.example.stockanalysis.ui.adapter.StockSelectionAdapter
import com.example.stockanalysis.ui.viewmodel.AnalysisViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AnalysisViewModel by viewModels()
    private lateinit var stockSelectionAdapter: StockSelectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupViews()
        observeViewModel()
        
        // 加载自选列表
        viewModel.loadWatchlist()
    }

    private fun setupRecyclerView() {
        stockSelectionAdapter = StockSelectionAdapter { stock, isSelected ->
            onStockSelectionChanged(stock, isSelected)
        }
        
        binding.rvSelectedStocks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stockSelectionAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupViews() {
        // 添加股票按钮
        binding.btnAddStock.setOnClickListener {
            startActivity(Intent(requireContext(), AddStockActivity::class.java))
        }
        
        // 开始分析按钮
        binding.btnStartAnalysis.setOnClickListener {
            viewModel.startAnalysis()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察自选列表
                viewModel.watchlist.collect { stocks ->
                    updateWatchlist(stocks)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察已选择股票列表
                viewModel.selectedStocks.collect { selectedStocks ->
                    updateSelectedStocks(selectedStocks)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察加载状态
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = 
                        if (isLoading) View.VISIBLE else View.GONE
                    binding.btnStartAnalysis.isEnabled = 
                        !isLoading && viewModel.selectedStocks.value.isNotEmpty()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察错误信息
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察分析状态
                viewModel.analysisState.collect { state ->
                    when (state) {
                        is AnalysisViewModel.AnalysisUiState.Analyzing -> {
                            binding.tvAnalysisStatus.apply {
                                visibility = View.VISIBLE
                                text = "正在分析: ${state.currentStock} (${state.progress}/${state.total})"
                            }
                        }
                        is AnalysisViewModel.AnalysisUiState.Completed -> {
                            binding.tvAnalysisStatus.visibility = View.GONE
                        }
                        is AnalysisViewModel.AnalysisUiState.Error -> {
                            binding.tvAnalysisStatus.apply {
                                visibility = View.VISIBLE
                                text = "分析失败: ${state.message}"
                            }
                        }
                        else -> {
                            binding.tvAnalysisStatus.visibility = View.GONE
                        }
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察导航事件
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is AnalysisViewModel.NavigationEvent.ToResult -> {
                            navigateToAnalysisResult(event.analysisId)
                        }
                        is AnalysisViewModel.NavigationEvent.ToBatchResult -> {
                            navigateToBatchResult(event.count)
                        }
                    }
                }
            }
        }
    }

    private fun updateWatchlist(stocks: List<Stock>) {
        stockSelectionAdapter.submitList(stocks)
        
        // 显示或隐藏空状态
        if (stocks.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvSelectedStocks.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvSelectedStocks.visibility = View.VISIBLE
        }
    }

    private fun updateSelectedStocks(selectedStocks: List<Stock>) {
        // 更新适配器中的选中状态
        stockSelectionAdapter.setSelectedStocks(selectedStocks)
        
        // 更新已选择数量显示
        binding.tvSelectedCount.text = "已选择: ${selectedStocks.size}"
        
        // 更新开始分析按钮状态
        binding.btnStartAnalysis.isEnabled = 
            selectedStocks.isNotEmpty() && !viewModel.isLoading.value
    }

    private fun onStockSelectionChanged(stock: Stock, isSelected: Boolean) {
        if (isSelected) {
            viewModel.addSelectedStock(stock)
        } else {
            viewModel.removeSelectedStock(stock)
        }
    }

    private fun navigateToAnalysisResult(analysisId: String) {
        val intent = AnalysisResultActivity.createIntent(
            requireContext(),
            analysisId = analysisId
        )
        startActivity(intent)
    }

    private fun navigateToBatchResult(count: Int) {
        // 批量分析完成后，导航到历史记录页面或显示提示
        Toast.makeText(
            requireContext(), 
            "已完成 $count 只股票的批量分析", 
            Toast.LENGTH_SHORT
        ).show()
        
        // 可以导航到历史记录页面查看结果
        // startActivity(Intent(requireContext(), HistoryActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        // 每次返回页面时刷新自选列表
        viewModel.loadWatchlist()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
