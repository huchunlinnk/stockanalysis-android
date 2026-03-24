package com.example.stockanalysis.data.cache

import androidx.room.*

/**
 * 缓存 DAO
 * 提供缓存数据的增删改查操作
 */
@Dao
interface CacheDao {
    
    /**
     * 根据键和类型获取缓存
     */
    @Query("SELECT * FROM cache_entities WHERE cacheKey = :key AND type = :type LIMIT 1")
    suspend fun getByKeyAndType(key: String, type: CacheType): CacheEntity?
    
    /**
     * 根据类型获取所有缓存
     */
    @Query("SELECT * FROM cache_entities WHERE type = :type ORDER BY timestamp DESC")
    suspend fun getAllByType(type: CacheType): List<CacheEntity>
    
    /**
     * 获取特定类型的缓存数量
     */
    @Query("SELECT COUNT(*) FROM cache_entities WHERE type = :type")
    suspend fun getCountByType(type: CacheType): Int
    
    /**
     * 获取所有缓存数量
     */
    @Query("SELECT COUNT(*) FROM cache_entities")
    suspend fun getTotalCount(): Int
    
    /**
     * 获取已过期的缓存
     */
    @Query("SELECT * FROM cache_entities WHERE expireTime < :currentTime")
    suspend fun getExpired(currentTime: Long = System.currentTimeMillis()): List<CacheEntity>
    
    /**
     * 获取已过期的缓存数量
     */
    @Query("SELECT COUNT(*) FROM cache_entities WHERE expireTime < :currentTime")
    suspend fun getExpiredCount(currentTime: Long = System.currentTimeMillis()): Int
    
    /**
     * 插入缓存（如果存在则替换）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: CacheEntity)
    
    /**
     * 批量插入缓存
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<CacheEntity>)
    
    /**
     * 更新缓存
     */
    @Update
    suspend fun update(cache: CacheEntity)
    
    /**
     * 删除指定缓存
     */
    @Delete
    suspend fun delete(cache: CacheEntity)
    
    /**
     * 根据键和类型删除缓存
     */
    @Query("DELETE FROM cache_entities WHERE cacheKey = :key AND type = :type")
    suspend fun deleteByKeyAndType(key: String, type: CacheType)
    
    /**
     * 根据类型删除所有缓存
     */
    @Query("DELETE FROM cache_entities WHERE type = :type")
    suspend fun deleteByType(type: CacheType)
    
    /**
     * 删除已过期的缓存
     */
    @Query("DELETE FROM cache_entities WHERE expireTime < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis()): Int
    
    /**
     * 删除最旧的缓存（按类型限制数量）
     * 保留最新的 N 条
     */
    @Query("""
        DELETE FROM cache_entities 
        WHERE type = :type 
        AND id NOT IN (
            SELECT id FROM cache_entities 
            WHERE type = :type 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun deleteOldestByType(type: CacheType, keepCount: Int): Int
    
    /**
     * 清空所有缓存
     */
    @Query("DELETE FROM cache_entities")
    suspend fun deleteAll()
    
    /**
     * 获取特定类型最旧的缓存
     */
    @Query("SELECT * FROM cache_entities WHERE type = :type ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldestByType(type: CacheType): CacheEntity?
    
    /**
     * 获取特定类型最新的缓存
     */
    @Query("SELECT * FROM cache_entities WHERE type = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun getNewestByType(type: CacheType): CacheEntity?
}
