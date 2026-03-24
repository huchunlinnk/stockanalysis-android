package com.example.stockanalysis.di

import android.content.Context
import com.example.stockanalysis.data.news.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 新闻搜索模块 - Hilt依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object NewsSearchModule {
    
    /**
     * 提供Tavily新闻搜索服务
     * 从SharedPreferences读取API Key
     */
    @Provides
    @Singleton
    fun provideTavilyNewsService(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): TavilyNewsService {
        // 从SharedPreferences读取API Key
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("tavily_api_key", "") ?: ""
        
        return TavilyNewsService(okHttpClient, apiKey)
    }
    
    /**
     * 提供Bocha新闻搜索服务
     * 从SharedPreferences读取API Key
     */
    @Provides
    @Singleton
    fun provideBochaNewsService(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): BochaNewsService {
        // 从SharedPreferences读取API Key
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("bocha_api_key", "") ?: ""
        
        return BochaNewsService(okHttpClient, apiKey)
    }
    
    /**
     * 提供新闻搜索管理器
     */
    @Provides
    @Singleton
    fun provideNewsSearchManager(
        tavilyService: TavilyNewsService,
        bochaService: BochaNewsService
    ): NewsSearchManager {
        return NewsSearchManager(tavilyService, bochaService)
    }
}
