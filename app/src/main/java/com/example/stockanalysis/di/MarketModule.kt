package com.example.stockanalysis.di

import com.example.stockanalysis.data.api.LLMApiService
import com.example.stockanalysis.data.api.StockApiService
import com.example.stockanalysis.data.datasource.DataSourceManager
import com.example.stockanalysis.data.local.StockDao
import com.example.stockanalysis.data.market.*
import com.example.stockanalysis.data.repository.MarketRepository
import com.example.stockanalysis.data.repository.MarketRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 市场复盘模块 - Hilt依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object MarketModule {
    
    @Provides
    @Singleton
    fun provideMarketReviewService(
        dataSourceManager: DataSourceManager,
        llmService: LLMApiService,
        stockDao: StockDao
    ): MarketReviewService {
        return MarketReviewServiceImpl(dataSourceManager, llmService, stockDao)
    }
    
    @Provides
    @Singleton
    fun provideMarketRepository(
        stockApiService: StockApiService
    ): MarketRepository {
        return MarketRepositoryImpl(stockApiService)
    }
}
