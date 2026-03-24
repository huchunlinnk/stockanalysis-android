package com.example.stockanalysis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.stockanalysis.R
import com.example.stockanalysis.data.local.StockDao
import com.example.stockanalysis.data.local.AnalysisResultDao
import com.example.stockanalysis.data.repository.AnalysisEngine
import com.example.stockanalysis.data.repository.AnalysisState
import com.example.stockanalysis.notification.NotificationHelper
import com.example.stockanalysis.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * 股票分析前台服务
 * 用于长时间运行的股票数据同步和分析任务
 */
@AndroidEntryPoint
class StockAnalysisService : Service() {

    @Inject
    lateinit var stockDao: StockDao

    @Inject
    lateinit var analysisResultDao: AnalysisResultDao

    @Inject
    lateinit var analysisEngine: AnalysisEngine

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isAnalyzing = false

    companion object {
        const val TAG = "StockAnalysisService"
        const val CHANNEL_ID = "stock_analysis_service"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_ANALYSIS = "com.example.stockanalysis.ACTION_START_ANALYSIS"
        const val ACTION_STOP_ANALYSIS = "com.example.stockanalysis.ACTION_STOP_ANALYSIS"
        const val EXTRA_STOCK_SYMBOL = "stock_symbol"
        const val EXTRA_STOCK_NAME = "stock_name"
    }

    inner class LocalBinder : Binder() {
        fun getService(): StockAnalysisService = this@StockAnalysisService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_ANALYSIS -> {
                val symbol = intent.getStringExtra(EXTRA_STOCK_SYMBOL)
                val name = intent.getStringExtra(EXTRA_STOCK_NAME)

                if (symbol != null && name != null) {
                    startForegroundWithNotification("分析 $name")
                    startAnalysis(symbol, name)
                } else {
                    startForegroundWithNotification("批量分析中...")
                    startBatchAnalysis()
                }
            }
            ACTION_STOP_ANALYSIS -> {
                stopAnalysis()
                stopSelf()
            }
            else -> {
                startForegroundWithNotification("股票分析服务运行中")
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "股票分析服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示股票分析任务的运行状态"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 启动前台服务
     */
    private fun startForegroundWithNotification(contentText: String) {
        val notification = createNotification(contentText)
        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * 创建通知
     */
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("股票分析")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * 更新通知内容
     */
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 开始分析单个股票
     */
    private fun startAnalysis(symbol: String, name: String) {
        if (isAnalyzing) {
            Log.w(TAG, "Already analyzing, skipping")
            return
        }

        isAnalyzing = true
        serviceScope.launch {
            try {
                Log.d(TAG, "Starting analysis for $name ($symbol)")
                updateNotification("正在分析 $name...")

                analysisEngine.analyzeStock(symbol, name).collect { state ->
                    when (state) {
                        is AnalysisState.Progress -> {
                            updateNotification("$name: ${state.message}")
                        }
                        is AnalysisState.Success -> {
                            val result = state.result
                            analysisResultDao.insertResult(result)

                            updateNotification("$name 分析完成")
                            Log.d(TAG, "Analysis completed: ${result.decision}")

                            // 发送完成通知
                            notificationHelper.showAnalysisCompleteNotification(
                                "$name ($symbol)",
                                result.summary
                            )

                            // 延迟后停止服务
                            delay(2000)
                            stopSelf()
                        }
                        is AnalysisState.Error -> {
                            Log.e(TAG, "Analysis error: ${state.message}")
                            updateNotification("分析失败: ${state.message}")
                            delay(3000)
                            stopSelf()
                        }
                        else -> { /* Loading state */ }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis exception", e)
                updateNotification("分析异常: ${e.message}")
            } finally {
                isAnalyzing = false
            }
        }
    }

    /**
     * 批量分析所有自选股
     */
    private fun startBatchAnalysis() {
        if (isAnalyzing) {
            Log.w(TAG, "Already analyzing, skipping")
            return
        }

        isAnalyzing = true
        serviceScope.launch {
            try {
                val stocks = stockDao.getAllStocksSync()
                Log.d(TAG, "Starting batch analysis for ${stocks.size} stocks")

                var completed = 0
                var succeeded = 0

                for (stock in stocks) {
                    updateNotification("分析中 ${completed + 1}/${stocks.size}: ${stock.name}")

                    try {
                        analysisEngine.analyzeStock(stock.symbol, stock.name).collect { state ->
                            if (state is AnalysisState.Success) {
                                analysisResultDao.insertResult(state.result)
                                succeeded++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to analyze ${stock.symbol}", e)
                    }

                    completed++
                }

                updateNotification("批量分析完成: $succeeded/$completed")
                notificationHelper.showMarketUpdateNotification(
                    "批量分析完成",
                    "成功: $succeeded, 总数: $completed"
                )

                delay(3000)
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Batch analysis exception", e)
            } finally {
                isAnalyzing = false
            }
        }
    }

    /**
     * 停止分析
     */
    private fun stopAnalysis() {
        Log.d(TAG, "Stopping analysis")
        isAnalyzing = false
        serviceScope.coroutineContext.cancelChildren()
    }

    /**
     * 获取服务状态
     */
    fun isAnalyzing(): Boolean = isAnalyzing
}
