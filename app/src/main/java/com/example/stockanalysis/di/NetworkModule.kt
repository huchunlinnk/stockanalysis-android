package com.example.stockanalysis.di

import android.content.Context
import com.example.stockanalysis.BuildConfig
import com.example.stockanalysis.data.api.LLMApiService
import com.example.stockanalysis.data.api.StockApiService
import com.example.stockanalysis.data.local.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 网络模块 - 提供 Retrofit 客户端
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供默认 OkHttpClient（用于普通HTTP请求）
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Debug版本显示详细日志，Release版本只显示基本信息
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 提供基础 OkHttpClient（不含认证）- 作为LLM Client的基础
     */
    @Provides
    @Singleton
    @Named("base")
    fun provideBaseOkHttpClient(okHttpClient: OkHttpClient): OkHttpClient {
        return okHttpClient
    }

    /**
     * 提供旧版OkHttpClient（兼容性）- 废弃，请使用无Named修饰符的版本
     */
    @Provides
    @Singleton
    @Named("legacy")
    @Deprecated("Use provideOkHttpClient() instead")
    fun provideLegacyOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Debug版本显示详细日志，Release版本只显示基本信息
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 提供带LLM认证的 OkHttpClient
     */
    @Provides
    @Singleton
    @Named("llm")
    fun provideLLMOkHttpClient(
        @Named("base") baseClient: OkHttpClient,
        preferencesManager: PreferencesManager
    ): OkHttpClient {

        val authInterceptor = Interceptor { chain ->
            val apiKey = preferencesManager.getLLMApiKey()
            val request = if (apiKey.isNotEmpty()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        return baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
    }

    /**
     * 提供股票数据 API
     * 注意：目前使用EFinanceDataSource直接调用API，此Service暂时未使用
     */
    @Provides
    @Singleton
    @Named("stockApi")
    fun provideStockRetrofit(@Named("base") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://push2.eastmoney.com/")  // 东方财富API
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 提供股票 API 服务
     * 注意：目前使用EFinanceDataSource直接调用API，此Service暂时未使用
     */
    @Provides
    @Singleton
    fun provideStockApiService(@Named("stockApi") retrofit: Retrofit): StockApiService {
        return retrofit.create(StockApiService::class.java)
    }

    /**
     * 提供 LLM API
     * 支持动态配置BaseUrl（从设置中读取）
     */
    @Provides
    @Singleton
    @Named("llmApi")
    fun provideLLMRetrofit(
        @Named("llm") okHttpClient: OkHttpClient,
        preferencesManager: PreferencesManager
    ): Retrofit {
        val baseUrl = preferencesManager.getLLMBaseUrl().ifEmpty {
            "https://api.openai.com/"  // 默认使用OpenAI
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 提供 LLM API 服务
     */
    @Provides
    @Singleton
    fun provideLLMApiService(@Named("llmApi") retrofit: Retrofit): LLMApiService {
        return retrofit.create(LLMApiService::class.java)
    }
}
