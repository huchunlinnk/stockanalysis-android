package com.example.stockanalysis.data.portfolio

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date

/**
 * 持仓管理DAO
 */
@Dao
interface PortfolioDao {
    
    // ==================== 持仓操作 ====================
    
    @Query("SELECT * FROM portfolio_holdings ORDER BY updatedAt DESC")
    fun getAllHoldings(): LiveData<List<PortfolioHolding>>
    
    @Query("SELECT * FROM portfolio_holdings WHERE stockSymbol = :symbol LIMIT 1")
    suspend fun getHoldingBySymbol(symbol: String): PortfolioHolding?
    
    @Query("SELECT * FROM portfolio_holdings WHERE totalQuantity > 0")
    suspend fun getActiveHoldings(): List<PortfolioHolding>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: PortfolioHolding)
    
    @Update
    suspend fun updateHolding(holding: PortfolioHolding)
    
    @Delete
    suspend fun deleteHolding(holding: PortfolioHolding)
    
    @Query("DELETE FROM portfolio_holdings WHERE stockSymbol = :symbol")
    suspend fun deleteHoldingBySymbol(symbol: String)
    
    // ==================== 交易记录操作 ====================
    
    @Query("SELECT * FROM portfolio_transactions ORDER BY tradeDate DESC, createdAt DESC")
    fun getAllTransactions(): LiveData<List<PortfolioTransaction>>
    
    @Query("SELECT * FROM portfolio_transactions WHERE stockSymbol = :symbol ORDER BY tradeDate DESC")
    suspend fun getTransactionsBySymbol(symbol: String): List<PortfolioTransaction>
    
    @Query("SELECT * FROM portfolio_transactions WHERE type = :type ORDER BY tradeDate DESC")
    suspend fun getTransactionsByType(type: TransactionType): List<PortfolioTransaction>
    
    @Query("""
        SELECT * FROM portfolio_transactions 
        WHERE tradeDate BETWEEN :startDate AND :endDate 
        ORDER BY tradeDate DESC
    """)
    suspend fun getTransactionsByDateRange(startDate: Date, endDate: Date): List<PortfolioTransaction>
    
    @Insert
    suspend fun insertTransaction(transaction: PortfolioTransaction)
    
    @Insert
    suspend fun insertTransactions(transactions: List<PortfolioTransaction>)
    
    @Delete
    suspend fun deleteTransaction(transaction: PortfolioTransaction)
    
    @Query("DELETE FROM portfolio_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)
    
    // ==================== 资金流水操作 ====================
    
    @Query("SELECT * FROM portfolio_cash ORDER BY date DESC")
    fun getAllCashFlows(): LiveData<List<CashFlow>>
    
    @Query("SELECT * FROM portfolio_cash WHERE type = :type ORDER BY date DESC")
    suspend fun getCashFlowsByType(type: CashFlowType): List<CashFlow>
    
    @Query("""
        SELECT SUM(CASE WHEN direction = 'IN' THEN amount ELSE -amount END) 
        FROM portfolio_cash
    """)
    suspend fun getCashBalance(): Double?
    
    @Insert
    suspend fun insertCashFlow(cashFlow: CashFlow)
    
    // ==================== 公司行为操作 ====================
    
    @Query("SELECT * FROM portfolio_corporate_actions WHERE stockSymbol = :symbol ORDER BY exDate DESC")
    suspend fun getCorporateActionsBySymbol(symbol: String): List<CorporateAction>
    
    @Query("SELECT * FROM portfolio_corporate_actions WHERE isProcessed = 0 ORDER BY exDate")
    suspend fun getPendingCorporateActions(): List<CorporateAction>
    
    @Insert
    suspend fun insertCorporateAction(action: CorporateAction)
    
    @Update
    suspend fun updateCorporateAction(action: CorporateAction)
    
    // ==================== 快照操作 ====================
    
    @Query("SELECT * FROM portfolio_snapshots ORDER BY snapshotDate DESC")
    fun getAllSnapshots(): LiveData<List<PortfolioSnapshot>>
    
    @Query("SELECT * FROM portfolio_snapshots WHERE snapshotDate = :date LIMIT 1")
    suspend fun getSnapshotByDate(date: Date): PortfolioSnapshot?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: PortfolioSnapshot)
    
    // ==================== 统计查询 ====================
    
    @Query("""
        SELECT 
            COALESCE(SUM(marketValue), 0) as totalMarketValue,
            COALESCE(SUM(totalCost), 0) as totalCost,
            COALESCE(SUM(profitLoss), 0) as totalProfitLoss,
            COUNT(*) as holdingCount,
            COUNT(DISTINCT stockSymbol) as stockCount
        FROM portfolio_holdings 
        WHERE totalQuantity > 0
    """)
    suspend fun getPortfolioTotals(): PortfolioTotals
    
    @Query("""
        SELECT 
            COALESCE(SUM(amount), 0)
        FROM portfolio_transactions 
        WHERE type = 'BUY'
    """)
    suspend fun getTotalBuyAmount(): Double?
    
    @Query("""
        SELECT 
            COALESCE(SUM(amount), 0)
        FROM portfolio_transactions 
        WHERE type = 'SELL'
    """)
    suspend fun getTotalSellAmount(): Double?
    
    // ==================== 复杂查询 ====================
    
    @Transaction
    @Query("SELECT * FROM portfolio_holdings WHERE totalQuantity > 0")
    suspend fun getHoldingsWithTransactions(): List<HoldingWithTransactions>
    
    @Query("""
        SELECT * FROM portfolio_transactions 
        WHERE stockSymbol = :symbol 
        AND type IN ('BUY', 'SELL')
        ORDER BY tradeDate ASC, createdAt ASC
    """)
    suspend fun getTradeHistoryForHolding(symbol: String): List<PortfolioTransaction>
    
    @Query("""
        SELECT 
            stockSymbol,
            stockName,
            SUM(CASE WHEN type = 'BUY' THEN quantity ELSE -quantity END) as netQuantity,
            SUM(CASE WHEN type = 'BUY' THEN amount ELSE -amount END) as netAmount
        FROM portfolio_transactions 
        WHERE type IN ('BUY', 'SELL')
        GROUP BY stockSymbol, stockName
    """)
    suspend fun getPositionSummary(): List<PositionSummary>
}

/**
 * 投资组合合计数据类
 */
data class PortfolioTotals(
    val totalMarketValue: Double,
    val totalCost: Double,
    val totalProfitLoss: Double,
    val holdingCount: Int,
    val stockCount: Int
)

/**
 * 持仓汇总
 */
data class PositionSummary(
    val stockSymbol: String,
    val stockName: String,
    val netQuantity: Int,
    val netAmount: Double
)
