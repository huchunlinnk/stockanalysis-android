package com.example.stockanalysis.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stockanalysis.data.local.StockDao
import com.example.stockanalysis.data.local.AnalysisResultDao
import com.example.stockanalysis.data.repository.AnalysisEngine
import com.example.stockanalysis.data.repository.AnalysisState
import com.example.stockanalysis.data.model.Decision
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 后台分析 Worker
 * 定时对自选股进行分析，并推送通知
 */
@HiltWorker
class AnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stockDao: StockDao,
    private val analysisResultDao: AnalysisResultDao,
    private val analysisEngine: AnalysisEngine,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "AnalysisWorker"
        const val KEY_STOCK_SYMBOL = "stock_symbol"
        const val KEY_STOCK_NAME = "stock_name"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting background analysis")

            // 获取要分析的股票
            val symbol = inputData.getString(KEY_STOCK_SYMBOL)
            val name = inputData.getString(KEY_STOCK_NAME)

            if (symbol != null && name != null) {
                // 分析单个股票
                analyzeSingleStock(symbol, name)
            } else {
                // 分析所有自选股
                analyzeAllStocks()
            }

            Log.d(TAG, "Background analysis completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background analysis failed", e)
            Result.retry()
        }
    }

    /**
     * 分析单个股票
     */
    private suspend fun analyzeSingleStock(symbol: String, name: String) {
        Log.d(TAG, "Analyzing stock: $name ($symbol)")

        try {
            analysisEngine.analyzeStock(symbol, name).collect { state ->
                when (state) {
                    is AnalysisState.Loading,
                    is AnalysisState.Progress,
                    is AnalysisState.AgentResult -> {
                        // 中间状态，不处理
                    }
                    is AnalysisState.Success -> {
                        val result = state.result

                        // 保存分析结果到数据库
                        analysisResultDao.insertResult(result)

                        // 根据决策类型发送不同的通知
                        val notificationText = when (result.decision) {
                            Decision.STRONG_BUY -> "【强烈买入】${result.summary}"
                            Decision.BUY -> "【买入】${result.summary}"
                            Decision.HOLD -> "【持有观望】${result.summary}"
                            Decision.SELL -> "【卖出】${result.summary}"
                            Decision.STRONG_SELL -> "【强烈卖出】${result.summary}"
                        }

                        // 只在重要信号时发送通知（买入/卖出）
                        if (result.decision in listOf(Decision.STRONG_BUY, Decision.BUY, Decision.SELL, Decision.STRONG_SELL)) {
                            notificationHelper.showAnalysisCompleteNotification(
                                "$name ($symbol)",
                                notificationText
                            )
                        }

                        Log.d(TAG, "Analysis completed for $symbol: ${result.decision}")
                    }
                    is AnalysisState.Error -> {
                        Log.e(TAG, "Analysis error for $symbol: ${state.message}")
                    }
                    is AnalysisState.Loading, is AnalysisState.Progress -> {
                        // Progress states, ignore
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze $symbol", e)
            throw e
        }
    }

    /**
     * 分析所有自选股
     */
    private suspend fun analyzeAllStocks() {
        Log.d(TAG, "Analyzing all favorite stocks")

        // 获取所有自选股
        val favoriteStocks = stockDao.getAllStocksSync()
        Log.d(TAG, "Found ${favoriteStocks.size} stocks")

        var successCount = 0
        var failCount = 0

        // 逐个分析
        for (stock in favoriteStocks) {
            try {
                analyzeSingleStock(stock.symbol, stock.name)
                successCount++
            } catch (e: Exception) {
                failCount++
                Log.e(TAG, "Failed to analyze ${stock.symbol}", e)
            }
        }

        // 发送汇总通知
        if (favoriteStocks.isNotEmpty()) {
            notificationHelper.showMarketUpdateNotification(
                "自选股分析完成",
                "成功: $successCount, 失败: $failCount, 共 ${favoriteStocks.size} 只"
            )
        }

        Log.d(TAG, "Batch analysis completed: success=$successCount, failed=$failCount")
    }
}
