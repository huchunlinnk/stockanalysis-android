package com.example.stockanalysis.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.stockanalysis.R
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.Decision
import com.example.stockanalysis.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知渠道类型
 */
enum class NotificationChannelType {
    LOCAL,           // 本地通知
    WECHAT_WORK,     // 企业微信
    FEISHU,          // 飞书
    DINGTALK,        // 钉钉
    TELEGRAM,        // Telegram
    EMAIL,           // 邮件
    PUSHPLUS,        // PushPlus
    SERVER_CHAN      // Server酱
}

/**
 * 通知服务接口
 */
interface NotificationService {
    /**
     * 发送分析结果通知
     */
    suspend fun sendAnalysisNotification(result: AnalysisResult): Boolean
    
    /**
     * 发送文本通知
     */
    suspend fun sendTextNotification(title: String, content: String): Boolean
    
    /**
     * 测试通知渠道
     */
    suspend fun testChannel(channelType: NotificationChannelType): Boolean
    
    /**
     * 获取支持的渠道
     */
    fun getSupportedChannels(): List<NotificationChannelType>
}

/**
 * 多渠道通知服务管理器
 */
@Singleton
class MultiChannelNotificationService @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) : NotificationService {
    
    companion object {
        const val TAG = "NotificationService"
        const val CHANNEL_ID_ANALYSIS = "stock_analysis_channel"
        const val CHANNEL_ID_GENERAL = "stock_general_channel"
        const val NOTIFICATION_ID_BASE = 1000
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    // 各渠道服务实例
    private val channelServices = mutableMapOf<NotificationChannelType, ChannelNotificationService>()
    
    init {
        createNotificationChannels()
        initializeChannelServices()
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 分析结果通知渠道
            val analysisChannel = NotificationChannel(
                CHANNEL_ID_ANALYSIS,
                "股票分析结果",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AI分析完成后的结果推送"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            
            // 普通通知渠道
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "一般通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "应用的一般通知消息"
            }
            
            notificationManager.createNotificationChannels(listOf(analysisChannel, generalChannel))
        }
    }
    
    /**
     * 初始化各渠道服务
     */
    private fun initializeChannelServices() {
        channelServices[NotificationChannelType.LOCAL] = LocalNotificationService(context, notificationManager)
        channelServices[NotificationChannelType.WECHAT_WORK] = WechatWorkService(okHttpClient)
        channelServices[NotificationChannelType.FEISHU] = FeishuService(okHttpClient)
        channelServices[NotificationChannelType.DINGTALK] = DingTalkService(okHttpClient)
        channelServices[NotificationChannelType.TELEGRAM] = TelegramService(okHttpClient)
        channelServices[NotificationChannelType.PUSHPLUS] = PushPlusService(okHttpClient)
        channelServices[NotificationChannelType.SERVER_CHAN] = ServerChanService(okHttpClient)
    }
    
    override suspend fun sendAnalysisNotification(result: AnalysisResult): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 构建通知内容
                val notification = buildAnalysisNotification(result)
                
