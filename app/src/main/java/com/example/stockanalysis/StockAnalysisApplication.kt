package com.example.stockanalysis

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.stockanalysis.util.CrashReportingManager
import com.example.stockanalysis.worker.DataRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StockAnalysisApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var crashReportingManager: CrashReportingManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // 保存实例
        instance = this

        // 初始化崩溃报告
        initializeCrashReporting()
        
        // 初始化定时数据刷新任务
        initializeDataRefreshWorker()
    }
    
    /**
     * 初始化定时数据刷新任务
     */
    private fun initializeDataRefreshWorker() {
        try {
            DataRefreshWorker.initialize(this)
            Log.d(TAG, "Data refresh worker initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize data refresh worker", e)
        }
    }

    private fun initializeCrashReporting() {
        try {
            // 启用 Crashlytics
            crashReportingManager.initialize(enableCollection = true)

            // 设置应用信息
            crashReportingManager.setCustomKeys(
                mapOf(
                    "app_version" to BuildConfig.VERSION_NAME,
                    "app_version_code" to BuildConfig.VERSION_CODE.toString(),
                    "build_type" to BuildConfig.BUILD_TYPE
                )
            )

            Log.d("Application", "Crash reporting initialized")
        } catch (e: Exception) {
            Log.e("Application", "Failed to initialize crash reporting", e)
        }
    }

    companion object {
        private const val TAG = "StockAnalysisApplication"
        
        @Volatile
        private var instance: StockAnalysisApplication? = null

        /**
         * 获取 Application 实例
         */
        fun getInstance(): StockAnalysisApplication {
            return instance ?: throw IllegalStateException("Application not initialized yet")
        }

        /**
         * 获取 Application Context
         */
        fun getContext(): android.content.Context {
            return getInstance().applicationContext
        }
    }
}
