package com.example.stockanalysis.data.news

import com.example.stockanalysis.data.model.NewsItem

/**
 * 新闻搜索服务接口
 */
interface NewsSearchService {
    
    /**
     * 服务名称
     */
    val name: String
    
    /**
     * 服务优先级
     */
    val priority: Int
    
    /**
     * 是否可用
     */
    var isAvailable: Boolean
    
    /**
     * 搜索新闻
     * 
     * @param query 搜索关键词
     * @param maxResults 最大结果数
     * @param maxAgeDays 最大天数
     */
    suspend fun search(
        query: String,
        maxResults: Int = 5,
        maxAgeDays: Int = 3
    ): Result<List<NewsArticle>>
}

/**
 * 新闻文章
 */
data class NewsArticle(
    val title: String,
    val url: String,
    val content: String? = null,
    val summary: String? = null,
    val publishedDate: java.util.Date,
    val source: String,
    val relevanceScore: Double = 0.0,
    val sentiment: String = "neutral"  // positive, negative, neutral
)

/**
 * 多维度新闻搜索结果
 */
data class MultiDimensionNewsResult(
    val latestNews: List<NewsArticle> = emptyList(),
    val riskNews: List<NewsArticle> = emptyList(),
    val performanceNews: List<NewsArticle> = emptyList(),
    val searchTime: java.util.Date = java.util.Date()
)

/**
 * 新闻搜索异常
 */
sealed class NewsSearchException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkException(message: String, cause: Throwable? = null) : NewsSearchException(message, cause)
    class ApiKeyInvalidException(message: String, cause: Throwable? = null) : NewsSearchException(message, cause)
    class RateLimitException(message: String, cause: Throwable? = null) : NewsSearchException(message, cause)
    class ParseException(message: String, cause: Throwable? = null) : NewsSearchException(message, cause)
}
