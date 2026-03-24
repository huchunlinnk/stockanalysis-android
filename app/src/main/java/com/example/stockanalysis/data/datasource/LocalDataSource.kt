package com.example.stockanalysis.data.datasource

import com.example.stockanalysis.data.local.KLineDataDao
import com.example.stockanalysis.data.local.LocalDataService
import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地数据源
 * 使用本地数据库和模拟数据作为最终降级方案
 */
@Singleton
class LocalDataSource @Inject constructor(
    private val localDataService: LocalDataService,
    private val kLineDataDao: KLineDataDao
) : StockDataSource {
    
    override val name: String = "Local"
    override val priority: Int = 999  // 最低优先级，作为最终降级
    override var isHealthy: Boolean = true
    
    override suspend fun fetchQuote(symbol: String): Result<RealtimeQuote> = withContext(Dispatchers.IO) {
        try {
            val quote = localDataService.getRealtimeQuote(symbol)
                ?: return@withContext Result.failure(DataSourceException.InvalidSymbolException("Stock not found: $symbol"))
            Result.success(quote)
        } catch (e: Exception) {
            Result.failure(DataSourceException.ParseException("Failed to fetch local quote", e))
        }
    }
    
    override suspend fun fetchQuotes(symbols: List<String>): Result<List<RealtimeQuote>> = withContext(Dispatchers.IO) {
        try {
            val quotes = symbols.mapNotNull { symbol ->
                localDataService.getRealtimeQuote(symbol)
            }
            Result.success(quotes)
        } catch (e: Exception) {
            Result.failure(DataSourceException.ParseException("Failed to fetch local quotes", e))
        }
    }
    
    override suspend fun fetchKLineData(symbol: String, days: Int): Result<List<KLineData>> = withContext(Dispatchers.IO) {
        try {
            val data = localDataService.getOrGenerateKLineData(symbol, days)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(DataSourceException.ParseException("Failed to fetch local K-line data", e))
        }
    }
    
    override suspend fun fetchTechnicalIndicators(symbol: String): Result<TechnicalIndicators> = withContext(Dispatchers.IO) {
        try {
            val indicators = localDataService.getTechnicalIndicators(symbol)
                ?: return@withContext Result.failure(DataSourceException.ParseException("Failed to calculate indicators"))
            Result.success(indicators)
        } catch (e: Exception) {
            Result.failure(DataSourceException.ParseException("Failed to fetch local indicators", e))
        }
    }
    
    override suspend fun fetchTrendAnalysis(symbol: String): Result<TrendAnalysis> = withContext(Dispatchers.IO) {
        try {
            val analysis = localDataService.getTrendAnalysis(symbol)
                ?: return@withContext Result.failure(DataSourceException.ParseException("Failed to analyze trend"))
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(DataSourceException.ParseException("Failed to fetch local trend analysis", e))
        }
    }
    
    override suspend fun fetchMarketOverview(): Result<MarketOverview> = withContext(Dispatchers.IO) {
        try {
            // 模拟市场概览数据
            val indices = listOf(
                MarketIndex("上证指数", "000001", 3050.0, 10.5, 0.35, 2500000000L, 3500.0),
                MarketIndex("深证成指", "399001", 9850.0, 25.3, 0.26, 1800000000L, 2800.0),
                MarketIndex("创业板指", "399006", 1950.0, 8.7, 0.45, 800000000L, 1200.0)
            )
            
            val stats = MarketStats(
                risingCount = 2800,
                fallingCount = 2100,
                flatCount = 150,
                limitUpCount = 65,
                limitDownCount = 8,
                riseDistribution = mapOf(">7%" to 45, "5-7%" to 123, "3-5%" to 456, "0-3%" to 2176),
                fallDistribution = mapOf("-7%" to 12, "-5-7%" to 34, "-3-5%" to 289, "0-3%" to 1765)
            )
            
            val sectorPerformance = SectorPerformance(
                topRisers = listOf(
                    SectorInfo("半导体", 3.5, listOf("600460", "603501")),
                    SectorInfo("新能源", 2.8, listOf("300750", "601012"))
                ),
                topFallers = listOf(
                    SectorInfo("银行", -1.2, listOf("600036", "000001")),
                    SectorInfo("保险", -0.8, listOf("601318"))
                )
            )
            
            val overview = MarketOverview(
                updateTime = Date(),
                marketType = MarketType.A_SHARE,
                indices = indices,
                stats = stats,
                sectorPerformance = sectorPerformance
            )
            Result.success(overview)
        } catch (e: Exception) {
            Result.failure(DataSourceException.ParseException("Failed to fetch local market overview", e))
        }
    }
    
    override suspend fun searchStocks(query: String): Result<List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        try {
            val results = localDataService.searchStocks(query)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(DataSourceException.ParseException("Failed to search local stocks", e))
        }
    }
}
