package com.example.stockanalysis.data.local

import androidx.room.*
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.DecisionCount
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 分析结果 DAO
 */
@Dao
interface AnalysisResultDao {
    
    @Query("SELECT * FROM analysis_results ORDER BY analysisTime DESC")
    fun getAllResults(): Flow<List<AnalysisResult>>
    
    @Query("SELECT * FROM analysis_results ORDER BY analysisTime DESC")
    suspend fun getAllResultsSync(): List<AnalysisResult>
    
    @Query("SELECT * FROM analysis_results WHERE stockSymbol = :symbol ORDER BY analysisTime DESC")
    fun getResultsForStock(symbol: String): Flow<List<AnalysisResult>>
    
    @Query("SELECT * FROM analysis_results WHERE stockSymbol = :symbol ORDER BY analysisTime DESC")
    fun getResultsBySymbol(symbol: String): Flow<List<AnalysisResult>>
    
    @Query("SELECT * FROM analysis_results WHERE stockSymbol = :symbol ORDER BY analysisTime DESC LIMIT 1")
    suspend fun getLatestResultForStock(symbol: String): AnalysisResult?
    
    @Query("SELECT * FROM analysis_results WHERE stockSymbol = :symbol ORDER BY analysisTime DESC LIMIT 1")
    suspend fun getLatestResultBySymbol(symbol: String): AnalysisResult?
    
    @Query("SELECT * FROM analysis_results WHERE id = :id LIMIT 1")
    suspend fun getResultById(id: String): AnalysisResult?
    
    @Query("SELECT * FROM analysis_results WHERE isSynced = 0")
    suspend fun getUnsyncedResults(): List<AnalysisResult>
    
    @Query("SELECT COUNT(*) FROM analysis_results")
    suspend fun getResultCount(): Int
    
    @Query("SELECT COUNT(*) FROM analysis_results WHERE stockSymbol = :symbol")
    suspend fun getResultCountBySymbol(symbol: String): Int
    
    @Query("SELECT AVG(score) FROM analysis_results")
    suspend fun getAverageScore(): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: AnalysisResult)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<AnalysisResult>)
    
    @Update
    suspend fun updateResult(result: AnalysisResult)
    
    @Delete
    suspend fun deleteResult(result: AnalysisResult)
    
    @Query("DELETE FROM analysis_results WHERE id = :id")
    suspend fun deleteResultById(id: String)
    
    @Query("DELETE FROM analysis_results WHERE stockSymbol = :symbol")
    suspend fun deleteResultsBySymbol(symbol: String)
    
    @Query("DELETE FROM analysis_results")
    suspend fun deleteAllResults()
    
    @Query("UPDATE analysis_results SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("UPDATE analysis_results SET isNotified = 1 WHERE id = :id")
    suspend fun markAsNotified(id: String)
    
    @Query("""
        SELECT * FROM analysis_results 
        WHERE analysisTime >= :startTime AND analysisTime <= :endTime 
        ORDER BY analysisTime DESC
    """)
    suspend fun getResultsByTimeRange(startTime: Date, endTime: Date): List<AnalysisResult>
    
    @Query("""
        SELECT decision, COUNT(*) as count FROM analysis_results 
        GROUP BY decision
    """)
    suspend fun getDecisionStatistics(): List<DecisionCount>
    
    @Query("SELECT * FROM analysis_results WHERE stockSymbol = :symbol ORDER BY analysisTime ASC")
    suspend fun getAnalysisHistory(symbol: String): List<AnalysisResult>
    
    // ==================== 历史信号对比查询 ====================
    
    /**
     * 获取某只股票的历史分析记录（带分页）
     */
    @Query("""
        SELECT * FROM analysis_results 
        WHERE stockSymbol = :symbol 
        ORDER BY analysisTime DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getAnalysisHistoryPaged(symbol: String, limit: Int, offset: Int): List<AnalysisResult>
    
    /**
     * 获取特定时间段内的分析结果
     */
    @Query("""
        SELECT * FROM analysis_results 
        WHERE stockSymbol = :symbol 
        AND analysisTime >= :startTime AND analysisTime <= :endTime 
        ORDER BY analysisTime DESC
    """)
    suspend fun getResultsByTimeRange(
        symbol: String, 
        startTime: Date, 
        endTime: Date
    ): List<AnalysisResult>
    
    /**
     * 获取所有股票在指定时间范围内的分析结果
     */
    @Query("""
        SELECT * FROM analysis_results 
        WHERE analysisTime >= :startTime AND analysisTime <= :endTime 
        ORDER BY analysisTime DESC
    """)
    fun getAllResultsByTimeRange(startTime: Date, endTime: Date): Flow<List<AnalysisResult>>
    
    /**
     * 获取某只股票的决策统计信息
     */
    @Query("""
        SELECT 
            decision as decision,
            COUNT(*) as count,
            AVG(score) as avgScore,
            MAX(analysisTime) as lastTime
        FROM analysis_results 
        WHERE stockSymbol = :symbol 
        GROUP BY decision
    """)
    suspend fun getDecisionStatsBySymbol(symbol: String): List<DecisionStat>
    
    /**
     * 获取某只股票最近的分析结果（用于信号验证）
     */
    @Query("""
        SELECT * FROM analysis_results 
        WHERE stockSymbol = :symbol 
        AND analysisTime <= :beforeDate
        AND decision IN ('BUY', 'STRONG_BUY', 'SELL', 'STRONG_SELL')
        ORDER BY analysisTime DESC 
        LIMIT :limit
    """)
    suspend fun getRecentTradeSignals(
        symbol: String, 
        beforeDate: Date, 
        limit: Int = 20
    ): List<AnalysisResult>
    
    /**
     * 获取需要验证的信号（分析后5-30天的）
     */
    @Query("""
        SELECT * FROM analysis_results 
        WHERE analysisTime BETWEEN :startTime AND :endTime
        AND decision IN ('BUY', 'STRONG_BUY', 'SELL', 'STRONG_SELL')
        ORDER BY analysisTime DESC
    """)
    suspend fun getSignalsNeedingValidation(
        startTime: Date,
        endTime: Date
    ): List<AnalysisResult>
    
    /**
     * 获取某只股票的信号数量
     */
    @Query("""
        SELECT COUNT(*) FROM analysis_results 
        WHERE stockSymbol = :symbol 
        AND decision IN ('BUY', 'STRONG_BUY', 'SELL', 'STRONG_SELL')
    """)
    suspend fun getSignalCount(symbol: String): Int
    
    /**
     * 获取某类型决策的历史记录
     */
    @Query("""
        SELECT * FROM analysis_results 
        WHERE stockSymbol = :symbol AND decision = :decision
        ORDER BY analysisTime DESC
    """)
    suspend fun getResultsByDecision(symbol: String, decision: String): List<AnalysisResult>
    
    /**
     * 获取最近的分析结果（所有股票）
     */
    @Query("""
        SELECT * FROM analysis_results 
        ORDER BY analysisTime DESC 
        LIMIT :limit
    """)
    suspend fun getRecentResults(limit: Int = 50): List<AnalysisResult>
}

/**
 * 决策统计
 */
data class DecisionStat(
    val decision: String,
    val count: Int,
    val avgScore: Double,
    val lastTime: Date
)
