package com.example.stockanalysis.di

import android.content.Context
import com.example.stockanalysis.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块 - Hilt依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StockDatabase {
        return StockDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideStockDao(database: StockDatabase): StockDao {
        return database.stockDao()
    }
    
    @Provides
    @Singleton
    fun provideAnalysisResultDao(database: StockDatabase): AnalysisResultDao {
        return database.analysisResultDao()
    }
    
    @Provides
    @Singleton
    fun provideKLineDataDao(database: StockDatabase): KLineDataDao {
        return database.kLineDataDao()
    }
    
    @Provides
    @Singleton
    fun providePortfolioDao(database: StockDatabase): com.example.stockanalysis.data.portfolio.PortfolioDao {
        return database.portfolioDao()
    }
    
    @Provides
    @Singleton
    fun provideLocalDataService(kLineDataDao: KLineDataDao): LocalDataService {
        return LocalDataService(kLineDataDao)
    }

    @Provides
    @Singleton
    fun provideUserMemoryDao(database: StockDatabase): UserMemoryDao {
        return database.userMemoryDao()
    }
    
    @Provides
    @Singleton
    fun provideFundamentalDao(database: StockDatabase): FundamentalDao {
        return database.fundamentalDao()
    }
    
    @Provides
    @Singleton
    fun provideChatSessionDao(database: StockDatabase): ChatSessionDao {
        return database.chatSessionDao()
    }
}
