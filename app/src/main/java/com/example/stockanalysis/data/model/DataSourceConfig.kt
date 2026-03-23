package com.example.stockanalysis.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据源配置模型
 *
 * 用于管理多个数据源的配置、优先级和状态
 */
data class DataSourceConfig(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    var enabled: Boolean = false,
    var priority: Int = 999,
    var apiKey: String = "",
    var apiUrl: String = "",
    var extraConfig: Map<String, String> = emptyMap(),
    var isHealthy: Boolean = true,
    var lastTestTime: Long = 0,
    var lastTestResult: String = "",
    val requiresApiKey: Boolean = true,
    val requiresConfig: Boolean = false,
    val configFields: List<ConfigField> = emptyList()
) {
    companion object {
        /**
         * 支持的数据源列表
         */
        fun getAvailableDataSources(): List<DataSourceConfig> {
            return listOf(
                DataSourceConfig(
                    id = "tushare",
                    name = "TushareDataSource",
                    displayName = "Tushare Pro",
                    description = "专业的中国A股数据服务，提供财务指标、基本面数据",
                    enabled = false,
                    priority = 1,
                    apiUrl = "http://api.tushare.pro",
                    requiresApiKey = true,
                    requiresConfig = false,
                    configFields = listOf(
                        ConfigField("token", "Token", "text", "请输入 Tushare Token")
                    )
                ),
                DataSourceConfig(
                    id = "akshare",
                    name = "AkShareDataSource",
                    displayName = "AKShare",
                    description = "开源免费的股票数据接口，无需注册",
                    enabled = true,
                    priority = 2,
                    apiUrl = "",
                    requiresApiKey = false,
                    requiresConfig = false
                ),
                DataSourceConfig(
                    id = "yfinance",
                    name = "YFinanceDataSource",
                    displayName = "Yahoo Finance",
                    description = "全球股票数据，支持美股、港股、A股",
                    enabled = true,
                    priority = 3,
                    apiUrl = "https://query1.finance.yahoo.com",
                    requiresApiKey = false,
                    requiresConfig = false
                ),
                DataSourceConfig(
                    id = "efinance",
                    name = "EFinanceDataSource",
                    displayName = "东方财富",
                    description = "东方财富网数据接口，提供A股实时行情",
                    enabled = true,
                    priority = 4,
                    apiUrl = "",
                    requiresApiKey = false,
                    requiresConfig = false
                ),
                DataSourceConfig(
                    id = "sina",
                    name = "SinaDataSource",
                    displayName = "新浪财经",
                    description = "新浪财经数据接口，提供实时行情和新闻",
                    enabled = false,
                    priority = 5,
                    apiUrl = "https://hq.sinajs.cn",
                    requiresApiKey = false,
                    requiresConfig = false
                ),
                DataSourceConfig(
                    id = "netease",
                    name = "NeteaseDataSource",
                    displayName = "网易财经",
                    description = "网易财经数据接口，提供历史数据和财报",
                    enabled = false,
                    priority = 6,
                    apiUrl = "https://money.163.com",
                    requiresApiKey = false,
                    requiresConfig = false
                ),
                DataSourceConfig(
                    id = "tencent",
                    name = "TencentDataSource",
                    displayName = "腾讯财经",
                    description = "腾讯财经数据接口，提供实时行情",
                    enabled = false,
                    priority = 7,
                    apiUrl = "https://qt.gtimg.cn",
                    requiresApiKey = false,
                    requiresConfig = false
                )
            )
        }

        /**
         * 从 JSON 反序列化
         */
        fun fromJson(json: String): List<DataSourceConfig> {
            return try {
                val type = object : TypeToken<List<DataSourceConfig>>() {}.type
                Gson().fromJson(json, type) ?: getAvailableDataSources()
            } catch (e: Exception) {
                getAvailableDataSources()
            }
        }

        /**
         * 序列化为 JSON
         */
        fun toJson(configs: List<DataSourceConfig>): String {
            return Gson().toJson(configs)
        }
    }

    /**
     * 配置字段定义
     */
    data class ConfigField(
        val key: String,
        val label: String,
        val type: String,  // text, password, url, number
        val hint: String
    )

    /**
     * 测试数据源连接
     */
    fun canTest(): Boolean {
        return if (requiresApiKey) {
            apiKey.isNotEmpty()
        } else {
            true
        }
    }

    /**
     * 获取状态图标
     */
    fun getStatusIcon(): String {
        return when {
            !enabled -> "⚪"
            isHealthy -> "✅"
            else -> "❌"
        }
    }

    /**
     * 获取状态描述
     */
    fun getStatusText(): String {
        return when {
            !enabled -> "未启用"
            lastTestTime == 0L -> "未测试"
            isHealthy -> "正常"
            else -> "异常"
        }
    }

    /**
     * 获取配置完整度
     */
    fun isConfigured(): Boolean {
        return when {
            !requiresApiKey -> true
            apiKey.isNotEmpty() -> true
            else -> false
        }
    }
}
