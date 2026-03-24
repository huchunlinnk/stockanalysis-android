package com.example.stockanalysis.data.cache

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 缓存类型枚举
 */
enum class CacheType {
    REALTIME_QUOTE,       // 实时行情
    KLINE_DATA,          // K线数据
    TECHNICAL_INDICATORS, // 技术指标
    MARKET_OVERVIEW,     // 市场概览
    FUNDAMENTAL_DATA,    // 基本面数据
    SEARCH_RESULT        // 搜索结果
}

/**
 * 缓存策略配置
 * @param ttlMinutes 缓存有效时间（分钟）
 * @param maxSize 最大缓存数量
 */
data class CachePolicy(
    val ttlMinutes: Int,
    val maxSize: Int
) {
    companion object {
        val DEFAULT = CachePolicy(ttlMinutes = 5, maxSize = 100)
    }
}

/**
 * 缓存实体类
 * 存储各种类型的缓存数据
 */
@Entity(
    tableName = "cache_entities",
    indices = [
        Index(value = ["cacheKey", "type"], unique = true),
        Index(value = ["type"]),
        Index(value = ["timestamp"])
    ]
)
data class CacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val cacheKey: String,      // 缓存键（如股票代码）
    val type: CacheType,       // 缓存类型
    val data: String,          // 缓存数据（JSON格式）
    val timestamp: Long = System.currentTimeMillis(),  // 缓存时间戳
    val expireTime: Long       // 过期时间戳
) {
    /**
     * 检查缓存是否已过期
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expireTime
    }
    
    /**
     * 获取缓存剩余有效时间（毫秒）
     */
    fun getRemainingTime(): Long {
        return expireTime - System.currentTimeMillis()
    }
    
    /**
     * 获取缓存年龄（毫秒）
     */
    fun getAge(): Long {
        return System.currentTimeMillis() - timestamp
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val totalCount: Int,                    // 总缓存数量
    val expiredCount: Int,                  // 已过期数量
    val byTypeCount: Map<CacheType, Int>,  // 各类型缓存数量
    val totalSizeBytes: Long                // 总大小（字节）
)

/**
 * 缓存条目包装类（用于返回缓存数据和元数据）
 */
data class CacheEntry<T>(
    val data: T,               // 实际数据
    val cachedAt: Long,        // 缓存时间
    val expireAt: Long,        // 过期时间
    val isExpired: Boolean     // 是否已过期
)
