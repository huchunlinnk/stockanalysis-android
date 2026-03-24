package com.example.stockanalysis.di

import android.content.Context
import com.example.stockanalysis.data.datasource.TushareDataSource
import com.example.stockanalysis.data.local.LocalDataService
import com.example.stockanalysis.data.repository.*
import com.example.stockanalysis.service.MultiChannelNotificationService
import com.example.stockanalysis.service.NotificationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 应用模块 - Hilt依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAnalysisEngine(
        localDataService: LocalDataService,
        tushareDataSource: TushareDataSource,
        crashReportingManager: com.example.stockanalysis.util.CrashReportingManager,
        agentOrchestrator: com.example.stockanalysis.data.agent.AgentOrchestrator
    ): AnalysisEngine {
        return AnalysisEngine(localDataService, tushareDataSource, crashReportingManager, agentOrchestrator)
    }
    
    @Provides
    @Singleton
    fun provideAnalysisRepository(
        analysisEngine: AnalysisEngine,
        analysisResultDao: com.example.stockanalysis.data.local.AnalysisResultDao
    ): AnalysisRepository {
        return AnalysisRepositoryImpl(analysisEngine, analysisResultDao)
    }
    

}
