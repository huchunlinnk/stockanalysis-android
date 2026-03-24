package com.example.stockanalysis.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stockanalysis.data.analysis.ComparisonResult
import com.example.stockanalysis.data.analysis.HistoryComparisonService
import com.example.stockanalysis.data.analysis.Outcome
import com.example.stockanalysis.data.analysis.SignalAccuracy
import com.example.stockanalysis.data.local.AnalysisResultDao
import com.example.stockanalysis.data.local.StockDao
import com.example.stockanalysis.data.model.Decision
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * 信号追踪 Worker
 * 定期后台任务，验证历史信号的准确性
 */
@HiltWorker
class SignalTrackingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val historyComparisonService: HistoryComparisonService,
    private val analysisResultDao: AnalysisResultDao,
    private val stockDao: StockDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "SignalTrackingWorker"
        const val WORK_NAME = "signal_tracking_work"
        
        // WorkManager 输入参数键
        const val KEY_SYMBOL = "symbol"
        const val KEY_MODE = "mode"
        const val KEY_DAYS = "days"
        
        // 工作模式
        const val MODE_SINGLE_STOCK = "single"
        const val MODE_ALL_STOCKS = "all"
        const val MODE_BATCH_UPDATE = "batch"
        
        // 默认配置
        const val DEFAULT_EVAL_DAYS = 30
        const val HIGH_ACCURACY_THRESHOLD = 70.0
        const val LOW_ACCURACY_THRESHOLD = 40.0
        
        /**
         * 计算下次运行延迟（每天上午9:30运行）
         */
        fun calculateInitialDelay(): Long {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 9)
                set(java.util.Calendar.MINUTE, 30)
                set(java.util.Calendar.SECOND, 0)
            }
            
            if (target.before(now)) {
                target.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            
            return target.timeInMillis - now.timeInMillis
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting signal tracking worker")
            
            val mode = inputData.getString(KEY_MODE) ?: MODE_ALL_STOCKS
            val symbol = inputData.getString(KEY_SYMBOL)
            val days = inputData.getInt(KEY_DAYS, DEFAULT_EVAL_DAYS)
            
            when (mode) {
                MODE_SINGLE_STOCK -> {
                    if (symbol != null) {
                        trackSingleStock(symbol, days)
                    } else {
                        Log.e(TAG, "Single stock mode requires symbol")
                    }
                }
                MODE_ALL_STOCKS -> trackAllStocks(days)
                MODE_BATCH_UPDATE -> batchUpdatePendingSignals()
            }
            
            Log.d(TAG, "Signal tracking completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Signal tracking failed", e)
            Result.retry()
        }
    }

    /**
     * 追踪单只股票
     */
    private suspend fun trackSingleStock(symbol: String, days: Int) {
        Log.d(TAG, "Tracking signals for $symbol over $days days")
        
        val accuracy = historyComparisonService.validateSignals(
            symbol = symbol,
            startDate = null,
            endDate = null
        )
        
        // 保存统计结果
        saveAccuracyStats(accuracy)
        
        // 检查是否需要发送通知
        checkAndNotifyAccuracyChange(symbol, accuracy)
        
        Log.d(TAG, "Tracking completed for $symbol: accuracy=${accuracy.winRate}%")
    }

    /**
     * 追踪所有股票
     */
    private suspend fun trackAllStocks(days: Int) {
        Log.d(TAG, "Tracking signals for all stocks")
        
        val stocks = stockDao.getAllStocksSync()
        val results = mutableListOf<Pair<String, SignalAccuracy>>()
        
        stocks.forEach { stock ->
            try {
                val accuracy = historyComparisonService.validateSignals(
                    symbol = stock.symbol,
                    startDate = null,
                    endDate = null
                )
                results.add(stock.symbol to accuracy)
                
                // 保存统计
                saveAccuracyStats(accuracy)
                
                Log.d(TAG, "Tracked ${stock.symbol}: ${accuracy.winRate}% accuracy")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track ${stock.symbol}", e)
            }
        }
        
        // 生成整体统计
        generateOverallStats(results)
        
        // 找出表现最好和最差的
        val sorted = results.sortedByDescending { it.second.winRate }
        val best = sorted.firstOrNull { it.second.totalSignals >= 5 }
        val worst = sorted.lastOrNull { it.second.totalSignals >= 5 }
        
        // 发送汇总通知
        if (results.isNotEmpty()) {
            sendSummaryNotification(results.size, best, worst)
        }
    }

    /**
     * 批量更新待验证的信号
     */
    private suspend fun batchUpdatePendingSignals() {
        Log.d(TAG, "Batch updating pending signals")
        
        // 获取所有需要验证的信号（分析时间距今5-30天的）
        val calendar = java.util.Calendar.getInstance()
        val now = calendar.time
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -5)
        val fiveDaysAgo = calendar.time
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -25)
        val thirtyDaysAgo = calendar.time
        
        val pendingResults = analysisResultDao.getResultsByTimeRange(thirtyDaysAgo, fiveDaysAgo)
        
        Log.d(TAG, "Found ${pendingResults.size} pending signals to validate")
        
        var validatedCount = 0
        var successCount = 0
        
        pendingResults.groupBy { it.stockSymbol }.forEach { (symbol, results) ->
            try {
                results.forEach { result ->
                    val record = historyComparisonService.recordSignal(result)
                    val comparison = historyComparisonService.validateSignal(record)
                    
                    if (comparison != null) {
                        validatedCount++
                        saveComparisonResult(comparison)
                        
                        if (comparison.isCorrect) {
                            successCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to validate signals for $symbol", e)
            }
        }
        
        Log.d(TAG, "Batch update completed: $validatedCount validated, $successCount correct")
        
        // 发送通知
        if (validatedCount > 0) {
            notificationHelper.showMarketUpdateNotification(
                "信号验证完成",
                "已验证 $validatedCount 个信号，其中 $successCount 个预测正确"
            )
        }
    }

    /**
     * 保存准确率统计
     */
    private suspend fun saveAccuracyStats(accuracy: SignalAccuracy) {
        // 保存到 SharedPreferences 或数据库
        val prefs = applicationContext.getSharedPreferences("signal_tracking", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("${accuracy.stockSymbol}_total", accuracy.totalSignals)
            putFloat("${accuracy.stockSymbol}_win_rate", accuracy.winRate.toFloat())
            putFloat("${accuracy.stockSymbol}_direction_acc", accuracy.directionAccuracy.toFloat())
            putLong("${accuracy.stockSymbol}_updated", accuracy.lastUpdated.time)
            apply()
        }
    }

    /**
     * 保存对比结果
     */
    private suspend fun saveComparisonResult(comparison: ComparisonResult) {
        val prefs = applicationContext.getSharedPreferences("signal_comparisons", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("${comparison.signalId}_validated", true)
            putBoolean("${comparison.signalId}_correct", comparison.isCorrect)
            putFloat("${comparison.signalId}_change", comparison.priceChangePercent.toFloat())
            apply()
        }
    }

    /**
     * 检查并通知准确率变化
     */
    private suspend fun checkAndNotifyAccuracyChange(symbol: String, current: SignalAccuracy) {
        val prefs = applicationContext.getSharedPreferences("signal_tracking", Context.MODE_PRIVATE)
        val previousRate = prefs.getFloat("${symbol}_win_rate", -1f)
        
        if (previousRate >= 0) {
            val change = current.winRate - previousRate
            
            // 准确率显著提升
            if (change >= 10 && current.winRate >= HIGH_ACCURACY_THRESHOLD) {
                notificationHelper.showAnalysisCompleteNotification(
                    "$symbol 信号准确率提升",
                    "准确率从 ${previousRate.toInt()}% 提升至 ${current.winRate.toInt()}%，表现优秀！"
                )
            }
            // 准确率显著下降
            else if (change <= -10 && current.winRate <= LOW_ACCURACY_THRESHOLD) {
                notificationHelper.showAnalysisCompleteNotification(
                    "$symbol 信号准确率下降",
                    "准确率从 ${previousRate.toInt()}% 下降至 ${current.winRate.toInt()}%，建议谨慎参考"
                )
            }
        }
        
        // 保存当前值
        prefs.edit().putFloat("${symbol}_win_rate", current.winRate.toFloat()).apply()
    }

    /**
     * 生成整体统计
     */
    private suspend fun generateOverallStats(results: List<Pair<String, SignalAccuracy>>) {
        if (results.isEmpty()) return
        
        val totalSignals = results.sumOf { it.second.totalSignals }
        val avgWinRate = results.map { it.second.winRate }.average()
        val avgDirectionAcc = results.map { it.second.directionAccuracy }.average()
        
        val prefs = applicationContext.getSharedPreferences("signal_tracking", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("overall_total_signals", totalSignals)
            putFloat("overall_avg_win_rate", avgWinRate.toFloat())
            putFloat("overall_avg_direction_acc", avgDirectionAcc.toFloat())
            putLong("overall_last_updated", Date().time)
            apply()
        }
        
        Log.d(TAG, "Overall stats: avgWinRate=$avgWinRate%, totalSignals=$totalSignals")
    }

    /**
     * 发送汇总通知
     */
    private suspend fun sendSummaryNotification(
        totalStocks: Int,
        best: Pair<String, SignalAccuracy>?,
        worst: Pair<String, SignalAccuracy>?
    ) {
        val title = "信号追踪报告"
        val message = buildString {
            append("已追踪 $totalStocks 只股票")
            best?.let { append(" | 最佳: ${it.first} (${it.second.winRate.toInt()}%)") }
            worst?.let { append(" | 需关注: ${it.first} (${it.second.winRate.toInt()}%)") }
        }
        
        notificationHelper.showMarketUpdateNotification(title, message)
    }

    /**
     * 获取信号统计信息（用于UI展示）
     */
    suspend fun getSignalStatistics(symbol: String): SignalTrackingStats? {
        val prefs = applicationContext.getSharedPreferences("signal_tracking", Context.MODE_PRIVATE)
        val total = prefs.getInt("${symbol}_total", 0)
        
        if (total == 0) return null
        
        return SignalTrackingStats(
            stockSymbol = symbol,
            totalSignals = total,
            winRate = prefs.getFloat("${symbol}_win_rate", 0f).toDouble(),
            directionAccuracy = prefs.getFloat("${symbol}_direction_acc", 0f).toDouble(),
            lastUpdated = Date(prefs.getLong("${symbol}_updated", 0))
        )
    }

    /**
     * 获取最近验证的信号
     */
    suspend fun getRecentValidatedSignals(limit: Int = 10): List<ComparisonResult> {
        val prefs = applicationContext.getSharedPreferences("signal_comparisons", Context.MODE_PRIVATE)
        // 这里简化实现，实际应该从数据库查询
        return emptyList()
    }
}

/**
 * 信号追踪统计
 */
data class SignalTrackingStats(
    val stockSymbol: String,
    val totalSignals: Int,
    val winRate: Double,
    val directionAccuracy: Double,
    val lastUpdated: Date
)

/**
 * 信号追踪配置
 */
data class SignalTrackingConfig(
    val evalWindowDays: Int = 5,
    val checkIntervalHours: Long = 24,
    val highAccuracyThreshold: Double = 70.0,
    val lowAccuracyThreshold: Double = 40.0,
    val minSignalsForReport: Int = 5,
    val enableNotifications: Boolean = true
)
