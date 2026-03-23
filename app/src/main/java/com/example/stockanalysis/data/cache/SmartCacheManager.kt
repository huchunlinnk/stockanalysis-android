package com.example.stockanalysis.data.cache

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 智能缓存管理器
 * 
 * 功能：
 * 1. 不同类型数据的不同缓存策略
 * 2. 自动过期检查和清理
 * 3. 缓存大小限制
 * 4. 离线数据支持
 */
@Singleton
class SmartCacheManager @Inject constructor(
    private val cacheDao: CacheDao,
    private val gson: Gson
) {
    companion object {
        const val TAG = "SmartCacheManager"
        
        // 默认缓存策略配置
        private val DEFAULT_POLICIES = mapOf(
            CacheType.REALTIME_QUOTE to CachePolicy(ttlMinutes = 1, maxSize = 1000),
            CacheType.KLINE_DATA to CachePolicy(ttlMinutes = 5, maxSize = 500),
            CacheType.TECHNICAL_INDICATORS to CachePolicy(ttlMinutes = 10, maxSize = 500),
            CacheType.MARKET_OVERVIEW to CachePolicy(ttlMinutes = 2, maxSize = 50),
            CacheType.FUNDAMENTAL_DATA to CachePolicy(ttlMinutes = 60, maxSize = 200),
            CacheType.SEARCH_RESULT to CachePolicy(ttlMinutes = 30, maxSize = 100)
        )
    }
    
    private val mutex = Mutex()
    private var customPolicies: MutableMap<CacheType, CachePolicy> = mutableMapOf()
    
    /**
     * 获取当前缓存策略（支持自定义覆盖）
     */
    fun getPolicy(type: CacheType): CachePolicy {
        return customPolicies[type] ?: DEFAULT_POLICIES[type] ?: CachePolicy.DEFAULT
    }
    
    /**
     * 设置自定义缓存策略
     */
    fun setPolicy(type: CacheType, policy: CachePolicy) {
        customPolicies[type] = policy
        Log.d(TAG, "Set custom policy for $type: ttl=${policy.ttlMinutes}min, maxSize=${policy.maxSize}")
    }
    
    /**
     * 重置为默认策略
     */
    fun resetPolicy(type: CacheType) {
        customPolicies.remove(type)
    }
    
    /**
     * 获取缓存或从数据源获取
     * 
     * @param key 缓存键
     * @param type 缓存类型
     * @param forceRefresh 是否强制刷新（忽略缓存）
     * @param fetch 数据源获取函数
     * @return 结果
     */
    suspend fun <T> getCachedOrFetch(
        key: String,
        type: CacheType,
        forceRefresh: Boolean = false,
        fetch: suspend () -> Result<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        
        // 如果不是强制刷新，先尝试获取缓存
        if (!forceRefresh) {
            val cached = getFromCache<T>(key, type)
            if (cached != null) {
                Log.d(TAG, "Cache hit: key=$key, type=$type")
                return@withContext Result.success(cached)
            }
        }
        
        // 从数据源获取
        Log.d(TAG, "Cache miss or force refresh: key=$key, type=$type")
        val result = fetch()
        
        // 成功则缓存结果
        result.onSuccess { data ->
            putToCache(key, type, data)
        }
        
        result
    }
    
    /**
     * 仅从缓存获取（不访问网络）
     * 
     * @param key 缓存键
     * @param type 缓存类型
     * @param allowExpired 是否允许返回过期缓存
     * @return 缓存数据，如果不存在或已过期则返回 null
     */
    suspend fun <T> getFromCache(
        key: String,
        type: CacheType,
        allowExpired: Boolean = false
    ): T? = withContext(Dispatchers.IO) {
        try {
            val entity = cacheDao.getByKeyAndType(key, type)
            
            if (entity != null) {
                // 检查是否过期
                if (!entity.isExpired() || allowExpired) {
                    // 解析数据
                    val data = parseFromJson<T>(entity.data, type)
                    
                    if (entity.isExpired()) {
                        Log.w(TAG, "Returning expired cache: key=$key, type=$type, expired ${entity.getAge()}ms ago")
                    }
                    
                    return@withContext data
                } else {
                    Log.d(TAG, "Cache expired: key=$key, type=$type")
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache: key=$key, type=$type", e)
            null
        }
    }
    
    /**
     * 存入缓存
     * 
     * @param key 缓存键
     * @param type 缓存类型
     * @param data 要缓存的数据
     */
    suspend fun <T> putToCache(
        key: String,
        type: CacheType,
        data: T
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val policy = getPolicy(type)
                val currentTime = System.currentTimeMillis()
                val expireTime = currentTime + (policy.ttlMinutes * 60 * 1000)
                
                val entity = CacheEntity(
                    cacheKey = key,
                    type = type,
                    data = gson.toJson(data),
                    timestamp = currentTime,
                    expireTime = expireTime
                )
                
                cacheDao.insert(entity)
                Log.d(TAG, "Cache put: key=$key, type=$type, ttl=${policy.ttlMinutes}min")
                
                // 检查并清理该类型缓存数量
                enforceSizeLimit(type, policy)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error putting cache: key=$key, type=$type", e)
            }
        }
    }
    
    /**
     * 批量存入缓存
     */
    suspend fun <T> putAllToCache(
        type: CacheType,
        dataMap: Map<String, T>
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val policy = getPolicy(type)
                val currentTime = System.currentTimeMillis()
                val expireTime = currentTime + (policy.ttlMinutes * 60 * 1000)
                
                val entities = dataMap.map { (key, data) ->
                    CacheEntity(
                        cacheKey = key,
                        type = type,
                        data = gson.toJson(data),
                        timestamp = currentTime,
                        expireTime = expireTime
                    )
                }
                
                cacheDao.insertAll(entities)
                Log.d(TAG, "Batch cache put: count=${entities.size}, type=$type")
                
                // 检查并清理该类型缓存数量
                enforceSizeLimit(type, policy)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error batch putting cache: type=$type", e)
            }
        }
    }
    
    /**
     * 清理过期缓存
     * 
     * @return 清理的缓存数量
     */
    suspend fun cleanExpiredCache(): Int = withContext(Dispatchers.IO) {
        try {
            val count = cacheDao.deleteExpired()
            Log.d(TAG, "Cleaned $count expired cache entries")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning expired cache", e)
            0
        }
    }
    
    /**
     * 清理特定类型的过期缓存
     */
    suspend fun cleanExpiredByType(type: CacheType): Int = withContext(Dispatchers.IO) {
        try {
            val expired = cacheDao.getExpired()
            val toDelete = expired.filter { it.type == type }
            toDelete.forEach { cacheDao.delete(it) }
            Log.d(TAG, "Cleaned ${toDelete.size} expired entries of type $type")
            toDelete.size
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning expired cache for type $type", e)
            0
        }
    }
    
    /**
     * 清空所有缓存
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            cacheDao.deleteAll()
            Log.d(TAG, "All cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all cache", e)
        }
    }
    
    /**
     * 清空特定类型的缓存
     */
    suspend fun clearCacheByType(type: CacheType) = withContext(Dispatchers.IO) {
        try {
            cacheDao.deleteByType(type)
            Log.d(TAG, "Cache cleared for type $type")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache for type $type", e)
        }
    }
    
    /**
     * 删除指定缓存
     */
    suspend fun removeFromCache(key: String, type: CacheType) = withContext(Dispatchers.IO) {
        try {
            cacheDao.deleteByKeyAndType(key, type)
            Log.d(TAG, "Cache removed: key=$key, type=$type")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing cache: key=$key, type=$type", e)
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        try {
            val totalCount = cacheDao.getTotalCount()
            val expiredCount = cacheDao.getExpiredCount()
            
            val byTypeCount = CacheType.values().associateWith { type ->
                cacheDao.getCountByType(type)
            }
            
            // 估算缓存大小（简化计算）
            val totalSizeBytes = 0L // 实际实现可以通过数据库文件大小获取
            
            CacheStats(
                totalCount = totalCount,
                expiredCount = expiredCount,
                byTypeCount = byTypeCount,
                totalSizeBytes = totalSizeBytes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache stats", e)
            CacheStats(0, 0, emptyMap(), 0)
        }
    }
    
    /**
     * 预加载缓存（用于离线模式准备）
     * 
     * @param keys 需要预加载的键列表
     * @param type 缓存类型
     * @param fetch 数据获取函数
     */
    suspend fun <T> preloadCache(
        keys: List<String>,
        type: CacheType,
        fetch: suspend (List<String>) -> Result<Map<String, T>>
    ): Result<Map<String, T>> = withContext(Dispatchers.IO) {
        try {
            // 过滤出需要刷新的键（未缓存或已过期）
            val keysToFetch = keys.filter { key ->
                val cached = cacheDao.getByKeyAndType(key, type)
                cached == null || cached.isExpired()
            }
            
            if (keysToFetch.isEmpty()) {
                Log.d(TAG, "All data already cached, no need to fetch")
                // 从缓存获取所有数据
                val cachedData = keys.mapNotNull { key ->
                    val entity = cacheDao.getByKeyAndType(key, type)
                    entity?.let {
                        parseFromJson<T>(it.data, type)?.let { data ->
                            key to data
                        }
                    }
                }.toMap()
                return@withContext Result.success(cachedData)
            }
            
            // 获取数据
            val result = fetch(keysToFetch)
            
            result.onSuccess { dataMap ->
                // 批量缓存
                putAllToCache(type, dataMap)
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading cache", e)
            Result.failure(e)
        }
    }
    
    /**
     * 强制执行缓存大小限制
     */
    private suspend fun enforceSizeLimit(type: CacheType, policy: CachePolicy) {
        try {
            val count = cacheDao.getCountByType(type)
            if (count > policy.maxSize) {
                val toDelete = count - policy.maxSize
                cacheDao.deleteOldestByType(type, policy.maxSize)
                Log.d(TAG, "Enforced size limit for $type: deleted $toDelete oldest entries")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enforcing size limit for $type", e)
        }
    }
    
    /**
     * 从JSON解析数据
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> parseFromJson(json: String, type: CacheType): T? {
        return try {
            when (type) {
                CacheType.REALTIME_QUOTE -> {
                    gson.fromJson(json, com.example.stockanalysis.data.model.RealtimeQuote::class.java) as T
                }
                CacheType.MARKET_OVERVIEW -> {
                    gson.fromJson(json, com.example.stockanalysis.data.model.MarketOverview::class.java) as T
                }
                CacheType.KLINE_DATA -> {
                    val listType = object : TypeToken<List<com.example.stockanalysis.data.model.KLineData>>() {}.type
                    gson.fromJson<List<com.example.stockanalysis.data.model.KLineData>>(json, listType) as T
                }
                CacheType.TECHNICAL_INDICATORS -> {
                    gson.fromJson(json, com.example.stockanalysis.data.model.TechnicalIndicators::class.java) as T
                }
                CacheType.FUNDAMENTAL_DATA -> {
                    gson.fromJson(json, com.example.stockanalysis.data.model.FundamentalData::class.java) as T
                }
                CacheType.SEARCH_RESULT -> {
                    val listType = object : TypeToken<List<Pair<String, String>>>() {}.type
                    gson.fromJson<List<Pair<String, String>>>(json, listType) as T
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cache data for type $type", e)
            null
        }
    }
}
