package com.example.stockanalysis.data.repository

import com.example.stockanalysis.data.local.AnalysisResultDao
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.AnalysisSummary
import com.example.stockanalysis.data.model.Decision
import com.example.stockanalysis.data.model.Stock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分析仓库接口
 */
interface AnalysisRepository {
    
    /**
     * 分析单只股票
     */
    fun analyzeStock(symbol: String, stockName: String): Flow<AnalysisState>
    
    /**
     * 批量分析股票
     */
    fun analyzeStocks(stocks: List<Stock>): Flow<BatchAnalysisState>
    
    /**
     * 获取所有分析结果
     */
    fun getAllResults(): Flow<List<AnalysisResult>>
    
    /**
     * 根据ID获取分析结果
     */
    suspend fun getResultById(id: String): AnalysisResult?
    
    /**
     * 获取某只股票的最新分析结果
     */
    suspend fun getLatestResultForStock(symbol: String): AnalysisResult?
    
    /**
     * 获取分析结果统计
     */
    suspend fun getAnalysisStatistics(): AnalysisSummary
    
    /**
     * 获取决策分布统计
     */
    suspend fun getDecisionDistribution(): Map<String, Int>
    
    /**
     * 删除分析结果
     */
    suspend fun deleteResult(id: String)
    
    /**
     * 清空所有历史
     */
    suspend fun clearAllHistory()
    
    /**
     * 获取某只股票的分析历史
     */
    fun getResultsForStock(symbol: String): Flow<List<AnalysisResult>>
}

/**
 * 分析仓库实现
 */
@Singleton
class AnalysisRepositoryImpl @Inject constructor(
    private val analysisEngine: AnalysisEngine,
    private val analysisResultDao: AnalysisResultDao
) : AnalysisRepository {

    override fun analyzeStock(symbol: String, stockName: String): Flow<AnalysisState> {
        return analysisEngine.analyzeStock(symbol, stockName)
    }

    override fun analyzeStocks(stocks: List<Stock>): Flow<BatchAnalysisState> {
        return analysisEngine.analyzeStocks(stocks)
    }

    override fun getAllResults(): Flow<List<AnalysisResult>> {
        return analysisResultDao.getAllResults()
    }

    override suspend fun getResultById(id: String): AnalysisResult? {
        return analysisResultDao.getResultById(id)
    }

    override suspend fun getLatestResultForStock(symbol: String): AnalysisResult? {
        return analysisResultDao.getLatestResultForStock(symbol)
    }

    override suspend fun getAnalysisStatistics(): AnalysisSummary {
        val stats = analysisResultDao.getDecisionStatistics()
        val total = stats.sumOf { it.count }
        val buyCount = stats.filter { 
            it.decision in listOf(Decision.BUY.name, Decision.STRONG_BUY.name) 
        }.sumOf { it.count }
        val sellCount = stats.filter { 
            it.decision in listOf(Decision.SELL.name, Decision.STRONG_SELL.name) 
        }.sumOf { it.count }
        val holdCount = stats.find { it.decision == Decision.HOLD.name }?.count ?: 0
        
        val avgScore = if (total > 0) {
            analysisResultDao.getAverageScore() ?: 0
        } else 0
        
        return AnalysisSummary(
            totalCount = total,
            buyCount = buyCount,
            holdCount = holdCount,
            sellCount = sellCount,
            averageScore = avgScore
        )
    }

    override suspend fun getDecisionDistribution(): Map<String, Int> {
        val stats = analysisResultDao.getDecisionStatistics()
        return stats.associate { it.decision to it.count }
    }

    override suspend fun deleteResult(id: String) {
        analysisResultDao.deleteResultById(id)
    }

    override suspend fun clearAllHistory() {
        analysisResultDao.deleteAllResults()
    }

    override fun getResultsForStock(symbol: String): Flow<List<AnalysisResult>> {
        return analysisResultDao.getResultsForStock(symbol)
    }
}
