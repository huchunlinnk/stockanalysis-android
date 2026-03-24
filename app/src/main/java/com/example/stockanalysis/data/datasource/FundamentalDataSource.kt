package com.example.stockanalysis.data.datasource

import android.util.Log
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

/**
 * 基本面数据源
 * 
 * 提供股票基本面数据获取，包括：
 * - 财务指标（ROE, ROA, 毛利率等）
 * - 成长性指标（营收增长率、净利润增长率等）
 * - 分红信息
 * - 机构持仓数据
 * 
 * 数据来源：
 * 1. 东方财富 API（主要）
 * 2. 新浪财经 API（备用）
 * 3. 腾讯财经 API（备用）
 * 
 * 参照 daily_stock_analysis 的 fundamental_adapter.py 实现
 */
@Singleton
class FundamentalDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        const val TAG = "FundamentalDataSource"
        
        // API 基础地址
        private const val EASTMONEY_BASE_URL = "https://push2.eastmoney.com/api"
        private const val EASTMONEY_F10_URL = "https://f10.eastmoney.com"
        private const val SINA_BASE_URL = "http://hq.sinajs.cn"
        
        // User-Agent 池
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"
        )
    }

    /**
     * 获取完整基本面数据包
     * 
     * 参照 fundamental_adapter.py 的 get_fundamental_bundle 方法
     */
    suspend fun getFundamentalBundle(symbol: String, name: String = ""): Result<FundamentalData> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始获取基本面数据: $symbol")
                
                // 1. 获取财务指标
                val financialIndicators = fetchFinancialIndicators(symbol)
                
                // 2. 获取成长性指标
                val growthMetrics = fetchGrowthMetrics(symbol)
                
                // 3. 获取分红信息
                val dividendInfo = fetchDividendInfo(symbol)
                
                // 4. 获取机构持仓
                val institutionalHolding = fetchInstitutionalHolding(symbol)
                
                // 5. 获取估值指标
                val valuationMetrics = fetchValuationMetrics(symbol)
                
                // 构建完整的基本面数据
                val fundamentalData = FundamentalData(
                    symbol = symbol,
                    name = name,
                    updateTime = Date(),
                    peRatio = valuationMetrics.getOrNull()?.peTtm,
                    pbRatio = valuationMetrics.getOrNull()?.pbRatio,
                    psRatio = valuationMetrics.getOrNull()?.psRatio,
                    financialIndicatorsJson = financialIndicators.getOrNull()?.let { serializeFinancialIndicators(it) },
                    growthMetricsJson = growthMetrics.getOrNull()?.let { serializeGrowthMetrics(it) },
                    dividendInfoJson = dividendInfo.getOrNull()?.let { serializeDividendInfo(it) },
                    institutionalHoldingJson = institutionalHolding.getOrNull()?.let { serializeInstitutionalHolding(it) },
                    source = "eastmoney",
                    isCacheValid = true
                )
                
                Log.d(TAG, "基本面数据获取成功: $symbol")
                Result.success(fundamentalData)
                
            } catch (e: Exception) {
                Log.e(TAG, "获取基本面数据失败: ${e.message}", e)
                Result.failure(Exception("获取基本面数据失败: ${e.message}"))
            }
        }

    /**
     * 获取财务指标
     * 
     * 从东方财富获取财务分析指标
     */
    suspend fun fetchFinancialIndicators(symbol: String): Result<FinancialIndicators> = 
        withContext(Dispatchers.IO) {
            try {
                // 东方财富财务指标 API
                val emCode = convertToEastMoneyCode(symbol)
                val url = "$EASTMONEY_F10_URL/NewFinanceAnalysis/MainTargetAjax?code=$emCode"
                
                val json = fetchJsonFromUrl(url)
                    ?: return@withContext Result.failure(Exception("无法获取财务指标数据"))
                
                // 解析财务指标
                val indicators = parseFinancialIndicators(json, symbol)
                Result.success(indicators)
                
            } catch (e: Exception) {
                Log.e(TAG, "获取财务指标失败: ${e.message}", e)
                // 返回空的财务指标，不阻止整体流程
                Result.success(FinancialIndicators())
            }
        }

    /**
     * 获取成长性指标
     */
    suspend fun fetchGrowthMetrics(symbol: String): Result<GrowthMetrics> = 
        withContext(Dispatchers.IO) {
            try {
                // 尝试从财务指标中提取增长数据
                val emCode = convertToEastMoneyCode(symbol)
                val url = "$EASTMONEY_F10_URL/NewFinanceAnalysis/MainTargetAjax?code=$emCode"
                
                val json = fetchJsonFromUrl(url)
                    ?: return@withContext Result.success(GrowthMetrics())
                
                val metrics = parseGrowthMetrics(json, symbol)
                Result.success(metrics)
                
            } catch (e: Exception) {
                Log.e(TAG, "获取成长性指标失败: ${e.message}", e)
                Result.success(GrowthMetrics())
            }
        }

    /**
     * 获取分红信息
     * 
     * 参照 fundamental_adapter.py 的分红数据解析逻辑
     */
    suspend fun fetchDividendInfo(symbol: String): Result<DividendInfo> = 
        withContext(Dispatchers.IO) {
            try {
                val emCode = convertToEastMoneyCode(symbol)
                val url = "$EASTMONEY_F10_URL/BonusFinancing/BonusFinancingAjax?code=$emCode"
                
                val json = fetchJsonFromUrl(url)
                    ?: return@withContext Result.success(DividendInfo(symbol = symbol))
                
                val dividendInfo = parseDividendInfo(json, symbol)
                Result.success(dividendInfo)
                
            } catch (e: Exception) {
                Log.e(TAG, "获取分红信息失败: ${e.message}", e)
                Result.success(DividendInfo(symbol = symbol))
            }
        }

    /**
     * 获取机构持仓数据
     */
    suspend fun fetchInstitutionalHolding(symbol: String): Result<InstitutionalHolding> = 
        withContext(Dispatchers.IO) {
            try {
                val emCode = convertToEastMoneyCode(symbol)
                
                // 机构持仓摘要
                val url = "$EASTMONEY_F10_URL/ShareholderResearch/ShareholderResearchAjax?code=$emCode"
                
                val json = fetchJsonFromUrl(url)
                    ?: return@withContext Result.success(InstitutionalHolding(symbol = symbol))
                
                val holding = parseInstitutionalHolding(json, symbol)
                Result.success(holding)
                
            } catch (e: Exception) {
                Log.e(TAG, "获取机构持仓失败: ${e.message}", e)
                Result.success(InstitutionalHolding(symbol = symbol))
            }
        }

    /**
     * 获取估值指标
     */
    suspend fun fetchValuationMetrics(symbol: String): Result<ValuationMetrics> = 
        withContext(Dispatchers.IO) {
            try {
                // 使用东方财富实时行情获取 PE/PB
                val market = if (symbol.startsWith("6")) "sh" else "sz"
                val url = "$EASTMONEY_BASE_URL/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&fltt=2&invt=2&volt=2&fields=f43,f44,f45,f46,f47,f48,f57,f58,f60,f84,f85,f116,f117,f162,f167,f170&secid=$market.$symbol"
                
                val json = fetchJsonFromUrl(url)
                    ?: return@withContext Result.success(ValuationMetrics(symbol = symbol))
                
                val data = json.optJSONObject("data")
                    ?: return@withContext Result.success(ValuationMetrics(symbol = symbol))
                
                val metrics = ValuationMetrics(
                    symbol = symbol,
                    peTtm = safeParseDouble(data.opt("f162")),
                    pbRatio = safeParseDouble(data.opt("f167")),
                    psRatio = safeParseDouble(data.opt("f170")),
                    updateTime = Date()
                )
                
                Result.success(metrics)
                
            } catch (e: Exception) {
                Log.e(TAG, "获取估值指标失败: ${e.message}", e)
                Result.success(ValuationMetrics(symbol = symbol))
            }
        }

    // ============ 私有辅助方法 ============

    /**
     * 转换股票代码为东方财富格式
     */
    private fun convertToEastMoneyCode(symbol: String): String {
        return when {
            symbol.startsWith("6") -> "SH$symbol"
            symbol.startsWith("0") || symbol.startsWith("3") -> "SZ$symbol"
            symbol.startsWith("8") || symbol.startsWith("4") -> "BJ$symbol"
            symbol.startsWith("HK") -> symbol
            else -> "SZ$symbol"
        }
    }

    /**
     * 从URL获取JSON
     */
    private suspend fun fetchJsonFromUrl(url: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENTS.random())
                .addHeader("Referer", "https://emweb.securities.eastmoney.com")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP请求失败: ${response.code}")
                return@withContext null
            }
            
            val body = response.body?.string()
            if (body.isNullOrEmpty()) {
                return@withContext null
            }
            
            JSONObject(body)
        } catch (e: Exception) {
            Log.e(TAG, "请求失败: ${e.message}")
            null
        }
    }

    /**
     * 解析财务指标
     */
    private fun parseFinancialIndicators(json: JSONObject, symbol: String): FinancialIndicators {
        return try {
            val data = json.optJSONObject("data")
            val records = data?.optJSONArray("records")
            
            if (records == null || records.length() == 0) {
                return FinancialIndicators()
            }
            
            // 获取最新一期数据
            val latest = records.optJSONArray(0)
            
            FinancialIndicators(
                roe = safeParseDouble(latest?.opt(5)),  // 净资产收益率
                roa = safeParseDouble(latest?.opt(6)),  // 总资产收益率
                grossMargin = safeParseDouble(latest?.opt(7)), // 毛利率
                netMargin = safeParseDouble(latest?.opt(8)),   // 净利率
                debtToEquity = safeParseDouble(latest?.opt(9)), // 资产负债率
                reportDate = latest?.optString(0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析财务指标失败: ${e.message}")
            FinancialIndicators()
        }
    }

    /**
     * 解析成长性指标
     */
    private fun parseGrowthMetrics(json: JSONObject, symbol: String): GrowthMetrics {
        return try {
            val data = json.optJSONObject("data")
            val records = data?.optJSONArray("records")
            
            if (records == null || records.length() < 2) {
                return GrowthMetrics()
            }
            
            // 获取最近两期数据进行同比计算
            val current = records.optJSONArray(0)
            val previous = records.optJSONArray(1)
            
            GrowthMetrics(
                revenueGrowthYoY = safeParseDouble(current?.opt(13)), // 营收同比增长
                netProfitGrowthYoY = safeParseDouble(current?.opt(14)), // 净利润同比增长
                reportDate = current?.optString(0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析成长性指标失败: ${e.message}")
            GrowthMetrics()
        }
    }

    /**
     * 解析分红信息
     * 
     * 参照 fundamental_adapter.py 的分红数据解析
     */
    private fun parseDividendInfo(json: JSONObject, symbol: String): DividendInfo {
        return try {
            val data = json.optJSONObject("data")
            val records = data?.optJSONArray("records")
            
            if (records == null || records.length() == 0) {
                return DividendInfo(symbol = symbol)
            }
            
            val events = mutableListOf<DividendEvent>()
            val now = Date()
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, -1)
            val oneYearAgo = calendar.time
            
            var ttmDividend = 0.0
            var ttmCount = 0
            var consecutiveYears = 0
            var lastYear = -1
            
            for (i in 0 until records.length()) {
                val record = records.optJSONObject(i) ?: continue
                
                val dateStr = record.optString("ReportingDate", "")
                val cashDividend = safeParseDouble(record.opt("BonusItionBeans")) ?: 0.0
                
                if (cashDividend > 0) {
                    val event = DividendEvent(
                        eventDate = dateStr,
                        exDividendDate = record.optString("ExRightExDividendDate"),
                        recordDate = record.optString("RecordDate"),
                        announcementDate = record.optString("NoticeDate"),
                        cashDividendPerShare = cashDividend,
                        isPreTax = true,
                        stockDividend = safeParseDouble(record.opt("Shares"))?.let { it / 10 },
                        capitalIncrease = safeParseDouble(record.opt("IncreaseShares"))?.let { it / 10 }
                    )
                    events.add(event)
                    
                    // 计算TTM分红
                    val eventDate = parseDateSafe(dateStr)
                    if (eventDate != null && eventDate.after(oneYearAgo) && eventDate.before(now)) {
                        ttmDividend += cashDividend
                        ttmCount++
                    }
                    
                    // 计算连续分红年数
                    val year = dateStr.take(4).toIntOrNull() ?: 0
                    if (year > 0) {
                        if (lastYear == -1 || year == lastYear - 1) {
                            consecutiveYears++
                        }
                        lastYear = year
                    }
                }
            }
            
            DividendInfo(
                symbol = symbol,
                dividendHistory = events.sortedByDescending { it.eventDate },
                ttmDividendPerShare = if (ttmCount > 0) ttmDividend else null,
                ttmDividendCount = ttmCount,
                consecutiveYears = consecutiveYears,
                dividendStability = when {
                    consecutiveYears >= 5 -> "稳定"
                    consecutiveYears >= 3 -> "较稳定"
                    consecutiveYears >= 1 -> "不稳定"
                    else -> "无分红"
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析分红信息失败: ${e.message}")
            DividendInfo(symbol = symbol)
        }
    }

    /**
     * 解析机构持仓
     */
    private fun parseInstitutionalHolding(json: JSONObject, symbol: String): InstitutionalHolding {
        return try {
            val data = json.optJSONObject("data")
            val holders = data?.optJSONArray("jgcc")
            
            val top10List = mutableListOf<ShareholderInfo>()
            var totalInstitutionHolding = 0.0
            
            holders?.let {
                for (i in 0 until minOf(it.length(), 10)) {
                    val holder = it.optJSONObject(i) ?: continue
                    
                    val info = ShareholderInfo(
                        name = holder.optString("HOLDER_NAME", ""),
                        holdingShares = safeParseDouble(holder.opt("HOLD_NUM"))?.let { it / 10000 } ?: 0.0,
                        holdingRatio = safeParseDouble(holder.opt("HOLD_RATIO")) ?: 0.0,
                        holdingChange = safeParseDouble(holder.opt("HOLD_CHANGE")),
                        shareholderType = holder.optString("HOLDER_TYPE", "机构")
                    )
                    
                    top10List.add(info)
                    totalInstitutionHolding += info.holdingRatio
                }
            }
            
            InstitutionalHolding(
                symbol = symbol,
                institutionCount = holders?.length(),
                holdingRatio = if (totalInstitutionHolding > 0) totalInstitutionHolding else null,
                top10Holders = top10List,
                top10HoldingRatio = if (totalInstitutionHolding > 0) totalInstitutionHolding else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析机构持仓失败: ${e.message}")
            InstitutionalHolding(symbol = symbol)
        }
    }

    // ============ 序列化方法 ============

    /**
     * 序列化财务指标为JSON
     */
    fun serializeFinancialIndicators(indicators: FinancialIndicators): String {
        return JSONObject().apply {
            put("roe", indicators.roe ?: JSONObject.NULL)
            put("roa", indicators.roa ?: JSONObject.NULL)
            put("grossMargin", indicators.grossMargin ?: JSONObject.NULL)
            put("netMargin", indicators.netMargin ?: JSONObject.NULL)
            put("operatingMargin", indicators.operatingMargin ?: JSONObject.NULL)
            put("debtToEquity", indicators.debtToEquity ?: JSONObject.NULL)
            put("currentRatio", indicators.currentRatio ?: JSONObject.NULL)
            put("quickRatio", indicators.quickRatio ?: JSONObject.NULL)
            put("interestCoverage", indicators.interestCoverage ?: JSONObject.NULL)
            put("inventoryTurnover", indicators.inventoryTurnover ?: JSONObject.NULL)
            put("receivablesTurnover", indicators.receivablesTurnover ?: JSONObject.NULL)
            put("assetTurnover", indicators.assetTurnover ?: JSONObject.NULL)
            put("operatingCashFlow", indicators.operatingCashFlow ?: JSONObject.NULL)
            put("freeCashFlow", indicators.freeCashFlow ?: JSONObject.NULL)
            put("cashFlowPerShare", indicators.cashFlowPerShare ?: JSONObject.NULL)
            put("reportDate", indicators.reportDate ?: JSONObject.NULL)
            put("reportType", indicators.reportType ?: JSONObject.NULL)
        }.toString()
    }

    /**
     * 反序列化财务指标
     */
    fun deserializeFinancialIndicators(json: String): FinancialIndicators {
        return try {
            val obj = JSONObject(json)
            FinancialIndicators(
                roe = if (obj.isNull("roe")) null else obj.optDouble("roe"),
                roa = if (obj.isNull("roa")) null else obj.optDouble("roa"),
                grossMargin = if (obj.isNull("grossMargin")) null else obj.optDouble("grossMargin"),
                netMargin = if (obj.isNull("netMargin")) null else obj.optDouble("netMargin"),
                operatingMargin = if (obj.isNull("operatingMargin")) null else obj.optDouble("operatingMargin"),
                debtToEquity = if (obj.isNull("debtToEquity")) null else obj.optDouble("debtToEquity"),
                currentRatio = if (obj.isNull("currentRatio")) null else obj.optDouble("currentRatio"),
                quickRatio = if (obj.isNull("quickRatio")) null else obj.optDouble("quickRatio"),
                interestCoverage = if (obj.isNull("interestCoverage")) null else obj.optDouble("interestCoverage"),
                inventoryTurnover = if (obj.isNull("inventoryTurnover")) null else obj.optDouble("inventoryTurnover"),
                receivablesTurnover = if (obj.isNull("receivablesTurnover")) null else obj.optDouble("receivablesTurnover"),
                assetTurnover = if (obj.isNull("assetTurnover")) null else obj.optDouble("assetTurnover"),
                operatingCashFlow = if (obj.isNull("operatingCashFlow")) null else obj.optDouble("operatingCashFlow"),
                freeCashFlow = if (obj.isNull("freeCashFlow")) null else obj.optDouble("freeCashFlow"),
                cashFlowPerShare = if (obj.isNull("cashFlowPerShare")) null else obj.optDouble("cashFlowPerShare"),
                reportDate = if (obj.isNull("reportDate")) null else obj.optString("reportDate"),
                reportType = if (obj.isNull("reportType")) null else obj.optString("reportType")
            )
        } catch (e: Exception) {
            FinancialIndicators()
        }
    }

    /**
     * 序列化成长性指标
     */
    fun serializeGrowthMetrics(metrics: GrowthMetrics): String {
        return JSONObject().apply {
            put("revenueGrowthYoY", metrics.revenueGrowthYoY ?: JSONObject.NULL)
            put("revenueGrowthQoQ", metrics.revenueGrowthQoQ ?: JSONObject.NULL)
            put("revenueGrowth3Y", metrics.revenueGrowth3Y ?: JSONObject.NULL)
            put("revenueGrowth5Y", metrics.revenueGrowth5Y ?: JSONObject.NULL)
            put("netProfitGrowthYoY", metrics.netProfitGrowthYoY ?: JSONObject.NULL)
            put("netProfitGrowthQoQ", metrics.netProfitGrowthQoQ ?: JSONObject.NULL)
            put("netProfitGrowth3Y", metrics.netProfitGrowth3Y ?: JSONObject.NULL)
            put("netProfitGrowth5Y", metrics.netProfitGrowth5Y ?: JSONObject.NULL)
            put("grossProfitGrowthYoY", metrics.grossProfitGrowthYoY ?: JSONObject.NULL)
            put("operatingProfitGrowthYoY", metrics.operatingProfitGrowthYoY ?: JSONObject.NULL)
            put("totalAssetGrowthYoY", metrics.totalAssetGrowthYoY ?: JSONObject.NULL)
            put("equityGrowthYoY", metrics.equityGrowthYoY ?: JSONObject.NULL)
            put("reportDate", metrics.reportDate ?: JSONObject.NULL)
        }.toString()
    }

    /**
     * 反序列化成长性指标
     */
    fun deserializeGrowthMetrics(json: String): GrowthMetrics {
        return try {
            val obj = JSONObject(json)
            GrowthMetrics(
                revenueGrowthYoY = if (obj.isNull("revenueGrowthYoY")) null else obj.optDouble("revenueGrowthYoY"),
                revenueGrowthQoQ = if (obj.isNull("revenueGrowthQoQ")) null else obj.optDouble("revenueGrowthQoQ"),
                revenueGrowth3Y = if (obj.isNull("revenueGrowth3Y")) null else obj.optDouble("revenueGrowth3Y"),
                revenueGrowth5Y = if (obj.isNull("revenueGrowth5Y")) null else obj.optDouble("revenueGrowth5Y"),
                netProfitGrowthYoY = if (obj.isNull("netProfitGrowthYoY")) null else obj.optDouble("netProfitGrowthYoY"),
                netProfitGrowthQoQ = if (obj.isNull("netProfitGrowthQoQ")) null else obj.optDouble("netProfitGrowthQoQ"),
                netProfitGrowth3Y = if (obj.isNull("netProfitGrowth3Y")) null else obj.optDouble("netProfitGrowth3Y"),
                netProfitGrowth5Y = if (obj.isNull("netProfitGrowth5Y")) null else obj.optDouble("netProfitGrowth5Y"),
                grossProfitGrowthYoY = if (obj.isNull("grossProfitGrowthYoY")) null else obj.optDouble("grossProfitGrowthYoY"),
                operatingProfitGrowthYoY = if (obj.isNull("operatingProfitGrowthYoY")) null else obj.optDouble("operatingProfitGrowthYoY"),
                totalAssetGrowthYoY = if (obj.isNull("totalAssetGrowthYoY")) null else obj.optDouble("totalAssetGrowthYoY"),
                equityGrowthYoY = if (obj.isNull("equityGrowthYoY")) null else obj.optDouble("equityGrowthYoY"),
                reportDate = if (obj.isNull("reportDate")) null else obj.optString("reportDate")
            )
        } catch (e: Exception) {
            GrowthMetrics()
        }
    }

    /**
     * 序列化分红信息
     */
    fun serializeDividendInfo(info: DividendInfo): String {
        val historyArray = JSONArray()
        info.dividendHistory.forEach { event ->
            historyArray.put(JSONObject().apply {
                put("eventDate", event.eventDate)
                put("exDividendDate", event.exDividendDate)
                put("recordDate", event.recordDate)
                put("announcementDate", event.announcementDate)
                put("cashDividendPerShare", event.cashDividendPerShare)
                put("isPreTax", event.isPreTax)
                put("stockDividend", event.stockDividend)
                put("capitalIncrease", event.capitalIncrease)
            })
        }
        
        return JSONObject().apply {
            put("symbol", info.symbol)
            put("dividendPerShare", info.dividendPerShare ?: JSONObject.NULL)
            put("dividendYield", info.dividendYield ?: JSONObject.NULL)
            put("payoutRatio", info.payoutRatio ?: JSONObject.NULL)
            put("exDividendDate", info.exDividendDate ?: JSONObject.NULL)
            put("recordDate", info.recordDate ?: JSONObject.NULL)
            put("dividendDate", info.dividendDate ?: JSONObject.NULL)
            put("isPreTax", info.isPreTax)
            put("dividendHistory", historyArray)
            put("ttmDividendPerShare", info.ttmDividendPerShare ?: JSONObject.NULL)
            put("ttmDividendCount", info.ttmDividendCount)
            put("consecutiveYears", info.consecutiveYears)
            put("dividendStability", info.dividendStability)
        }.toString()
    }

    /**
     * 反序列化分红信息
     */
    fun deserializeDividendInfo(json: String): DividendInfo {
        return try {
            val obj = JSONObject(json)
            val historyArray = obj.optJSONArray("dividendHistory")
            val history = mutableListOf<DividendEvent>()
            
            historyArray?.let {
                for (i in 0 until it.length()) {
                    val eventObj = it.optJSONObject(i) ?: continue
                    history.add(DividendEvent(
                        eventDate = eventObj.optString("eventDate"),
                        exDividendDate = eventObj.optString("exDividendDate").takeIf { it.isNotEmpty() },
                        recordDate = eventObj.optString("recordDate").takeIf { it.isNotEmpty() },
                        announcementDate = eventObj.optString("announcementDate").takeIf { it.isNotEmpty() },
                        cashDividendPerShare = eventObj.optDouble("cashDividendPerShare", 0.0),
                        isPreTax = eventObj.optBoolean("isPreTax", true),
                        stockDividend = if (eventObj.isNull("stockDividend")) null else eventObj.optDouble("stockDividend"),
                        capitalIncrease = if (eventObj.isNull("capitalIncrease")) null else eventObj.optDouble("capitalIncrease")
                    ))
                }
            }
            
            DividendInfo(
                symbol = obj.optString("symbol"),
                dividendPerShare = if (obj.isNull("dividendPerShare")) null else obj.optDouble("dividendPerShare"),
                dividendYield = if (obj.isNull("dividendYield")) null else obj.optDouble("dividendYield"),
                payoutRatio = if (obj.isNull("payoutRatio")) null else obj.optDouble("payoutRatio"),
                exDividendDate = if (obj.isNull("exDividendDate")) null else obj.optString("exDividendDate"),
                recordDate = if (obj.isNull("recordDate")) null else obj.optString("recordDate"),
                dividendDate = if (obj.isNull("dividendDate")) null else obj.optString("dividendDate"),
                isPreTax = obj.optBoolean("isPreTax", true),
                dividendHistory = history,
                ttmDividendPerShare = if (obj.isNull("ttmDividendPerShare")) null else obj.optDouble("ttmDividendPerShare"),
                ttmDividendCount = obj.optInt("ttmDividendCount", 0),
                consecutiveYears = obj.optInt("consecutiveYears", 0),
                dividendStability = obj.optString("dividendStability", "未知")
            )
        } catch (e: Exception) {
            DividendInfo()
        }
    }

    /**
     * 序列化机构持仓
     */
    fun serializeInstitutionalHolding(holding: InstitutionalHolding): String {
        val holdersArray = JSONArray()
        holding.top10Holders.forEach { holder ->
            holdersArray.put(JSONObject().apply {
                put("name", holder.name)
                put("holdingShares", holder.holdingShares)
                put("holdingRatio", holder.holdingRatio)
                put("holdingChange", holder.holdingChange)
                put("shareholderType", holder.shareholderType)
            })
        }
        
        return JSONObject().apply {
            put("symbol", holding.symbol)
            put("institutionCount", holding.institutionCount ?: JSONObject.NULL)
            put("holdingRatio", holding.holdingRatio ?: JSONObject.NULL)
            put("holdingChange", holding.holdingChange ?: JSONObject.NULL)
            put("holdingChangeRatio", holding.holdingChangeRatio ?: JSONObject.NULL)
            put("top10Holders", holdersArray)
            put("top10HoldingRatio", holding.top10HoldingRatio ?: JSONObject.NULL)
            put("fundHoldingRatio", holding.fundHoldingRatio ?: JSONObject.NULL)
            put("fundHoldingChange", holding.fundHoldingChange ?: JSONObject.NULL)
            put("northboundHolding", holding.northboundHolding ?: JSONObject.NULL)
            put("northboundChange", holding.northboundChange ?: JSONObject.NULL)
            put("reportDate", holding.reportDate ?: JSONObject.NULL)
        }.toString()
    }

    /**
     * 反序列化机构持仓
     */
    fun deserializeInstitutionalHolding(json: String): InstitutionalHolding {
        return try {
            val obj = JSONObject(json)
            val holdersArray = obj.optJSONArray("top10Holders")
            val holders = mutableListOf<ShareholderInfo>()
            
            holdersArray?.let {
                for (i in 0 until it.length()) {
                    val holderObj = it.optJSONObject(i) ?: continue
                    holders.add(ShareholderInfo(
                        name = holderObj.optString("name"),
                        holdingShares = holderObj.optDouble("holdingShares", 0.0),
                        holdingRatio = holderObj.optDouble("holdingRatio", 0.0),
                        holdingChange = if (holderObj.isNull("holdingChange")) null else holderObj.optDouble("holdingChange"),
                        shareholderType = holderObj.optString("shareholderType", "机构")
                    ))
                }
            }
            
            InstitutionalHolding(
                symbol = obj.optString("symbol"),
                institutionCount = if (obj.isNull("institutionCount")) null else obj.optInt("institutionCount"),
                holdingRatio = if (obj.isNull("holdingRatio")) null else obj.optDouble("holdingRatio"),
                holdingChange = if (obj.isNull("holdingChange")) null else obj.optDouble("holdingChange"),
                holdingChangeRatio = if (obj.isNull("holdingChangeRatio")) null else obj.optDouble("holdingChangeRatio"),
                top10Holders = holders,
                top10HoldingRatio = if (obj.isNull("top10HoldingRatio")) null else obj.optDouble("top10HoldingRatio"),
                fundHoldingRatio = if (obj.isNull("fundHoldingRatio")) null else obj.optDouble("fundHoldingRatio"),
                fundHoldingChange = if (obj.isNull("fundHoldingChange")) null else obj.optDouble("fundHoldingChange"),
                northboundHolding = if (obj.isNull("northboundHolding")) null else obj.optDouble("northboundHolding"),
                northboundChange = if (obj.isNull("northboundChange")) null else obj.optDouble("northboundChange"),
                reportDate = if (obj.isNull("reportDate")) null else obj.optString("reportDate")
            )
        } catch (e: Exception) {
            InstitutionalHolding()
        }
    }

    // ============ 工具方法 ============

    private fun safeParseDouble(value: Any?): Double? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun parseDateSafe(dateStr: String?): Date? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            format.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
}
