package com.example.stockanalysis.ui.fragment

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
import com.example.stockanalysis.databinding.FragmentFundamentalSettingsBinding
import com.example.stockanalysis.ui.viewmodel.FundamentalSettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 基本面数据配置Fragment
 *
 * 功能：
 * 1. 配置数据源参数（自动刷新、缓存有效期）
 * 2. 配置显示选项（显示/隐藏各个数据模块）
 * 3. 配置分析选项（是否在分析中包含基本面）
 * 4. 缓存管理（查看缓存、清空缓存）
 */
@AndroidEntryPoint
class FundamentalSettingsFragment : Fragment() {

    private var _binding: FragmentFundamentalSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FundamentalSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFundamentalSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeViewModel()

        // 加载当前配置
        viewModel.loadSettings()
    }

    private fun setupViews() {
        // 自动刷新开关
        binding.switchAutoRefresh.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoRefresh(isChecked)
        }

        // 缓存有效期滑块
        binding.sliderCacheDays.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val days = value.toInt()
                binding.tvCacheDays.text = "${days}天"
                viewModel.setCacheDays(days)
            }
        }

        // 显示配置开关
        binding.switchShowFinancial.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowFinancial(isChecked)
        }

        binding.switchShowGrowth.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowGrowth(isChecked)
        }

        binding.switchShowDividend.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowDividend(isChecked)
        }

        binding.switchShowInstitution.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowInstitution(isChecked)
        }

        // 分析配置开关
        binding.switchIncludeFundamentalInAnalysis.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIncludeFundamentalInAnalysis(isChecked)
        }

        // 清空缓存按钮
        binding.btnClearCache.setOnClickListener {
            showClearCacheConfirmation()
        }
    }

    private fun observeViewModel() {
        // 观察配置变化
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    // 更新UI（不触发监听器）
                    binding.switchAutoRefresh.isChecked = settings.autoRefresh
                    binding.sliderCacheDays.value = settings.cacheDays.toFloat()
                    binding.tvCacheDays.text = "${settings.cacheDays}天"

                    binding.switchShowFinancial.isChecked = settings.showFinancial
                    binding.switchShowGrowth.isChecked = settings.showGrowth
                    binding.switchShowDividend.isChecked = settings.showDividend
                    binding.switchShowInstitution.isChecked = settings.showInstitution

                    binding.switchIncludeFundamentalInAnalysis.isChecked = settings.includeFundamentalInAnalysis
                }
            }
        }

        // 观察缓存信息
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cacheInfo.collect { info ->
                    binding.tvCacheInfo.text = "缓存数据: ${info.cacheCount} 条\n" +
                            "缓存大小: ${formatBytes(info.cacheSize)}"
                }
            }
        }

        // 观察操作结果
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.message.collect { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showClearCacheConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空缓存")
            .setMessage("确定要清空所有基本面数据缓存吗？\n\n清空后下次查看将重新从网络获取数据。")
            .setPositiveButton("确定") { _, _ ->
                viewModel.clearCache()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FundamentalSettingsFragment()
    }
}
