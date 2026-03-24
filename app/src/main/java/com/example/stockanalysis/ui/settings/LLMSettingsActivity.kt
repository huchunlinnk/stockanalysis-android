package com.example.stockanalysis.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stockanalysis.R
import com.example.stockanalysis.data.llm.LLMProvider
import com.example.stockanalysis.data.llm.LLMService
import com.example.stockanalysis.data.llm.getDefaultModels
import com.example.stockanalysis.data.llm.getRecommendedModels
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.databinding.ActivityLlmSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 大模型详细配置页面
 *
 * 功能：
 * 1. 提供商选择（DeepSeek/通义千问/Kimi/讯飞星火/智谱GLM/Gemini/OpenAI/Claude/Ollama）
 * 2. 多 API Key 支持（负载均衡）
 * 3. 主模型和 Fallback 模型选择
 * 4. 温度参数调节
 * 5. 超时时间配置
 * 6. 连接测试
 * 7. 智能配置：选择提供商后自动填充Base URL和推荐模型
 */
@AndroidEntryPoint
class LLMSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLlmSettingsBinding

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var llmService: LLMService

    private var selectedProvider: LLMProvider = LLMProvider.DEEPSEEK
    private var lastProvider: LLMProvider? = null
    private var selectedModel: String = ""
    private var selectedFallbackModel: String = ""

    // 模型列表
    private val recommendedModels = getRecommendedModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLlmSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadSettings()
        setupViews()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadSettings() {
        // 加载当前提供商
        val providerName = preferencesManager.getLLMProvider()
        selectedProvider = try {
            LLMProvider.valueOf(providerName)
        } catch (e: Exception) {
            LLMProvider.DEEPSEEK
        }
        lastProvider = selectedProvider

        // 设置提供商选中状态（只设置布局中存在的RadioButton）
        when (selectedProvider) {
            LLMProvider.DEEPSEEK -> binding.radioDeepSeek.isChecked = true
            LLMProvider.QWEN -> binding.radioQwen.isChecked = true
            LLMProvider.GEMINI -> binding.radioGemini.isChecked = true
            LLMProvider.OPENAI -> binding.radioOpenAI.isChecked = true
            LLMProvider.ANTHROPIC -> binding.radioClaude.isChecked = true
            LLMProvider.OLLAMA -> binding.radioOllama.isChecked = true
            else -> binding.radioDeepSeek.isChecked = true
        }

        // 加载 API Key
        binding.etApiKey.setText(preferencesManager.getLLMApiKey())

        // 加载备用 API Keys
        val backupKeys = preferencesManager.getLLMBackupApiKeys()
        binding.etBackupApiKeys.setText(backupKeys.joinToString(", "))

        // 加载 Base URL
        val baseUrl = preferencesManager.getLLMBaseUrl()
        binding.etBaseUrl.setText(baseUrl)

        // 加载模型选择
        selectedModel = preferencesManager.getLLMModel()
        selectedFallbackModel = preferencesManager.getLLMFallbackModel()

        // 加载温度参数
        val temperature = preferencesManager.getLLMTemperature()
        binding.sliderTemperature.value = temperature
        binding.tvTemperatureValue.text = String.format("%.1f", temperature)

        // 加载超时时间
        val timeout = preferencesManager.getLLMTimeout()
        binding.sliderTimeout.value = timeout.toFloat()
        binding.tvTimeoutValue.text = "${timeout} 秒"

        // 更新模型列表
        updateModelSpinners()
    }

    private fun setupViews() {
        // 提供商选择
        binding.radioGroupProvider.setOnCheckedChangeListener { _, checkedId ->
            val previousProvider = selectedProvider
            selectedProvider = when (checkedId) {
                R.id.radioDeepSeek -> LLMProvider.DEEPSEEK
                R.id.radioQwen -> LLMProvider.QWEN
                R.id.radioGemini -> LLMProvider.GEMINI
                R.id.radioOpenAI -> LLMProvider.OPENAI
                R.id.radioClaude -> LLMProvider.ANTHROPIC
                R.id.radioOllama -> LLMProvider.OLLAMA
                else -> LLMProvider.DEEPSEEK
            }
            
            // 智能自动填充Base URL
            val currentUrl = binding.etBaseUrl.text.toString().trim()
            val previousDefaultUrl = previousProvider.defaultBaseUrl
            val newDefaultUrl = selectedProvider.defaultBaseUrl
            
            // 自动填充条件：当前为空，或当前值等于上一个提供商的默认值
            if (currentUrl.isEmpty() || currentUrl == previousDefaultUrl) {
                binding.etBaseUrl.setText(newDefaultUrl)
            }
            
            // 记录上一个提供商
            lastProvider = previousProvider
            
            updateModelSpinners()
            updateHints()
            
            // 自动选择推荐模型
            autoSelectRecommendedModel()
        }

        // 温度滑块
        binding.sliderTemperature.addOnChangeListener { _, value, _ ->
            binding.tvTemperatureValue.text = String.format("%.1f", value)
        }

        // 超时滑块
        binding.sliderTimeout.addOnChangeListener { _, value, _ ->
            binding.tvTimeoutValue.text = "${value.toInt()} 秒"
        }

        // 测试连接
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }

        // 保存
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }

    /**
     * 自动选择推荐模型
     */
    private fun autoSelectRecommendedModel() {
        val defaultModel = selectedProvider.getDefaultModels().firstOrNull() ?: return
        
        // 设置主模型
        val models = selectedProvider.getDefaultModels()
        val modelIndex = models.indexOf(defaultModel)
        if (modelIndex >= 0) {
            binding.spinnerModel.setSelection(modelIndex)
            selectedModel = defaultModel
            updateModelInfo(defaultModel)
        }
        
        // 设置Fallback模型为"无"
        binding.spinnerFallbackModel.setSelection(0)
        selectedFallbackModel = ""
    }

    private fun updateModelSpinners() {
        // 获取当前提供商的推荐模型
        val providerModels = recommendedModels.filter { it.provider == selectedProvider }

        val modelNames = if (providerModels.isEmpty()) {
            // 如果没有推荐模型，使用默认模型列表
            selectedProvider.getDefaultModels()
        } else {
            providerModels.map { it.modelName }
        }

        setupModelSpinner(modelNames)
        setupFallbackSpinner(listOf("无", *modelNames.toTypedArray()))
    }

    private fun setupModelSpinner(models: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, models)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerModel.adapter = adapter

        // 设置当前选中的模型（如果有）
        val currentModel = preferencesManager.getLLMModel()
        val index = if (currentModel.isNotEmpty() && models.contains(currentModel)) {
            models.indexOf(currentModel)
        } else {
            // 使用默认模型
            val defaultModel = selectedProvider.getDefaultModels().firstOrNull()
            defaultModel?.let { models.indexOf(it) } ?: 0
        }
        
        if (index >= 0) {
            binding.spinnerModel.setSelection(index)
            selectedModel = models.getOrNull(index) ?: models.firstOrNull() ?: ""
        }

        binding.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedModel = models[position]
                updateModelInfo(selectedModel)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFallbackSpinner(models: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, models)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFallbackModel.adapter = adapter

        // 设置当前选中的 Fallback 模型
        val currentFallback = preferencesManager.getLLMFallbackModel()
        val index = if (currentFallback.isEmpty()) {
            0 // "无"
        } else {
            models.indexOf(currentFallback).takeIf { it >= 0 } ?: 0
        }
        binding.spinnerFallbackModel.setSelection(index)

        binding.spinnerFallbackModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFallbackModel = if (position == 0) "" else models[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateModelInfo(modelName: String) {
        val model = recommendedModels.find { it.modelName == modelName }
        if (model != null) {
            val costText = when (model.costTier) {
                com.example.stockanalysis.data.llm.CostTier.FREE -> "免费"
                com.example.stockanalysis.data.llm.CostTier.LOW -> "低"
                com.example.stockanalysis.data.llm.CostTier.MEDIUM -> "中"
                com.example.stockanalysis.data.llm.CostTier.HIGH -> "高"
            }
            binding.tvModelInfo.text = "上下文: ${model.contextWindow / 1000}K | 成本: $costText"
        } else {
            binding.tvModelInfo.text = "自定义模型"
        }
    }

    private fun updateHints() {
        // 根据提供商更新提示
        when (selectedProvider) {
            LLMProvider.DEEPSEEK -> {
                binding.etBaseUrl.hint = "https://api.deepseek.com/"
                binding.etApiKey.hint = "输入 DeepSeek API Key"
            }
            LLMProvider.QWEN -> {
                binding.etBaseUrl.hint = "https://dashscope.aliyuncs.com/compatible-mode/"
                binding.etApiKey.hint = "输入阿里云 DashScope API Key"
            }
            LLMProvider.KIMI -> {
                binding.etBaseUrl.hint = "https://api.moonshot.cn/"
                binding.etApiKey.hint = "输入 Moonshot API Key"
            }
            LLMProvider.SPARK -> {
                binding.etBaseUrl.hint = "https://spark-api.xf-yun.com/"
                binding.etApiKey.hint = "输入讯飞星火 API Key"
            }
            LLMProvider.ZHIPU -> {
                binding.etBaseUrl.hint = "https://open.bigmodel.cn/api/paas/v4/"
                binding.etApiKey.hint = "输入智谱 AI API Key"
            }
            LLMProvider.GEMINI -> {
                binding.etBaseUrl.hint = "https://generativelanguage.googleapis.com/"
                binding.etApiKey.hint = "输入 Google AI API Key"
            }
            LLMProvider.OPENAI -> {
                binding.etBaseUrl.hint = "https://api.openai.com/"
                binding.etApiKey.hint = "输入 OpenAI API Key"
            }
            LLMProvider.ANTHROPIC -> {
                binding.etBaseUrl.hint = "https://api.anthropic.com/"
                binding.etApiKey.hint = "输入 Anthropic API Key"
            }
            LLMProvider.OLLAMA -> {
                binding.etBaseUrl.hint = "http://localhost:11434/"
                binding.etApiKey.hint = "Ollama 不需要 API Key"
            }
            else -> {
                binding.etBaseUrl.hint = "自定义 API 地址"
                binding.etApiKey.hint = "输入 API Key"
            }
        }
    }

    private fun testConnection() {
        binding.btnTestConnection.isEnabled = false
        binding.btnTestConnection.text = "测试中..."
        binding.tvTestResult.visibility = View.GONE

        // 临时保存配置
        val apiKey = binding.etApiKey.text.toString().trim()
        val baseUrl = binding.etBaseUrl.text.toString().trim()
        val model = selectedModel

        preferencesManager.setLLMProvider(selectedProvider.name)
        preferencesManager.setLLMApiKey(apiKey)
        preferencesManager.setLLMBaseUrl(baseUrl.ifEmpty { selectedProvider.defaultBaseUrl })
        preferencesManager.setLLMModel(model)

        lifecycleScope.launch {
            try {
                val result = llmService.testConnection()

                if (result.isSuccess) {
                    binding.tvTestResult.text = "✅ 连接成功: ${result.getOrNull()}"
                    binding.tvTestResult.setTextColor(Color.parseColor("#4CAF50"))
                    Toast.makeText(this@LLMSettingsActivity, "LLM 连接成功", Toast.LENGTH_SHORT).show()
                } else {
                    val error = result.exceptionOrNull()
                    binding.tvTestResult.text = "❌ 连接失败: ${error?.message}"
                    binding.tvTestResult.setTextColor(Color.parseColor("#F44336"))
                }
            } catch (e: Exception) {
                binding.tvTestResult.text = "❌ 连接失败: ${e.message}"
                binding.tvTestResult.setTextColor(Color.parseColor("#F44336"))
            } finally {
                binding.btnTestConnection.isEnabled = true
                binding.btnTestConnection.text = "测试连接"
                binding.tvTestResult.visibility = View.VISIBLE
            }
        }
    }

    private fun saveSettings() {
        val apiKey = binding.etApiKey.text.toString().trim()
        val backupApiKeysText = binding.etBackupApiKeys.text.toString().trim()
        val baseUrl = binding.etBaseUrl.text.toString().trim()
        val temperature = binding.sliderTemperature.value
        val timeout = binding.sliderTimeout.value.toInt()

        // 解析备用 API Keys
        val backupApiKeys = backupApiKeysText
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 保存配置
        preferencesManager.setLLMProvider(selectedProvider.name)
        preferencesManager.setLLMApiKey(apiKey)
        preferencesManager.setLLMBackupApiKeys(backupApiKeys)
        preferencesManager.setLLMBaseUrl(baseUrl.ifEmpty { selectedProvider.defaultBaseUrl })
        preferencesManager.setLLMModel(selectedModel)
        preferencesManager.setLLMFallbackModel(selectedFallbackModel)
        preferencesManager.setLLMTemperature(temperature)
        preferencesManager.setLLMTimeout(timeout)

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
}
