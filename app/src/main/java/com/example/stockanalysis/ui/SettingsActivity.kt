package com.example.stockanalysis.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stockanalysis.R
import com.example.stockanalysis.data.datasource.TushareDataSource
import com.example.stockanalysis.data.llm.LLMService
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.databinding.ActivitySettingsNewBinding
import com.example.stockanalysis.ui.settings.DataSourceConfigActivity
import com.example.stockanalysis.ui.settings.LLMSettingsActivity
import com.example.stockanalysis.ui.session.SessionManagerFragment
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面 - 新版统一设置中心
 *
 * 功能：
 * 1. AI 分析设置（跳转到 LLMSettingsActivity）
 * 2. 数据源设置
 * 3. 智能导入设置
 * 4. 基本面设置
 * 5. 通知设置
 *
 * 符合中国用户习惯的设计：
 * - 国产大模型优先展示（DeepSeek、通义千问）
 * - 统一入口，无需在多个页面间跳转
 * - 卡片式布局，视觉层次清晰
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsNewBinding

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var tushareDataSource: TushareDataSource

    @Inject
    lateinit var llmService: LLMService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadSettings()
        setupViews()
        updateStatus()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    /**
     * 加载当前配置
     */
    private fun loadSettings() {
        // 加载温度参数
        val temperature = preferencesManager.getLLMTemperature()
        binding.sliderTemperature.value = temperature
        binding.tvTemperatureValue.text = String.format("%.1f", temperature)

        // 加载 Tushare Token
        binding.etTushareToken.setText(preferencesManager.getTushareToken())

        // 加载通知设置
        binding.switchAnalysisNotification.isChecked = preferencesManager.isAnalysisNotificationsEnabled()
        binding.switchDailyReport.isChecked = preferencesManager.isDailyReportEnabled()
    }

    /**
     * 设置 UI 交互
     */
    private fun setupViews() {
        // AI 分析设置 - 跳转到 LLM 详细配置
        binding.btnLLMSettings.setOnClickListener {
            startActivity(Intent(this, LLMSettingsActivity::class.java))
        }

        // 温度滑块
        binding.sliderTemperature.addOnChangeListener { _, value, _ ->
            binding.tvTemperatureValue.text = String.format("%.1f", value)
            preferencesManager.setLLMTemperature(value)
        }

        // 数据源管理
        binding.btnDataSourceSettings.setOnClickListener {
            startActivity(Intent(this, DataSourceConfigActivity::class.java))
        }

        // Tushare Token 保存
        binding.btnSaveTushare.setOnClickListener {
            val token = binding.etTushareToken.text.toString().trim()
            preferencesManager.setTushareToken(token)
            Toast.makeText(this, "Tushare Token 已保存", Toast.LENGTH_SHORT).show()
            updateTushareStatus()
        }

        // Tushare 连接测试
        binding.btnTestTushare.setOnClickListener {
            testTushareConnection()
        }

        // 智能导入设置
        binding.btnImportSettings.setOnClickListener {
            startActivity(Intent(this, ImportSettingsActivity::class.java))
        }

        // 基本面设置
        binding.btnFundamentalSettings.setOnClickListener {
            // TODO: 打开基本面设置页面
            Toast.makeText(this, "基本面设置功能开发中", Toast.LENGTH_SHORT).show()
        }

        // 通知开关
        binding.switchAnalysisNotification.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setAnalysisNotificationsEnabled(isChecked)
        }

        binding.switchDailyReport.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setDailyReportEnabled(isChecked)
        }

        // Webhook 设置
        binding.btnWebhookSettings.setOnClickListener {
            // TODO: 打开 Webhook 配置页面
            Toast.makeText(this, "Webhook 配置功能开发中", Toast.LENGTH_SHORT).show()
        }

        // 关于页面
        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    /**
     * 更新状态显示
     */
    private fun updateStatus() {
        updateLLMStatus()
        updateDataSourceStatus()
        updateTushareStatus()
    }

    /**
     * 更新 LLM 状态
     */
    private fun updateLLMStatus() {
        val provider = preferencesManager.getLLMProvider()
        val model = preferencesManager.getLLMModel()
        
        if (llmService.isConfigured()) {
            binding.tvLLMStatus.text = "已配置: ${getProviderDisplayName(provider)} / $model"
            binding.tvLLMStatus.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            binding.tvLLMStatus.text = "未配置"
            binding.tvLLMStatus.setTextColor(Color.parseColor("#999999"))
        }
    }

    /**
     * 获取提供商显示名称
     */
    private fun getProviderDisplayName(provider: String): String {
        return when (provider) {
            "DEEPSEEK" -> "DeepSeek"
            "QWEN" -> "通义千问"
            "GEMINI" -> "Gemini"
            "OPENAI" -> "OpenAI"
            "ANTHROPIC" -> "Claude"
            "OLLAMA" -> "Ollama"
            else -> provider
        }
    }

    /**
     * 更新数据源状态
     */
    private fun updateDataSourceStatus() {
        val configs = preferencesManager.getDataSourceConfigs()
        val enabledCount = configs.count { it.enabled }
        val healthyCount = configs.count { it.enabled && it.isHealthy }

        binding.tvDataSourceStatus.text = "已启用: $enabledCount 个 | 健康: $healthyCount 个"
    }

    /**
     * 更新 Tushare 状态
     */
    private fun updateTushareStatus() {
        val token = preferencesManager.getTushareToken()
        
        if (token.isEmpty()) {
            binding.tvTushareStatus.text = "未配置"
            binding.tvTushareStatus.setTextColor(Color.parseColor("#999999"))
        } else {
            binding.tvTushareStatus.text = "已配置"
            binding.tvTushareStatus.setTextColor(Color.parseColor("#4CAF50"))
        }
    }

    /**
     * 测试 Tushare 连接
     */
    private fun testTushareConnection() {
        val token = binding.etTushareToken.text.toString().trim()
        
        if (token.isEmpty()) {
            binding.tvTushareStatus.text = "⚠️ 请输入 Token"
            binding.tvTushareStatus.setTextColor(Color.parseColor("#FFA500"))
            return
        }

        // 保存 Token
        preferencesManager.setTushareToken(token)
        
        binding.btnTestTushare.isEnabled = false
        binding.tvTushareStatus.text = "测试中..."
        binding.tvTushareStatus.setTextColor(Color.parseColor("#999999"))

        lifecycleScope.launch {
            try {
                val result = tushareDataSource.fetchQuote("000001")
                
                if (result.isSuccess) {
                    binding.tvTushareStatus.text = "✅ 连接成功"
                    binding.tvTushareStatus.setTextColor(Color.parseColor("#4CAF50"))
                    Toast.makeText(this@SettingsActivity, "Tushare 连接成功", Toast.LENGTH_SHORT).show()
                } else {
                    binding.tvTushareStatus.text = "❌ 连接失败"
                    binding.tvTushareStatus.setTextColor(Color.parseColor("#F44336"))
                    Toast.makeText(this@SettingsActivity, "连接失败，请检查 Token", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.tvTushareStatus.text = "❌ 连接失败"
                binding.tvTushareStatus.setTextColor(Color.parseColor("#F44336"))
                Toast.makeText(this@SettingsActivity, "连接失败: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnTestTushare.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从子页面返回时更新状态
        updateStatus()
    }
}
