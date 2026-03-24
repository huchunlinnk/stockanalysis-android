package com.example.stockanalysis.data.repository

import androidx.lifecycle.LiveData
import com.example.stockanalysis.data.datasource.DataSourceManager
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
 * 股票数据仓库接口
 */
interface StockRepository {
    fun getAllStocks(): LiveData<List<Stock>>
    suspend fun getAllStocksSync(): List<Stock>
    suspend fun getStockBySymbol(symbol: String): Stock?
    suspend fun addStock(symbol: String, name: String): Result<Unit>
    suspend fun removeStock(symbol: String): Result<Unit>
    suspend fun updateStock(stock: Stock)
    suspend fun isStockExists(symbol: String): Boolean
    fun searchStocks(query: String): Flow<List<Pair<String, String>>>
    
    // 本地行情数据
    suspend fun getQuote(symbol: String): Result<RealtimeQuote>
    suspend fun getQuotes(symbols: List<String>): Result<List<RealtimeQuote>>
    suspend fun getTechnicalIndicators(symbol: String): Result<TechnicalIndicators>
    suspend fun getKLineData(symbol: String, days: Int = 90): Result<List<KLineData>>
    suspend fun getTrendAnalysis(symbol: String): Result<TrendAnalysis>
}

/**
 * 股票数据仓库实现
 */
@Singleton
class StockRepositoryImpl @Inject constructor(
    private val stockDao: StockDao,
    private val localDataService: LocalDataService,
    private val dataSourceManager: DataSourceManager
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
            
            // 验证股票代码是否有效（在股票池中）
            val poolStocks = localDataService.getStockPool()
            val stockInfo = poolStocks.find { it.first == symbol }
                ?: return Result.failure(Exception("无效的股票代码"))
            
            val stock = Stock(
                symbol = symbol,
                name = stockInfo.second,
                market = MarketType.A_SHARE
            )
            
            stockDao.insertStock(stock)
            
            // 预加载股票数据
            localDataService.getOrGenerateKLineData(symbol, 90)
            
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
        
        val results = localDataService.searchStocks(query)
        emit(results)
    }.flowOn(Dispatchers.IO)

    override suspend fun getQuote(symbol: String): Result<RealtimeQuote> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用DataSourceManager获取真实行情数据
                dataSourceManager.fetchQuote(symbol)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getQuotes(symbols: List<String>): Result<List<RealtimeQuote>> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用DataSourceManager获取批量真实行情数据
                dataSourceManager.fetchQuotes(symbols)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTechnicalIndicators(symbol: String): Result<TechnicalIndicators> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用DataSourceManager获取技术指标
                dataSourceManager.fetchTechnicalIndicators(symbol)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getKLineData(symbol: String, days: Int): Result<List<KLineData>> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用DataSourceManager获取K线数据
                dataSourceManager.fetchKLineData(symbol, days)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTrendAnalysis(symbol: String): Result<TrendAnalysis> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用DataSourceManager获取趋势分析
                dataSourceManager.fetchTrendAnalysis(symbol)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
