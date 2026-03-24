package com.example.stockanalysis.di

import android.content.Context
import com.example.stockanalysis.data.api.LLMApiService
import com.example.stockanalysis.data.import.SmartImportService
import com.example.stockanalysis.data.local.StockDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 智能导入模块 - Hilt依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object ImportModule {
    
    @Provides
    @Singleton
    fun provideSmartImportService(
        @ApplicationContext context: Context,
        stockDao: StockDao,
        llmService: LLMApiService
    ): SmartImportService {
        return SmartImportService(context, stockDao, llmService)
    }
}
