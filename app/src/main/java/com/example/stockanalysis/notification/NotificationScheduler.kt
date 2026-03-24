package com.example.stockanalysis.notification

import android.content.Context
import androidx.work.*
import com.example.stockanalysis.data.local.PreferencesManager
import java.util.concurrent.TimeUnit

class NotificationScheduler(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {

    fun scheduleAnalysisNotification(delayMinutes: Int) {
        if (!preferencesManager.isAnalysisNotificationsEnabled()) return

        val workRequest = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "analysis_notification",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun scheduleMarketUpdate() {
        if (!preferencesManager.isMarketNotificationsEnabled()) return

        val workRequest = PeriodicWorkRequestBuilder<AnalysisWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "market_update",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
