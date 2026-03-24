package com.example.stockanalysis.data.local

import androidx.room.*
import com.example.stockanalysis.data.model.MemoryType
import com.example.stockanalysis.data.model.UserMemory
import kotlinx.coroutines.flow.Flow

/**
 * 用户记忆 DAO
 */
@Dao
interface UserMemoryDao {

    /**
     * 插入记忆
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: UserMemory): Long

    /**
     * 批量插入记忆
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memories: List<UserMemory>)

    /**
     * 更新记忆
     */
    @Update
    suspend fun update(memory: UserMemory)

    /**
     * 删除记忆
     */
    @Delete
    suspend fun delete(memory: UserMemory)

    /**
     * 根据 ID 删除
     */
    @Query("DELETE FROM user_memory WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 根据类型删除
     */
    @Query("DELETE FROM user_memory WHERE type = :type")
    suspend fun deleteByType(type: MemoryType)

    /**
     * 根据 Key 删除
     */
    @Query("DELETE FROM user_memory WHERE key = :key")
    suspend fun deleteByKey(key: String)

    /**
     * 清空所有记忆
     */
    @Query("DELETE FROM user_memory")
    suspend fun deleteAll()

    /**
     * 根据 ID 查询
     */
    @Query("SELECT * FROM user_memory WHERE id = :id")
    suspend fun getById(id: Long): UserMemory?

    /**
     * 根据 Key 查询
     */
    @Query("SELECT * FROM user_memory WHERE key = :key ORDER BY lastAccessedAt DESC LIMIT 1")
    suspend fun getByKey(key: String): UserMemory?

    /**
     * 根据类型查询
     */
    @Query("SELECT * FROM user_memory WHERE type = :type ORDER BY lastAccessedAt DESC")
    suspend fun getByType(type: MemoryType): List<UserMemory>

    /**
     * 根据类型查询（Flow）
     */
    @Query("SELECT * FROM user_memory WHERE type = :type ORDER BY lastAccessedAt DESC")
    fun getByTypeFlow(type: MemoryType): Flow<List<UserMemory>>

    /**
     * 查询所有记忆
     */
    @Query("SELECT * FROM user_memory ORDER BY lastAccessedAt DESC")
    suspend fun getAll(): List<UserMemory>

    /**
     * 查询所有记忆（Flow）
     */
    @Query("SELECT * FROM user_memory ORDER BY lastAccessedAt DESC")
    fun getAllFlow(): Flow<List<UserMemory>>

    /**
     * 搜索记忆（模糊匹配 key 或 value）
     */
    @Query("""
        SELECT * FROM user_memory
        WHERE key LIKE '%' || :query || '%'
           OR value LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY lastAccessedAt DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<UserMemory>

    /**
     * 获取最近访问的记忆
     */
    @Query("SELECT * FROM user_memory ORDER BY lastAccessedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<UserMemory>

    /**
     * 获取最常访问的记忆
     */
    @Query("SELECT * FROM user_memory ORDER BY accessCount DESC LIMIT :limit")
    suspend fun getMostAccessed(limit: Int = 20): List<UserMemory>

    /**
     * 获取高置信度记忆
     */
    @Query("SELECT * FROM user_memory WHERE confidence >= :minConfidence ORDER BY confidence DESC, accessCount DESC")
    suspend fun getHighConfidence(minConfidence: Float = 0.8f): List<UserMemory>

    /**
     * 更新访问统计
     */
    @Query("""
        UPDATE user_memory
        SET accessCount = accessCount + 1,
            lastAccessedAt = :timestamp
        WHERE id = :id
    """)
    suspend fun updateAccessStats(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * 更新置信度
     */
    @Query("UPDATE user_memory SET confidence = :confidence WHERE id = :id")
    suspend fun updateConfidence(id: Long, confidence: Float)

    /**
     * 删除过期记忆
     */
    @Query("DELETE FROM user_memory WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())

    /**
     * 统计记忆数量
     */
    @Query("SELECT COUNT(*) FROM user_memory")
    suspend fun getCount(): Int

    /**
     * 按类型统计
     */
    @Query("SELECT COUNT(*) FROM user_memory WHERE type = :type")
    suspend fun getCountByType(type: MemoryType): Int

    /**
     * 获取平均置信度
     */
    @Query("SELECT AVG(confidence) FROM user_memory")
    suspend fun getAverageConfidence(): Float?

    /**
     * 获取常用股票（按访问次数）
     */
    @Query("""
        SELECT * FROM user_memory
        WHERE type = 'FREQUENT_STOCK'
        ORDER BY accessCount DESC, lastAccessedAt DESC
        LIMIT :limit
    """)
    suspend fun getFrequentStocks(limit: Int = 10): List<UserMemory>

    /**
     * 获取用户偏好
     */
    @Query("""
        SELECT * FROM user_memory
        WHERE type = 'PREFERENCE'
        ORDER BY confidence DESC
    """)
    suspend fun getPreferences(): List<UserMemory>

    /**
     * 获取历史决策
     */
    @Query("""
        SELECT * FROM user_memory
        WHERE type = 'HISTORICAL_DECISION'
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getHistoricalDecisions(limit: Int = 50): List<UserMemory>
}
