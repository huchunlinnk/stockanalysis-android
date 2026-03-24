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
 * Bocha 新闻搜索服务
 * 中文搜索优化
 * 文档: https://bochaai.com/
 */
@Singleton
class BochaNewsService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String
) : NewsSearchService {
    
    override val name: String = "Bocha"
    override val priority: Int = 1
    override var isAvailable: Boolean = true
    
    companion object {
        const val BASE_URL = "https://api.bochaai.com"
        const val SEARCH_ENDPOINT = "/v1/web-search"
    }
    
    override suspend fun search(
        query: String,
        maxResults: Int,
        maxAgeDays: Int
    ): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                NewsSearchException.ApiKeyInvalidException("Bocha API key is not configured")
            )
        }
        
        try {
            val requestBody = JSONObject().apply {
                put("query", query)
                put("freshness", "week")  // day, week, month
                put("count", maxResults)
                put("answer", false)
            }
            
            val request = Request.Builder()
                .url("$BASE_URL$SEARCH_ENDPOINT")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    when (response.code) {
                        401 -> NewsSearchException.ApiKeyInvalidException("Invalid API key")
                        429 -> NewsSearchException.RateLimitException("Rate limit exceeded")
                        else -> NewsSearchException.NetworkException("HTTP ${response.code}")
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
    
    private fun parseResponse(json: String): List<NewsArticle> {
        return try {
            val root = JSONObject(json)
            val webPages = root.optJSONObject("webPages") 
                ?: return emptyList()
            val value = webPages.optJSONArray("value") 
                ?: return emptyList()
            
            val articles = mutableListOf<NewsArticle>()
            
            for (i in 0 until value.length()) {
                val item = value.optJSONObject(i) ?: continue
                
                val article = NewsArticle(
                    title = item.optString("name", ""),
                    url = item.optString("url", ""),
                    summary = item.optString("snippet", null),
                    publishedDate = parseDate(item.optString("dateLastCrawled", "")),
                    source = item.optString("siteName", "Unknown"),
                    relevanceScore = 0.0
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
    
    private fun parseDate(dateString: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}
