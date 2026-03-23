package com.example.stockanalysis.data.backtest

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date

/**
 * 回测DAO
 */
@Dao
interface BacktestDao {
    
    // ==================== 回测结果操作 ====================
    
    @Query("SELECT * FROM backtest_results ORDER BY createdAt DESC")
    fun getAllResults(): LiveData<List<BacktestResult>>
    
    @Query("SELECT * FROM backtest_results WHERE stockSymbol = :symbol ORDER BY createdAt DESC")
    suspend fun getResultsBySymbol(symbol: String): List<BacktestResult>
    
    @Query("SELECT * FROM backtest_results WHERE strategyId = :strategyId ORDER BY createdAt DESC")
    suspend fun getResultsByStrategy(strategyId: String): List<BacktestResult>
    
    @Query("""
        SELECT * FROM backtest_results 
        WHERE stockSymbol = :symbol AND strategyId = :strategyId 
        ORDER BY createdAt DESC LIMIT 1
    """)
    suspend fun getLatestResult(symbol: String, strategyId: String): BacktestResult?
    
    @Insert
    suspend fun insertResult(result: BacktestResult)
    
    @Update
    suspend fun updateResult(result: BacktestResult)
    
    @Delete
    suspend fun deleteResult(result: BacktestResult)
    
    @Query("DELETE FROM backtest_results WHERE id = :id")
    suspend fun deleteResultById(id: String)
    
    // ==================== 信号详情操作 ====================
    
    @Query("SELECT * FROM backtest_signals WHERE backtestResultId = :resultId ORDER BY signalDate")
    suspend fun getSignalsByResultId(resultId: String): List<BacktestSignal>
    
    @Query("""
        SELECT * FROM backtest_signals 
        WHERE backtestResultId = :resultId AND isCorrect = 1 
        ORDER BY signalDate
    """)
    suspend fun getCorrectSignals(resultId: String): List<BacktestSignal>
    
    @Query("""
        SELECT * FROM backtest_signals 
        WHERE backtestResultId = :resultId AND isCorrect = 0 
        ORDER BY signalDate
    """)
    suspend fun getIncorrectSignals(resultId: String): List<BacktestSignal>
    
    @Insert
    suspend fun insertSignal(signal: BacktestSignal)
    
    @Insert
    suspend fun insertSignals(signals: List<BacktestSignal>)
    
    // ==================== 统计查询 ====================
    
    @Query("""
        SELECT 
            AVG(accuracy) as avgAccuracy,
            AVG(totalReturn) as avgReturn,
            AVG(winRate) as avgWinRate
        FROM backtest_results 
        WHERE strategyId = :strategyId
    """)
    suspend fun getStrategyStats(strategyId: String): StrategyStats?
    
    @Query("""
        SELECT 
            AVG(accuracy) as avgAccuracy,
            COUNT(*) as testCount
        FROM backtest_results 
        WHERE stockSymbol = :symbol
    """)
    suspend fun getStockStats(symbol: String): StockStats?
    
    @Query("""
        SELECT * FROM backtest_results 
        ORDER BY accuracy DESC 
        LIMIT :limit
    """)
    suspend fun getTopResults(limit: Int = 10): List<BacktestResult>
    
    @Query("DELETE FROM backtest_results WHERE createdAt < :date")
    suspend fun deleteOldResults(date: Date)
}

/**
 * 策略统计数据
 */
data class StrategyStats(
    val avgAccuracy: Double?,
    val avgReturn: Double?,
    val avgWinRate: Double?
)

/**
 * 股票统计数据
 */
data class StockStats(
    val avgAccuracy: Double?,
    val testCount: Int
)
