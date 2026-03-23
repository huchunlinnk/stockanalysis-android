package com.example.stockanalysis.data.notification

import android.util.Log
import com.example.stockanalysis.util.CrashReportingManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 远程通知服务
 *
 * 支持多种通知渠道：
 * - 微信（企业微信机器人）
 * - 飞书（飞书机器人）
 * - Telegram
 * - 钉钉（钉钉机器人）
 * - Email
 * - Slack
 */
@Singleton
class RemoteNotificationService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val crashReportingManager: CrashReportingManager
) {

    companion object {
        private const val TAG = "RemoteNotificationService"
    }

    /**
     * 发送通知
     *
     * @param channel 通知渠道配置
     * @param message 通知消息
     */
    suspend fun sendNotification(
        channel: NotificationChannelConfig,
        message: NotificationMessage
    ): NotificationResult = withContext(Dispatchers.IO) {
        if (!channel.enabled) {
            return@withContext NotificationResult(
                success = false,
                message = "Channel is disabled"
            )
        }

        try {
            Log.d(TAG, "Sending notification via ${channel.type}: ${message.title}")

            val result = when (channel.type) {
                NotificationChannelType.WECHAT -> sendWechatNotification(channel, message)
                NotificationChannelType.FEISHU -> sendFeishuNotification(channel, message)
                NotificationChannelType.TELEGRAM -> sendTelegramNotification(channel, message)
                NotificationChannelType.DINGTALK -> sendDingtalkNotification(channel, message)
                NotificationChannelType.EMAIL -> sendEmailNotification(channel, message)
                NotificationChannelType.SLACK -> sendSlackNotification(channel, message)
                NotificationChannelType.DISCORD -> sendDiscordNotification(channel, message)
                NotificationChannelType.LOCAL_PUSH -> sendLocalPushNotification(message)
            }

            if (result.success) {
                Log.d(TAG, "Notification sent successfully via ${channel.type}")
            } else {
                Log.w(TAG, "Failed to send notification via ${channel.type}: ${result.message}")
                crashReportingManager.log("Notification failed: ${channel.type} - ${result.message}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification via ${channel.type}", e)
            crashReportingManager.recordException(e, "Notification error: ${channel.type}")

            NotificationResult(
                success = false,
                message = e.message ?: "Unknown error",
                errorCode = e.javaClass.simpleName
            )
        }
    }

    /**
     * 发送微信（企业微信机器人）通知
     */
    private suspend fun sendWechatNotification(
        channel: NotificationChannelConfig,
        message: NotificationMessage
    ): NotificationResult {
        val webhookUrl = channel.webhookUrl ?: return NotificationResult(
            success = false,
            message = "Webhook URL not configured"
        )

        // 企业微信 Markdown 格式
        val markdown = buildString {
            append("**${message.title}**\n\n")
            append(message.content)
            if (message.stockSymbol != null && message.stockName != null) {
                append("\n\n> 股票: ${message.stockName} (${message.stockSymbol})")
            }
        }

        val payload = JsonObject().apply {
            addProperty("msgtype", "markdown")
            add("markdown", JsonObject().apply {
                addProperty("content", markdown)
            })
        }

        return sendWebhookRequest(webhookUrl, payload)
    }

    /**
     * 发送飞书（飞书机器人）通知
     */
    private suspend fun sendFeishuNotification(
        channel: NotificationChannelConfig,
        message: NotificationMessage
    ): NotificationResult {
        val webhookUrl = channel.webhookUrl ?: return NotificationResult(
            success = false,
            message = "Webhook URL not configured"
        )

        // 飞书富文本格式
        val payload = JsonObject().apply {
            addProperty("msg_type", "interactive")
            add("card", JsonObject().apply {
                add("header", JsonObject().apply {
                    addProperty("title", JsonObject().apply {
                        addProperty("content", message.title)
                        addProperty("tag", "plain_text")
                    }.toString())
                    addProperty("template", when (message.type) {
                        NotificationMessageType.SUCCESS -> "green"
                        NotificationMessageType.WARNING -> "orange"
                        NotificationMessageType.ERROR -> "red"
                        else -> "blue"
                    })
                })
                add("elements", gson.toJsonTree(listOf(
                    mapOf(
                        "tag" to "div",
                        "text" to mapOf(
                            "content" to message.content,
                            "tag" to "plain_text"
                        )
                    )
                )))
            })
        }

        return sendWebhookRequest(webhookUrl, payload)
    }

    /**
     * 发送 Telegram 通知
     */
    private suspend fun sendTelegramNotification(
        channel: NotificationChannelConfig,
        message: NotificationMessage
    ): NotificationResult {
        val apiToken = channel.apiToken ?: return NotificationResult(
            success = false,
            message = "API token not configured"
        )

        val chatId = channel.chatId ?: return NotificationResult(
            success = false,
            message = "Chat ID not configured"
        )

        val url = "https://api.telegram.org/bot$apiToken/sendMessage"

        // Telegram Markdown 格式
        val text = buildString {
            append("*${message.title}*\n\n")
            append(message.content)
            if (message.stockSymbol != null && message.stockName != null) {
                append("\n\n📊 股票: ${message.stockName} (${message.stockSymbol})")
            }
        }

        val payload = JsonObject().apply {
            addProperty("chat_id", chatId)
            addProperty("text", text)
            addProperty("parse_mode", "Markdown")
        }

        return sendWebhookRequest(url, payload)
    }

    /**
     * 发送钉钉（钉钉机器人）通知
     */
    private suspend fun sendDingtalkNotification(
        channel: NotificationChannelConfig,
        message: NotificationMessage
    ): NotificationResult {
        val webhookUrl = channel.webhookUrl ?: return NotificationResult(
            success = false,
            message = "Webhook URL not configured"
        )

        // 钉钉 Markdown 格式
        val markdown = buildString {
            append("### ${message.title}\n\n")
            append(message.content)
            if (message.stockSymbol != null && message.stockName != null) {
                append("\n\n> 股票: ${message.stockName} (${message.stockSymbol})")
            }
        }

        val payload = JsonObject().apply {
            addProperty("msgtype", "markdown")
            add("markdown", JsonObject().apply {
                addProperty("title", message.title)
                addProperty("text", markdown)
            })
        }

        return sendWebhookRequest(webhookUrl, payload)
    }

    /**
     * 发送 Email 通知
     */
    private suspend fun sendEmailNotification(
        channel: NotificationChannelConfig,
        message: NotificationMessage
    ): NotificationResult {
        // Email 需要配置 SMTP 服务器，这里返回未实现
        // 实际项目中可以集成 JavaMail 或使用第三方邮件服务（如 SendGrid）
        return NotificationResult(
            success = false,
            message = "Email notification not implemented yet",
            errorCode = "NOT_IMPLEMENTED"
        )
    }

    /**
     * 发送 Slack 通知
     */
    private suspend fun sendSlackNotification(
        channel: NotificationChannelConfig,
        message: NotificationMessage
    ): NotificationResult {
        val webhookUrl = channel.webhookUrl ?: return NotificationResult(
            success = false,
            message = "Webhook URL not configured"
        )

        val payload = JsonObject().apply {
            addProperty("text", message.title)
            add("blocks", gson.toJsonTree(listOf(
                mapOf(
                    "type" to "header",
                    "text" to mapOf(
                        "type" to "plain_text",
                        "text" to message.title
                    )
                ),
                mapOf(
                    "type" to "section",
                    "text" to mapOf(
                        "type" to "mrkdwn",
                        "text" to message.content
                    )
                )
            )))
        }

        return sendWebhookRequest(webhookUrl, payload)
    }

    /**
     * 发送 Discord 通知
     */
    private suspend fun sendDiscordNotification(
        channel: NotificationChannelConfig,
        message: NotificationMessage
    ): NotificationResult {
        val webhookUrl = channel.webhookUrl ?: return NotificationResult(
            success = false,
            message = "Webhook URL not configured"
        )

        val payload = JsonObject().apply {
            add("embeds", gson.toJsonTree(listOf(
                mapOf(
                    "title" to message.title,
                    "description" to message.content,
                    "color" to when (message.type) {
                        NotificationMessageType.SUCCESS -> 0x00FF00
                        NotificationMessageType.WARNING -> 0xFFA500
                        NotificationMessageType.ERROR -> 0xFF0000
                        else -> 0x0099FF
                    },
                    "timestamp" to java.time.Instant.now().toString()
                )
            )))
        }

        return sendWebhookRequest(webhookUrl, payload)
    }

    /**
     * 发送本地推送通知
     */
    private suspend fun sendLocalPushNotification(
        message: NotificationMessage
    ): NotificationResult {
        // 本地推送通知应该在 Android NotificationManager 中实现
        // 这里只是占位，实际实现应该在 LocalNotificationManager 中
        return NotificationResult(
            success = true,
            message = "Local push notification should be handled by NotificationManager"
        )
    }

    /**
     * 发送 Webhook 请求
     */
    private suspend fun sendWebhookRequest(
        url: String,
        payload: JsonObject
    ): NotificationResult {
        return try {
            val json = gson.toJson(payload)
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                NotificationResult(
                    success = true,
                    message = "Notification sent successfully"
                )
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                NotificationResult(
                    success = false,
                    message = "HTTP ${response.code}: $errorBody",
                    errorCode = response.code.toString()
                )
            }
        } catch (e: Exception) {
            NotificationResult(
                success = false,
                message = e.message ?: "Network error",
                errorCode = e.javaClass.simpleName
            )
        }
    }

    /**
     * 批量发送通知
     */
    suspend fun sendNotificationToMultipleChannels(
        channels: List<NotificationChannelConfig>,
        message: NotificationMessage
    ): List<NotificationResult> {
        return channels.map { channel ->
            sendNotification(channel, message)
        }
    }

    /**
     * 测试通知渠道
     */
    suspend fun testChannel(channel: NotificationChannelConfig): NotificationResult {
        val testMessage = NotificationMessage(
            title = "测试通知",
            content = "这是一条来自股票分析应用的测试通知。如果您收到此消息，说明通知配置正确。",
            type = NotificationMessageType.INFO
        )

        return sendNotification(channel, testMessage)
    }
}
