package com.example.stockanalysis.data.local

import androidx.room.*
import com.example.stockanalysis.data.model.ChatSession
import com.example.stockanalysis.data.model.SessionType
import kotlinx.coroutines.flow.Flow

/**
 * 会话数据访问对象
 */
@Dao
interface ChatSessionDao {

    /**
     * 获取所有会话(按更新时间倒序)
     */
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    /**
     * 根据ID获取会话
     */
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChatSession?

    /**
     * 根据股票代码获取会话
     */
    @Query("SELECT * FROM chat_sessions WHERE stockSymbol = :stockSymbol ORDER BY updatedAt DESC")
    fun getSessionsByStock(stockSymbol: String): Flow<List<ChatSession>>

    /**
     * 根据类型获取会话
     */
    @Query("SELECT * FROM chat_sessions WHERE sessionType = :type ORDER BY updatedAt DESC")
    fun getSessionsByType(type: SessionType): Flow<List<ChatSession>>

    /**
     * 搜索会话
     */
    @Query("SELECT * FROM chat_sessions WHERE title LIKE '%' || :query || '%' OR stockSymbol LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchSessions(query: String): Flow<List<ChatSession>>

    /**
     * 插入会话
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    /**
     * 更新会话
     */
    @Update
    suspend fun updateSession(session: ChatSession)

    /**
     * 删除会话
     */
    @Delete
    suspend fun deleteSession(session: ChatSession)

    /**
     * 根据ID删除会话
     */
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    /**
     * 删除所有会话
     */
    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    /**
     * 获取最近N个会话
     */
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<ChatSession>>
}
