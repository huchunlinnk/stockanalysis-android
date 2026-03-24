package com.example.stockanalysis.data.notification

/**
 * 通知渠道类型
 */
enum class NotificationChannelType {
    /**
     * 微信（企业微信机器人）
     */
    WECHAT,

    /**
     * 飞书（飞书机器人）
     */
    FEISHU,

    /**
     * Telegram
     */
    TELEGRAM,

    /**
     * 钉钉（钉钉机器人）
     */
    DINGTALK,

    /**
     * Email
     */
    EMAIL,

    /**
     * Slack
     */
    SLACK,

    /**
     * Discord
     */
    DISCORD,

    /**
     * 本地推送
     */
    LOCAL_PUSH
}

/**
 * 通知渠道配置
 */
data class NotificationChannelConfig(
    val type: NotificationChannelType,
    val name: String,
    val webhookUrl: String? = null,
    val apiToken: String? = null,
    val chatId: String? = null,
    val email: String? = null,
    val enabled: Boolean = true,
    val notifyOnAnalysisComplete: Boolean = true,
    val notifyOnMarketAlert: Boolean = true,
    val notifyOnDailyReport: Boolean = true
)

/**
 * 通知消息
 */
data class NotificationMessage(
    val title: String,
    val content: String,
    val type: NotificationMessageType = NotificationMessageType.INFO,
    val stockSymbol: String? = null,
    val stockName: String? = null,
    val attachments: List<NotificationAttachment> = emptyList()
)

/**
 * 消息类型
 */
enum class NotificationMessageType {
    INFO,       // 信息
    SUCCESS,    // 成功
    WARNING,    // 警告
    ERROR,      // 错误
    ANALYSIS,   // 分析结果
    ALERT       // 市场警报
}

/**
 * 通知附件
 */
data class NotificationAttachment(
    val type: AttachmentType,
    val url: String? = null,
    val data: ByteArray? = null,
    val fileName: String? = null
) {
    enum class AttachmentType {
        IMAGE,
        FILE,
        CHART,
        LINK
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationAttachment

        if (type != other.type) return false
        if (url != other.url) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (fileName != other.fileName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (fileName?.hashCode() ?: 0)
        return result
    }
}

/**
 * 通知发送结果
 */
data class NotificationResult(
    val success: Boolean,
    val message: String,
    val errorCode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
