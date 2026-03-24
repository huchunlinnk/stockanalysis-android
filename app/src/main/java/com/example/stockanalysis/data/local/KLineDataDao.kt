package com.example.stockanalysis.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.stockanalysis.data.model.KLineData
import java.util.Date

/**
 * K线数据 DAO
 */
@Dao
interface KLineDataDao {
    
    @Query("SELECT * FROM kline_data WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getKLineData(symbol: String, limit: Int = 100): List<KLineData>
    
    @Query("SELECT * FROM kline_data WHERE symbol = :symbol AND period = :period ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getKLineDataByPeriod(symbol: String, period: String, limit: Int = 100): List<KLineData>
    
    @Query("SELECT * FROM kline_data WHERE symbol = :symbol AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getKLineDataRange(symbol: String, startDate: Date, endDate: Date): List<KLineData>
    
    @Query("SELECT * FROM kline_data WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestKLine(symbol: String): KLineData?
    
    @Query("SELECT * FROM kline_data WHERE symbol = :symbol AND date(timestamp) = date(:date) LIMIT 1")
    suspend fun getKLineByDate(symbol: String, date: Date): KLineData?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKLineData(data: KLineData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKLineDataList(dataList: List<KLineData>)
    
    @Query("SELECT EXISTS(SELECT 1 FROM kline_data WHERE symbol = :symbol AND date(timestamp) = date('now', 'localtime'))")
    suspend fun hasTodayData(symbol: String): Boolean
    
    @Query("DELETE FROM kline_data WHERE symbol = :symbol")
    suspend fun deleteKLineDataBySymbol(symbol: String)
    
    @Query("DELETE FROM kline_data WHERE symbol = :symbol AND timestamp < :beforeDate")
    suspend fun deleteOldData(symbol: String, beforeDate: Date)
    
    @Query("SELECT COUNT(*) FROM kline_data WHERE symbol = :symbol")
    suspend fun getDataCount(symbol: String): Int
    
    @Query("SELECT DISTINCT symbol FROM kline_data")
    suspend fun getAllSymbols(): List<String>
    
    @Query("""
        SELECT * FROM kline_data 
        WHERE symbol = :symbol 
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getKLineDataPaged(symbol: String, limit: Int, offset: Int): List<KLineData>
}
