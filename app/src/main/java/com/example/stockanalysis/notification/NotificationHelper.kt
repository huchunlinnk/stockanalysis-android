package com.example.stockanalysis.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.stockanalysis.R
import com.example.stockanalysis.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID_ANALYSIS = "analysis_channel"
        const val CHANNEL_ID_MARKET = "market_channel"
        const val CHANNEL_ID_ALERT = "alert_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val analysisChannel = NotificationChannel(
                CHANNEL_ID_ANALYSIS,
                "分析通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "股票分析完成通知"
            }

            val marketChannel = NotificationChannel(
                CHANNEL_ID_MARKET,
                "行情通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "市场行情更新通知"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                "预警通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "价格预警通知"
            }

            notificationManager.createNotificationChannels(
                listOf(analysisChannel, marketChannel, alertChannel)
            )
        }
    }

    fun showAnalysisCompleteNotification(stockSymbol: String, result: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ANALYSIS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("分析完成")
            .setContentText("$stockSymbol: $result")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(stockSymbol.hashCode(), notification)
    }

    fun showPriceAlertNotification(stockSymbol: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("价格预警")
            .setContentText("$stockSymbol: $message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((stockSymbol + "alert").hashCode(), notification)
    }

    fun showMarketUpdateNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MARKET)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify("market".hashCode(), notification)
    }
}
