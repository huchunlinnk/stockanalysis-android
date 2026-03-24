package com.example.stockanalysis.data.datasource

import com.example.stockanalysis.data.model.*

/**
 * 股票数据源接口
 * 所有数据源实现此接口以提供统一访问方式
 */
interface StockDataSource {
    
    /**
     * 数据源名称
     */
    val name: String
    
    /**
     * 数据源优先级（数字越小优先级越高）
     */
    val priority: Int
    
    /**
     * 数据源健康状态
     */
    var isHealthy: Boolean
    
    /**
     * 获取实时行情
     */
    suspend fun fetchQuote(symbol: String): Result<RealtimeQuote>
    
    /**
     * 获取批量实时行情
     */
    suspend fun fetchQuotes(symbols: List<String>): Result<List<RealtimeQuote>>
    
    /**
     * 获取K线数据
     */
    suspend fun fetchKLineData(symbol: String, days: Int = 90): Result<List<KLineData>>
    
    /**
     * 获取技术指标
     */
    suspend fun fetchTechnicalIndicators(symbol: String): Result<TechnicalIndicators>
    
    /**
     * 获取趋势分析
     */
    suspend fun fetchTrendAnalysis(symbol: String): Result<TrendAnalysis>
    
    /**
     * 获取市场概览
     */
    suspend fun fetchMarketOverview(): Result<MarketOverview>
    
    /**
     * 搜索股票
     */
    suspend fun searchStocks(query: String): Result<List<Pair<String, String>>>
}

/**
 * 数据源异常
 */
sealed class DataSourceException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkException(message: String, cause: Throwable? = null) : DataSourceException(message, cause)
    class ParseException(message: String, cause: Throwable? = null) : DataSourceException(message, cause)
    class RateLimitException(message: String, cause: Throwable? = null) : DataSourceException(message, cause)
    class InvalidSymbolException(message: String, cause: Throwable? = null) : DataSourceException(message, cause)
    class ServiceUnavailableException(message: String, cause: Throwable? = null) : DataSourceException(message, cause)
    class NotSupportedException(message: String, cause: Throwable? = null) : DataSourceException(message, cause)
}