                // 发送到所有已启用的渠道
                var success = false
                getEnabledChannels().forEach { channelType ->
                    try {
                        val service = channelServices[channelType]
                        if (service != null) {
                            val channelSuccess = service.sendNotification(notification)
                            if (channelSuccess) success = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send to channel $channelType", e)
                    }
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send analysis notification", e)
                false
            }
        }
    }
    
    override suspend fun sendTextNotification(title: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val notification = NotificationData(
                    title = title,
                    content = content,
                    type = NotificationType.GENERAL
                )
                
                var success = false
                getEnabledChannels().forEach { channelType ->
                    try {
                        val service = channelServices[channelType]
                        if (service != null) {
                            val channelSuccess = service.sendNotification(notification)
                            if (channelSuccess) success = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send to channel $channelType", e)
                    }
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send text notification", e)
                false
            }
        }
    }
    
    override suspend fun testChannel(channelType: NotificationChannelType): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val service = channelServices[channelType] ?: return@withContext false
                
                val testNotification = NotificationData(
                    title = "通知测试",
                    content = "这是一条测试消息，如果您收到说明${channelType.displayName()}配置正确。",
                    type = NotificationType.TEST
                )
                
                service.sendNotification(testNotification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to test channel $channelType", e)
                false
            }
        }
    }
    
    override fun getSupportedChannels(): List<NotificationChannelType> {
        return NotificationChannelType.values().toList()
    }
    
    /**
     * 获取已启用的渠道列表
     */
    private fun getEnabledChannels(): List<NotificationChannelType> {
        // TODO: 从配置中读取启用的渠道
        return listOf(NotificationChannelType.LOCAL)
    }
    
    /**
     * 构建分析结果通知
     */
    private fun buildAnalysisNotification(result: AnalysisResult): NotificationData {
        val decisionEmoji = when (result.decision) {
            Decision.STRONG_BUY -> "🚀"
            Decision.BUY -> "📈"
            Decision.HOLD -> "➖"
            Decision.SELL -> "📉"
            Decision.STRONG_SELL -> "⚠️"
        }
        
        val decisionText = when (result.decision) {
            Decision.STRONG_BUY -> "强烈买入"
            Decision.BUY -> "买入"
            Decision.HOLD -> "持有观望"
            Decision.SELL -> "卖出"
            Decision.STRONG_SELL -> "强烈卖出"
        }
        
        val title = "$decisionEmoji ${result.stockName}(${result.stockSymbol}) - $decisionText"
        
        val content = buildString {
            appendLine("📊 综合评分: ${result.score}/100")
            appendLine("🎯 置信度: ${getConfidenceText(result.confidence)}")
            appendLine()
            appendLine("💡 分析摘要:")
            appendLine(result.summary)
            
            result.actionPlan?.let { plan ->
                appendLine()
                appendLine("📋 操作建议:")
                plan.entryPrice?.let { appendLine("  买入价: ¥${String.format("%.2f", it)}") }
                plan.targetPrice?.let { appendLine("  目标价: ¥${String.format("%.2f", it)}") }
                plan.stopLossPrice?.let { appendLine("  止损价: ¥${String.format("%.2f", it)}") }
            }
        }
        
        return NotificationData(
            title = title,
            content = content,
            type = NotificationType.ANALYSIS_RESULT,
            symbol = result.stockSymbol,
            decision = result.decision,
            score = result.score
        )
    }
    
    private fun getConfidenceText(confidence: com.example.stockanalysis.data.model.ConfidenceLevel): String {
        return when (confidence) {
            com.example.stockanalysis.data.model.ConfidenceLevel.HIGH -> "高"
            com.example.stockanalysis.data.model.ConfidenceLevel.MEDIUM -> "中"
            com.example.stockanalysis.data.model.ConfidenceLevel.LOW -> "低"
        }
    }
    
    /**
     * 设置渠道配置
     */
    fun configureChannel(channelType: NotificationChannelType, config: ChannelConfig) {
        val service = channelServices[channelType]
        service?.configure(config)
    }
    
    /**
     * 启用/禁用渠道
     */
    fun setChannelEnabled(channelType: NotificationChannelType, enabled: Boolean) {
        // TODO: 保存到配置
    }
}

/**
 * 通知渠道类型显示名称
 */
fun NotificationChannelType.displayName(): String {
    return when (this) {
        NotificationChannelType.LOCAL -> "本地通知"
        NotificationChannelType.WECHAT_WORK -> "企业微信"
        NotificationChannelType.FEISHU -> "飞书"
        NotificationChannelType.DINGTALK -> "钉钉"
        NotificationChannelType.TELEGRAM -> "Telegram"
        NotificationChannelType.EMAIL -> "邮件"
        NotificationChannelType.PUSHPLUS -> "PushPlus"
        NotificationChannelType.SERVER_CHAN -> "Server酱"
    }
}

/**
 * 通知类型
 */
enum class NotificationType {
    ANALYSIS_RESULT,    // 分析结果
    PRICE_ALERT,        // 价格提醒
    GENERAL,            // 一般通知
    TEST                // 测试通知
}

/**
 * 通知数据
 */
data class NotificationData(
    val title: String,
    val content: String,
    val type: NotificationType,
    val symbol: String? = null,
    val decision: Decision? = null,
    val score: Int? = null
)

