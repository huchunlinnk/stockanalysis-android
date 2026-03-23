package com.example.stockanalysis.data.datasource

import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/**
 * Yahoo Finance 数据源 (Priority 2)
 * 用于获取美股实时行情和历史数据
 * 支持美股股票和美股指数（如 ^GSPC, ^DJI, ^IXIC）
 */
@Singleton
class YFinanceDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) : StockDataSource {

    override val name: String = "YFinance"
    override val priority: Int = 2  // 美股数据源优先级
    override var isHealthy: Boolean = true

    companion object {
        // Yahoo Finance API 基础 URL
        private const val QUOTE_URL = "https://query1.finance.yahoo.com/v8/finance/chart"
        private const val SEARCH_URL = "https://query1.finance.yahoo.com/v1/finance/search"
        
        // User-Agent 池，用于随机轮换
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
        )
        
        // USD to CNY 汇率（简化实现，实际应用中应从汇率 API 获取）
        private const val USD_TO_CNY_RATE = 7.2
        
        // K线数据周期映射
        private val INTERVAL_MAP = mapOf(
            "1d" to "1d",   // 日线
            "1wk" to "1wk", // 周线
            "1mo" to "1mo"  // 月线
        )
    }

    override suspend fun fetchQuote(symbol: String): Result<RealtimeQuote> = withContext(Dispatchers.IO) {
        try {
            val yahooSymbol = normalizeSymbol(symbol)
            val url = "$QUOTE_URL/$yahooSymbol?interval=1d&range=1d&includePrePost=false"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENTS.random())
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    DataSourceException.NetworkException("Yahoo Finance HTTP ${response.code}")
                )
            }
            
            val body = response.body?.string()
                ?: return@withContext Result.failure(DataSourceException.ParseException("Empty response from Yahoo Finance"))
            
            val quote = parseQuoteResponse(body, symbol)
                ?: return@withContext Result.failure(DataSourceException.ParseException("Failed to parse Yahoo Finance quote"))
            
            Result.success(quote)
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Yahoo Finance network error", e))
        }
    }

    override suspend fun fetchQuotes(symbols: List<String>): Result<List<RealtimeQuote>> = withContext(Dispatchers.IO) {
        try {
            if (symbols.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            val results = mutableListOf<RealtimeQuote>()
            
            // Yahoo Finance 不支持批量查询，逐个获取
            symbols.forEach { symbol ->
                val result = fetchQuote(symbol)
                result.getOrNull()?.let { results.add(it) }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Batch fetch error", e))
        }
    }

    override suspend fun fetchKLineData(symbol: String, days: Int): Result<List<KLineData>> = withContext(Dispatchers.IO) {
        try {
            val yahooSymbol = normalizeSymbol(symbol)
            // 根据天数确定数据范围
            val range = when {
                days <= 5 -> "5d"
                days <= 30 -> "1mo"
                days <= 90 -> "3mo"
                days <= 180 -> "6mo"
                days <= 365 -> "1y"
                else -> "2y"
            }
            
            val url = "$QUOTE_URL/$yahooSymbol?interval=1d&range=$range&includeAdjustedClose=true"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENTS.random())
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    DataSourceException.NetworkException("Yahoo Finance HTTP ${response.code}")
                )
            }
            
            val body = response.body?.string()
                ?: return@withContext Result.failure(DataSourceException.ParseException("Empty response"))
            
            val klines = parseKLineResponse(body, symbol)
            Result.success(klines.takeLast(days))
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("KLine fetch error", e))
        }
    }

    override suspend fun fetchTechnicalIndicators(symbol: String): Result<TechnicalIndicators> = withContext(Dispatchers.IO) {
        // 从K线数据计算技术指标
        val klineResult = fetchKLineData(symbol, 90)
        
        klineResult.fold(
            onSuccess = { klines ->
                try {
                    val indicators = calculateIndicators(klines)
                    Result.success(indicators)
                } catch (e: Exception) {
                    Result.failure(DataSourceException.ParseException("Failed to calculate indicators", e))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun fetchTrendAnalysis(symbol: String): Result<TrendAnalysis> = withContext(Dispatchers.IO) {
        // 简化实现，基于技术指标生成趋势分析
        val indicatorsResult = fetchTechnicalIndicators(symbol)
        
        indicatorsResult.fold(
            onSuccess = { indicators ->
                val analysis = generateTrendAnalysis(symbol, indicators)
                Result.success(analysis)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun fetchMarketOverview(): Result<MarketOverview> = withContext(Dispatchers.IO) {
        try {
            // 获取主要美股指数
            val indices = listOf("SPX", "DJI", "IXIC")
            val quotesResult = fetchQuotes(indices)
            
            quotesResult.fold(
                onSuccess = { quotes ->
                    val marketIndices = quotes.map { quote ->
                        val indexInfo = USMarketIndex.fromCode(quote.symbol)
                        MarketIndex(
                            name = indexInfo?.chineseName ?: quote.name,
                            symbol = quote.symbol,
                            currentValue = quote.price,
                            change = quote.change,
                            changePercent = quote.changePercent,
                            volume = quote.volume,
                            turnover = quote.amount
                        )
                    }
                    
                    val stats = MarketStats(
                        risingCount = 0,  // 美股不提供这个数据
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
                        marketType = MarketType.US,
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
        try {
            // Yahoo Finance 搜索 API
            val url = "$SEARCH_URL?q=$query&quotesCount=10&newsCount=0&enableFuzzyQuery=false"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENTS.random())
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    DataSourceException.NetworkException("Search HTTP ${response.code}")
                )
            }
            
            val body = response.body?.string()
                ?: return@withContext Result.success(emptyList())
            
            val results = parseSearchResponse(body)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Search error", e))
        }
    }

    /**
     * 将用户输入的代码标准化为 Yahoo Finance 格式
     */
    private fun normalizeSymbol(symbol: String): String {
        return USMarketIndex.toYahooFinanceSymbol(symbol)
    }

    /**
     * 将 USD 价格转换为 CNY（简化实现）
     */
    private fun usdToCny(usdPrice: Double): Double {
        return usdPrice * USD_TO_CNY_RATE
    }

    /**
     * 解析行情响应
     */
    private fun parseQuoteResponse(json: String, originalSymbol: String): RealtimeQuote? {
        return try {
            val root = JSONObject(json)
            val chart = root.optJSONObject("chart") ?: return null
            
            val error = chart.optString("error", null)
            if (error != null) {
                return null
            }
            
            val result = chart.optJSONArray("result")?.optJSONObject(0) ?: return null
            val meta = result.optJSONObject("meta") ?: return null
            
            // 获取股票名称
            val shortName = meta.optString("shortName", "")
            val longName = meta.optString("longName", "")
            val symbol = meta.optString("symbol", originalSymbol)
            val name = when {
                shortName.isNotEmpty() -> shortName
                longName.isNotEmpty() -> longName
                else -> symbol
            }
            
            // 获取最新价格数据
            val regularMarketPrice = meta.optDouble("regularMarketPrice", 0.0)
            val regularMarketPreviousClose = meta.optDouble("previousClose", 0.0)
            val regularMarketOpen = meta.optDouble("regularMarketOpen", 0.0)
            val regularMarketDayHigh = meta.optDouble("regularMarketDayHigh", 0.0)
            val regularMarketDayLow = meta.optDouble("regularMarketDayLow", 0.0)
            val regularMarketVolume = meta.optLong("regularMarketVolume", 0)
            
            // 计算涨跌幅
            val change = regularMarketPrice - regularMarketPreviousClose
            val changePercent = if (regularMarketPreviousClose != 0.0) {
                (change / regularMarketPreviousClose) * 100
            } else 0.0
            
            // 估算成交额
            val amount = regularMarketPrice * regularMarketVolume
            
            RealtimeQuote(
                symbol = originalSymbol,
                name = name,
                price = regularMarketPrice,
                open = regularMarketOpen,
                high = regularMarketDayHigh,
                low = regularMarketDayLow,
                preClose = regularMarketPreviousClose,
                volume = regularMarketVolume,
                amount = amount,
                change = change,
                changePercent = changePercent,
                volumeRatio = null,  // Yahoo Finance 不直接提供量比
                turnoverRate = null,  // 需要额外数据计算换手率
                peRatio = meta.optDouble("trailingPE", 0.0).takeIf { it > 0 },
                pbRatio = null,  // Yahoo Finance 在 quoteType 中提供
                marketCap = meta.optLong("marketCap", 0).toDouble().takeIf { it > 0 },
                circMarketCap = null,
                bidPrice = null,
                bidVolume = null,
                askPrice = null,
                askVolume = null
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析 K 线响应
     */
    private fun parseKLineResponse(json: String, symbol: String): List<KLineData> {
        return try {
            val root = JSONObject(json)
            val chart = root.optJSONObject("chart") ?: return emptyList()
            
            val error = chart.optString("error", null)
            if (error != null) {
                return emptyList()
            }
            
            val result = chart.optJSONArray("result")?.optJSONObject(0) ?: return emptyList()
            val timestampArray = result.optJSONArray("timestamp") ?: return emptyList()
            val indicators = result.optJSONObject("indicators") ?: return emptyList()
            
            // 获取 OHLCV 数据
            val quote = indicators.optJSONObject("quote") ?: return emptyList()
            val opens = quote.optJSONArray("open")
            val highs = quote.optJSONArray("high")
            val lows = quote.optJSONArray("low")
            val closes = quote.optJSONArray("close")
            val volumes = quote.optJSONArray("volume")
            
            if (opens == null || highs == null || lows == null || closes == null || volumes == null) {
                return emptyList()
            }
            
            val klines = mutableListOf<KLineData>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            for (i in 0 until timestampArray.length()) {
                val timestamp = timestampArray.optLong(i, 0)
                if (timestamp == 0L) continue
                
                val open = opens.optDouble(i, 0.0)
                val high = highs.optDouble(i, 0.0)
                val low = lows.optDouble(i, 0.0)
                val close = closes.optDouble(i, 0.0)
                val volume = volumes.optLong(i, 0)
                
                if (open == 0.0 && close == 0.0) continue
                
                val date = Date(timestamp * 1000)
                val change = close - open
                val changePercent = if (open != 0.0) (change / open) * 100 else 0.0
                val amount = close * volume
                
                klines.add(KLineData(
                    symbol = symbol,
                    timestamp = date,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    amount = amount,
                    change = change,
                    changePercent = changePercent,
                    source = "yfinance"
                ))
            }
            
            klines
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 解析搜索响应
     */
    private fun parseSearchResponse(json: String): List<Pair<String, String>> {
        return try {
            val root = JSONObject(json)
            val quotes = root.optJSONArray("quotes") ?: return emptyList()
            
            val results = mutableListOf<Pair<String, String>>()
            
            for (i in 0 until quotes.length()) {
                val item = quotes.optJSONObject(i) ?: continue
                
                val symbol = item.optString("symbol", "")
                val shortName = item.optString("shortname", "")
                val longName = item.optString("longname", "")
                val name = when {
                    longName.isNotEmpty() -> longName
                    shortName.isNotEmpty() -> shortName
                    else -> symbol
                }
                
                // 只返回股票和 ETF，排除其他类型
                val quoteType = item.optString("quoteType", "")
                if (symbol.isNotEmpty() && name.isNotEmpty() && 
                    (quoteType == "EQUITY" || quoteType == "ETF")) {
                    results.add(symbol to name)
                }
            }
            
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 计算技术指标
     */
    private fun calculateIndicators(klines: List<KLineData>): TechnicalIndicators {
        val closes = klines.map { it.close }
        val volumes = klines.map { it.volume.toDouble() }
        
        // 计算均线
        val ma5 = calculateMA(closes, 5)
        val ma10 = calculateMA(closes, 10)
        val ma20 = calculateMA(closes, 20)
        val ma60 = calculateMA(closes, 60)
        
        // 计算MACD
        val macd = calculateMACD(closes)
        
        // 计算RSI
        val rsi6 = calculateRSI(closes, 6)
        
        // 计算KDJ
        val kdj = calculateKDJ(klines)
        
        val symbol = klines.firstOrNull()?.symbol ?: ""
        
        return TechnicalIndicators(
            symbol = symbol,
            timestamp = Date(),
            movingAverages = MovingAverages(
                symbol = symbol,
                ma5 = ma5,
                ma10 = ma10,
                ma20 = ma20,
                ma60 = ma60
            ),
            macd = macd,
            rsi6 = rsi6,
            rsi12 = calculateRSI(closes, 12),
            rsi24 = calculateRSI(closes, 24),
            kdj = kdj,
            boll = calculateBOLL(closes),
            volumeMa5 = calculateMA(volumes, 5),
            volumeMa10 = calculateMA(volumes, 10)
        )
    }

    /**
     * 生成趋势分析
     */
    private fun generateTrendAnalysis(symbol: String, indicators: TechnicalIndicators): TrendAnalysis {
        val reasons = mutableListOf<String>()
        val risks = mutableListOf<String>()
        
        // 基于均线判断趋势
        val ma5 = indicators.movingAverages?.ma5
        val ma10 = indicators.movingAverages?.ma10
        val ma20 = indicators.movingAverages?.ma20
        
        val trendStatus = when {
            ma5 != null && ma10 != null && ma20 != null && 
            ma5 > ma10 && ma10 > ma20 -> {
                reasons.add("均线多头排列，趋势向上")
                TrendStatus.STRONG_UP
            }
            ma5 != null && ma10 != null && ma20 != null && 
            ma5 < ma10 && ma10 < ma20 -> {
                risks.add("均线空头排列，趋势向下")
                TrendStatus.STRONG_DOWN
            }
            ma5 != null && ma10 != null && ma5 > ma10 -> {
                reasons.add("短期均线向上")
                TrendStatus.UP
            }
            ma5 != null && ma10 != null && ma5 < ma10 -> {
                risks.add("短期均线向下")
                TrendStatus.DOWN
            }
            else -> TrendStatus.SIDEWAYS
        }
        
        // 基于 MACD 判断
        val macd = indicators.macd
        if (macd != null) {
            if (macd.dif > macd.dea && macd.macd > 0) {
                reasons.add("MACD 金叉，动能向上")
            } else if (macd.dif < macd.dea && macd.macd < 0) {
                risks.add("MACD 死叉，动能向下")
            }
        }
        
        // 基于 RSI 判断
        val rsi6 = indicators.rsi6
        if (rsi6 != null) {
            when {
                rsi6 > 80 -> risks.add("RSI 超买，注意回调风险")
                rsi6 < 20 -> reasons.add("RSI 超卖，可能存在反弹机会")
                rsi6 > 50 -> reasons.add("RSI 强势区域")
                else -> risks.add("RSI 弱势区域")
            }
        }
        
        // 计算信号评分
        val signalScore = when {
            reasons.size > risks.size -> 70 + (reasons.size - risks.size) * 5
            reasons.size < risks.size -> 30 - (risks.size - reasons.size) * 5
            else -> 50
        }.coerceIn(0, 100)
        
        val buySignal = when {
            signalScore >= 80 -> BuySignal.STRONG_BUY
            signalScore >= 60 -> BuySignal.BUY
            signalScore >= 40 -> BuySignal.NEUTRAL
            signalScore >= 20 -> BuySignal.SELL
            else -> BuySignal.STRONG_SELL
        }
        
        return TrendAnalysis(
            symbol = symbol,
            trendStatus = trendStatus,
            trendStrength = signalScore.toDouble(),
            buySignal = buySignal,
            signalScore = signalScore,
            signalReasons = reasons,
            riskFactors = risks,
            volumeStatus = VolumeStatus.NORMAL
        )
    }

    // ===== 技术指标计算工具方法 =====
    
    private fun calculateMA(data: List<Double>, period: Int): Double? {
        if (data.size < period) return null
        return data.takeLast(period).average()
    }

    private fun calculateMACD(closes: List<Double>): MacdData? {
        if (closes.size < 26) return null
        
        val ema12 = calculateEMA(closes, 12)
        val ema26 = calculateEMA(closes, 26)
        
        if (ema12.isEmpty() || ema26.isEmpty()) return null
        
        val dif = ema12.zip(ema26).map { it.first - it.second }
        val dea = calculateEMA(dif, 9)
        
        if (dif.isEmpty() || dea.isEmpty()) return null
        
        val macd = dif.zip(dea).map { (it.first - it.second) * 2 }
        
        return MacdData(
            dif = round(dif.lastOrNull() ?: 0.0),
            dea = round(dea.lastOrNull() ?: 0.0),
            macd = round(macd.lastOrNull() ?: 0.0)
        )
    }

    private fun calculateEMA(data: List<Double>, period: Int): List<Double> {
        if (data.size < period) return emptyList()
        
        val multiplier = 2.0 / (period + 1)
        val ema = mutableListOf<Double>()
        
        // 初始值使用SMA
        var emaValue = data.take(period).average()
        ema.add(emaValue)
        
        for (i in period until data.size) {
            emaValue = (data[i] - emaValue) * multiplier + emaValue
            ema.add(emaValue)
        }
        
        return ema
    }

    private fun calculateRSI(closes: List<Double>, period: Int): Double? {
        if (closes.size < period + 1) return null
        
        var gains = 0.0
        var losses = 0.0
        
        for (i in closes.size - period until closes.size) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) gains += change
            else losses -= change
        }
        
        if (losses == 0.0) return 100.0
        
        val rs = gains / losses
        return round(100.0 - (100.0 / (1.0 + rs)))
    }

    private fun calculateKDJ(klines: List<KLineData>): KdjData? {
        if (klines.size < 9) return null
        
        val period = 9
        val recentData = klines.takeLast(period)
        
        val highestHigh = recentData.maxOfOrNull { it.high } ?: return null
        val lowestLow = recentData.minOfOrNull { it.low } ?: return null
        val close = recentData.last().close
        
        val rsv = if (highestHigh != lowestLow) {
            (close - lowestLow) / (highestHigh - lowestLow) * 100
        } else 50.0
        
        // 简化计算
        val k = round(rsv)
        val d = round(rsv)
        val j = round(3 * k - 2 * d)
        
        return KdjData(k, d, j)
    }

    private fun calculateBOLL(closes: List<Double>): BollData? {
        if (closes.size < 20) return null
        
        val period = 20
        val stdDevMultiplier = 2.0
        
        val recentCloses = closes.takeLast(period)
        val ma = recentCloses.average()
        
        val variance = recentCloses.map { (it - ma) * (it - ma) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        
        return BollData(
            upper = round(ma + stdDevMultiplier * stdDev),
            middle = round(ma),
            lower = round(ma - stdDevMultiplier * stdDev)
        )
    }
}
