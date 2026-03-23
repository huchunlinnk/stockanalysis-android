package com.example.stockanalysis.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.stockanalysis.data.cache.CacheDao
import com.example.stockanalysis.data.cache.CacheEntity
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.ChatSession
import com.example.stockanalysis.data.model.FundamentalData
import com.example.stockanalysis.data.model.KLineData
import com.example.stockanalysis.data.model.Stock
import com.example.stockanalysis.data.model.UserMemory
import com.example.stockanalysis.data.portfolio.*
import com.example.stockanalysis.data.backtest.*

/**
 * 应用数据库
 * 包含：股票列表、K线数据、分析结果、持仓管理、基本面数据
 */
@Database(
    entities = [
        Stock::class,
        AnalysisResult::class,
        KLineData::class,
        PortfolioHolding::class,
        PortfolioTransaction::class,
        CashFlow::class,
        CorporateAction::class,
        PortfolioSnapshot::class,
        BacktestResult::class,
        BacktestSignal::class,
        UserMemory::class,
        FundamentalData::class,
        ChatSession::class,
        CacheEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StockDatabase : RoomDatabase() {
    
    abstract fun stockDao(): StockDao
    abstract fun analysisResultDao(): AnalysisResultDao
    abstract fun kLineDataDao(): KLineDataDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun backtestDao(): BacktestDao
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun fundamentalDao(): FundamentalDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun cacheDao(): CacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null
        
        fun getDatabase(context: Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stock_analysis_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