/**
 * 渠道配置基类
 */
abstract class ChannelConfig

/**
 * 渠道通知服务接口
 */
interface ChannelNotificationService {
    /**
     * 发送通知
     */
    suspend fun sendNotification(notification: NotificationData): Boolean
    
    /**
     * 配置渠道
     */
    fun configure(config: ChannelConfig)
    
    /**
     * 检查是否已配置
     */
    fun isConfigured(): Boolean
}

/**
 * 本地通知服务
 */
class LocalNotificationService(
    private val context: Context,
    private val notificationManager: NotificationManager
) : ChannelNotificationService {
    
    private var notificationId = MultiChannelNotificationService.NOTIFICATION_ID_BASE
    
    override suspend fun sendNotification(notification: NotificationData): Boolean {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                notification.symbol?.let { putExtra("symbol", it) }
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val builder = NotificationCompat.Builder(context, MultiChannelNotificationService.CHANNEL_ID_ANALYSIS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notification.title)
                .setContentText(notification.content.take(100) + if (notification.content.length > 100) "..." else "")
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            
            // 根据决策设置颜色
            notification.decision?.let { decision ->
                val color = when (decision) {
                    Decision.STRONG_BUY, Decision.BUY -> 0xFF4CAF50.toInt()
                    Decision.SELL, Decision.STRONG_SELL -> 0xFFF44336.toInt()
                    Decision.HOLD -> 0xFFFF9800.toInt()
                }
                builder.color = color
            }
            
            notificationManager.notify(notificationId++, builder.build())
            true
        } catch (e: Exception) {
            Log.e("LocalNotification", "Failed to send notification", e)
            false
        }
    }
    
    override fun configure(config: ChannelConfig) {
        // 本地通知不需要额外配置
    }
    
    override fun isConfigured(): Boolean = true
}

/**
 * 企业微信配置
 */
data class WechatWorkConfig(
    val webhookUrl: String,
    val mentionedList: List<String>? = null
) : ChannelConfig()

/**
 * 企业微信服务
 */
class WechatWorkService(private val httpClient: OkHttpClient) : ChannelNotificationService {
    
    private var config: WechatWorkConfig? = null
    
    override suspend fun sendNotification(notification: NotificationData): Boolean {
        if (!isConfigured()) return false
        
        return try {
            val json = JSONObject().apply {
                put("msgtype", "text")
                put("text", JSONObject().apply {
                    put("content", "${notification.title}\n\n${notification.content}")
                    config?.mentionedList?.let { put("mentioned_list", it) }
                })
            }
            
            val request = Request.Builder()
                .url(config!!.webhookUrl)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("WechatWork", "Failed to send notification", e)
            false
        }
    }
    
    override fun configure(config: ChannelConfig) {
        if (config is WechatWorkConfig) {
            this.config = config
        }
    }
    
    override fun isConfigured(): Boolean = config != null
}

/**
 * 飞书配置
 */
data class FeishuConfig(
    val webhookUrl: String,
    val secret: String? = null
) : ChannelConfig()

/**
 * 飞书服务
 */
class FeishuService(private val httpClient: OkHttpClient) : ChannelNotificationService {
    
    private var config: FeishuConfig? = null
    
    override suspend fun sendNotification(notification: NotificationData): Boolean {
        if (!isConfigured()) return false
        
        return try {
            val json = JSONObject().apply {
                put("msg_type", "text")
                put("content", JSONObject().apply {
                    put("text", "${notification.title}\n\n${notification.content}")
                })
            }
            
            val request = Request.Builder()
                .url(config!!.webhookUrl)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("Feishu", "Failed to send notification", e)
            false
        }
    }
    
    override fun configure(config: ChannelConfig) {
        if (config is FeishuConfig) {
            this.config = config
        }
    }
    
    override fun isConfigured(): Boolean = config != null
}

/**
 * 钉钉配置
 */
data class DingTalkConfig(
    val webhookUrl: String,
    val secret: String? = null
) : ChannelConfig()

/**
 * 钉钉服务
 */
