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
import com.example.stockanalysis.databinding.FragmentWatchlistBinding
import com.example.stockanalysis.ui.adapter.StockAdapter
import com.example.stockanalysis.ui.viewmodel.WatchlistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WatchlistFragment : Fragment() {

    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WatchlistViewModel by viewModels()
    private lateinit var stockAdapter: StockAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        
        binding.fabAddStock.setOnClickListener {
            startActivity(Intent(requireContext(), AddStockActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(
            onItemClick = { stock ->
                navigateToDetail(stock)
            },
            onDeleteClick = { stock ->
                viewModel.removeStock(stock.symbol)
            }
        )
        
        binding.rvWatchlist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stockAdapter
        }
    }

    private fun observeViewModel() {
        // 观察自选股列表
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.watchlist.collect { stocks ->
                    stockAdapter.submitList(stocks)
                    binding.llEmpty.visibility = 
                        if (stocks.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        
        // 观察实时行情数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quotes.collect { quotes ->
                    stockAdapter.submitQuotes(quotes)
                }
            }
        }
        
        // 观察加载状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // 可以在这里显示/隐藏加载指示器
                    // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }
        
        // 观察错误信息
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次页面恢复时刷新行情数据
        viewModel.refreshQuotes()
    }

    private fun navigateToDetail(stock: Stock) {
        val intent = Intent(requireContext(), StockDetailActivity::class.java).apply {
            putExtra("stock_symbol", stock.symbol)
            putExtra("stock_name", stock.name)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
