package com.example.stockanalysis.data.notification

import android.content.Context
import android.util.Log
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.data.model.AnalysisResult
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知管理器
 *
 * 统一管理本地和远程通知
 */
@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val remoteNotificationService: RemoteNotificationService,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "NotificationManager"
        private const val PREF_NOTIFICATION_CHANNELS = "notification_channels"
    }

    /**
     * 保存通知渠道配置
     */
    suspend fun saveChannelConfig(config: NotificationChannelConfig) = withContext(Dispatchers.IO) {
        try {
            val channels = getChannelConfigs().toMutableList()
            val existingIndex = channels.indexOfFirst { it.type == config.type }

            if (existingIndex >= 0) {
                channels[existingIndex] = config
            } else {
                channels.add(config)
            }

            val json = gson.toJson(channels)
            preferencesManager.setCustomValue(PREF_NOTIFICATION_CHANNELS, json)

            Log.d(TAG, "Saved channel config: ${config.type}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save channel config", e)
        }
    }

    /**
     * 获取所有通知渠道配置
     */
    suspend fun getChannelConfigs(): List<NotificationChannelConfig> = withContext(Dispatchers.IO) {
        try {
            val json = preferencesManager.getCustomValue(PREF_NOTIFICATION_CHANNELS) ?: return@withContext emptyList()
            gson.fromJson(json, Array<NotificationChannelConfig>::class.java).toList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get channel configs", e)
            emptyList()
        }
    }

    /**
     * 获取启用的通知渠道
     */
    suspend fun getEnabledChannels(): List<NotificationChannelConfig> {
        return getChannelConfigs().filter { it.enabled }
    }

    /**
     * 删除通知渠道配置
     */
    suspend fun deleteChannelConfig(type: NotificationChannelType) = withContext(Dispatchers.IO) {
        try {
            val channels = getChannelConfigs().filter { it.type != type }
            val json = gson.toJson(channels)
            preferencesManager.setCustomValue(PREF_NOTIFICATION_CHANNELS, json)

            Log.d(TAG, "Deleted channel config: $type")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete channel config", e)
        }
    }

    /**
     * 发送分析完成通知
     */
    suspend fun notifyAnalysisComplete(result: AnalysisResult): List<NotificationResult> {
        val channels = getEnabledChannels().filter { it.notifyOnAnalysisComplete }

        if (channels.isEmpty()) {
            Log.d(TAG, "No channels configured for analysis notifications")
            return emptyList()
        }

        val message = NotificationMessage(
            title = "分析完成",
            content = buildAnalysisMessage(result),
            type = NotificationMessageType.ANALYSIS,
            stockSymbol = result.stockSymbol,
            stockName = result.stockName
        )

        return remoteNotificationService.sendNotificationToMultipleChannels(channels, message)
    }

    /**
     * 发送市场警报通知
     */
    suspend fun notifyMarketAlert(
        title: String,
        content: String,
        stockSymbol: String? = null,
        stockName: String? = null
    ): List<NotificationResult> {
        val channels = getEnabledChannels().filter { it.notifyOnMarketAlert }

        if (channels.isEmpty()) {
            Log.d(TAG, "No channels configured for market alert notifications")
            return emptyList()
        }

        val message = NotificationMessage(
            title = title,
            content = content,
            type = NotificationMessageType.ALERT,
            stockSymbol = stockSymbol,
            stockName = stockName
        )

        return remoteNotificationService.sendNotificationToMultipleChannels(channels, message)
    }

    /**
     * 发送每日报告通知
     */
    suspend fun notifyDailyReport(
        summary: String,
        stats: Map<String, Any>
    ): List<NotificationResult> {
        val channels = getEnabledChannels().filter { it.notifyOnDailyReport }

        if (channels.isEmpty()) {
            Log.d(TAG, "No channels configured for daily report notifications")
            return emptyList()
        }

        val content = buildDailyReportMessage(summary, stats)

        val message = NotificationMessage(
            title = "每日分析报告",
            content = content,
            type = NotificationMessageType.INFO
        )

        return remoteNotificationService.sendNotificationToMultipleChannels(channels, message)
    }

    /**
     * 测试通知渠道
     */
    suspend fun testChannel(type: NotificationChannelType): NotificationResult {
        val channel = getChannelConfigs().find { it.type == type }
            ?: return NotificationResult(
                success = false,
                message = "Channel not configured"
            )

        return remoteNotificationService.testChannel(channel)
    }

    /**
     * 构建分析结果消息
     */
    private fun buildAnalysisMessage(result: AnalysisResult): String {
        return buildString {
            append("**${result.stockName} (${result.stockSymbol})**\n\n")

            // 决策建议
            append("📊 **决策建议**: ${result.decision}\n")
            append("📈 **综合评分**: ${String.format("%.1f", result.score)}/100\n\n")

            // 技术面
            result.technicalAnalysis?.let { tech ->
                append("**技术分析**\n")
                append("- 趋势: ${tech.trend}\n")
                append("- 支撑位: ${tech.supportLevel}\n")
                append("- 阻力位: ${tech.resistanceLevel}\n\n")
            }

            // 基本面
            result.fundamentalAnalysis?.let { fund ->
                append("**基本面分析**\n")
                append("- 估值: ${fund.valuation}\n")
                append("- 盈利能力: ${fund.profitability}\n\n")
            }

            // 风险提示
            result.riskAssessment?.let { risk ->
                append("⚠️ **风险评估**: ${risk.riskLevel}\n")
                if (risk.specificRisks.isNotEmpty()) {
                    append("风险点: ${risk.specificRisks.joinToString(", ")}\n")
                }
            }

            append("\n⏰ 分析时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(result.analysisTime)}")
        }
    }

    /**
     * 构建每日报告消息
     */
    private fun buildDailyReportMessage(summary: String, stats: Map<String, Any>): String {
        return buildString {
            append("📊 **今日分析统计**\n\n")
            append(summary)
            append("\n\n")

            stats.forEach { (key, value) ->
                append("- $key: $value\n")
            }

            append("\n⏰ 报告时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())}")
        }
    }
}

