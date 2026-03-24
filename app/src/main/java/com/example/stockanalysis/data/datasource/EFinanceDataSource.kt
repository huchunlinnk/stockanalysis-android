package com.example.stockanalysis.data.datasource

import android.util.Log
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
 * 东方财富数据源 (Priority 0)
 * 通过东方财富API获取实时行情数据、市场统计、板块数据
 * 搜索功能支持东方财富API + 新浪API作为备用
 */
@Singleton
class EFinanceDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) : StockDataSource {
    
    override val name: String = "EFinance"
    override val priority: Int = 0  // 最高优先级
    override var isHealthy: Boolean = true
    
    companion object {
        const val TAG = "EFinanceDataSource"
        const val BASE_URL = "https://push2.eastmoney.com/api/qt/stock/get"
        const val BATCH_URL = "https://push2.eastmoney.com/api/qt/ulist.np/get"
        const val KLINE_URL = "https://push2.eastmoney.com/api/qt/stock/kline/get"
        // 板块数据URL
        const val SECTOR_URL = "https://push2.eastmoney.com/api/qt/clist/get"
        // 市场统计URL
        const val MARKET_STATS_URL = "https://push2.eastmoney.com/api/qt/uzqt/get"
        // 涨跌停统计URL
        const val LIMIT_UP_DOWN_URL = "https://push2.eastmoney.com/api/qt/stock/get"
        // 新浪搜索API（作为备用）
        const val SINA_SUGGEST_URL = "https://suggest3.sinajs.cn/suggest"
    }
    
    override suspend fun fetchQuote(symbol: String): Result<RealtimeQuote> = withContext(Dispatchers.IO) {
        try {
            val secId = getSecId(symbol)
            val url = "$BASE_URL?secid=$secId&fields=f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f55,f57,f58,f60,f84,f85,f170,f171,f162,f167,f116,f117"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    DataSourceException.NetworkException("HTTP ${response.code}")
                )
            }
            
            val body = response.body?.string()
                ?: return@withContext Result.failure(DataSourceException.ParseException("Empty response"))
            
            val quote = parseQuoteResponse(body, symbol)
                ?: return@withContext Result.failure(DataSourceException.ParseException("Failed to parse quote"))
            
