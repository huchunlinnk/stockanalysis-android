package com.example.stockanalysis.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.R
import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.databinding.ActivityMultiAgentAnalysisBinding
import com.example.stockanalysis.ui.adapter.AgentExecutionAdapter
import com.example.stockanalysis.ui.adapter.AgentOpinionAdapter
import com.example.stockanalysis.ui.viewmodel.*
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 多 Agent 分析 Activity
 * 展示多 Agent 分析流程和结果
 */
@AndroidEntryPoint
class MultiAgentAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMultiAgentAnalysisBinding
    private val viewModel: MultiAgentAnalysisViewModel by viewModels()
    
    private lateinit var agentExecutionAdapter: AgentExecutionAdapter
    private lateinit var agentOpinionAdapter: AgentOpinionAdapter
    
    private var stockSymbol: String = ""
    private var stockName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultiAgentAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        stockSymbol = intent.getStringExtra(EXTRA_STOCK_SYMBOL) ?: ""
        stockName = intent.getStringExtra(EXTRA_STOCK_NAME) ?: ""
        
        if (stockSymbol.isEmpty()) {
            Toast.makeText(this, "股票代码无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupUI()
        setupRecyclerViews()
        observeViewModel()
        
        // 设置股票信息
        binding.tvStockName.text = stockName
        binding.tvStockSymbol.text = stockSymbol
    }
    
    private fun setupUI() {
        // Toolbar
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        // 模式选择
        binding.chipGroupMode.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.chipModeQuick -> PresetMode.QUICK
                R.id.chipModeStandard -> PresetMode.STANDARD
                R.id.chipModeFull -> PresetMode.FULL
                R.id.chipModeVoting -> PresetMode.VOTING
                else -> PresetMode.FULL
            }
            viewModel.applyPresetMode(mode)
        }
        
        // 开始分析按钮
        binding.btnStartAnalysis.setOnClickListener {
            startAnalysis()
        }
        
        // 查看详细报告
        binding.btnViewDetail.setOnClickListener {
            viewModel.analysisResult.value?.let { result ->
                val intent = Intent(this, AnalysisResultActivity::class.java).apply {
                    putExtra("analysis_id", result.id)
                }
                startActivity(intent)
            }
        }
        
        // 重新分析
        binding.btnNewAnalysis.setOnClickListener {
            viewModel.reset()
            resetUI()
        }
    }
    
    private fun setupRecyclerViews() {
        // Agent 执行流程列表
        agentExecutionAdapter = AgentExecutionAdapter()
        binding.recyclerAgents.apply {
            layoutManager = LinearLayoutManager(this@MultiAgentAnalysisActivity)
            adapter = agentExecutionAdapter
        }
        
        // Agent 意见列表
        agentOpinionAdapter = AgentOpinionAdapter()
        binding.recyclerOpinions.apply {
            layoutManager = LinearLayoutManager(this@MultiAgentAnalysisActivity)
            adapter = agentOpinionAdapter
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 分析状态
                viewModel.analysisState.collect { state ->
                    handleAnalysisState(state)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Agent 状态
                viewModel.agentStates.collect { states ->
                    agentExecutionAdapter.submitStates(states.values.toList())
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Agent 意见
                viewModel.allOpinions.collect { opinions ->
                    agentOpinionAdapter.submitOpinions(opinions)
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 进度
                viewModel.progress.collect { progress ->
                    binding.progressBar.progress = (progress * 100).toInt()
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 错误信息
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Toast.makeText(this@MultiAgentAnalysisActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 执行统计
                viewModel.executionStats.collect { stats ->
                    stats?.let {
                        binding.tvExecutionStats.text = buildString {
                            appendLine("执行时间: ${String.format("%.1f", it.durationSeconds)}秒")
                            appendLine("完成 Agent: ${it.completedAgents}")
                            appendLine("成功率: ${String.format("%.0f%%", it.successRate * 100)}")
                        }
                        binding.cardExecutionStats.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
    
    private fun handleAnalysisState(state: MultiAgentAnalysisUiState) {
        when (state) {
            is MultiAgentAnalysisUiState.Idle -> {
                // 显示模式选择，隐藏其他
                binding.cardModeSelection.visibility = View.VISIBLE
                binding.cardProgress.visibility = View.GONE
                binding.cardAgentFlow.visibility = View.GONE
                binding.cardAgentOpinions.visibility = View.GONE
                binding.cardVoteResult.visibility = View.GONE
                binding.cardFinalDecision.visibility = View.GONE
                binding.cardExecutionStats.visibility = View.GONE
                binding.layoutActions.visibility = View.GONE
            }
            is MultiAgentAnalysisUiState.Analyzing -> {
                // 显示进度和执行流程
                binding.cardModeSelection.visibility = View.GONE
                binding.cardProgress.visibility = View.VISIBLE
                binding.cardAgentFlow.visibility = View.VISIBLE
                binding.tvProgressStatus.text = "分析进行中..."
            }
            is MultiAgentAnalysisUiState.Completed -> {
                // 显示结果
                binding.cardProgress.visibility = View.GONE
                binding.cardAgentOpinions.visibility = View.VISIBLE
                binding.cardFinalDecision.visibility = View.VISIBLE
                binding.layoutActions.visibility = View.VISIBLE
                
                // 更新最终决策
                updateFinalDecision()
            }
            is MultiAgentAnalysisUiState.Error -> {
                // 显示错误
                binding.cardProgress.visibility = View.GONE
                binding.tvProgressStatus.text = "分析失败: ${state.message}"
                binding.cardModeSelection.visibility = View.VISIBLE
            }
        }
    }
    
    private fun updateFinalDecision() {
        val finalOpinion = viewModel.allOpinions.value.find { it.agentType == AgentType.DECISION }
        
        finalOpinion?.let { opinion ->
            binding.tvFinalDecision.text = when (opinion.signal) {
                Signal.STRONG_BUY -> "强烈买入"
                Signal.BUY -> "买入"
                Signal.HOLD -> "持有观望"
                Signal.SELL -> "卖出"
                Signal.STRONG_SELL -> "强烈卖出"
            }
            
            binding.tvFinalConfidence.text = "置信度: ${String.format("%.0f%%", opinion.confidence * 100)}"
            binding.tvFinalReasoning.text = opinion.reasoning
            
            // 根据信号设置颜色
            val colorRes = when (opinion.signal) {
                Signal.STRONG_BUY, Signal.BUY -> android.R.color.holo_green_dark
                Signal.HOLD -> android.R.color.holo_orange_dark
                Signal.SELL, Signal.STRONG_SELL -> android.R.color.holo_red_dark
            }
            binding.tvFinalDecision.setTextColor(resources.getColor(colorRes, theme))
        }
    }
    
    private fun startAnalysis() {
        viewModel.startAnalysis(stockSymbol, stockName)
    }
    
    private fun resetUI() {
        binding.cardModeSelection.visibility = View.VISIBLE
        binding.cardProgress.visibility = View.GONE
        binding.cardAgentFlow.visibility = View.GONE
        binding.cardAgentOpinions.visibility = View.GONE
        binding.cardVoteResult.visibility = View.GONE
        binding.cardFinalDecision.visibility = View.GONE
        binding.cardExecutionStats.visibility = View.GONE
        binding.layoutActions.visibility = View.GONE
        
        agentExecutionAdapter.submitStates(emptyList())
        agentOpinionAdapter.submitOpinions(emptyList())
    }
    
    companion object {
        const val EXTRA_STOCK_SYMBOL = "stock_symbol"
        const val EXTRA_STOCK_NAME = "stock_name"
        
        fun newIntent(
            context: android.content.Context,
            stockSymbol: String,
            stockName: String
        ): Intent {
            return Intent(context, MultiAgentAnalysisActivity::class.java).apply {
                putExtra(EXTRA_STOCK_SYMBOL, stockSymbol)
                putExtra(EXTRA_STOCK_NAME, stockName)
            }
        }
    }
}
