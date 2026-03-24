package com.example.stockanalysis.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tavily 新闻搜索服务
 * 推荐，每月1000次免费
 * 文档: https://tavily.com/
 */
@Singleton
class TavilyNewsService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String
) : NewsSearchService {
    
    override val name: String = "Tavily"
    override val priority: Int = 0
    override var isAvailable: Boolean = true
    
    companion object {
        const val BASE_URL = "https://api.tavily.com"
        const val SEARCH_ENDPOINT = "/search"
    }
    
    override suspend fun search(
        query: String,
        maxResults: Int,
        maxAgeDays: Int
    ): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                NewsSearchException.ApiKeyInvalidException("Tavily API key is not configured")
            )
        }
        
        try {
            val requestBody = JSONObject().apply {
                put("api_key", apiKey)
                put("query", query)
                put("search_depth", "advanced")
                put("max_results", maxResults)
                put("include_answer", false)
                put("include_images", false)
                put("include_raw_content", false)
                put("days", maxAgeDays)
            }
            
            val request = Request.Builder()
                .url("$BASE_URL$SEARCH_ENDPOINT")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                return@withContext Result.failure(
                    when (response.code) {
                        401 -> NewsSearchException.ApiKeyInvalidException("Invalid API key")
                        429 -> NewsSearchException.RateLimitException("Rate limit exceeded")
                        else -> NewsSearchException.NetworkException("HTTP ${response.code}: $errorBody")
                    }
                )
            }
            
            val body = response.body?.string()
                ?: return@withContext Result.failure(NewsSearchException.ParseException("Empty response"))
            
            val articles = parseResponse(body)
            Result.success(articles)
            
        } catch (e: Exception) {
            Result.failure(NewsSearchException.NetworkException("Network error", e))
        }
    }
    
    /**
     * 解析Tavily响应
     */
    private fun parseResponse(json: String): List<NewsArticle> {
        return try {
            val root = JSONObject(json)
            val results = root.optJSONArray("results") ?: return emptyList()
            
            val articles = mutableListOf<NewsArticle>()
            
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                
                val article = NewsArticle(
                    title = item.optString("title", ""),
                    url = item.optString("url", ""),
                    content = item.optString("content", null),
                    summary = item.optString("content", null)?.take(200),
                    publishedDate = parseDate(item.optString("published_date", "")),
                    source = item.optString("source", "Unknown"),
                    relevanceScore = item.optDouble("score", 0.0),
                    sentiment = "neutral"
                )
                
                if (article.title.isNotBlank() && article.url.isNotBlank()) {
                    articles.add(article)
                }
            }
            
            articles
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 解析日期字符串
     */
    private fun parseDate(dateString: String): Date {
        return try {
            // 尝试多种日期格式
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    return sdf.parse(dateString) ?: Date()
                } catch (e: Exception) {
                    // 尝试下一个格式
                }
            }
            
            Date()
        } catch (e: Exception) {
            Date()
        }
    }
}
