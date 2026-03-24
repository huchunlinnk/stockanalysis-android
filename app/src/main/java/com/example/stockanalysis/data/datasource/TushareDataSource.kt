package com.example.stockanalysis.data.datasource

import android.util.Log
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tushare Pro 数据源实现
 *
 * 提供基本面数据：PE/PB、财务指标、行业分析等
 *
 * 优先级：-1（最高，如果配置了Token）
 * 数据来源：Tushare Pro API (https://tushare.pro/)
 *
 * 配额说明（免费用户）：
 * - 每分钟最多 80 次请求
 * - 每天最多 500 次请求
 *
 * 使用方法：
 * 1. 在设置中配置 Tushare Token
 * 2. 数据源将自动提升为最高优先级
 * 3. AnalysisEngine 将使用真实基本面数据替代模拟数据
 */
@Singleton
class TushareDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val preferencesManager: PreferencesManager
) : StockDataSource {

    companion object {
        const val TAG = "TushareDataSource"
        const val API_URL = "http://api.tushare.pro"
        const val DEFAULT_TIMEOUT_MS = 15000L

        // Tushare 速率限制（免费用户）
        const val RATE_LIMIT_PER_MINUTE = 80
    }

    override val name: String = "TushareDataSource"
    override var priority: Int = 2 // 默认优先级，Token配置后提升为-1
    override var isHealthy: Boolean = true

    private var callCount = 0
    private var minuteStart: Long? = null

    /**
     * 检查速率限制
     *
     * 流控策略：
     * 1. 检查是否进入新的一分钟
     * 2. 如果是，重置计数器
     * 3. 如果当前分钟调用次数超过限制，延迟执行
     */
    private fun checkRateLimit() {
        val currentTime = System.currentTimeMillis()

        if (minuteStart == null) {
            minuteStart = currentTime
            callCount = 0
        } else if (currentTime - minuteStart!! >= 60000) {
            // 已经过了一分钟，重置计数器
            minuteStart = currentTime
            callCount = 0
            Log.d(TAG, "速率限制计数器已重置")
        }

        // 检查是否超过配额
        if (callCount >= RATE_LIMIT_PER_MINUTE) {
            val elapsed = currentTime - minuteStart!!
            val sleepTime = maxOf(0, 60000 - elapsed) + 1000 // +1秒缓冲

            Log.w(TAG, "Tushare 达到速率限制 ($callCount/$RATE_LIMIT_PER_MINUTE 次/分钟)，等待 ${sleepTime}ms...")
            Thread.sleep(sleepTime)

            // 重置计数器
            minuteStart = System.currentTimeMillis()
            callCount = 0
        }

        callCount++
        Log.d(TAG, "Tushare 当前分钟调用次数: $callCount/$RATE_LIMIT_PER_MINUTE")
    }

    /**
     * 转换股票代码为 Tushare 格式
     *
     * 格式要求：
     * - 沪市：600519.SH
     * - 深市：000001.SZ
     * - 北交所：8xxxxx.BJ
     */
    private fun convertStockCode(stockCode: String): String {
        val code = stockCode.trim()

        // 已经包含后缀
        if (code.contains('.')) {
            return code.uppercase()
        }

        // 根据前缀判断市场
        return when {
            code.startsWith("600") || code.startsWith("601") ||
            code.startsWith("603") || code.startsWith("688") -> "$code.SH"
            code.startsWith("000") || code.startsWith("002") ||
            code.startsWith("300") -> "$code.SZ"
            code.startsWith("8") || code.startsWith("4") ||
            code.startsWith("92") -> "$code.BJ"
            else -> {
                Log.w(TAG, "无法确定股票 $code 的市场，默认使用深市")
                "$code.SZ"
            }
        }
    }

    /**
     * 调用 Tushare API
     */
    private suspend fun callApi(
        apiName: String,
        params: Map<String, Any> = emptyMap(),
        fields: String = ""
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val token = preferencesManager.getTushareToken()
            if (token.isEmpty()) {
                Log.w(TAG, "Tushare Token 未配置")
                return@withContext Result.failure(Exception("Tushare Token 未配置"))
            }

            // 速率限制检查
            checkRateLimit()

            // 构建请求体
            val requestBody = JSONObject().apply {
                put("api_name", apiName)
                put("token", token)
                put("params", JSONObject(params))
                put("fields", fields)
            }

            Log.d(TAG, "调用 Tushare API: $apiName, params: $params")

            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Tushare API 请求失败: ${response.code}")
                return@withContext Result.failure(
                    Exception("HTTP ${response.code}")
                )
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                return@withContext Result.failure(
                    Exception("响应体为空")
                )
            }

            val json = JSONObject(responseBody)

            // 检查 API 返回码
            val code = json.optInt("code", -1)
            if (code != 0) {
                val msg = json.optString("msg", "未知错误")
                Log.e(TAG, "Tushare API 返回错误: $code - $msg")

                // 检测配额超限
                if (msg.contains("quota", ignoreCase = true) ||
                    msg.contains("配额", ignoreCase = true) ||
                    msg.contains("limit", ignoreCase = true)) {
                    return@withContext Result.failure(
                        Exception("Tushare 配额超限: $msg")
                    )
                }

                return@withContext Result.failure(
                    Exception("Tushare API 错误: $msg")
                )
            }

            Log.d(TAG, "Tushare API 调用成功: $apiName")
            Result.success(json)

        } catch (e: Exception) {
            Log.e(TAG, "Tushare API 调用异常: ${e.message}", e)
            Result.failure(Exception("请求失败: ${e.message}", e))
        }
    }

    /**
     * 获取基本面分析数据
     *
     * 包含：
     * - PE/PB 估值
     * - 财务健康度
     * - 盈利能力
     * - 成长性
     */
    suspend fun fetchFundamentalAnalysis(symbol: String): Result<FundamentalAnalysis> {
        return withContext(Dispatchers.IO) {
            try {
                val tsCode = convertStockCode(symbol)

                // 1. 获取实时行情（包含PE/PB）
                val quoteResult = fetchQuote(symbol)
                val quote = quoteResult.getOrNull()

                // 2. 获取财务数据
                val incomeResult = callApi(
                    "income",
                    mapOf("ts_code" to tsCode),
                    "revenue,n_income,gross_profit_margin"
                )

                // 3. 获取资产负债表
                val balanceResult = callApi(
                    "balancesh",
                    mapOf("ts_code" to tsCode),
                    "total_assets,total_liab"
                )

                // 解析财务数据
                val incomeData = incomeResult.getOrNull()?.optJSONObject("data")
                val balanceData = balanceResult.getOrNull()?.optJSONObject("data")

                // 构建基本面分析
                val peRatio = quote?.peRatio ?: 0.0
                val pbRatio = quote?.pbRatio ?: 0.0

                // 估值分析
                val valuation = when {
                    peRatio > 0 && peRatio < 15 -> "估值偏低，具备安全边际（PE: ${String.format("%.2f", peRatio)}）"
                    peRatio > 0 && peRatio < 30 -> "估值合理（PE: ${String.format("%.2f", peRatio)}）"
                    peRatio > 0 -> "估值偏高，注意风险（PE: ${String.format("%.2f", peRatio)}）"
                    else -> "估值数据不可用"
                }

                // 成长性分析
                val revenue = incomeData?.optJSONArray("items")?.optJSONArray(0)?.optDouble(0, 0.0) ?: 0.0
                val growth = if (revenue > 0) {
                    "营收 ${String.format("%.2f", revenue / 100000000)}亿"
                } else {
                    "财务数据暂无"
                }

                // 盈利能力
                val grossMargin = incomeData?.optJSONArray("items")?.optJSONArray(0)?.optDouble(2, 0.0) ?: 0.0
                val profitability = if (grossMargin > 0) {
                    "毛利率 ${String.format("%.1f", grossMargin)}%"
                } else {
                    "盈利数据暂无"
                }

                // 财务健康度
                val totalAssets = balanceData?.optJSONArray("items")?.optJSONArray(0)?.optDouble(0, 0.0) ?: 0.0
                val totalLiab = balanceData?.optJSONArray("items")?.optJSONArray(0)?.optDouble(1, 0.0) ?: 0.0
                val liabRatio = if (totalAssets > 0) {
                    (totalLiab / totalAssets) * 100
                } else {
                    0.0
                }
                val financialHealth = if (liabRatio > 0) {
                    "资产负债率 ${String.format("%.1f", liabRatio)}%"
                } else {
                    "财务数据暂无"
                }

                // 计算综合评分
                var score = 60
                if (peRatio > 0) {
                    score += when {
                        peRatio < 15 -> 20
                        peRatio < 30 -> 10
                        else -> 0
                    }
                }
                if (pbRatio > 0 && pbRatio < 3) score += 10
                if (grossMargin > 20) score += 10

                val analysis = FundamentalAnalysis(
                    valuation = valuation,
                    growth = growth,
                    profitability = profitability,
                    financialHealth = financialHealth,
                    fundamentalScore = score.coerceIn(0, 100)
                )

                Log.d(TAG, "基本面分析完成: $symbol, 评分: $score")
                Result.success(analysis)

            } catch (e: Exception) {
                Log.e(TAG, "获取基本面数据失败: ${e.message}", e)
                Result.failure(Exception("解析基本面数据失败", e))
            }
        }
    }

    override suspend fun fetchQuote(symbol: String): Result<RealtimeQuote> {
        return withContext(Dispatchers.IO) {
            try {
                val tsCode = convertStockCode(symbol)

                // 调用 Tushare daily 接口获取最新数据
                val result = callApi(
                    "daily",
                    mapOf(
                        "ts_code" to tsCode,
                        "start_date" to getCurrentDate(-5),
                        "end_date" to getCurrentDate()
                    )
                )

                val json = result.getOrThrow()
                val data = json.optJSONObject("data")
                val items = data?.optJSONArray("items")

                if (items == null || items.length() == 0) {
                    return@withContext Result.failure(
                        Exception("无实时行情数据")
                    )
                }

                // 获取最新一天的数据
                val latest = items.optJSONArray(0)

                val close = latest.optDouble(5, 0.0) // close
                val open = latest.optDouble(2, 0.0) // open
                val preClose = latest.optDouble(6, 0.0) // pre_close
                val change = close - preClose
                val changePercent = if (preClose > 0) (change / preClose * 100) else 0.0

                val quote = RealtimeQuote(
                    symbol = symbol,
                    name = "", // Tushare daily 不返回名称，需要单独查询
                    price = close,
                    open = open,
                    high = latest.optDouble(3, 0.0),
                    low = latest.optDouble(4, 0.0),
                    preClose = preClose,
                    volume = (latest.optDouble(9, 0.0) * 100).toLong(), // vol (手转股)
                    amount = latest.optDouble(10, 0.0) * 1000, // amount (千元转元)
                    change = change,
                    changePercent = changePercent,
                    peRatio = null, // daily 不包含，需要 daily_basic
                    pbRatio = null,
                    marketCap = null
                )

                Log.d(TAG, "实时行情获取成功: $symbol, 价格: ${quote.price}")
                Result.success(quote)

            } catch (e: Exception) {
                Log.e(TAG, "获取实时行情失败: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun fetchQuotes(symbols: List<String>): Result<List<RealtimeQuote>> {
        return Result.failure(Exception("Tushare 暂不支持批量获取"))
    }

    override suspend fun fetchKLineData(symbol: String, days: Int): Result<List<KLineData>> {
        return withContext(Dispatchers.IO) {
            try {
                val tsCode = convertStockCode(symbol)

                val result = callApi(
                    "daily",
                    mapOf(
                        "ts_code" to tsCode,
                        "start_date" to getCurrentDate(-days),
                        "end_date" to getCurrentDate()
                    )
                )

                val json = result.getOrThrow()
                val data = json.optJSONObject("data")
                val items = data?.optJSONArray("items")

                if (items == null || items.length() == 0) {
                    return@withContext Result.failure(
                        Exception("无K线数据")
                    )
                }

                val klineList = mutableListOf<KLineData>()

                for (i in 0 until items.length()) {
                    val item = items.optJSONArray(i) ?: continue

                    val close = item.optDouble(5, 0.0)
                    val preClose = item.optDouble(6, 0.0)
                    val change = close - preClose
                    val changePercent = if (preClose > 0) (change / preClose * 100) else 0.0

                    klineList.add(
                        KLineData(
                            symbol = symbol,
                            timestamp = item.optString(1).parseDate(), // trade_date
                            open = item.optDouble(2, 0.0),
                            high = item.optDouble(3, 0.0),
                            low = item.optDouble(4, 0.0),
                            close = close,
                            volume = (item.optDouble(9, 0.0) * 100).toLong(), // vol
                            amount = item.optDouble(10, 0.0) * 1000, // amount
                            change = change,
                            changePercent = changePercent,
                            source = "tushare"
                        )
                    )
                }

                Log.d(TAG, "K线数据获取成功: $symbol, ${klineList.size}条")
                Result.success(klineList.reversed()) // Tushare 返回倒序，需要反转

            } catch (e: Exception) {
                Log.e(TAG, "获取K线数据失败: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun fetchTechnicalIndicators(symbol: String): Result<TechnicalIndicators> {
        return Result.failure(Exception("Tushare 不提供技术指标"))
    }

    override suspend fun fetchTrendAnalysis(symbol: String): Result<TrendAnalysis> {
        return Result.failure(Exception("Tushare 不提供趋势分析"))
    }

    override suspend fun fetchMarketOverview(): Result<MarketOverview> {
        return Result.failure(Exception("Tushare 不提供市场概览"))
    }

    override suspend fun searchStocks(query: String): Result<List<Pair<String, String>>> {
        return Result.failure(Exception("Tushare 不提供股票搜索"))
    }

    /**
     * 获取当前日期字符串（Tushare 格式：YYYYMMDD）
     */
    private fun getCurrentDate(offsetDays: Int = 0): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, offsetDays)
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%04d%02d%02d", year, month, day)
    }

    /**
     * 解析 Tushare 日期格式（YYYYMMDD）
     */
    private fun String.parseDate(): java.util.Date {
        val year = substring(0, 4).toInt()
        val month = substring(4, 6).toInt() - 1
        val day = substring(6, 8).toInt()
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month, day)
        return calendar.time
    }
}

