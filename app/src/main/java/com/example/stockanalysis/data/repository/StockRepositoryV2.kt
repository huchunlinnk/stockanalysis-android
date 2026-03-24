package com.example.stockanalysis.data.repository

import androidx.lifecycle.LiveData
import com.example.stockanalysis.data.datasource.DataSourceManager
import com.example.stockanalysis.data.datasource.LocalDataSource
import com.example.stockanalysis.data.local.LocalDataService
import com.example.stockanalysis.data.local.StockDao
import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 股票数据仓库V2 - 支持多数据源故障切换
 */
@Singleton
class StockRepositoryV2 @Inject constructor(
    private val stockDao: StockDao,
    private val localDataService: LocalDataService,
    private val dataSourceManager: DataSourceManager,
    private val localDataSource: LocalDataSource
) : StockRepository {

    override fun getAllStocks(): LiveData<List<Stock>> {
        return stockDao.getAllStocks()
    }

    override suspend fun getAllStocksSync(): List<Stock> {
        return stockDao.getAllStocksSync()
    }

    override suspend fun getStockBySymbol(symbol: String): Stock? {
        return stockDao.getStockBySymbol(symbol)
    }

    override suspend fun addStock(symbol: String, name: String): Result<Unit> {
        return try {
            // 检查股票是否已在自选
            if (stockDao.isStockExists(symbol)) {
                return Result.failure(Exception("该股票已在自选列表中"))
            }
            
            // 验证股票代码是否有效（通过数据源搜索确认）
            val searchResult = dataSourceManager.searchStocks(symbol)
            val exists = searchResult.getOrNull()?.any { it.first == symbol } ?: false
            
            if (!exists) {
                // 尝试本地数据源
                val localPool = localDataService.getStockPool()
                if (!localPool.any { it.first == symbol }) {
                    return Result.failure(Exception("无效的股票代码: $symbol"))
                }
            }
            
            val stock = Stock(
                symbol = symbol,
                name = name,
                market = MarketType.A_SHARE
            )
            
            stockDao.insertStock(stock)
            
            // 预加载股票数据
            dataSourceManager.fetchKLineData(symbol, 90)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeStock(symbol: String): Result<Unit> {
        return try {
            stockDao.deleteStockBySymbol(symbol)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStock(stock: Stock) {
        stockDao.updateStock(stock)
    }

    override suspend fun isStockExists(symbol: String): Boolean {
        return stockDao.isStockExists(symbol)
    }

    override fun searchStocks(query: String): Flow<List<Pair<String, String>>> = flow {
        if (query.length < 2) {
            emit(emptyList())
            return@flow
        }
        
        // 首先尝试使用远程数据源搜索
        val remoteResult = dataSourceManager.searchStocks(query)
        
        if (remoteResult.isSuccess) {
            val results = remoteResult.getOrNull() ?: emptyList()
            if (results.isNotEmpty()) {
                emit(results)
                return@flow
            }
        }
        
        // 远程搜索失败或为空，使用本地数据源
        val localResults = localDataService.searchStocks(query)
        emit(localResults)
        
    }.flowOn(Dispatchers.IO)

    override suspend fun getQuote(symbol: String): Result<RealtimeQuote> {
        return dataSourceManager.fetchQuote(symbol)
    }

    override suspend fun getQuotes(symbols: List<String>): Result<List<RealtimeQuote>> {
        return dataSourceManager.fetchQuotes(symbols)
    }

    override suspend fun getTechnicalIndicators(symbol: String): Result<TechnicalIndicators> {
        return dataSourceManager.fetchTechnicalIndicators(symbol)
    }

    override suspend fun getKLineData(symbol: String, days: Int): Result<List<KLineData>> {
        return dataSourceManager.fetchKLineData(symbol, days)
    }

    override suspend fun getTrendAnalysis(symbol: String): Result<TrendAnalysis> {
        return dataSourceManager.fetchTrendAnalysis(symbol)
    }
    
    /**
     * 获取数据源状态
     */
    suspend fun getDataSourceStatus(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        dataSourceManager.getDataSourceStatus()
    }
    
    /**
     * 获取市场概览
     */
    suspend fun getMarketOverview(): Result<MarketOverview> {
        return dataSourceManager.fetchMarketOverview()
    }
}
