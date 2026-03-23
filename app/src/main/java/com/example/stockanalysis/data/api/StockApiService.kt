package com.example.stockanalysis.data.api

import com.example.stockanalysis.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 股票数据 API 服务
 * 
 * 注：这里使用模拟数据，实际项目中需要接入真实的股票数据 API
 * 如：东方财富、新浪财经、腾讯财经等
 */
interface StockApiService {
    
    /**
     * 获取实时行情
     */
    @GET("api/stock/quote/{symbol}")
    suspend fun getQuote(@Path("symbol") symbol: String): Response<StockQuote>
    
    /**
     * 批量获取实时行情
     */
    @GET("api/stock/quotes")
    suspend fun getQuotes(@Query("symbols") symbols: String): Response<List<StockQuote>>
    
    /**
     * 获取K线数据
     */
    @GET("api/stock/kline/{symbol}")
    suspend fun getKLineData(
        @Path("symbol") symbol: String,
        @Query("period") period: String = "day",
        @Query("count") count: Int = 100
    ): Response<List<KLineData>>
    
    /**
     * 获取技术指标
     */
    @GET("api/stock/technical/{symbol}")
    suspend fun getTechnicalIndicators(@Path("symbol") symbol: String): Response<TechnicalIndicators>
    
    /**
     * 获取大盘指数
     */
    @GET("api/market/indices")
    suspend fun getMarketIndices(): Response<List<MarketIndex>>
    
    /**
     * 获取市场统计
     */
    @GET("api/market/stats")
    suspend fun getMarketStats(): Response<MarketStats>
    
    /**
     * 获取板块表现
     */
    @GET("api/market/sectors")
    suspend fun getSectorPerformance(): Response<SectorPerformance>
    
    /**
     * 搜索股票
     */
    @GET("api/stock/search")
    suspend fun searchStocks(@Query("keyword") keyword: String): Response<List<Stock>>
    
    /**
     * 获取股票基本面信息
     */
    @GET("api/stock/fundamental/{symbol}")
    suspend fun getFundamentalInfo(@Path("symbol") symbol: String): Response<FundamentalInfo>
    
    /**
     * 获取相关新闻
     */
    @GET("api/stock/news/{symbol}")
    suspend fun getNews(@Path("symbol") symbol: String): Response<List<NewsItem>>
}

/**
 * K线数据
 */
data class KLineData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

/**
 * 基本面信息
 */
data class FundamentalInfo(
    val peRatio: Double?,
    val pbRatio: Double?,
    val eps: Double?,
    val totalShares: Long?,
    val marketCap: Double?,
    val revenue: Double?,
    val netProfit: Double?,
    val roe: Double?,
    val debtRatio: Double?
)
