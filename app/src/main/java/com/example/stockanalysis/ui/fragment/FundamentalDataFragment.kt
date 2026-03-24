package com.example.stockanalysis.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.databinding.FragmentFundamentalDataBinding
import com.example.stockanalysis.ui.adapter.MetricAdapter
import com.example.stockanalysis.ui.adapter.ScoreAdapter
import com.example.stockanalysis.ui.viewmodel.StockDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 基本面数据展示Fragment
 *
 * 功能：
 * 1. 展示综合评分和投资建议
 * 2. 展示各维度评分（估值、盈利、成长、财务健康、分红、机构关注）
 * 3. 展示详细的财务指标
 * 4. 展示成长性分析
 * 5. 展示机构持仓信息
 * 6. 展示风险提示
 */
@AndroidEntryPoint
class FundamentalDataFragment : Fragment() {

    private var _binding: FragmentFundamentalDataBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockDetailViewModel by activityViewModels()

    private lateinit var scoresAdapter: ScoreAdapter
    private lateinit var financialAdapter: MetricAdapter
    private lateinit var growthAdapter: MetricAdapter
    private lateinit var institutionAdapter: MetricAdapter

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFundamentalDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // 各维度评分列表
        scoresAdapter = ScoreAdapter()
        binding.rvScores.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scoresAdapter
        }

        // 财务指标列表
        financialAdapter = MetricAdapter()
        binding.rvFinancialIndicators.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = financialAdapter
        }

        // 成长性指标列表
        growthAdapter = MetricAdapter()
        binding.rvGrowthMetrics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = growthAdapter
        }

        // 机构持仓列表
        institutionAdapter = MetricAdapter()
        binding.rvInstitutionalHolding.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = institutionAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fundamentalAnalysis.collect { result ->
                    result?.let { analysis ->
                        showContent()
                        displayFundamentalAnalysis(analysis)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoadingFundamental.collect { isLoading ->
                    if (isLoading) {
                        showLoading()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        if (viewModel.fundamentalAnalysis.value == null) {
                            showError(it)
                        }
                    }
                }
            }
        }
    }

    private fun displayFundamentalAnalysis(analysis: com.example.stockanalysis.data.model.FundamentalAnalysisResult) {
        // 综合评分和投资建议
        binding.tvOverallScore.text = analysis.overallScore.toString()
        binding.tvInvestmentAdvice.text = analysis.investmentAdvice
        binding.tvUpdateTime.text = "更新时间：${dateFormat.format(analysis.updateTime)}"

        // 各维度评分
        val scores = listOf(
            ScoreAdapter.ScoreItem("估值", analysis.valuationScore, analysis.valuationConclusion),
            ScoreAdapter.ScoreItem("盈利能力", analysis.profitabilityScore, analysis.profitabilityConclusion),
            ScoreAdapter.ScoreItem("成长性", analysis.growthScore, analysis.growthConclusion),
            ScoreAdapter.ScoreItem("财务健康", analysis.financialHealthScore, analysis.financialHealthConclusion),
            ScoreAdapter.ScoreItem("分红", analysis.dividendScore, analysis.dividendConclusion),
            ScoreAdapter.ScoreItem("机构关注", analysis.institutionScore, analysis.institutionConclusion)
        )
        scoresAdapter.submitList(scores)

        // 结论文本
        binding.tvProfitabilityConclusion.text = analysis.profitabilityConclusion
        binding.tvGrowthConclusion.text = analysis.growthConclusion
        binding.tvInstitutionConclusion.text = analysis.institutionConclusion

        // 风险提示
        if (analysis.riskFactors.isNotEmpty()) {
            binding.cardRiskFactors.visibility = View.VISIBLE
            binding.tvRiskFactors.text = analysis.riskFactors.joinToString("\n") { "• $it" }
        } else {
            binding.cardRiskFactors.visibility = View.GONE
        }

        // 加载详细指标
        loadDetailedMetrics()
    }

    private fun loadDetailedMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 加载财务指标
            viewModel.financialIndicators.collect { indicators ->
                indicators?.let {
                    val metrics = mutableListOf<MetricAdapter.MetricItem>()

                    // 盈利能力
                    it.roe?.let { roe ->
                        metrics.add(MetricAdapter.MetricItem("净资产收益率(ROE)", "${String.format("%.2f", roe)}%"))
                    }
                    it.roa?.let { roa ->
                        metrics.add(MetricAdapter.MetricItem("总资产收益率(ROA)", "${String.format("%.2f", roa)}%"))
                    }
                    it.grossMargin?.let { margin ->
                        metrics.add(MetricAdapter.MetricItem("毛利率", "${String.format("%.2f", margin)}%"))
                    }
                    it.netMargin?.let { margin ->
                        metrics.add(MetricAdapter.MetricItem("净利率", "${String.format("%.2f", margin)}%"))
                    }

                    // 偿债能力
                    it.debtToEquity?.let { debt ->
                        metrics.add(MetricAdapter.MetricItem("资产负债率", "${String.format("%.2f", debt)}%"))
                    }
                    it.currentRatio?.let { ratio ->
                        metrics.add(MetricAdapter.MetricItem("流动比率", String.format("%.2f", ratio)))
                    }
                    it.quickRatio?.let { ratio ->
                        metrics.add(MetricAdapter.MetricItem("速动比率", String.format("%.2f", ratio)))
                    }

                    // 运营效率
                    it.inventoryTurnover?.let { turnover ->
                        metrics.add(MetricAdapter.MetricItem("存货周转率", String.format("%.2f", turnover)))
                    }
                    it.assetTurnover?.let { turnover ->
                        metrics.add(MetricAdapter.MetricItem("总资产周转率", String.format("%.2f", turnover)))
                    }

                    // 现金流
                    it.operatingCashFlow?.let { flow ->
                        metrics.add(MetricAdapter.MetricItem("经营现金流", "${String.format("%.2f", flow)}亿元"))
                    }
                    it.freeCashFlow?.let { flow ->
                        metrics.add(MetricAdapter.MetricItem("自由现金流", "${String.format("%.2f", flow)}亿元"))
                    }

                    financialAdapter.submitList(metrics)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // 加载成长性指标
            viewModel.growthMetrics.collect { growth ->
                growth?.let {
                    val metrics = mutableListOf<MetricAdapter.MetricItem>()

                    // 营收增长
                    it.revenueGrowthYoY?.let { growth ->
                        metrics.add(MetricAdapter.MetricItem("营收同比增长", "${String.format("%.2f", growth)}%"))
                    }
                    it.revenueGrowthQoQ?.let { growth ->
                        metrics.add(MetricAdapter.MetricItem("营收环比增长", "${String.format("%.2f", growth)}%"))
                    }
                    it.revenueGrowth3Y?.let { growth ->
                        metrics.add(MetricAdapter.MetricItem("3年复合增长率", "${String.format("%.2f", growth)}%"))
                    }

                    // 利润增长
                    it.netProfitGrowthYoY?.let { growth ->
                        metrics.add(MetricAdapter.MetricItem("净利润同比增长", "${String.format("%.2f", growth)}%"))
                    }
                    it.netProfitGrowthQoQ?.let { growth ->
                        metrics.add(MetricAdapter.MetricItem("净利润环比增长", "${String.format("%.2f", growth)}%"))
                    }
                    it.netProfitGrowth3Y?.let { growth ->
                        metrics.add(MetricAdapter.MetricItem("净利润3年复合增长", "${String.format("%.2f", growth)}%"))
                    }

                    // 其他增长
                    it.grossProfitGrowthYoY?.let { growth ->
                        metrics.add(MetricAdapter.MetricItem("毛利润同比增长", "${String.format("%.2f", growth)}%"))
                    }
                    it.totalAssetGrowthYoY?.let { growth ->
                        metrics.add(MetricAdapter.MetricItem("总资产同比增长", "${String.format("%.2f", growth)}%"))
                    }

                    growthAdapter.submitList(metrics)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // 加载机构持仓
            viewModel.institutionalHolding.collect { holding ->
                holding?.let {
                    val metrics = mutableListOf<MetricAdapter.MetricItem>()

                    it.institutionCount?.let { count ->
                        metrics.add(MetricAdapter.MetricItem("机构数量", "${count}家"))
                    }
                    it.holdingRatio?.let { ratio ->
                        metrics.add(MetricAdapter.MetricItem("机构持仓比例", "${String.format("%.2f", ratio)}%"))
                    }
                    it.holdingChange?.let { change ->
                        metrics.add(MetricAdapter.MetricItem("持仓变动", "${String.format("%.2f", change)}万股"))
                    }
                    it.holdingChangeRatio?.let { ratio ->
                        metrics.add(MetricAdapter.MetricItem("持仓变动比例", "${String.format("%.2f", ratio)}%"))
                    }
                    it.top10HoldingRatio?.let { ratio ->
                        metrics.add(MetricAdapter.MetricItem("前十大股东持股", "${String.format("%.2f", ratio)}%"))
                    }
                    it.fundHoldingRatio?.let { ratio ->
                        metrics.add(MetricAdapter.MetricItem("基金持仓比例", "${String.format("%.2f", ratio)}%"))
                    }
                    it.northboundHolding?.let { holding ->
                        metrics.add(MetricAdapter.MetricItem("北向资金持仓", "${String.format("%.2f", holding)}万股"))
                    }
                    it.northboundChange?.let { change ->
                        metrics.add(MetricAdapter.MetricItem("北向资金变动", "${String.format("%.2f", change)}万股"))
                    }

                    institutionAdapter.submitList(metrics)
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        binding.contentLayout.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
        binding.contentLayout.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FundamentalDataFragment()
    }
}
