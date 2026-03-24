package com.example.stockanalysis.data.news

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 新闻搜索管理器
 * 管理多个新闻搜索服务，提供故障切换和多维度搜索
 */
@Singleton
class NewsSearchManager @Inject constructor(
    private val tavilyService: TavilyNewsService,
    private val bochaService: BochaNewsService
) {
    companion object {
        const val TAG = "NewsSearchManager"
        const val DEFAULT_TIMEOUT_MS = 10000L  // 10秒超时
    }
    
    /**
     * 可用的新闻搜索服务列表
     */
    private val services: List<NewsSearchService> by lazy {
        listOf(tavilyService, bochaService)
            .filter { it.isAvailable }
            .sortedBy { it.priority }
    }
    
    /**
     * 多维度搜索
     * 同时搜索最新消息、风险新闻、业绩新闻
     */
    suspend fun searchMultiDimension(
        stockSymbol: String,
        stockName: String
    ): MultiDimensionNewsResult {
        val queries = mapOf(
            "latest" to "$stockName $stockSymbol 最新消息",
            "risk" to "$stockName $stockSymbol 风险 问题 诉讼 警示",
            "performance" to "$stockName $stockSymbol 业绩 财报 预期 营收 利润"
        )
        
        return coroutineScope {
            val results = queries.map { (dimension, query) ->
                async {
                    dimension to searchWithFallback(query, maxResults = 5, maxAgeDays = 7)
                }
            }.awaitAll().toMap()
            
            MultiDimensionNewsResult(
                latestNews = results["latest"] ?: emptyList(),
                riskNews = results["risk"] ?: emptyList(),
                performanceNews = results["performance"] ?: emptyList()
            )
        }
    }
    
    /**
     * 带故障切换的搜索
     */
    suspend fun searchWithFallback(
        query: String,
        maxResults: Int = 5,
        maxAgeDays: Int = 3
    ): List<NewsArticle> {
        for (service in services) {
            try {
                Log.d(TAG, "Trying news service: ${service.name}")
                
                val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                    service.search(query, maxResults, maxAgeDays)
                }
                
                if (result != null && result.isSuccess) {
                    val articles = result.getOrNull()
                    if (!articles.isNullOrEmpty()) {
                        Log.d(TAG, "Success with ${service.name}, found ${articles.size} articles")
                        return articles
                    }
                }
                
                // 标记服务为不可用
                service.isAvailable = false
                
            } catch (e: Exception) {
                Log.w(TAG, "News service ${service.name} failed: ${e.message}")
                service.isAvailable = false
            }
        }
        
        Log.w(TAG, "All news services failed for query: $query")
        return emptyList()
    }
    
    /**
     * 搜索新闻（简单接口）
     */
    suspend fun search(
        query: String,
        maxResults: Int = 5,
        maxAgeDays: Int = 3
    ): List<NewsArticle> {
        return searchWithFallback(query, maxResults, maxAgeDays)
    }
    
    /**
     * 刷新服务可用性状态
     */
    fun refreshServiceAvailability() {
        services.forEach { it.isAvailable = true }
    }
    
    /**
     * 获取服务状态
     */
    fun getServiceStatus(): Map<String, Boolean> {
        return services.associate { it.name to it.isAvailable }
    }
    
    /**
     * 是否有可用的搜索服务
     */
    fun hasAvailableService(): Boolean {
        return services.any { it.isAvailable }
    }
}
