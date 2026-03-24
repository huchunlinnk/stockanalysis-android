package com.example.stockanalysis.di

import com.example.stockanalysis.data.analysis.HistoryComparisonService
import com.example.stockanalysis.data.backtest.BacktestDao
import com.example.stockanalysis.data.backtest.BacktestEngine
import com.example.stockanalysis.data.local.AnalysisResultDao
import com.example.stockanalysis.data.local.KLineDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 回测模块 - Hilt依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object BacktestModule {
    
    @Provides
    @Singleton
    fun provideBacktestDao(database: com.example.stockanalysis.data.local.StockDatabase): BacktestDao {
        return database.backtestDao()
    }
    
    @Provides
    @Singleton
    fun provideBacktestEngine(
        analysisResultDao: AnalysisResultDao,
        backtestDao: BacktestDao,
        historyComparisonService: HistoryComparisonService
    ): BacktestEngine {
        return BacktestEngine(analysisResultDao, backtestDao, historyComparisonService)
    }
    
    @Provides
    @Singleton
    fun provideHistoryComparisonService(
        analysisResultDao: AnalysisResultDao,
        kLineDataDao: KLineDataDao
    ): HistoryComparisonService {
        return HistoryComparisonService(analysisResultDao, kLineDataDao)
    }
}
