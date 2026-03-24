package com.example.stockanalysis.di

import android.content.Context
import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.data.agent.agents.*
import com.example.stockanalysis.data.agent.tools.*
import com.example.stockanalysis.data.api.LLMApiService
import com.example.stockanalysis.data.llm.LLMService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Agent模块 - Hilt依赖注入（多 Agent 架构）
 */
@Module
@InstallIn(SingletonComponent::class)
object AgentModule {
    
    @Provides
    @Singleton
    fun provideAgentConfigurationManager(
        @ApplicationContext context: Context
    ): AgentConfigurationManager {
        return AgentConfigurationManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAgentToolRegistry(
        quoteTool: GetRealtimeQuoteTool,
        klineTool: GetKLineDataTool,
        indicatorsTool: GetTechnicalIndicatorsTool,
        trendTool: GetTrendAnalysisTool,
        newsTool: SearchNewsTool
    ): AgentToolRegistry {
        return AgentToolRegistry().apply {
            register(quoteTool)
            register(klineTool)
            register(indicatorsTool)
            register(trendTool)
            register(newsTool)
        }
    }
    
    @Provides
    @Singleton
    fun provideAgentPipeline(
        llmService: LLMService,
        toolRegistry: AgentToolRegistry,
        configManager: AgentConfigurationManager
    ): AgentPipeline {
        return AgentPipeline(llmService, toolRegistry, configManager)
    }
    
    @Provides
    @Singleton
    fun provideAgentOrchestrator(
        llmService: LLMService,
        llmApiService: LLMApiService,
        pipeline: AgentPipeline,
        configManager: AgentConfigurationManager,
        quoteTool: GetRealtimeQuoteTool,
        klineTool: GetKLineDataTool,
        indicatorsTool: GetTechnicalIndicatorsTool,
        trendTool: GetTrendAnalysisTool,
        newsTool: SearchNewsTool
    ): AgentOrchestrator {
        return AgentOrchestrator(
            llmService = llmService,
            llmApiService = llmApiService,
            pipeline = pipeline,
            configManager = configManager,
            quoteTool = quoteTool,
            klineTool = klineTool,
            indicatorsTool = indicatorsTool,
            trendTool = trendTool,
            newsTool = newsTool
        )
    }
    
    // 提供各个专业 Agent（用于单独调用场景）
    
    @Provides
    @Singleton
    fun provideTechnicalAnalysisAgent(
        llmService: LLMService,
        toolRegistry: AgentToolRegistry
    ): TechnicalAnalysisAgent {
        return TechnicalAnalysisAgent(llmService, toolRegistry)
    }
    
    @Provides
    @Singleton
    fun provideFundamentalAnalysisAgent(
        llmService: LLMService,
        toolRegistry: AgentToolRegistry
    ): FundamentalAnalysisAgent {
        return FundamentalAnalysisAgent(llmService, toolRegistry)
    }
    
    @Provides
    @Singleton
    fun provideNewsAnalysisAgent(
        llmService: LLMService,
        toolRegistry: AgentToolRegistry
    ): NewsAnalysisAgent {
        return NewsAnalysisAgent(llmService, toolRegistry)
    }
    
    @Provides
    @Singleton
    fun provideRiskAssessmentAgent(
        llmService: LLMService,
        toolRegistry: AgentToolRegistry
    ): RiskAssessmentAgent {
        return RiskAssessmentAgent(llmService, toolRegistry)
    }
    
    @Provides
    @Singleton
    fun provideDecisionAgent(
        llmService: LLMService,
        toolRegistry: AgentToolRegistry
    ): DecisionAgent {
        return DecisionAgent(llmService, toolRegistry)
    }
    
    // 兼容旧版接口
    
    @Provides
    @Singleton
    fun provideStrategyLoader(): StrategyLoader {
        return BuiltInStrategyLoader()
    }
}
