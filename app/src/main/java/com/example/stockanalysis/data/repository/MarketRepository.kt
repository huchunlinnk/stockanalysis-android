package com.example.stockanalysis.data.repository

import com.example.stockanalysis.data.api.StockApiService
import com.example.stockanalysis.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 市场数据仓库接口
 */
interface MarketRepository {
    suspend fun getMarketOverview(): Result<MarketOverview>
    suspend fun getMarketIndices(): Result<List<MarketIndex>>
    suspend fun getMarketStats(): Result<MarketStats>
    suspend fun getSectorPerformance(): Result<SectorPerformance>
}

/**
 * 市场数据仓库实现
 */
@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val stockApiService: StockApiService
) : MarketRepository {

    override suspend fun getMarketOverview(): Result<MarketOverview> {
        return withContext(Dispatchers.IO) {
            try {
                // 获取主要指数
                val indicesResponse = stockApiService.getMarketIndices()
                val statsResponse = stockApiService.getMarketStats()
                val sectorResponse = stockApiService.getSectorPerformance()
                
                if (indicesResponse.isSuccessful && statsResponse.isSuccessful) {
                    val indices = indicesResponse.body() ?: emptyList()
                    val stats = statsResponse.body()
                    val sector = sectorResponse.body()
                    
                    if (stats != null) {
                        Result.success(MarketOverview(
                            marketType = MarketType.A_SHARE,
                            indices = indices,
                            stats = stats,
                            sectorPerformance = sector ?: SectorPerformance(emptyList(), emptyList())
                        ))
                    } else {
                        Result.failure(IOException("Failed to get market stats"))
                    }
                } else {
                    Result.failure(IOException("API error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getMarketIndices(): Result<List<MarketIndex>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = stockApiService.getMarketIndices()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(IOException("API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getMarketStats(): Result<MarketStats> {
        return withContext(Dispatchers.IO) {
            try {
                val response = stockApiService.getMarketStats()
                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(IOException("Empty response"))
                } else {
                    Result.failure(IOException("API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getSectorPerformance(): Result<SectorPerformance> {
        return withContext(Dispatchers.IO) {
            try {
                val response = stockApiService.getSectorPerformance()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: SectorPerformance(emptyList(), emptyList()))
                } else {
                    Result.failure(IOException("API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
