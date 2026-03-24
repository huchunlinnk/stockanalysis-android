package com.example.stockanalysis.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.stockanalysis.data.local.StockDao
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.StrategyConfig
import com.example.stockanalysis.data.model.StrategyInstance
import com.example.stockanalysis.data.model.PresetStrategies
import com.example.stockanalysis.data.repository.AnalysisEngine
import com.example.stockanalysis.service.MultiChannelNotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 每日分析Worker
 * 执行定时股票分析任务
 */
@HiltWorker
class DailyAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stockDao: StockDao,
    private val analysisEngine: AnalysisEngine
) : CoroutineWorker(context, params) {
    
    private val notificationService: com.example.stockanalysis.service.NotificationService by lazy {
        com.example.stockanalysis.service.MultiChannelNotificationService(
            context,
            okhttp3.OkHttpClient()
        )
    }
    
    companion object {
        const val TAG = "DailyAnalysisWorker"
        const val WORK_NAME = "daily_stock_analysis"
        
        // 输入参数键
        const val KEY_STRATEGY_ID = "strategy_id"
        const val KEY_SYMBOLS = "symbols"
        const val KEY_NOTIFY_RESULTS = "notify_results"
        const val KEY_MIN_SCORE = "min_score"
        
        // 输出参数键
        const val KEY_RESULT_COUNT = "result_count"
        const val KEY_SUCCESS_COUNT = "success_count"
        const val KEY_ERROR_MESSAGE = "error_message"
        
        /**
         * 安排每日分析任务
         * @param context 上下文
         * @param hour 执行小时（0-23）
         * @param minute 执行分钟（0-59）
         */
        fun scheduleDailyAnalysis(
            context: Context,
            hour: Int = 15,  // 默认下午3点（收盘后）
            minute: Int = 30
        ) {
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }
            
            // 如果今天的时间已过，设置为明天
            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
            val initialDelay = timeDiff / 1000 / 60 // 转换为分钟
            
            // 创建周期性任务（每24小时执行一次）
            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyAnalysisWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest
            )
            
            Log.i(TAG, "Scheduled daily analysis at $hour:$minute, initial delay: $initialDelay minutes")
        }
        
        /**
         * 取消每日分析任务
         */
        fun cancelDailyAnalysis(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled daily analysis")
        }
        
        /**
         * 检查是否已安排每日分析
         */
        fun isScheduled(context: Context): Boolean {
            val workManager = WorkManager.getInstance(context)
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            return workInfos?.any { 
                !it.state.isFinished 
            } ?: false
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting daily analysis work")
        
        try {
            // 获取输入参数
            val strategyId = inputData.getString(KEY_STRATEGY_ID) ?: PresetStrategies.standardAnalysis().id
            val symbolsInput = inputData.getString(KEY_SYMBOLS)
            val notifyResults = inputData.getBoolean(KEY_NOTIFY_RESULTS, true)
            val minScore = inputData.getInt(KEY_MIN_SCORE, 0)
            
            // 获取要分析的股票列表
            val symbols = if (symbolsInput != null) {
                symbolsInput.split(",").map { it.trim() }
            } else {
                // 从数据库获取自选股
                stockDao.getAllStocksSync().map { it.symbol }
            }
            
            if (symbols.isEmpty()) {
                Log.w(TAG, "No stocks to analyze")
                return@withContext Result.success(
                    workDataOf(KEY_RESULT_COUNT to 0, KEY_SUCCESS_COUNT to 0)
                )
            }
            
            Log.i(TAG, "Analyzing ${symbols.size} stocks with strategy: $strategyId")
            
            // 获取策略配置
            val strategyConfig = getStrategyConfig(strategyId)
            if (strategyConfig == null) {
                Log.e(TAG, "Strategy not found: $strategyId")
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Strategy not found: $strategyId")
                )
            }
            
            // 执行批量分析
            val results = performBatchAnalysis(symbols, strategyConfig, minScore)
            
            // 发送通知
            if (notifyResults && results.isNotEmpty()) {
                sendBatchNotification(results)
            }
            
            // 输出结果
            val successCount = results.count { it.decision != com.example.stockanalysis.data.model.Decision.HOLD }
            val outputData = workDataOf(
                KEY_RESULT_COUNT to results.size,
                KEY_SUCCESS_COUNT to successCount
            )
            
            Log.i(TAG, "Daily analysis completed: ${results.size} results, $successCount actionable")
            
            Result.success(outputData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Daily analysis failed", e)
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Unknown error"))
            )
        }
    }
    
    /**
     * 执行批量分析
     */
    private suspend fun performBatchAnalysis(
        symbols: List<String>,
        strategyConfig: StrategyConfig,
        minScore: Int
    ): List<AnalysisResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<AnalysisResult>()
        
        // 限制并发数量
        val batchSize = 3
        symbols.chunked(batchSize).forEach { batch ->
            val batchResults = batch.map { symbol ->
                async {
                    try {
                        val result = analysisEngine.analyze(
                            symbol = symbol,
                            strategyConfig = strategyConfig,
                            params = strategyConfig.getDefaultParams()
                        )
                        
                        // 过滤低分结果
                        if (result.score >= minScore) {
                            result
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to analyze $symbol", e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()
            
            results.addAll(batchResults)
            
            // 添加小延迟避免请求过快
            kotlinx.coroutines.delay(500)
        }
        
        results
    }
    
    /**
     * 获取策略配置
     */
    private fun getStrategyConfig(strategyId: String): StrategyConfig? {
        return PresetStrategies.getAll().find { it.id == strategyId }
    }
    
    /**
     * 发送批量分析通知
     */
    private suspend fun sendBatchNotification(results: List<AnalysisResult>) {
        val buySignals = results.filter { 
            it.decision == com.example.stockanalysis.data.model.Decision.BUY || 
            it.decision == com.example.stockanalysis.data.model.Decision.STRONG_BUY 
        }
        val sellSignals = results.filter { 
            it.decision == com.example.stockanalysis.data.model.Decision.SELL || 
            it.decision == com.example.stockanalysis.data.model.Decision.STRONG_SELL 
        }
        
        val title = "📊 每日分析完成 (${results.size}只股票)"
        val content = buildString {
            appendLine("✅ 买入信号: ${buySignals.size}只")
            appendLine("❌ 卖出信号: ${sellSignals.size}只")
            appendLine("➖ 持有观望: ${results.size - buySignals.size - sellSignals.size}只")
            appendLine()
            
            if (buySignals.isNotEmpty()) {
                appendLine("🚀 买入推荐:")
                buySignals.sortedByDescending { it.score }.take(5).forEach {
                    appendLine("  • ${it.stockName}(${it.stockSymbol}): ${it.score}分")
                }
            }
            
            if (sellSignals.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ 卖出提醒:")
                sellSignals.sortedBy { it.score }.take(3).forEach {
                    appendLine("  • ${it.stockName}(${it.stockSymbol}): ${it.score}分")
                }
            }
        }
        
        notificationService.sendTextNotification(title, content)
        
        // 单独发送强信号通知
        val strongSignals = results.filter { 
            it.decision == com.example.stockanalysis.data.model.Decision.STRONG_BUY || 
            it.decision == com.example.stockanalysis.data.model.Decision.STRONG_SELL 
        }
        
        strongSignals.forEach { result ->
            notificationService.sendAnalysisNotification(result)
        }
    }
    
    /**
     * 辅助函数：创建WorkData
     */
    private fun workDataOf(vararg pairs: Pair<String, Any>): Data {
        return Data.Builder().apply {
            pairs.forEach { (key, value) ->
                when (value) {
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }.build()
    }
}

/**
 * 实时价格监控Worker
 * 监控股票价格并在达到条件时发送提醒
 */
@HiltWorker
class PriceAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stockDao: StockDao
) : CoroutineWorker(context, params) {
    
    private val notificationService: com.example.stockanalysis.service.NotificationService by lazy {
        com.example.stockanalysis.service.MultiChannelNotificationService(
            context,
            okhttp3.OkHttpClient()
        )
    }
    
    companion object {
        const val TAG = "PriceAlertWorker"
        const val WORK_NAME = "price_alert_monitor"
        
        const val KEY_SYMBOL = "symbol"
        const val KEY_TARGET_PRICE = "target_price"
        const val KEY_ALERT_TYPE = "alert_type" // above, below
        
        /**
         * 安排价格提醒
         */
        fun schedulePriceAlert(
            context: Context,
            symbol: String,
            targetPrice: Double,
            alertType: String // "above" 或 "below"
        ) {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<PriceAlertWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_SYMBOL, symbol)
                        .putDouble(KEY_TARGET_PRICE, targetPrice)
                        .putString(KEY_ALERT_TYPE, alertType)
                        .build()
                )
                .addTag("price_alert_$symbol")
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
    
    override suspend fun doWork(): Result {
        // TODO: 实现价格监控逻辑
        return Result.success()
    }
}

/**
 * 数据同步Worker
 * 定期同步股票数据和持仓信息
 */
@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stockDao: StockDao
) : CoroutineWorker(context, params) {
    
    companion object {
        const val TAG = "DataSyncWorker"
        const val WORK_NAME = "data_sync"
        
        /**
         * 安排定期数据同步
         */
        fun schedulePeriodicSync(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(15, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
    
    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting data sync")
        
        return try {
            // TODO: 实现数据同步逻辑
            // 1. 同步自选股最新价格
            // 2. 同步分析结果
            // 3. 清理过期缓存
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Data sync failed", e)
            Result.retry()
        }
    }
}