            Result.success(quote)
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Network error", e))
        }
    }
    
    override suspend fun fetchQuotes(symbols: List<String>): Result<List<RealtimeQuote>> = withContext(Dispatchers.IO) {
        try {
            if (symbols.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            // 分批获取，每批最多20只
            val batchSize = 20
            val results = mutableListOf<RealtimeQuote>()
            
            symbols.chunked(batchSize).forEach { batch ->
                val secIds = batch.joinToString(",") { getSecId(it) }
                val fields = "f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f55,f57,f58,f60,f84,f85,f170,f171,f162,f167,f116,f117"
                val url = "$BATCH_URL?secids=$secIds&fields=$fields"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    body?.let { parseBatchQuotes(it, batch) }?.let { results.addAll(it) }
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Network error", e))
        }
    }
    
    override suspend fun fetchKLineData(symbol: String, days: Int): Result<List<KLineData>> = withContext(Dispatchers.IO) {
        try {
            val secId = getSecId(symbol)
            val endDate = Date()
            val startCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days - 30) }
            val startDate = startCalendar.time
            
            val fmt = java.text.SimpleDateFormat("yyyyMMdd", Locale.CHINA)
            val url = "$KLINE_URL?secid=$secId&fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&klt=101&fqt=0&beg=${fmt.format(startDate)}&end=${fmt.format(endDate)}"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    DataSourceException.NetworkException("HTTP ${response.code}")
                )
            }
            
            val body = response.body?.string()
                ?: return@withContext Result.failure(DataSourceException.ParseException("Empty response"))
            
            val klines = parseKLineResponse(body, symbol)
            Result.success(klines.takeLast(days))
        } catch (e: Exception) {
            Result.failure(DataSourceException.NetworkException("Network error", e))
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
        // 简化实现，实际应从数据源获取
        Result.failure(DataSourceException.ServiceUnavailableException("Not implemented"))
    }
    
    override suspend fun fetchMarketOverview(): Result<MarketOverview> = withContext(Dispatchers.IO) {
        try {
            // 1. 获取主要指数
            val indices = fetchMarketIndicesInternal()
            
            // 2. 获取市场统计（涨跌家数等）
            val stats = fetchMarketStatsInternal()
            
            // 3. 获取板块涨跌数据
            val sectorPerformance = fetchSectorPerformanceInternal()
            
            // 4. 获取港股指数
            val hkIndices = fetchHKMarketIndices()
            
            // 5. 合并所有指数
            val allIndices = mutableListOf<MarketIndex>()
            allIndices.addAll(indices)
            allIndices.addAll(hkIndices)
            
            val overview = MarketOverview(
                updateTime = Date(),
                marketType = MarketType.A_SHARE,
                indices = allIndices,
                stats = stats,
                sectorPerformance = sectorPerformance
            )
            
            Result.success(overview)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch market overview", e)
            Result.failure(DataSourceException.NetworkException("Failed to fetch market overview", e))
        }
    }
    
    /**
     * 获取市场指数（A股主要指数）
     */
    private suspend fun fetchMarketIndicesInternal(): List<MarketIndex> {
        val indexSymbols = listOf(
            "1.000001" to "上证指数",
            "0.399001" to "深证成指", 
            "0.399006" to "创业板指",
            "1.000688" to "科创50",
            "0.399300" to "沪深300",
            "0.399905" to "中证500"
        )
        
        val results = mutableListOf<MarketIndex>()
        
        indexSymbols.forEach { (secId, name) ->
            try {
                val url = "$BASE_URL?secid=$secId&fields=f43,f44,f45,f46,f47,f48,f57,f58,f60,f170"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body == null) return@forEach
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data")
                    if (data == null) return@forEach
                    
                    val symbol = data.optString("f57", "")
                    val currentValue = data.optDouble("f43", 0.0) / 100.0
                    val change = data.optDouble("f60", 0.0) / 100.0
                    val changePercent = data.optDouble("f170", 0.0) / 100.0
                    val volume = data.optLong("f47", 0L) * 100
                    val turnover = data.optDouble("f48", 0.0)
                    
                    results.add(MarketIndex(
                        name = name,
                        symbol = symbol,
                        currentValue = currentValue,
                        change = change,
                        changePercent = changePercent,
                        volume = volume,
                        turnover = turnover,
                        marketType = MarketType.A_SHARE
                    ))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch index $name", e)
            }
        }
        
        return results
    }
    
    /**
     * 获取港股市场指数
     */
    private suspend fun fetchHKMarketIndices(): List<MarketIndex> {
        // 港股指数代码映射（使用东方财富的港股指数API）
        val hkIndexSymbols = listOf(
            "116.HSI" to "恒生指数",
            "116.HSCEI" to "国企指数",
            "116.HSCCI" to "红筹指数",
            "116.HSTECH" to "恒生科技"
        )
        
        val results = mutableListOf<MarketIndex>()
        
        hkIndexSymbols.forEach { (secId, name) ->
            try {
                val url = "$BASE_URL?secid=$secId&fields=f43,f44,f45,f46,f47,f48,f57,f58,f60,f170"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body == null) return@forEach
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data")
                    if (data == null) return@forEach
                    
                    val symbol = data.optString("f57", "")
                    val currentValue = data.optDouble("f43", 0.0) / 100.0
                    val change = data.optDouble("f60", 0.0) / 100.0
                    val changePercent = data.optDouble("f170", 0.0) / 100.0
                    val volume = data.optLong("f47", 0L)
                    val turnover = data.optDouble("f48", 0.0)
                    
                    results.add(MarketIndex(
                        name = name,
                        symbol = symbol,
                        currentValue = currentValue,
                        change = change,
                        changePercent = changePercent,
                        volume = volume,
                        turnover = turnover,
                        marketType = MarketType.HK
                    ))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch HK index $name", e)
            }
        }
        
        return results
    }
    
    /**
     * 获取市场统计（涨跌家数等）
     */
    private suspend fun fetchMarketStatsInternal(): MarketStats {
        try {
            // 获取A股市场涨跌统计
            // 使用东方财富的涨跌家数接口
            val risingUrl = "https://push2ex.eastmoney.com/getTopicZDFenBu?ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wz.ztzt&Pageindex=0&pagesize=5000&sort=f3:desc&date=${getCurrentDate()}"
            val fallingUrl = "https://push2ex.eastmoney.com/getTopicZDFenBu?ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wz.ztzt&Pageindex=0&pagesize=5000&sort=f3:asc&date=${getCurrentDate()}"
            
            var risingCount = 0
            var fallingCount = 0
            var flatCount = 0
            var limitUpCount = 0
            var limitDownCount = 0
            
            // 尝试获取涨跌家数
            try {
                val statsUrl = "https://push2.eastmoney.com/api/qt/uzqt/get?ut=fa5fd1943c7b386f172d6893dbfba10b"
                val request = Request.Builder()
                    .url(statsUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    body?.let { parseMarketStats(it) }?.let { stats ->
                        return stats
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch market stats from primary API", e)
            }
            
            // 使用板块数据计算市场统计
            val sectorUrl = "$SECTOR_URL?pn=1&pz=500&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f20&fs=m:0+t:6,m:0+t:13,m:0+t:80,m:1+t:2,m:1+t:23&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f22,f11,f62,f128,f136,f115,f152"
            
            val request = Request.Builder()
                .url(sectorUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string()
                body?.let { json ->
                    val root = JSONObject(json)
                    val data = root.optJSONObject("data")
                    val diff = data?.optJSONArray("diff")
                    
                    if (diff != null) {
                        for (i in 0 until diff.length()) {
                            val item = diff.optJSONObject(i) ?: continue
                            val changePercent = item.optDouble("f3", 0.0)
                            
                            when {
                                changePercent >= 990 -> limitUpCount++ // 涨停（考虑科创板10%和20%的涨幅）
                                changePercent <= -990 -> limitDownCount++ // 跌停
                                changePercent > 0 -> risingCount++
                                changePercent < 0 -> fallingCount++
                                else -> flatCount++
                            }
                        }
                    }
                }
            }
            
            // 构建涨跌分布
            val riseDistribution = mapOf(
                ">7%" to 0, "5-7%" to 0, "3-5%" to 0, "0-3%" to risingCount
            )
            val fallDistribution = mapOf(
                "0-3%" to fallingCount, "3-5%" to 0, "5-7%" to 0, ">7%" to 0
            )
            
            return MarketStats(
                risingCount = risingCount,
                fallingCount = fallingCount,
                flatCount = flatCount,
                limitUpCount = limitUpCount,
                limitDownCount = limitDownCount,
                riseDistribution = riseDistribution,
                fallDistribution = fallDistribution
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch market stats", e)
            // 返回默认空统计
            return MarketStats(
                risingCount = 0,
                fallingCount = 0,
                flatCount = 0,
                limitUpCount = 0,
                limitDownCount = 0,
                riseDistribution = emptyMap(),
                fallDistribution = emptyMap()
            )
        }
    }
    
    /**
     * 解析市场统计数据
     */
    private fun parseMarketStats(json: String): MarketStats? {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data") ?: return null
            
            // 解析涨跌家数
            val risingCount = data.optInt("f107", 0)
            val fallingCount = data.optInt("f108", 0)
            val flatCount = data.optInt("f109", 0)
            val limitUpCount = data.optInt("f110", 0)
            val limitDownCount = data.optInt("f111", 0)
            
            MarketStats(
                risingCount = risingCount,
                fallingCount = fallingCount,
                flatCount = flatCount,
                limitUpCount = limitUpCount,
                limitDownCount = limitDownCount,
                riseDistribution = emptyMap(),
                fallDistribution = emptyMap()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse market stats", e)
            null
        }
    }
    
    /**
     * 获取板块表现数据
     */
    private suspend fun fetchSectorPerformanceInternal(): SectorPerformance {
        try {
            // 获取行业板块涨跌数据
            val url = "$SECTOR_URL?pn=1&pz=100&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f20&fs=m:90+t:2&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f22,f11,f62,f128,f136,f115,f152"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return SectorPerformance(emptyList(), emptyList())
            }
            
            val body = response.body?.string() ?: return SectorPerformance(emptyList(), emptyList())
            
            val sectors = parseSectorData(body)
            
            // 排序获取领涨和领跌板块
            val sortedSectors = sectors.sortedByDescending { it.changePercent }
            val topRisers = sortedSectors.take(5)
            val topFallers = sortedSectors.takeLast(5).reversed()
            
            return SectorPerformance(topRisers, topFallers)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch sector performance", e)
            return SectorPerformance(emptyList(), emptyList())
        }
    }
    
    /**
     * 解析板块数据
     */
    private fun parseSectorData(json: String): List<SectorInfo> {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data")
            val diff = data?.optJSONArray("diff") ?: return emptyList()
            
            val sectors = mutableListOf<SectorInfo>()
            
            for (i in 0 until diff.length()) {
                val item = diff.optJSONObject(i) ?: continue
                
                val name = item.optString("f14", "")
                val changePercent = item.optDouble("f3", 0.0) / 100.0
                
                // 获取领涨股列表（如果有）
                val leadingStocks = mutableListOf<String>()
                // 从其他字段解析领涨股信息
                
                if (name.isNotEmpty()) {
                    sectors.add(SectorInfo(
                        name = name,
                        changePercent = changePercent,
                        leadingStocks = leadingStocks
                    ))
                }
            }
            
            sectors
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse sector data", e)
            emptyList()
        }
    }
    
    /**
     * 获取当前日期字符串（YYYYMMDD）
     */
    private fun getCurrentDate(): String {
        val fmt = java.text.SimpleDateFormat("yyyyMMdd", Locale.CHINA)
        return fmt.format(Date())
    }
    
    /**
     * 搜索股票（增强版，支持多API回退）
     * 
     * 策略：
     * 1. 首先尝试东方财富搜索API
     * 2. 如果失败或结果为空，使用新浪搜索API作为备用
     * 3. 合并结果并去重
     */
    override suspend fun searchStocks(query: String): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Pair<String, String>>()
        var primaryFailed = false
        
        // 1. 首先尝试东方财富搜索API
        try {
            Log.d(TAG, "Trying Eastmoney search for query: $query")
            val eastmoneyResults = searchEastmoney(query)
            if (eastmoneyResults.isNotEmpty()) {
                results.addAll(eastmoneyResults)
                Log.d(TAG, "Eastmoney search returned ${eastmoneyResults.size} results")
            } else {
                Log.d(TAG, "Eastmoney search returned empty results, will try Sina")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Eastmoney search failed: ${e.message}")
            primaryFailed = true
        }
        
        // 2. 如果东方财富失败或结果为空，尝试新浪搜索API
        if (results.isEmpty() || primaryFailed) {
            try {
                Log.d(TAG, "Trying Sina search for query: $query")
                val sinaResults = searchSina(query)
                if (sinaResults.isNotEmpty()) {
                    results.addAll(sinaResults)
                    Log.d(TAG, "Sina search returned ${sinaResults.size} results")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sina search failed: ${e.message}")
            }
        }
        
        // 3. 去重并按代码排序
        val uniqueResults = results
            .distinctBy { it.first }
            .sortedBy { it.first }
            .take(20) // 最多返回20条
        
        return@withContext if (uniqueResults.isNotEmpty()) {
            Result.success(uniqueResults)
        } else {
            Result.failure(DataSourceException.ServiceUnavailableException("No search results found from any source"))
        }
    }
    
    /**
     * 东方财富搜索API
     */
    private suspend fun searchEastmoney(query: String): List<Pair<String, String>> {
        val url = "https://searchapi.eastmoney.com/api/suggest/get?input=$query&type=14&count=10"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw DataSourceException.NetworkException("HTTP ${response.code}")
        }
        
        val body = response.body?.string()
            ?: return emptyList()
        
        return parseEastmoneySearchResponse(body)
    }
    
    /**
     * 新浪搜索API（备用）
     * API: https://suggest3.sinajs.cn/suggest/type=11&key=$query
     * 
     * 返回格式: var suggestvalue="..."
     * 数据格式: symbol,name,py,type,...
     * 例如: "600519,贵州茅台,gzmt,11,..."
     * 
     * type说明:
     * - 11: A股
     * - 12: B股  
     * - 21: 美股
     * - 22: 港股
     * - 31: 基金
     */
    private suspend fun searchSina(query: String): List<Pair<String, String>> {
        // type=11 搜索A股，可根据需要调整
        val url = "$SINA_SUGGEST_URL?type=11&key=$query"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Referer", "https://finance.sina.com.cn")
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw DataSourceException.NetworkException("HTTP ${response.code}")
        }
        
        val body = response.body?.string()
            ?: return emptyList()
        
        return parseSinaSearchResponse(body)
    }
    
    /**
     * 解析新浪搜索响应
     * 
     * 响应格式示例:
     * var suggestvalue="600519,贵州茅台,gzmt,11;000001,平安银行,payh,11;"
     * 
     * 每个条目格式: symbol,name,pinyin,type
     */
    private fun parseSinaSearchResponse(response: String): List<Pair<String, String>> {
        return try {
            val results = mutableListOf<Pair<String, String>>()
            
            // 提取JSON/文本内容
            // 响应格式: var suggestvalue="..."
            val content = if (response.contains("=")) {
                response.substringAfter("=").trim().trim('"', ';')
            } else {
                response.trim()
            }
            
            if (content.isEmpty()) {
                return emptyList()
            }
            
            // 解析条目（分号分隔）
            val items = content.split(";")
            
            for (item in items) {
                val parts = item.split(",")
                if (parts.size >= 2) {
                    val symbol = parts[0].trim()
                    val name = parts[1].trim()
                    
                    // 过滤无效数据
                    if (symbol.isNotEmpty() && name.isNotEmpty() && !name.contains(" ")) {
                        results.add(symbol to name)
                    }
                }
            }
            
            Log.d(TAG, "Parsed ${results.size} results from Sina response")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Sina search response", e)
            emptyList()
        }
    }
    
    /**
     * 获取SecId (0-深圳, 1-上海, 116-港股, 其他-美股)
     */
    private fun getSecId(symbol: String): String {
        return when {
            // 港股
            symbol.length == 5 && symbol.all { it.isDigit() } -> "116.$symbol"
            // 上证指数、科创板
            symbol.startsWith("6") || symbol.startsWith("688") || symbol.startsWith("000") || symbol.startsWith("510") -> "1.$symbol"
            // 深证成指、创业板
            symbol.startsWith("0") || symbol.startsWith("3") || symbol.startsWith("159") || symbol.startsWith("399") -> "0.$symbol"
            // 美股或其他
            else -> symbol
        }
    }
    
    /**
     * 解析行情响应
     */
    private fun parseQuoteResponse(json: String, symbol: String): RealtimeQuote? {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data") ?: return null
            
            // 获取股票名称
            val name = data.optString("f58", "Unknown")
            
            val price = data.optInt("f43", 0) / 100.0
            val high = data.optInt("f44", 0) / 100.0
            val low = data.optInt("f45", 0) / 100.0
            val open = data.optInt("f46", 0) / 100.0
            val preClose = data.optInt("f55", 0) / 100.0
            val volume = data.optLong("f47", 0) * 100  // 手转股
            val amount = data.optDouble("f48", 0.0)
            val change = data.optInt("f60", 0) / 100.0
            val changePercent = data.optDouble("f170", 0.0) / 100.0
            
            // 计算其他字段
            val turnoverRate = if (preClose > 0) {
                (volume / data.optDouble("f85", 1.0)) * 100
            } else 0.0
            
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
                volumeRatio = 1.0,  // 需要历史数据计算
                turnoverRate = round(turnoverRate * 100) / 100,
                peRatio = data.optDouble("f162", 0.0),
                pbRatio = data.optDouble("f167", 0.0),
                marketCap = data.optLong("f116", 0).toDouble(),
                circMarketCap = data.optLong("f117", 0).toDouble(),
                bidPrice = price - 0.01,
                bidVolume = data.optLong("f51", 0),
                askPrice = price + 0.01,
                askVolume = data.optLong("f52", 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse quote response", e)
            null
        }
    }
    
    /**
     * 解析批量行情响应
     */
    private fun parseBatchQuotes(json: String, symbols: List<String>): List<RealtimeQuote> {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data")
            val diff = data?.optJSONArray("diff") ?: return emptyList()
            
            val quotes = mutableListOf<RealtimeQuote>()
            
            for (i in 0 until diff.length()) {
                val item = diff.optJSONObject(i) ?: continue
                val symbol = item.optString("f57", "")
                
                val quote = RealtimeQuote(
                    symbol = symbol,
                    name = item.optString("f58", "Unknown"),
                    price = item.optInt("f43", 0) / 100.0,
                    open = item.optInt("f46", 0) / 100.0,
                    high = item.optInt("f44", 0) / 100.0,
                    low = item.optInt("f45", 0) / 100.0,
                    preClose = item.optInt("f55", 0) / 100.0,
                    volume = item.optLong("f47", 0) * 100,
                    amount = item.optDouble("f48", 0.0),
                    change = item.optInt("f60", 0) / 100.0,
                    changePercent = item.optDouble("f170", 0.0) / 100.0,
                    volumeRatio = 1.0,
                    turnoverRate = item.optDouble("f168", 0.0),
                    peRatio = item.optDouble("f162", 0.0),
                    pbRatio = item.optDouble("f167", 0.0),
                    marketCap = item.optLong("f116", 0).toDouble(),
                    circMarketCap = item.optLong("f117", 0).toDouble(),
                    bidPrice = item.optInt("f43", 0) / 100.0 - 0.01,
                    bidVolume = item.optLong("f51", 0),
                    askPrice = item.optInt("f43", 0) / 100.0 + 0.01,
                    askVolume = item.optLong("f52", 0)
                )
                
                quotes.add(quote)
            }
            
            quotes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse batch quotes", e)
            emptyList()
        }
    }
    
    /**
     * 解析K线响应
     */
    private fun parseKLineResponse(json: String, symbol: String): List<KLineData> {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data")
            val klines = data?.optJSONArray("klines") ?: return emptyList()
            
            val result = mutableListOf<KLineData>()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            
            for (i in 0 until klines.length()) {
                val line = klines.optString(i, "") ?: continue
                val parts = line.split(",")
                if (parts.size < 9) continue
                
                val date = dateFormat.parse(parts[0]) ?: Date()
                val open = parts[1].toDoubleOrNull() ?: 0.0
                val close = parts[2].toDoubleOrNull() ?: 0.0
                val low = parts[3].toDoubleOrNull() ?: 0.0
                val high = parts[4].toDoubleOrNull() ?: 0.0
                val volume = parts[5].toLongOrNull() ?: 0L
                val amount = parts[6].toDoubleOrNull() ?: 0.0
                val changePercent = parts[8].toDoubleOrNull() ?: 0.0
                val change = close - open
                
                result.add(KLineData(
                    symbol = symbol,
                    timestamp = date,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    amount = amount,
                    change = change,
                    changePercent = changePercent
                ))
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse K-line response", e)
            emptyList()
        }
    }
    
    /**
     * 解析东方财富搜索响应
     */
    private fun parseEastmoneySearchResponse(json: String): List<Pair<String, String>> {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONArray("QuotationCodeTable")
                ?: return emptyList()
            
            val results = mutableListOf<Pair<String, String>>()
            
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val code = item.optString("Code", "")
                val name = item.optString("Name", "")
                if (code.isNotEmpty() && name.isNotEmpty()) {
                    results.add(code to name)
                }
            }
            
            results
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Eastmoney search response", e)
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
            boll = calculateBOLL(closes)
        )
    }
    
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
            dif = dif.lastOrNull() ?: 0.0,
            dea = dea.lastOrNull() ?: 0.0,
            macd = macd.lastOrNull() ?: 0.0
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
        return 100.0 - (100.0 / (1.0 + rs))
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
        
        // 简化计算，使用默认初始值
        val k = rsv
        val d = rsv
        val j = 3 * k - 2 * d
        
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
            upper = ma + stdDevMultiplier * stdDev,
            middle = ma,
            lower = ma - stdDevMultiplier * stdDev
        )
    }
}
