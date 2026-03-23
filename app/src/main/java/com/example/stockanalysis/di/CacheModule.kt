package com.example.stockanalysis.di

import com.example.stockanalysis.data.cache.CacheDao
import com.example.stockanalysis.data.cache.SmartCacheManager
import com.example.stockanalysis.data.local.StockDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 缓存模块
 * 提供SmartCacheManager和相关依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    
    @Provides
    @Singleton
    fun provideCacheDao(database: StockDatabase): CacheDao {
        return database.cacheDao()
    }
    
    @Provides
    @Singleton
    fun provideSmartCacheManager(
        cacheDao: CacheDao,
        gson: Gson
    ): SmartCacheManager {
        return SmartCacheManager(cacheDao, gson)
    }
}
