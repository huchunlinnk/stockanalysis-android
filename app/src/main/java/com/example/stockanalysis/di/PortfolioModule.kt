package com.example.stockanalysis.di

import com.example.stockanalysis.data.datasource.DataSourceManager
import com.example.stockanalysis.data.portfolio.PortfolioDao
import com.example.stockanalysis.data.portfolio.PortfolioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 持仓管理模块 - Hilt依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object PortfolioModule {
    
    @Provides
    @Singleton
    fun providePortfolioRepository(
        portfolioDao: PortfolioDao,
        dataSourceManager: DataSourceManager
    ): PortfolioRepository {
        return PortfolioRepository(portfolioDao, dataSourceManager)
    }
}
