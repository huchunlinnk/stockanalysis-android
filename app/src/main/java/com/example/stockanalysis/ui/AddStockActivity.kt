package com.example.stockanalysis.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.databinding.ActivityAddStockBinding
import com.example.stockanalysis.ui.adapter.SearchResultAdapter
import com.example.stockanalysis.ui.viewmodel.AddStockViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddStockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStockBinding
    private val viewModel: AddStockViewModel by viewModels()
    private lateinit var adapter: SearchResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter { item ->
            viewModel.addStock(item.first, item.second)
        }
        
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString()
            if (query.isNotEmpty()) {
                viewModel.searchStocks(query)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collect { results ->
                    adapter.submitList(results)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Toast.makeText(this@AddStockActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addStockResult.collect { result ->
                    when (result) {
                        is AddStockViewModel.AddStockResult.Success -> {
                            Toast.makeText(this@AddStockActivity, "添加成功", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is AddStockViewModel.AddStockResult.Error -> {
                            Toast.makeText(this@AddStockActivity, result.message, Toast.LENGTH_SHORT).show()
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}
