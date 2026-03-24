package com.example.stockanalysis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 会话实体
 */
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 会话标题
    val title: String,

    // 关联的股票代码(可选)
    val stockSymbol: String? = null,

    // 会话内容(JSON格式存储消息列表)
    val content: String = "[]",

    // 创建时间
    val createdAt: Date = Date(),

    // 最后更新时间
    val updatedAt: Date = Date(),

    // 会话类型
    val sessionType: SessionType = SessionType.GENERAL
)

/**
 * 会话类型
 */
enum class SessionType {
    GENERAL,        // 普通对话
    STOCK_ANALYSIS, // 股票分析
    MULTI_AGENT     // 多Agent分析
}

/**
 * 会话消息
 */
data class ChatMessage(
    val role: String,      // user, assistant, system
    val content: String,    // 消息内容
    val timestamp: Date = Date()
)
