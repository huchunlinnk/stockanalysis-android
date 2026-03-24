package com.example.stockanalysis.data.datasource

import android.util.Log
import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据源管理器
 * 管理多个数据源，提供故障自动切换功能
 */
@Singleton
class DataSourceManager @Inject constructor(
    private val eFinanceDataSource: EFinanceDataSource,
    private val akShareDataSource: AkShareDataSource,
    private val yFinanceDataSource: YFinanceDataSource,
    private val baostockDataSource: BaostockDataSource,
    private val localDataSource: LocalDataSource,
    private val chipDistributionDataSource: EFinanceChipDistributionDataSource
) {
    companion object {
        const val TAG = "DataSourceManager"
        const val DEFAULT_TIMEOUT_MS = 15000L  // 15秒超时
    }
    
    /**
     * 数据源列表，按优先级排序
     */
    private val dataSources: List<StockDataSource> by lazy {
        listOf(eFinanceDataSource, akShareDataSource, yFinanceDataSource, baostockDataSource, localDataSource)
            .sortedBy { it.priority }
    }
    
    /**
     * 获取适合特定市场的数据源
     * 对于美股，优先使用 YFinanceDataSource
     */
    private fun getDataSourcesForSymbol(symbol: String): List<StockDataSource> {
        return when {
            USMarketIndex.isValidUSCode(symbol) -> {
                // 美股优先使用 YFinance，然后是本地数据源
                listOf(yFinanceDataSource, localDataSource)
                    .filter { it.isHealthy }
                    .sortedBy { it.priority }
            }
            else -> {
                // A股使用所有数据源
                dataSources.filter { it.isHealthy }
            }
        }
    }
    
    /**
     * 健康检查间隔（毫秒）
     */
    private var lastHealthCheck: Long = 0
    private val HEALTH_CHECK_INTERVAL = 60000L // 60秒
    
    /**
     * 获取实时行情（带故障切换）
     * 美股自动路由到 YFinance 数据源
     */
    suspend fun fetchQuote(symbol: String): Result<RealtimeQuote> {
        return fetchWithFallback(
            symbol = symbol,
            operation = { source -> source.fetchQuote(symbol) },
            timeoutMs = DEFAULT_TIMEOUT_MS
        )
    }
    
    /**
     * 获取批量实时行情（带故障切换）
     * 美股自动路由到 YFinance 数据源
     */
    suspend fun fetchQuotes(symbols: List<String>): Result<List<RealtimeQuote>> {
        // 分离美股和非美股
        val usSymbols = symbols.filter { USMarketIndex.isValidUSCode(it) }
        val nonUsSymbols = symbols.filter { !USMarketIndex.isValidUSCode(it) }
        
        val results = mutableListOf<RealtimeQuote>()
        
        // 获取美股数据
        if (usSymbols.isNotEmpty()) {
            val usResult = fetchWithFallback(
                symbol = usSymbols.first(),
                operation = { source -> source.fetchQuotes(usSymbols) },
                timeoutMs = DEFAULT_TIMEOUT_MS
            )
            usResult.getOrNull()?.let { results.addAll(it) }
        }
        
        // 获取非美股数据
        if (nonUsSymbols.isNotEmpty()) {
            val nonUsResult = fetchWithFallback(
                symbol = nonUsSymbols.first(),
                operation = { source -> source.fetchQuotes(nonUsSymbols) },
                timeoutMs = DEFAULT_TIMEOUT_MS
            )
            nonUsResult.getOrNull()?.let { results.addAll(it) }
        }
        
        return if (results.isNotEmpty()) {
            Result.success(results)
        } else {
            Result.failure(DataSourceException.ServiceUnavailableException("All data sources failed"))
        }
    }
    
    /**
     * 获取K线数据（带故障切换）
     * 美股自动路由到 YFinance 数据源
     */
    suspend fun fetchKLineData(symbol: String, days: Int = 90): Result<List<KLineData>> {
        return fetchWithFallback(
            symbol = symbol,
            operation = { source -> source.fetchKLineData(symbol, days) },
            timeoutMs = DEFAULT_TIMEOUT_MS
        )
    }
    
    /**
     * 获取技术指标（带故障切换）
     * 美股自动路由到 YFinance 数据源
     */
    suspend fun fetchTechnicalIndicators(symbol: String): Result<TechnicalIndicators> {
        return fetchWithFallback(
            symbol = symbol,
            operation = { source -> source.fetchTechnicalIndicators(symbol) },
            timeoutMs = DEFAULT_TIMEOUT_MS
        )
    }
    
    /**
     * 获取趋势分析（带故障切换）
     * 美股自动路由到 YFinance 数据源
     */
    suspend fun fetchTrendAnalysis(symbol: String): Result<TrendAnalysis> {
        return fetchWithFallback(
            symbol = symbol,
            operation = { source -> source.fetchTrendAnalysis(symbol) },
            timeoutMs = DEFAULT_TIMEOUT_MS
        )
    }
    
    /**
     * 获取市场概览（带故障切换）
     * 默认获取A股市场，美股市场概览请使用 fetchUSMarketOverview()
     */
    suspend fun fetchMarketOverview(): Result<MarketOverview> {
        return fetchWithFallback(
            symbol = "000001",  // A股上证指数
            operation = { source -> source.fetchMarketOverview() },
            timeoutMs = DEFAULT_TIMEOUT_MS
        )
    }
    
    /**
     * 获取美股市场概览
     */
    suspend fun fetchUSMarketOverview(): Result<MarketOverview> {
        return fetchWithFallback(
            symbol = "SPX",  // 美股指数
            operation = { source -> source.fetchMarketOverview() },
            timeoutMs = DEFAULT_TIMEOUT_MS
        )
    }
    
    /**
     * 搜索股票（增强版，带多API回退和结果合并）
     * 
     * 搜索策略：
     * 1. 首先尝试EFinanceDataSource搜索（东方财富 + 新浪备用）
     * 2. 如果失败或结果为空，尝试本地股票池搜索（支持拼音搜索）
     * 3. 对于美股查询，同时使用YFinance搜索
     * 4. 合并所有结果并去重，按相关度排序
     */
    suspend fun searchStocks(query: String): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext Result.success(emptyList())
        }
        
        val normalizedQuery = query.trim()
        val isUSQuery = USMarketIndex.isValidUSCode(normalizedQuery) || 
                       USMarketIndex.isUSStockCode(normalizedQuery) ||
                       normalizedQuery.uppercase().matches(Regex("^[A-Z]{1,5}$"))
        
        Log.d(TAG, "Searching stocks for query: '$normalizedQuery', isUSQuery: $isUSQuery")
        
        val allResults = mutableListOf<Pair<String, String>>()
        val errors = mutableListOf<Throwable>()
        
        // 1. 首先尝试EFinanceDataSource（包含东方财富和新浪备用API）
        try {
            Log.d(TAG, "Trying EFinanceDataSource search...")
            val eFinanceResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                eFinanceDataSource.searchStocks(normalizedQuery)
            }
            
            eFinanceResult.fold(
                onSuccess = { results ->
                    if (results.isNotEmpty()) {
                        Log.d(TAG, "EFinanceDataSource returned ${results.size} results")
                        allResults.addAll(results)
                    } else {
                        Log.d(TAG, "EFinanceDataSource returned empty results")
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "EFinanceDataSource search failed: ${error.message}")
                    errors.add(error)
                }
            )
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "EFinanceDataSource search timed out")
            errors.add(e)
        } catch (e: Exception) {
            Log.w(TAG, "EFinanceDataSource search error: ${e.message}")
            errors.add(e)
        }
        
        // 2. 对于美股查询，同时尝试YFinance
        if (isUSQuery) {
            try {
                Log.d(TAG, "Trying YFinanceDataSource search for US stock...")
                val yFinanceResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                    yFinanceDataSource.searchStocks(normalizedQuery)
                }
                
                yFinanceResult.fold(
                    onSuccess = { results ->
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "YFinanceDataSource returned ${results.size} results")
                            allResults.addAll(results)
                        }
                    },
                    onFailure = { error ->
                        Log.w(TAG, "YFinanceDataSource search failed: ${error.message}")
                        errors.add(error)
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "YFinanceDataSource search error: ${e.message}")
                errors.add(e)
            }
        }
        
        // 3. 如果在线搜索失败或结果为空，尝试本地搜索
        if (allResults.isEmpty() || errors.isNotEmpty()) {
            try {
                Log.d(TAG, "Trying LocalDataSource search...")
                val localResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                    localDataSource.searchStocks(normalizedQuery)
                }
                
                localResult.fold(
                    onSuccess = { results ->
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "LocalDataSource returned ${results.size} results")
                            allResults.addAll(results)
                        }
                    },
                    onFailure = { error ->
                        Log.w(TAG, "LocalDataSource search failed: ${error.message}")
                        errors.add(error)
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "LocalDataSource search error: ${e.message}")
                errors.add(e)
            }
        }
        
        // 4. 去重并排序结果
        // 使用代码作为key去重，保留第一次出现的结果
        val uniqueResults = allResults
            .distinctBy { it.first.uppercase() } // 按代码去重（不区分大小写）
            .sortedWith(
                compareByDescending<Pair<String, String>> { (symbol, name) ->
                    // 计算匹配得分用于排序
                    calculateSearchScore(normalizedQuery, symbol, name)
                }.thenBy { it.first } // 得分相同则按代码排序
            )
            .take(20) // 最多返回20条结果
        
        Log.d(TAG, "Total unique results: ${uniqueResults.size}")
        
        return@withContext if (uniqueResults.isNotEmpty()) {
            Result.success(uniqueResults)
        } else if (errors.isNotEmpty()) {
            // 所有数据源都失败
            Result.failure(
                DataSourceException.ServiceUnavailableException(
                    "All search data sources failed",
                    errors.firstOrNull()
                )
            )
        } else {
            Result.success(emptyList())
        }
    }
    
    /**
     * 计算搜索结果匹配得分（用于排序）
     */
    private fun calculateSearchScore(query: String, symbol: String, name: String): Int {
        val normalizedQuery = query.trim().lowercase()
        var score = 0
        
        // 代码精确匹配（最高分）
        if (symbol.equals(normalizedQuery, ignoreCase = true)) {
            score += 1000
        }
        // 代码开头匹配
        else if (symbol.startsWith(normalizedQuery, ignoreCase = true)) {
            score += 800
        }
        // 代码包含匹配
        else if (symbol.contains(normalizedQuery, ignoreCase = true)) {
            score += 600
        }
        
        // 名称开头匹配
        if (name.startsWith(query, ignoreCase = true)) {
            score += 700
        }
        // 名称包含匹配
        else if (name.contains(query, ignoreCase = true)) {
            score += 500
        }
        
        return score
    }
    
    /**
     * 获取筹码分布数据
     */
    suspend fun fetchChipDistribution(symbol: String, days: Int = 90): Result<ChipDistribution> {
        return withContext(Dispatchers.IO) {
            try {
                chipDistributionDataSource.fetchChipDistribution(symbol, days)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch chip distribution", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 获取筹码分布历史
     */
    suspend fun fetchChipDistributionHistory(symbol: String, days: Int = 30): Result<List<ChipDistribution>> {
        return withContext(Dispatchers.IO) {
            try {
                chipDistributionDataSource.fetchChipDistributionHistory(symbol, days)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch chip distribution history", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 获取数据源状态报告
     */
    fun getDataSourceStatus(): Map<String, Boolean> {
        return dataSources.associate { it.name to it.isHealthy }
    }
    
    /**
     * 强制刷新数据源健康状态
     */
    fun refreshDataSourceHealth() {
        lastHealthCheck = 0
        checkDataSourceHealth()
    }

    /**
     * 获取财务指标数据（使用 Baostock）
     */
    suspend fun fetchFinancialIndicators(symbol: String): Result<FinancialIndicators> {
        return withContext(Dispatchers.IO) {
            try {
                baostockDataSource.fetchFinancialIndicators(symbol)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch financial indicators from Baostock", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 带故障切换的通用数据获取方法（智能路由）
     * 根据 symbol 自动选择合适的数据源
     */
    private suspend fun <T> fetchWithFallback(
        symbol: String,
        operation: suspend (StockDataSource) -> Result<T>,
        timeoutMs: Long
    ): Result<T> = withContext(Dispatchers.IO) {
        // 检查数据源健康状态
        checkDataSourceHealth()
        
        // 获取适合该 symbol 的数据源列表
        val sources = getDataSourcesForSymbol(symbol)
        
        if (sources.isEmpty()) {
            return@withContext Result.failure(
                DataSourceException.ServiceUnavailableException("No healthy data sources available")
            )
        }
        
        var lastError: Throwable? = null
        
        for (source in sources) {
            try {
                Log.d(TAG, "Trying data source: ${source.name} (priority ${source.priority}) for $symbol")
                
                val result = withTimeout(timeoutMs) {
                    operation(source)
                }
                
                if (result.isSuccess) {
                    Log.d(TAG, "Success with data source: ${source.name}")
                    return@withContext result
                } else {
                    val error = result.exceptionOrNull()
                    Log.w(TAG, "Data source ${source.name} returned error: ${error?.message}")
                    lastError = error
                    
                    // 标记数据源为不健康（如果是严重错误）
                    if (isCriticalError(error)) {
                        source.isHealthy = false
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Data source ${source.name} timed out")
                lastError = e
                source.isHealthy = false
            } catch (e: Exception) {
                Log.w(TAG, "Data source ${source.name} failed: ${e.message}")
                lastError = e
                
                if (isCriticalError(e)) {
                    source.isHealthy = false
                }
            }
        }
        
        // 所有数据源都失败
        Log.e(TAG, "All data sources failed for $symbol")
        return@withContext Result.failure(
            lastError ?: DataSourceException.ServiceUnavailableException("All data sources failed for $symbol")
        )
    }
    
    /**
     * 判断是否为严重错误（需要标记数据源为不健康）
     */
    private fun isCriticalError(error: Throwable?): Boolean {
        return when (error) {
            is DataSourceException.NetworkException -> true
            is DataSourceException.ServiceUnavailableException -> true
            is DataSourceException.RateLimitException -> true
            is TimeoutCancellationException -> true
            else -> false
        }
    }
    
    /**
     * 检查数据源健康状态
     * 定期将不健康的数据源重置为健康状态，以便重试
     */
    private fun checkDataSourceHealth() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastHealthCheck > HEALTH_CHECK_INTERVAL) {
            dataSources.forEach { source ->
                if (!source.isHealthy) {
                    Log.d(TAG, "Resetting health status for ${source.name}")
                    source.isHealthy = true
                }
            }
            lastHealthCheck = currentTime
        }
    }
}