class DingTalkService(private val httpClient: OkHttpClient) : ChannelNotificationService {
    
    private var config: DingTalkConfig? = null
    
    override suspend fun sendNotification(notification: NotificationData): Boolean {
        if (!isConfigured()) return false
        
        return try {
            val json = JSONObject().apply {
                put("msgtype", "text")
                put("text", JSONObject().apply {
                    put("content", "${notification.title}\n\n${notification.content}")
                })
            }
            
            val url = if (config?.secret != null) {
                // 如果有密钥，需要签名
                val timestamp = System.currentTimeMillis()
                val sign = calculateDingTalkSign(config!!.secret!!, timestamp)
                "${config!!.webhookUrl}&timestamp=$timestamp&sign=$sign"
            } else {
                config!!.webhookUrl
            }
            
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DingTalk", "Failed to send notification", e)
            false
        }
    }
    
    private fun calculateDingTalkSign(secret: String, timestamp: Long): String {
        val stringToSign = "$timestamp\n$secret"
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val signData = mac.doFinal(stringToSign.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(signData)
    }
    
    override fun configure(config: ChannelConfig) {
        if (config is DingTalkConfig) {
            this.config = config
        }
    }
    
    override fun isConfigured(): Boolean = config != null
}

/**
 * Telegram配置
 */
data class TelegramConfig(
    val botToken: String,
    val chatId: String
) : ChannelConfig()

/**
 * Telegram服务
 */
class TelegramService(private val httpClient: OkHttpClient) : ChannelNotificationService {
    
    private var config: TelegramConfig? = null
    
    override suspend fun sendNotification(notification: NotificationData): Boolean {
        if (!isConfigured()) return false
        
        return try {
            val url = "https://api.telegram.org/bot${config!!.botToken}/sendMessage"
            
            val json = JSONObject().apply {
                put("chat_id", config!!.chatId)
                put("text", "<b>${notification.title}</b>\n\n${notification.content}")
                put("parse_mode", "HTML")
            }
            
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("Telegram", "Failed to send notification", e)
            false
        }
    }
    
    override fun configure(config: ChannelConfig) {
        if (config is TelegramConfig) {
            this.config = config
        }
    }
    
    override fun isConfigured(): Boolean = config != null
}

/**
 * PushPlus配置
 */
data class PushPlusConfig(
    val token: String,
    val topic: String? = null
) : ChannelConfig()

/**
 * PushPlus服务
 */
class PushPlusService(private val httpClient: OkHttpClient) : ChannelNotificationService {
    
    private var config: PushPlusConfig? = null
    
    override suspend fun sendNotification(notification: NotificationData): Boolean {
        if (!isConfigured()) return false
        
        return try {
            val json = JSONObject().apply {
                put("token", config!!.token)
                put("title", notification.title)
                put("content", notification.content)
                put("template", "txt")
                config?.topic?.let { put("topic", it) }
            }
            
            val request = Request.Builder()
                .url("http://www.pushplus.plus/send")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("PushPlus", "Failed to send notification", e)
            false
        }
    }
    
    override fun configure(config: ChannelConfig) {
        if (config is PushPlusConfig) {
            this.config = config
        }
    }
    
    override fun isConfigured(): Boolean = config != null
}

/**
 * Server酱配置
 */
data class ServerChanConfig(
    val sendKey: String
) : ChannelConfig()

/**
 * Server酱服务
 */
class ServerChanService(private val httpClient: OkHttpClient) : ChannelNotificationService {
    
    private var config: ServerChanConfig? = null
    
    override suspend fun sendNotification(notification: NotificationData): Boolean {
        if (!isConfigured()) return false
        
        return try {
            val url = "https://sctapi.ftqq.com/${config!!.sendKey}.send"
            
            val json = JSONObject().apply {
                put("title", notification.title)
                put("desp", notification.content.replace("\n", "\n\n"))
            }
            
            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ServerChan", "Failed to send notification", e)
            false
        }
    }
    
    override fun configure(config: ChannelConfig) {
        if (config is ServerChanConfig) {
            this.config = config
        }
    }
    
    override fun isConfigured(): Boolean = config != null
}
