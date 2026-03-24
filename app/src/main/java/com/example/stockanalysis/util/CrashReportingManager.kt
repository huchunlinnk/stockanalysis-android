package com.example.stockanalysis.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 崩溃报告管理器
 *
 * 功能：
 * 1. 集成 Firebase Crashlytics
 * 2. 自动捕获崩溃
 * 3. 记录自定义日志
 * 4. 设置用户属性
 * 5. 记录非致命异常
 */
@Singleton
class CrashReportingManager @Inject constructor() {

    companion object {
        private const val TAG = "CrashReportingManager"
    }

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    /**
     * 初始化崩溃报告
     *
     * @param enableCollection 是否启用崩溃收集（默认启用）
     */
    fun initialize(enableCollection: Boolean = true) {
        crashlytics.setCrashlyticsCollectionEnabled(enableCollection)
        Log.d(TAG, "Crashlytics initialized, collection enabled: $enableCollection")
    }

    /**
     * 设置用户标识
     *
     * @param userId 用户 ID（可选，用于追踪特定用户的崩溃）
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
        Log.d(TAG, "User ID set: $userId")
    }

    /**
     * 设置自定义属性
     *
     * @param key 属性名
     * @param value 属性值
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
        Log.d(TAG, "Custom key set: $key = $value")
    }

    /**
     * 设置多个自定义属性
     */
    fun setCustomKeys(keys: Map<String, String>) {
        keys.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value)
        }
        Log.d(TAG, "Multiple custom keys set: ${keys.size} keys")
    }

    /**
     * 记录日志
     *
     * @param message 日志消息
     */
    fun log(message: String) {
        crashlytics.log(message)
    }

    /**
     * 记录非致命异常
     *
     * 用于捕获不会导致崩溃但需要关注的异常
     *
     * @param throwable 异常对象
     * @param message 附加说明（可选）
     */
    fun recordException(throwable: Throwable, message: String? = null) {
        if (message != null) {
            crashlytics.log("Exception: $message")
        }
        crashlytics.recordException(throwable)
        Log.w(TAG, "Exception recorded: ${throwable.message}", throwable)
    }

    /**
     * 记录网络错误
     *
     * @param url 请求 URL
     * @param statusCode HTTP 状态码
     * @param errorMessage 错误信息
     */
    fun recordNetworkError(url: String, statusCode: Int, errorMessage: String) {
        crashlytics.apply {
            setCustomKey("network_url", url)
            setCustomKey("network_status_code", statusCode)
            setCustomKey("network_error", errorMessage)
            log("Network error: $statusCode - $url - $errorMessage")
        }
        Log.w(TAG, "Network error recorded: $statusCode - $url")
    }

    /**
     * 记录分析失败
     *
     * @param stockCode 股票代码
     * @param errorType 错误类型
     * @param errorMessage 错误信息
     */
    fun recordAnalysisError(stockCode: String, errorType: String, errorMessage: String) {
        crashlytics.apply {
            setCustomKey("analysis_stock_code", stockCode)
            setCustomKey("analysis_error_type", errorType)
            setCustomKey("analysis_error_message", errorMessage)
            log("Analysis error: $errorType - $stockCode - $errorMessage")
        }
        Log.w(TAG, "Analysis error recorded: $errorType - $stockCode")
    }

    /**
     * 记录 LLM 调用失败
     *
     * @param provider LLM 提供商
     * @param model 模型名称
     * @param errorMessage 错误信息
     */
    fun recordLLMError(provider: String, model: String, errorMessage: String) {
        crashlytics.apply {
            setCustomKey("llm_provider", provider)
            setCustomKey("llm_model", model)
            setCustomKey("llm_error", errorMessage)
            log("LLM error: $provider - $model - $errorMessage")
        }
        Log.w(TAG, "LLM error recorded: $provider - $model")
    }

    /**
     * 测试崩溃报告
     *
     * 仅用于测试，强制抛出异常验证 Crashlytics 是否工作
     */
    fun testCrash() {
        Log.w(TAG, "Test crash triggered!")
        throw RuntimeException("This is a test crash from CrashReportingManager")
    }

    /**
     * 启用/禁用崩溃收集
     *
     * @param enabled 是否启用
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
        Log.d(TAG, "Crashlytics collection ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * 检查是否有未发送的崩溃报告
     */
    fun checkForUnsentReports(): Boolean {
        return try {
            crashlytics.checkForUnsentReports().isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check unsent reports", e)
            false
        }
    }

    /**
     * 发送所有未发送的崩溃报告
     */
    fun sendUnsentReports() {
        try {
            crashlytics.sendUnsentReports()
            Log.d(TAG, "Unsent reports sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send unsent reports", e)
        }
    }

    /**
     * 删除所有未发送的崩溃报告
     */
    fun deleteUnsentReports() {
        try {
            crashlytics.deleteUnsentReports()
            Log.d(TAG, "Unsent reports deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete unsent reports", e)
        }
    }
}
