package com.example.stockanalysis.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.stockanalysis.databinding.ActivityStockDetailBinding
import com.example.stockanalysis.ui.adapter.StockDetailPagerAdapter
import com.example.stockanalysis.ui.viewmodel.StockDetailViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 股票详情页面
 *
 * 功能：
 * 1. 展示股票基本信息和实时行情
 * 2. 使用TabLayout展示不同维度的数据：基本面、技术面、新闻
 * 3. 支持快速操作：智能分析、加入自选
 */
@AndroidEntryPoint
class StockDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockDetailBinding
    private val viewModel: StockDetailViewModel by viewModels()

    private var stockSymbol: String = ""
    private var stockName: String = ""

    private lateinit var pagerAdapter: StockDetailPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取参数
        stockSymbol = intent.getStringExtra("stock_symbol") ?: return finish()
        stockName = intent.getStringExtra("stock_name") ?: ""

        setupToolbar()
        setupViewPager()
        setupViews()
        observeViewModel()

        // 加载数据
        viewModel.loadStockDetail(stockSymbol, stockName)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = stockName
        }
    }

    private fun setupViewPager() {
        // 设置ViewPager2
        pagerAdapter = StockDetailPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // 关联TabLayout和ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = StockDetailPagerAdapter.TAB_TITLES[position]
        }.attach()
    }

    private fun setupViews() {
        binding.btnAnalyze.setOnClickListener {
            viewModel.startAnalysis()
        }

        binding.btnWatch.setOnClickListener {
            viewModel.addToWatchlist()
        }
    }

    private fun observeViewModel() {
        // 观察行情数据
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stockQuote.collect { quote ->
                    quote?.let {
                        binding.tvStockName.text = it.name
                        binding.tvStockCode.text = it.symbol
                        binding.tvCurrentPrice.text = String.format("%.2f", it.price)

                        val changePercent = it.changePercent ?: 0.0
                        val changeText = String.format("%+.2f%%", changePercent)
                        binding.tvChange.text = changeText

                        // 设置涨跌颜色
                        val color = when {
                            changePercent > 0 -> android.graphics.Color.RED
                            changePercent < 0 -> android.graphics.Color.GREEN
                            else -> android.graphics.Color.GRAY
                        }
                        binding.tvChange.setTextColor(color)
                    }
                }
            }
        }

        // 观察错误消息
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Toast.makeText(this@StockDetailActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 观察加载状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility =
                        if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }

        // 观察分析事件
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.analysisEvent.collect { event ->
                    when (event) {
                        is StockDetailViewModel.AnalysisEvent.NavigateToResult -> {
                            // 跳转到分析结果页面
                            val intent = Intent(
                                this@StockDetailActivity,
                                AnalysisResultActivity::class.java
                            ).apply {
                                putExtra("analysis_id", event.analysisId)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_STOCK_SYMBOL = "stock_symbol"
        const val EXTRA_STOCK_NAME = "stock_name"
    }
}
