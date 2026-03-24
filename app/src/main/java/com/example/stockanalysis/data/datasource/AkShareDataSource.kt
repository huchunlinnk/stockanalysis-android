package com.example.stockanalysis.data.datasource

import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/**
 * AkShare数据源 (Priority 1)
 * 通过新浪财经和腾讯财经API获取实时行情数据
 * 参照 daily_stock_analysis 的 AkShareFetcher 实现
 */
@Singleton
class AkShareDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) : StockDataSource {
    
    override val name: String = "AkShare"
    override val priority: Int = 1  // 高于本地数据源，低于东方财富
    override var isHealthy: Boolean = true
    
    companion object {
        private const val SINA_BASE_URL = "http://hq.sinajs.cn/list"
        private const val TENCENT_BASE_URL = "http://qt.gtimg.cn/q"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        
        // User-Agent 池，用于随机轮换（反爬策略）
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
    }
    
    override suspend fun fetchQuote(symbol: String): Result<RealtimeQuote> = withContext(Dispatchers.IO) {
        try {
            // 先尝试新浪API，失败则尝试腾讯API
            val sinaResult = fetchFromSina(symbol)
            if (sinaResult.isSuccess) {
                return@withContext sinaResult
            }
            
            val tencentResult = fetchFromTencent(symbol)
            if (tencentResult.isSuccess) {
                return@withContext tencentResult
            }
            
            // 两个API都失败
            Result.failure(
                DataSourceException.ServiceUnavailableException("AkShare: Both Sina and Tencent APIs failed")
            )
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("AkShare network error", e))
        }
    }
    
    override suspend fun fetchQuotes(symbols: List<String>): Result<List<RealtimeQuote>> = withContext(Dispatchers.IO) {
        try {
            if (symbols.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            val results = mutableListOf<RealtimeQuote>()
            
            // 分批获取，每批最多10只（新浪腾讯API限制）
            val batchSize = 10
            symbols.chunked(batchSize).forEach { batch ->
                // 尝试新浪批量接口
                val sinaSymbols = batch.joinToString(",") { toSinaSymbol(it) }
                val sinaUrl = "$SINA_BASE_URL=$sinaSymbols"
                
                val sinaRequest = Request.Builder()
                    .url(sinaUrl)
                    .addHeader("User-Agent", USER_AGENTS.random())
                    .addHeader("Referer", "http://finance.sina.com.cn")
                    .build()
                
                val sinaResponse = okHttpClient.newCall(sinaRequest).execute()
                
                if (sinaResponse.isSuccessful) {
                    val body = sinaResponse.body?.string()
                    body?.let { parseSinaBatchQuotes(it, batch) }?.let { results.addAll(it) }
                } else {
                    // 新浪失败，尝试腾讯
                    batch.forEach { symbol ->
                        val tencentResult = fetchFromTencent(symbol)
                        tencentResult.getOrNull()?.let { results.add(it) }
                    }
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Batch fetch error", e))
        }
    }
    
    override suspend fun fetchKLineData(symbol: String, days: Int): Result<List<KLineData>> = withContext(Dispatchers.IO) {
        // AkShare不直接提供K线API，返回空结果让其他数据源处理
        Result.failure(DataSourceException.ServiceUnavailableException("KLine data not available from AkShare"))
    }
    
    override suspend fun fetchTechnicalIndicators(symbol: String): Result<TechnicalIndicators> = withContext(Dispatchers.IO) {
        // 从K线数据计算技术指标，但AkShare不提供K线数据
        Result.failure(DataSourceException.ServiceUnavailableException("Technical indicators not available from AkShare"))
    }
    
    override suspend fun fetchTrendAnalysis(symbol: String): Result<TrendAnalysis> = withContext(Dispatchers.IO) {
        Result.failure(DataSourceException.ServiceUnavailableException("Trend analysis not available from AkShare"))
    }
    
    override suspend fun fetchMarketOverview(): Result<MarketOverview> = withContext(Dispatchers.IO) {
        try {
            // 获取主要指数
            val indices = listOf("000001", "399001", "399006")
            val quotesResult = fetchQuotes(indices)
            
            quotesResult.fold(
                onSuccess = { quotes ->
                    val shQuote = quotes.find { it.symbol == "000001" }
                    val szQuote = quotes.find { it.symbol == "399001" }
                    val chuangQuote = quotes.find { it.symbol == "399006" }
                    
                    val marketIndices = listOf(
                        shQuote?.let { MarketIndex("上证指数", it.symbol, it.price, it.change, it.changePercent, it.volume, it.amount) }
                            ?: MarketIndex("上证指数", "000001", 0.0, 0.0, 0.0, 0L, 0.0),
                        szQuote?.let { MarketIndex("深证成指", it.symbol, it.price, it.change, it.changePercent, it.volume, it.amount) }
                            ?: MarketIndex("深证成指", "399001", 0.0, 0.0, 0.0, 0L, 0.0),
                        chuangQuote?.let { MarketIndex("创业板指", it.symbol, it.price, it.change, it.changePercent, it.volume, it.amount) }
                            ?: MarketIndex("创业板指", "399006", 0.0, 0.0, 0.0, 0L, 0.0)
                    )
                    
                    val stats = MarketStats(
                        risingCount = 0,
                        fallingCount = 0,
                        flatCount = 0,
                        limitUpCount = 0,
                        limitDownCount = 0,
                        riseDistribution = emptyMap(),
                        fallDistribution = emptyMap()
                    )
                    
                    val sectorPerformance = SectorPerformance(
                        topRisers = emptyList(),
                        topFallers = emptyList()
                    )
                    
                    val overview = MarketOverview(
                        updateTime = Date(),
                        marketType = MarketType.A_SHARE,
                        indices = marketIndices,
                        stats = stats,
                        sectorPerformance = sectorPerformance
                    )
                    Result.success(overview)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Failed to fetch market overview", e))
        }
    }
    
    override suspend fun searchStocks(query: String): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        // AkShare不提供搜索API
        Result.success(emptyList())
    }
    
    /**
     * 从新浪API获取单个股票行情
     */
    private suspend fun fetchFromSina(symbol: String): Result<RealtimeQuote> = withContext(Dispatchers.IO) {
        try {
            val sinaSymbol = toSinaSymbol(symbol)
            val url = "$SINA_BASE_URL=$sinaSymbol"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENTS.random())
                .addHeader("Referer", "http://finance.sina.com.cn")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    DataSourceException.NetworkException("Sina HTTP ${response.code}")
                )
            }
            
            val body = response.body?.string()
                ?: return@withContext Result.failure(DataSourceException.ParseException("Empty response from Sina"))
            
            val quote = parseSinaQuoteResponse(body, symbol)
                ?: return@withContext Result.failure(DataSourceException.ParseException("Failed to parse Sina quote"))
            
            Result.success(quote)
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Sina API error", e))
        }
    }
    
    /**
     * 从腾讯API获取单个股票行情
     */
    private suspend fun fetchFromTencent(symbol: String): Result<RealtimeQuote> = withContext(Dispatchers.IO) {
        try {
            val tencentSymbol = toTencentSymbol(symbol)
            val url = "$TENCENT_BASE_URL=$tencentSymbol"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENTS.random())
                .addHeader("Referer", "http://finance.qq.com")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    DataSourceException.NetworkException("Tencent HTTP ${response.code}")
                )
            }
            
            val body = response.body?.string()
                ?: return@withContext Result.failure(DataSourceException.ParseException("Empty response from Tencent"))
            
            val quote = parseTencentQuoteResponse(body, symbol)
                ?: return@withContext Result.failure(DataSourceException.ParseException("Failed to parse Tencent quote"))
            
            Result.success(quote)
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Tencent API error", e))
        }
    }
    
    /**
     * 转换为新浪符号格式
     */
    private fun toSinaSymbol(symbol: String): String {
        return when {
            symbol.startsWith("6") -> "sh$symbol"
            symbol.startsWith("0") || symbol.startsWith("3") -> "sz$symbol"
            symbol.startsWith("hk") -> "hk$symbol"  // 港股
            symbol.startsWith("us") -> symbol  // 美股
            else -> "sz$symbol"
        }
    }
    
    /**
     * 转换为腾讯符号格式
     */
    private fun toTencentSymbol(symbol: String): String {
        return when {
            symbol.startsWith("6") -> "sh$symbol"
            symbol.startsWith("0") || symbol.startsWith("3") -> "sz$symbol"
            symbol.startsWith("hk") -> "rt_hk$symbol"  // 港股
            symbol.startsWith("us") -> "us$symbol"  // 美股
            else -> "sz$symbol"
        }
    }
    
    /**
     * 解析新浪行情响应
     */
    private fun parseSinaQuoteResponse(response: String, symbol: String): RealtimeQuote? {
        return try {
            // 格式: var hq_str_sh600519="贵州茅台,1866.000,1870.000,..."
            val content = response.trim()
            if (content.isEmpty() || "\"=\"" in content) return null
            
            val dataStart = content.indexOf('"')
            val dataEnd = content.lastIndexOf('"')
            if (dataStart == -1 || dataEnd == -1) return null
            
            val dataStr = content.substring(dataStart + 1, dataEnd)
            val fields = dataStr.split(',')
            
            if (fields.size < 32) return null
            
            // 新浪数据字段顺序：
            // 0:名称 1:今开 2:昨收 3:最新价 4:最高 5:最低 6:买一价 7:卖一价
            // 8:成交量(股) 9:成交额(元) ... 30:日期 31:时间
            val name = fields[0]
            val open = fields[1].toDoubleOrNull() ?: 0.0
            val preClose = fields[2].toDoubleOrNull() ?: 0.0
            val price = fields[3].toDoubleOrNull() ?: 0.0
            val high = fields[4].toDoubleOrNull() ?: 0.0
            val low = fields[5].toDoubleOrNull() ?: 0.0
            val volume = fields[8].toLongOrNull() ?: 0L  // 股数
            val amount = fields[9].toDoubleOrNull() ?: 0.0
            val change = price - preClose
            val changePercent = if (preClose != 0.0) change / preClose * 100 else 0.0
            
            RealtimeQuote(
                symbol = symbol,
                name = name,
                price = price,
                open = open,
                high = high,
                low = low,
                preClose = preClose,
                volume = volume,
                amount = amount,
                change = change,
                changePercent = changePercent,
                volumeRatio = 1.0,  // 新浪不提供量比
                turnoverRate = 0.0,  // 新浪不提供换手率
                peRatio = 0.0,  // 新浪不提供PE
                pbRatio = 0.0,  // 新浪不提供PB
                marketCap = 0.0,
                circMarketCap = 0.0,
                bidPrice = fields[6].toDoubleOrNull() ?: price,
                bidVolume = 0L,
                askPrice = fields[7].toDoubleOrNull() ?: price,
                askVolume = 0L
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 解析腾讯行情响应
     */
    private fun parseTencentQuoteResponse(response: String, symbol: String): RealtimeQuote? {
        return try {
            // 格式: v_sh600519="1~贵州茅台~600519~1866.00~..."
            val content = response.trim()
            if (content.isEmpty() || "\"=\"" in content) return null
            
            val dataStart = content.indexOf('"')
            val dataEnd = content.lastIndexOf('"')
            if (dataStart == -1 || dataEnd == -1) return null
            
            val dataStr = content.substring(dataStart + 1, dataEnd)
            val fields = dataStr.split('~')
            
            if (fields.size < 45) return null
            
            // 腾讯数据字段顺序：
            // 1:名称 2:代码 3:最新价 4:昨收 5:今开 6:成交量(手) 7:外盘 8:内盘
            // 9-28:买卖五档 30:时间戳 31:涨跌额 32:涨跌幅(%) 33:最高 34:最低 35:收盘/成交量/成交额
            // 38:换手率(%) 39:市盈率(动态) 43:市净率
            val name = fields[1]
            val price = fields[3].toDoubleOrNull() ?: 0.0
            val preClose = fields[4].toDoubleOrNull() ?: 0.0
            val open = fields[5].toDoubleOrNull() ?: 0.0
            val volume = (fields[6].toLongOrNull() ?: 0L) * 100  // 手转股
            val high = fields[33].toDoubleOrNull() ?: 0.0
            val low = fields[34].toDoubleOrNull() ?: 0.0
            val change = fields[31].toDoubleOrNull() ?: (price - preClose)
            val changePercent = fields[32].toDoubleOrNull() ?: (if (preClose != 0.0) change / preClose * 100 else 0.0)
            val turnoverRate = fields[38].toDoubleOrNull() ?: 0.0
            val peRatio = fields[39].toDoubleOrNull() ?: 0.0
            val pbRatio = fields[43].toDoubleOrNull() ?: 0.0
            
            // 估算成交额: 最新价 * 成交量
            val amount = price * volume
            
            RealtimeQuote(
                symbol = symbol,
                name = name,
                price = price,
                open = open,
                high = high,
                low = low,
                preClose = preClose,
                volume = volume,
                amount = amount,
                change = change,
                changePercent = changePercent,
                volumeRatio = 1.0,  // 腾讯不提供量比
                turnoverRate = turnoverRate,
                peRatio = peRatio,
                pbRatio = pbRatio,
                marketCap = 0.0,
                circMarketCap = 0.0,
                bidPrice = price,
                bidVolume = 0L,
                askPrice = price,
                askVolume = 0L
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 解析新浪批量行情响应
     */
    private fun parseSinaBatchQuotes(response: String, symbols: List<String>): List<RealtimeQuote> {
        return try {
            val quotes = mutableListOf<RealtimeQuote>()
            val lines = response.trim().lines()
            
            lines.forEachIndexed { index, line ->
                if (index >= symbols.size) return@forEachIndexed
                
                val symbol = symbols[index]
                val quote = parseSinaQuoteResponse(line, symbol)
                quote?.let { quotes.add(it) }
            }
            
            quotes
        } catch (e: Exception) {
            emptyList()
        }
    }
}