package com.example.stockanalysis.data.datasource

import android.util.Log
import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Baostock 数据源
 *
 * 数据来源: Baostock (http://baostock.com/)
 * 特点: 开源 A股数据接口，提供财务指标、估值指标等基本面数据
 * 限制: 需要登录获取 token，部分数据有延迟
 *
 * 支持的数据:
 * - 财务指标 (ROE, 毛利率, 净利率等)
 * - 估值指标 (PE, PB, PS 等)
 * - 成长指标 (营收增长率, 净利润增长率等)
 * - 每股指标 (EPS, BPS, 每股现金流等)
 */
@Singleton
class BaostockDataSource @Inject constructor(
    private val httpClient: OkHttpClient
) : StockDataSource {

    companion object {
        const val TAG = "BaostockDataSource"
        const val BASE_URL = "http://api.baostock.com"
    }

    override val name: String = "Baostock"
    override val priority: Int = 5  // 优先级较低，作为补充数据源
    override var isHealthy: Boolean = true

    // 股票代码映射 (A股代码格式转换)
    private fun convertToBaostockCode(symbol: String): String {
        val cleanCode = symbol.trim().uppercase()
        return when {
            cleanCode.startsWith("6") -> "sh.$cleanCode"
            cleanCode.startsWith("0") || cleanCode.startsWith("3") -> "sz.$cleanCode"
            cleanCode.startsWith("68") || cleanCode.startsWith("8") || cleanCode.startsWith("4") -> {
                // 科创板和北交所
                if (cleanCode.length == 6) "sh.$cleanCode" else cleanCode
            }
            else -> "sh.$cleanCode"
        }
    }

    override suspend fun fetchQuote(symbol: String): Result<RealtimeQuote> {
        return Result.failure(DataSourceException.NotSupportedException("Baostock does not provide real-time quotes"))
    }

    override suspend fun fetchQuotes(symbols: List<String>): Result<List<RealtimeQuote>> {
        return Result.failure(DataSourceException.NotSupportedException("Baostock does not provide real-time quotes"))
    }

    override suspend fun fetchKLineData(symbol: String, days: Int): Result<List<KLineData>> {
        return withContext(Dispatchers.IO) {
            try {
                val bsCode = convertToBaostockCode(symbol)
                val url = "$BASE_URL/api/quotation/kline?code=$bsCode&start=${getStartDate(days)}&end=${getEndDate()}"
                
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "StockAnalysisApp/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        DataSourceException.NetworkException("HTTP ${response.code}")
                    )
                }

                val body = response.body?.string() ?: ""
                val klineData = parseKLineData(symbol, body)
                
                if (klineData.isNotEmpty()) {
                    Result.success(klineData)
                } else {
                    Result.failure(DataSourceException.ParseException("Empty kline data"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch kline data for $symbol", e)
                Result.failure(DataSourceException.NetworkException("Failed to fetch kline data", e))
            }
        }
    }

    override suspend fun fetchTechnicalIndicators(symbol: String): Result<TechnicalIndicators> {
        return Result.failure(DataSourceException.NotSupportedException("Baostock does not provide technical indicators"))
    }

    override suspend fun fetchTrendAnalysis(symbol: String): Result<TrendAnalysis> {
        return Result.failure(DataSourceException.NotSupportedException("Baostock does not provide trend analysis"))
    }

    override suspend fun fetchMarketOverview(): Result<MarketOverview> {
        return Result.failure(DataSourceException.NotSupportedException("Baostock does not provide market overview"))
    }

    override suspend fun searchStocks(query: String): Result<List<Pair<String, String>>> {
        return Result.failure(DataSourceException.NotSupportedException("Baostock does not support stock search"))
    }

    /**
     * 获取财务指标数据 (Baostock 的主要功能)
     * 使用 model 包中的 FinancialIndicators 类
     */
    suspend fun fetchFinancialIndicators(symbol: String): Result<FinancialIndicators> {
        return withContext(Dispatchers.IO) {
            try {
                val bsCode = convertToBaostockCode(symbol)
                val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1
                
                // 获取盈利能力指标
                val profitUrl = "$BASE_URL/api/finance/profit?code=$bsCode&year=$year&quarter=4"
                val profitData = fetchJson(profitUrl)
                
                // 获取偿债能力指标
                val debtUrl = "$BASE_URL/api/finance/debt?code=$bsCode&year=$year&quarter=4"
                val debtData = fetchJson(debtUrl)
                
                // 获取成长能力指标
                val growthUrl = "$BASE_URL/api/finance/growth?code=$bsCode&year=$year&quarter=4"
                val growthData = fetchJson(growthUrl)
                
                val indicators = parseFinancialIndicators(profitData, debtData, growthData)
                Result.success(indicators)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch financial indicators for $symbol", e)
                Result.failure(DataSourceException.NetworkException("Failed to fetch financial indicators", e))
            }
        }
    }

    private suspend fun fetchJson(url: String): JSONObject {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "StockAnalysisApp/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw DataSourceException.NetworkException("HTTP ${response.code}")
            }

            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        }
    }

    private fun parseKLineData(symbol: String, jsonString: String): List<KLineData> {
        val result = mutableListOf<KLineData>()
        try {
            val json = JSONObject(jsonString)
            val data = json.optJSONArray("data") ?: return result
            
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val dateStr = item.getString("date")
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val timestamp = dateFormat.parse(dateStr) ?: Date()
                
                result.add(KLineData(
                    symbol = symbol,
                    timestamp = timestamp,
                    open = item.getDouble("open"),
                    high = item.getDouble("high"),
                    low = item.getDouble("low"),
                    close = item.getDouble("close"),
                    volume = item.getLong("volume"),
                    amount = item.optDouble("amount", 0.0),
                    change = item.optDouble("change", 0.0),
                    changePercent = item.optDouble("changePercent", 0.0),
                    period = "daily",
                    source = "baostock"
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse kline data", e)
        }
        return result.reversed()
    }

    private fun parseFinancialIndicators(
        profitData: JSONObject,
        debtData: JSONObject,
        growthData: JSONObject
    ): FinancialIndicators {
        return FinancialIndicators(
            roe = profitData.optDouble("roe").takeIf { it != 0.0 },
            roa = profitData.optDouble("roa").takeIf { it != 0.0 },
            grossMargin = profitData.optDouble("grossMargin").takeIf { it != 0.0 },
            netMargin = profitData.optDouble("netMargin").takeIf { it != 0.0 },
            operatingMargin = profitData.optDouble("operatingMargin").takeIf { it != 0.0 },
            debtToEquity = debtData.optDouble("debtToEquity").takeIf { it != 0.0 },
            currentRatio = debtData.optDouble("currentRatio").takeIf { it != 0.0 },
            quickRatio = debtData.optDouble("quickRatio").takeIf { it != 0.0 },
            interestCoverage = debtData.optDouble("interestCoverage").takeIf { it != 0.0 },
            inventoryTurnover = debtData.optDouble("inventoryTurnover").takeIf { it != 0.0 },
            receivablesTurnover = debtData.optDouble("receivablesTurnover").takeIf { it != 0.0 },
            assetTurnover = debtData.optDouble("assetTurnover").takeIf { it != 0.0 },
            operatingCashFlow = debtData.optDouble("operatingCashFlow").takeIf { it != 0.0 },
            freeCashFlow = debtData.optDouble("freeCashFlow").takeIf { it != 0.0 },
            cashFlowPerShare = debtData.optDouble("cashFlowPerShare").takeIf { it != 0.0 },
            reportDate = profitData.optString("reportDate").takeIf { it.isNotEmpty() },
            reportType = profitData.optString("reportType").takeIf { it.isNotEmpty() }
        )
    }

    private fun getStartDate(days: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    private fun getEndDate(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
}
