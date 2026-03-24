package com.example.stockanalysis.di

import com.example.stockanalysis.data.datasource.*
import com.example.stockanalysis.data.local.FundamentalDao
import com.example.stockanalysis.data.local.KLineDataDao
import com.example.stockanalysis.data.local.LocalDataService
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.data.repository.FundamentalRepository
import com.example.stockanalysis.data.repository.FundamentalRepositoryImpl
import com.example.stockanalysis.data.repository.StockRepository
import com.example.stockanalysis.data.repository.StockRepositoryV2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 数据源模块 - Hilt依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    
    /**
     * 提供东方财富数据源
     */
    @Provides
    @Singleton
    fun provideEFinanceDataSource(okHttpClient: OkHttpClient): EFinanceDataSource {
        return EFinanceDataSource(okHttpClient)
    }
    
    /**
     * 提供AkShare数据源
     */
    @Provides
    @Singleton
    fun provideAkShareDataSource(okHttpClient: OkHttpClient): AkShareDataSource {
        return AkShareDataSource(okHttpClient)
    }
    
    /**
     * 提供Yahoo Finance数据源（美股数据）
     */
    @Provides
    @Singleton
    fun provideYFinanceDataSource(okHttpClient: OkHttpClient): YFinanceDataSource {
        return YFinanceDataSource(okHttpClient)
    }
    
    /**
     * 提供Tushare数据源
     */
    @Provides
    @Singleton
    fun provideTushareDataSource(
        okHttpClient: OkHttpClient,
        preferencesManager: PreferencesManager
    ): TushareDataSource {
        return TushareDataSource(okHttpClient, preferencesManager)
    }

    /**
     * 提供本地数据源
     */
    @Provides
    @Singleton
    fun provideLocalDataSource(
        localDataService: LocalDataService,
        kLineDataDao: KLineDataDao
    ): LocalDataSource {
        return LocalDataSource(localDataService, kLineDataDao)
    }

    /**
     * 提供Baostock数据源（财务数据补充）
     */
    @Provides
    @Singleton
    fun provideBaostockDataSource(okHttpClient: OkHttpClient): BaostockDataSource {
        return BaostockDataSource(okHttpClient)
    }

    /**
     * 提供数据源管理器
     */
    @Provides
    @Singleton
    fun provideDataSourceManager(
        eFinanceDataSource: EFinanceDataSource,
        akShareDataSource: AkShareDataSource,
        yFinanceDataSource: YFinanceDataSource,
        baostockDataSource: BaostockDataSource,
        localDataSource: LocalDataSource,
        chipDistributionDataSource: com.example.stockanalysis.data.datasource.EFinanceChipDistributionDataSource
    ): DataSourceManager {
        return DataSourceManager(eFinanceDataSource, akShareDataSource, yFinanceDataSource, baostockDataSource, localDataSource, chipDistributionDataSource)
    }
    
    /**
     * 提供股票数据仓库V2（带多数据源故障切换）
     */
    @Provides
    @Singleton
    fun provideStockRepository(
        stockDao: com.example.stockanalysis.data.local.StockDao,
        localDataService: LocalDataService,
        dataSourceManager: DataSourceManager,
        localDataSource: LocalDataSource
    ): StockRepository {
        return StockRepositoryV2(stockDao, localDataService, dataSourceManager, localDataSource)
    }
    
    /**
     * 提供基本面数据源
     */
    @Provides
    @Singleton
    fun provideFundamentalDataSource(okHttpClient: OkHttpClient): FundamentalDataSource {
        return FundamentalDataSource(okHttpClient)
    }
    
    /**
     * 提供基本面数据仓库
     */
    @Provides
    @Singleton
    fun provideFundamentalRepository(
        fundamentalDao: FundamentalDao,
        fundamentalDataSource: FundamentalDataSource
    ): FundamentalRepository {
        return FundamentalRepositoryImpl(fundamentalDao, fundamentalDataSource)
    }
}
