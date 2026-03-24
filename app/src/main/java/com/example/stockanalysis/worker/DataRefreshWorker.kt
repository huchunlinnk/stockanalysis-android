package com.example.stockanalysis.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.stockanalysis.data.cache.CacheType
import com.example.stockanalysis.data.cache.SmartCacheManager
import com.example.stockanalysis.data.datasource.DataSourceManager
import com.example.stockanalysis.data.local.StockDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 数据刷新 Worker
 * 
 * 功能：
 * 1. 定时刷新自选股行情
 * 2. 刷新市场指数
 * 3. 清理过期缓存
 * 
 * 触发条件：
 * - 应用启动时初始化
 * - 每15分钟最小间隔（WorkManager限制）
 * - 设备充电且网络可用时
 */
@HiltWorker
class DataRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stockDao: StockDao,
    private val dataSourceManager: DataSourceManager,
    private val cacheManager: SmartCacheManager
) : CoroutineWorker(context, params) {
    
    companion object {
        const val TAG = "DataRefreshWorker"
        const val WORK_NAME = "data_refresh_work"
        const val WORK_NAME_ONE_TIME = "data_refresh_work_one_time"
        
        // WorkManager 最小间隔15分钟（系统限制）
        const val MIN_REPEAT_INTERVAL_MINUTES = 15L
        const val FLEX_TIME_MINUTES = 5L
        
        /**
         * 初始化定时刷新任务
         * 在 Application.onCreate 中调用
         */
        fun initialize(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            // 检查是否已存在
            workManager.getWorkInfosForUniqueWorkLiveData(WORK_NAME).observeForever { workInfos ->
                if (workInfos.isNullOrEmpty() || workInfos.all { it.state.isFinished }) {
                    // 创建约束条件
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                    
                    // 创建周期性任务（最小15分钟）
                    val periodicWorkRequest = PeriodicWorkRequestBuilder<DataRefreshWorker>(
                        MIN_REPEAT_INTERVAL_MINUTES, TimeUnit.MINUTES,
                        FLEX_TIME_MINUTES, TimeUnit.MINUTES
                    )
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            WorkRequest.MIN_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS
                        )
                        .addTag("data_refresh")
                        .build()
                    
                    // 加入队列（保持现有任务）
                    workManager.enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicWorkRequest
                    )
                    
                    Log.d(TAG, "Scheduled periodic data refresh work every $MIN_REPEAT_INTERVAL_MINUTES minutes")
                }
            }
        }
        
        /**
         * 立即执行一次数据刷新
         */
        fun executeOnce(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DataRefreshWorker>()
                .setConstraints(constraints)
                .addTag("data_refresh_one_time")
                .build()
            
            workManager.enqueueUniqueWork(
                WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest
            )
            
            Log.d(TAG, "Enqueued one-time data refresh work")
        }
        
        /**
         * 取消定时刷新任务
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled periodic data refresh work")
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting data refresh work")
        
        return try {
            var success = true
            val results = mutableListOf<String>()
            
            // 1. 刷新自选股行情
            val quoteResult = refreshWatchlistQuotes()
            results.add("Quotes: ${if (quoteResult) "OK" else "FAIL"}")
            success = success && quoteResult
            
            // 2. 刷新市场指数
            val marketResult = refreshMarketOverview()
            results.add("Market: ${if (marketResult) "OK" else "FAIL"}")
            success = success && marketResult
            
            // 3. 清理过期缓存
            val cleanResult = cleanExpiredCache()
            results.add("Clean: ${cleanResult} expired entries removed")
            
            Log.d(TAG, "Data refresh completed: ${results.joinToString(", ")}")
            
            if (success) {
                Result.success()
            } else {
                // 部分失败，但可以重试
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Data refresh failed", e)
            Result.retry()
        }
    }
    
    /**
     * 刷新自选股行情
     */
    private suspend fun refreshWatchlistQuotes(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取所有自选股代码
            val stocks = stockDao.getAllStocksSync()
            
            if (stocks.isEmpty()) {
                Log.d(TAG, "No watchlist stocks to refresh")
                return@withContext true
            }
            
            val symbols = stocks.map { it.symbol }
            Log.d(TAG, "Refreshing quotes for ${symbols.size} stocks: ${symbols.take(5)}...")
            
            // 获取行情数据
            val result = dataSourceManager.fetchQuotes(symbols)
            
            result.fold(
                onSuccess = { quotes ->
                    // 转换为Map并缓存
                    val quoteMap = quotes.associateBy { it.symbol }
                    cacheManager.putAllToCache(CacheType.REALTIME_QUOTE, quoteMap)
                    
                    Log.d(TAG, "Successfully refreshed ${quotes.size} quotes")
                    true
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to refresh quotes: ${error.message}")
                    false
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing watchlist quotes", e)
            false
        }
    }
    
    /**
     * 刷新市场概览
     */
    private suspend fun refreshMarketOverview(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Refreshing market overview")
            
            val result = dataSourceManager.fetchMarketOverview()
            
            result.fold(
                onSuccess = { overview ->
                    // 缓存市场概览
                    cacheManager.putToCache("market_overview", CacheType.MARKET_OVERVIEW, overview)
                    
                    Log.d(TAG, "Successfully refreshed market overview")
                    true
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to refresh market overview: ${error.message}")
                    false
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing market overview", e)
            false
        }
    }
    
    /**
     * 清理过期缓存
     */
    private suspend fun cleanExpiredCache(): Int = withContext(Dispatchers.IO) {
        try {
            val count = cacheManager.cleanExpiredCache()
            Log.d(TAG, "Cleaned $count expired cache entries")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning expired cache", e)
            0
        }
    }
}
