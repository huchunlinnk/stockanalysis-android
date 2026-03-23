package com.example.stockanalysis.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.data.datasource.*
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.data.model.DataSourceConfig
import com.example.stockanalysis.databinding.FragmentDataSourceConfigBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 数据源配置 Fragment
 *
 * 功能：
 * 1. 管理多个数据源的配置（7个数据源）
 * 2. 拖拽调整优先级
 * 3. 启用/禁用数据源
 * 4. 测试连接
 * 5. Fallback 策略配置
 */
@AndroidEntryPoint
class DataSourceConfigFragment : Fragment() {

    private var _binding: FragmentDataSourceConfigBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var tushareDataSource: TushareDataSource

    @Inject
    lateinit var akShareDataSource: AkShareDataSource

    @Inject
    lateinit var yFinanceDataSource: YFinanceDataSource

    @Inject
    lateinit var eFinanceDataSource: EFinanceDataSource

    private lateinit var adapter: DataSourceAdapter
    private val configs = mutableListOf<DataSourceConfig>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataSourceConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadConfigs()
        setupRecyclerView()
        setupViews()
    }

    /**
     * 加载配置
     */
    private fun loadConfigs() {
        configs.clear()
        configs.addAll(preferencesManager.getDataSourceConfigs())

        // 按优先级排序
        configs.sortBy { it.priority }

        // 更新 UI
        updateEmptyState()
    }

    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = DataSourceAdapter(
            configs = configs,
            onTestClicked = { config -> testDataSource(config) },
            onConfigChanged = { config -> onConfigChanged(config) }
        )

        binding.recyclerViewDataSources.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DataSourceConfigFragment.adapter

            // 设置拖拽排序
            val callback = DataSourceAdapter.DragCallback(this@DataSourceConfigFragment.adapter)
            val itemTouchHelper = ItemTouchHelper(callback)
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    /**
     * 设置 UI 交互
     */
    private fun setupViews() {
        // Fallback 策略开关
        binding.switchFallback.isChecked = preferencesManager.isDataSourceFallbackEnabled()
        binding.switchFallback.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setDataSourceFallbackEnabled(isChecked)
            showToast(if (isChecked) "已启用故障自动切换" else "已禁用故障自动切换")
        }

        // 保存按钮
        binding.fabSave.setOnClickListener {
            saveConfigs()
        }
    }

    /**
     * 配置变化回调
     */
    private fun onConfigChanged(config: DataSourceConfig) {
        // 实时更新（可选）
        // preferencesManager.saveDataSourceConfig(config)
    }

    /**
     * 测试数据源连接
     */
    private fun testDataSource(config: DataSourceConfig) {
        // 禁用测试按钮
        showToast("正在测试 ${config.displayName}...")

        lifecycleScope.launch {
            try {
                val result = when (config.id) {
                    "tushare" -> {
                        // 先保存 Token 到 PreferencesManager
                        preferencesManager.setTushareToken(config.apiKey)
                        tushareDataSource.fetchQuote("000001")
                    }
                    "akshare" -> {
                        akShareDataSource.fetchQuote("000001")
                    }
                    "yfinance" -> {
                        yFinanceDataSource.fetchQuote("AAPL")
                    }
                    "efinance" -> {
                        eFinanceDataSource.fetchQuote("000001")
                    }
                    else -> {
                        // 其他数据源暂未实现，返回成功
                        Result.success(null)
                    }
                }

                if (result.isSuccess) {
                    adapter.updateTestResult(config.id, true, "✅ 连接成功")
                    showToast("${config.displayName} 连接成功")
                } else {
                    val error = result.exceptionOrNull()
                    adapter.updateTestResult(config.id, false, "❌ ${error?.message ?: "连接失败"}")
                    showToast("${config.displayName} 连接失败: ${error?.message}")
                }
            } catch (e: Exception) {
                adapter.updateTestResult(config.id, false, "❌ ${e.message ?: "连接失败"}")
                showToast("${config.displayName} 连接失败: ${e.message}")
            }
        }
    }

    /**
     * 保存配置
     */
    private fun saveConfigs() {
        val updatedConfigs = adapter.getConfigs()

        // 保存到 PreferencesManager
        preferencesManager.saveDataSourceConfigs(updatedConfigs)

        // 同步到实际的数据源实例（保存 API Keys）
        updatedConfigs.forEach { config ->
            when (config.id) {
                "tushare" -> {
                    if (config.apiKey.isNotEmpty()) {
                        preferencesManager.setTushareToken(config.apiKey)
                    }
                }
                // 其他数据源暂无需特殊处理
            }
        }

        // 注意：数据源的 priority 在 DataSourceManager 中根据配置动态读取
        // 不需要直接修改数据源实例的 priority 属性

        showToast("配置已保存")

        // 返回上一页
        requireActivity().supportFragmentManager.popBackStack()
    }

    /**
     * 更新空状态
     */
    private fun updateEmptyState() {
        if (configs.isEmpty()) {
            binding.recyclerViewDataSources.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewDataSources.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    /**
     * 显示 Toast
     */
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): DataSourceConfigFragment {
            return DataSourceConfigFragment()
        }
    }
}
