package com.example.stockanalysis.notification

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * 信号追踪调度器
 * 用于调度 SignalTrackingWorker 的定期执行
 */
object SignalTrackingScheduler {
    
    const val TAG_PERIODIC_TRACKING = "periodic_signal_tracking"
    
    /**
     * 设置定期信号追踪
     * 每天执行一次
     */
    fun schedulePeriodicTracking(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        // 延迟到每天早上9:30执行
        val initialDelay = SignalTrackingWorker.calculateInitialDelay()
        
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SignalTrackingWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(TAG_PERIODIC_TRACKING)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG_PERIODIC_TRACKING,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
    
    /**
     * 立即执行单次信号追踪
     */
    fun runImmediateTracking(context: Context, symbol: String? = null) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val inputData = Data.Builder().apply {
            putString(SignalTrackingWorker.KEY_MODE, 
                if (symbol != null) SignalTrackingWorker.MODE_SINGLE_STOCK 
                else SignalTrackingWorker.MODE_ALL_STOCKS
            )
            symbol?.let { putString(SignalTrackingWorker.KEY_SYMBOL, it) }
        }.build()
        
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<SignalTrackingWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
        
        WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
    }
    
    /**
     * 执行批量更新（验证待处理的信号）
     */
    fun runBatchUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val inputData = Data.Builder()
            .putString(SignalTrackingWorker.KEY_MODE, SignalTrackingWorker.MODE_BATCH_UPDATE)
            .build()
        
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<SignalTrackingWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()
        
        WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
    }
    
    /**
     * 取消定期追踪
     */
    fun cancelPeriodicTracking(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TAG_PERIODIC_TRACKING)
    }
    
    /**
     * 检查是否已调度
     */
    fun isScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosByTag(TAG_PERIODIC_TRACKING)
            .get()
        
        return workInfos.any { 
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING 
        }
    }
    
    /**
     * 获取追踪状态
     */
    fun getTrackingStatus(context: Context): TrackingStatus {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosByTag(TAG_PERIODIC_TRACKING)
            .get()
        
        val workInfo = workInfos.firstOrNull()
        
        return TrackingStatus(
            isScheduled = workInfo != null,
            state = workInfo?.state,
            nextRunTime = workInfo?.nextScheduleTimeMillis
        )
    }
}

/**
 * 追踪状态
 */
data class TrackingStatus(
    val isScheduled: Boolean,
    val state: WorkInfo.State?,
    val nextRunTime: Long?
)
