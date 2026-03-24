package com.example.stockanalysis.data.repository

import com.example.stockanalysis.data.local.ChatSessionDao
import com.example.stockanalysis.data.model.ChatMessage
import com.example.stockanalysis.data.model.ChatSession
import com.example.stockanalysis.data.model.SessionType
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话管理Repository
 */
@Singleton
class ChatSessionRepository @Inject constructor(
    private val chatSessionDao: ChatSessionDao
) {

    /**
     * 获取所有会话
     */
    fun getAllSessions(): Flow<List<ChatSession>> {
        return chatSessionDao.getAllSessions()
    }

    /**
     * 获取最近会话
     */
    fun getRecentSessions(limit: Int = 10): Flow<List<ChatSession>> {
        return chatSessionDao.getRecentSessions(limit)
    }

    /**
     * 根据ID获取会话
     */
    suspend fun getSessionById(sessionId: Long): ChatSession? {
        return chatSessionDao.getSessionById(sessionId)
    }

    /**
     * 根据股票代码获取会话
     */
    fun getSessionsByStock(stockSymbol: String): Flow<List<ChatSession>> {
        return chatSessionDao.getSessionsByStock(stockSymbol)
    }

    /**
     * 搜索会话
     */
    fun searchSessions(query: String): Flow<List<ChatSession>> {
        return chatSessionDao.searchSessions(query)
    }

    /**
     * 创建新会话
     */
    suspend fun createSession(
        title: String,
        stockSymbol: String? = null,
        sessionType: SessionType = SessionType.GENERAL
    ): Long {
        val session = ChatSession(
            title = title,
            stockSymbol = stockSymbol,
            sessionType = sessionType,
            createdAt = Date(),
            updatedAt = Date()
        )
        return chatSessionDao.insertSession(session)
    }

    /**
     * 添加消息到会话
     */
    suspend fun addMessageToSession(sessionId: Long, message: ChatMessage) {
        val session = chatSessionDao.getSessionById(sessionId) ?: return

        // 解析现有消息
        val messages = parseMessages(session.content).toMutableList()

        // 添加新消息
        messages.add(message)

        // 更新会话
        val updatedSession = session.copy(
            content = messagesToJson(messages),
            updatedAt = Date()
        )
        chatSessionDao.updateSession(updatedSession)
    }

    /**
     * 获取会话消息
     */
    suspend fun getSessionMessages(sessionId: Long): List<ChatMessage> {
        val session = chatSessionDao.getSessionById(sessionId) ?: return emptyList()
        return parseMessages(session.content)
    }

    /**
     * 更新会话标题
     */
    suspend fun updateSessionTitle(sessionId: Long, newTitle: String) {
        val session = chatSessionDao.getSessionById(sessionId) ?: return
        val updatedSession = session.copy(
            title = newTitle,
            updatedAt = Date()
        )
        chatSessionDao.updateSession(updatedSession)
    }

    /**
     * 删除会话
     */
    suspend fun deleteSession(sessionId: Long) {
        chatSessionDao.deleteSessionById(sessionId)
    }

    /**
     * 删除所有会话
     */
    suspend fun deleteAllSessions() {
        chatSessionDao.deleteAllSessions()
    }

    /**
     * 导出会话为Markdown
     */
    suspend fun exportSessionAsMarkdown(sessionId: Long): String {
        val session = chatSessionDao.getSessionById(sessionId) ?: return ""
        val messages = parseMessages(session.content)

        val sb = StringBuilder()
        sb.appendLine("# ${session.title}")
        sb.appendLine()

        if (session.stockSymbol != null) {
            sb.appendLine("**股票代码**: ${session.stockSymbol}")
            sb.appendLine()
        }

        sb.appendLine("**创建时间**: ${session.createdAt}")
        sb.appendLine("**更新时间**: ${session.updatedAt}")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        messages.forEach { message ->
            val role = when (message.role) {
                "user" -> "用户"
                "assistant" -> "AI助手"
                "system" -> "系统"
                else -> message.role
            }

            sb.appendLine("## $role (${message.timestamp})")
            sb.appendLine()
            sb.appendLine(message.content)
            sb.appendLine()
        }

        return sb.toString()
    }

    // ==================== 私有方法 ====================

    /**
     * 解析JSON消息列表
     */
    private fun parseMessages(json: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                messages.add(
                    ChatMessage(
                        role = obj.optString("role"),
                        content = obj.optString("content"),
                        timestamp = Date(obj.optLong("timestamp"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return messages
    }

    /**
     * 消息列表转JSON
     */
    private fun messagesToJson(messages: List<ChatMessage>): String {
        val jsonArray = JSONArray()

        messages.forEach { message ->
            val obj = JSONObject()
            obj.put("role", message.role)
            obj.put("content", message.content)
            obj.put("timestamp", message.timestamp.time)
            jsonArray.put(obj)
        }

        return jsonArray.toString()
    }
}
