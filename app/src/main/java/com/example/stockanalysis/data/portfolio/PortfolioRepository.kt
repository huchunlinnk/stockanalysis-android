package com.example.stockanalysis.data.portfolio

import com.example.stockanalysis.data.datasource.DataSourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 持仓管理仓库
 */
@Singleton
class PortfolioRepository @Inject constructor(
    private val portfolioDao: PortfolioDao,
    private val dataSourceManager: DataSourceManager
) {
    
    // ==================== 查询操作 ====================
    
    fun getAllHoldings() = portfolioDao.getAllHoldings()
    
    fun getAllTransactions() = portfolioDao.getAllTransactions()
    
    fun getAllCashFlows() = portfolioDao.getAllCashFlows()
    
    suspend fun getHoldingBySymbol(symbol: String) = portfolioDao.getHoldingBySymbol(symbol)
    
    suspend fun getTransactionsBySymbol(symbol: String) = 
        portfolioDao.getTransactionsBySymbol(symbol)
    
    // ==================== 买入操作 ====================
    
    /**
     * 买入股票
     * 
     * @param symbol 股票代码
     * @param name 股票名称
     * @param quantity 买入数量
     * @param price 买入价格
     * @param commission 佣金
     * @param tradeDate 交易日期
     */
    suspend fun buyStock(
        symbol: String,
        name: String,
        quantity: Int,
        price: Double,
        commission: Double = 0.0,
        tradeDate: Date = Date()
    ): Result<PortfolioTransaction> = withContext(Dispatchers.IO) {
        try {
            // 参数校验
            if (quantity <= 0) {
                return@withContext Result.failure(IllegalArgumentException("买入数量必须大于0"))
            }
            if (price <= 0) {
                return@withContext Result.failure(IllegalArgumentException("买入价格必须大于0"))
            }
            
            // 计算金额和费用
            val amount = quantity * price
            val stampTax = 0.0  // 买入不收印花税
            val transferFee = calculateTransferFee(amount)
            val totalFees = commission + stampTax + transferFee
            
            // 1. 创建交易记录
            val transaction = PortfolioTransaction(
                stockSymbol = symbol,
                stockName = name,
                type = TransactionType.BUY,
                direction = TradeDirection.LONG,
                quantity = quantity,
                price = price,
                amount = amount,
                commission = commission,
                stampTax = stampTax,
                transferFee = transferFee,
                totalFees = totalFees,
                tradeDate = tradeDate
            )
            portfolioDao.insertTransaction(transaction)
            
            // 2. 创建资金流水
            val cashFlow = CashFlow(
                type = CashFlowType.BUY,
                direction = CashDirection.OUT,
                amount = amount + totalFees,
                stockSymbol = symbol,
                transactionId = transaction.id,
                description = "买入 $name ($symbol) $quantity 股 @ $price"
            )
            portfolioDao.insertCashFlow(cashFlow)
            
            // 3. 更新持仓
            updateHoldingAfterBuy(symbol, name, quantity, price, amount)
            
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 卖出操作 ====================
    
    /**
     * 卖出股票
     * 
     * @param symbol 股票代码
     * @param quantity 卖出数量
     * @param price 卖出价格
     * @param commission 佣金
     * @param tradeDate 交易日期
     */
    suspend fun sellStock(
        symbol: String,
        quantity: Int,
        price: Double,
        commission: Double = 0.0,
        tradeDate: Date = Date()
    ): Result<PortfolioTransaction> = withContext(Dispatchers.IO) {
        try {
            // 参数校验
            if (quantity <= 0) {
                return@withContext Result.failure(IllegalArgumentException("卖出数量必须大于0"))
            }
            if (price <= 0) {
                return@withContext Result.failure(IllegalArgumentException("卖出价格必须大于0"))
            }
            
            // 检查持仓
            val holding = portfolioDao.getHoldingBySymbol(symbol)
                ?: return@withContext Result.failure(IllegalStateException("未持有该股票: $symbol"))
            
            if (holding.availableQuantity < quantity) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "可用持仓不足: ${holding.availableQuantity} < $quantity"
                    )
                )
            }
            
            // 计算金额和费用
            val amount = quantity * price
            val stampTax = calculateStampTax(amount)  // 卖出收印花税
            val transferFee = calculateTransferFee(amount)
            val totalFees = commission + stampTax + transferFee
            val netAmount = amount - totalFees
            
            // 1. 创建交易记录
            val transaction = PortfolioTransaction(
                stockSymbol = symbol,
                stockName = holding.stockName,
                type = TransactionType.SELL,
                direction = TradeDirection.LONG,
                quantity = quantity,
                price = price,
                amount = amount,
                commission = commission,
                stampTax = stampTax,
                transferFee = transferFee,
                totalFees = totalFees,
                tradeDate = tradeDate
            )
            portfolioDao.insertTransaction(transaction)
            
            // 2. 创建资金流水
            val cashFlow = CashFlow(
                type = CashFlowType.SELL,
                direction = CashDirection.IN,
                amount = netAmount,
                stockSymbol = symbol,
                transactionId = transaction.id,
                description = "卖出 ${holding.stockName} ($symbol) $quantity 股 @ $price"
            )
            portfolioDao.insertCashFlow(cashFlow)
            
            // 3. 更新持仓
            updateHoldingAfterSell(holding, quantity, price)
            
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 持仓更新逻辑 ====================
    
    private suspend fun updateHoldingAfterBuy(
        symbol: String,
        name: String,
        quantity: Int,
        price: Double,
        amount: Double
    ) {
        val existingHolding = portfolioDao.getHoldingBySymbol(symbol)
        
        if (existingHolding == null) {
            // 新建持仓
            val newHolding = PortfolioHolding(
                stockSymbol = symbol,
                stockName = name,
                totalQuantity = quantity,
                availableQuantity = quantity,
                averageCost = price,
                totalCost = amount,
                currentPrice = price,
                marketValue = amount,
                firstBuyDate = Date(),
                lastTradeDate = Date(),
                tradeCount = 1
            )
            portfolioDao.insertHolding(newHolding)
        } else {
            // 更新现有持仓 (平均成本法)
            val newTotalQuantity = existingHolding.totalQuantity + quantity
            val newTotalCost = existingHolding.totalCost + amount
            val newAverageCost = newTotalCost / newTotalQuantity
            
            val updatedHolding = existingHolding.copy(
                totalQuantity = newTotalQuantity,
                availableQuantity = existingHolding.availableQuantity + quantity,
                averageCost = newAverageCost,
                totalCost = newTotalCost,
                currentPrice = price,
                marketValue = price * newTotalQuantity,
                profitLoss = (price - newAverageCost) * newTotalQuantity,
                profitLossPercent = ((price - newAverageCost) / newAverageCost) * 100,
                lastTradeDate = Date(),
                tradeCount = existingHolding.tradeCount + 1,
                updatedAt = Date()
            )
            portfolioDao.updateHolding(updatedHolding)
        }
    }
    
    private suspend fun updateHoldingAfterSell(
        holding: PortfolioHolding,
        quantity: Int,
        price: Double
    ) {
        val newTotalQuantity = holding.totalQuantity - quantity
        
        if (newTotalQuantity <= 0) {
            // 清仓，删除持仓
            portfolioDao.deleteHolding(holding)
        } else {
            // 部分卖出
            val soldCost = holding.averageCost * quantity
            val newTotalCost = holding.totalCost - soldCost
            val newAverageCost = if (newTotalQuantity > 0) newTotalCost / newTotalQuantity else 0.0
            
            val updatedHolding = holding.copy(
                totalQuantity = newTotalQuantity,
                availableQuantity = holding.availableQuantity - quantity,
                averageCost = newAverageCost,
                totalCost = newTotalCost,
                currentPrice = price,
                marketValue = price * newTotalQuantity,
                profitLoss = (price - newAverageCost) * newTotalQuantity,
                profitLossPercent = if (newAverageCost > 0) ((price - newAverageCost) / newAverageCost) * 100 else 0.0,
                lastTradeDate = Date(),
                tradeCount = holding.tradeCount + 1,
                updatedAt = Date()
            )
            portfolioDao.updateHolding(updatedHolding)
        }
    }
    
    // ==================== 刷新持仓市值 ====================
    
    /**
     * 刷新所有持仓的当前价格和市值
     */
    suspend fun refreshHoldingsValue(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val holdings = portfolioDao.getActiveHoldings()
            
            holdings.forEach { holding ->
                val quoteResult = dataSourceManager.fetchQuote(holding.stockSymbol)
                
                quoteResult.onSuccess { quote ->
                    val currentPrice = quote.price
                    val marketValue = currentPrice * holding.totalQuantity
                    val profitLoss = marketValue - holding.totalCost
                    val profitLossPercent = if (holding.totalCost > 0) {
                        (profitLoss / holding.totalCost) * 100
                    } else 0.0
                    
                    val updatedHolding = holding.copy(
                        currentPrice = currentPrice,
                        marketValue = marketValue,
                        profitLoss = profitLoss,
                        profitLossPercent = profitLossPercent,
                        updatedAt = Date()
                    )
                    portfolioDao.updateHolding(updatedHolding)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 汇总统计 ====================
    
    /**
     * 获取投资组合汇总
     */
    suspend fun getPortfolioSummary(): PortfolioSummary = withContext(Dispatchers.IO) {
        val totals = portfolioDao.getPortfolioTotals()
        val cashBalance = portfolioDao.getCashBalance() ?: 0.0
        
        val totalAssets = totals.totalMarketValue + cashBalance
        val profitLossPercent = if (totals.totalCost > 0) {
            (totals.totalProfitLoss / totals.totalCost) * 100
        } else 0.0
        
        PortfolioSummary(
            totalMarketValue = totals.totalMarketValue,
            totalCost = totals.totalCost,
            totalProfitLoss = totals.totalProfitLoss,
            totalProfitLossPercent = profitLossPercent,
            cashBalance = cashBalance,
            totalAssets = totalAssets,
            holdingCount = totals.holdingCount,
            stockCount = totals.stockCount
        )
    }
    
    /**
     * 计算风险指标
     */
    suspend fun calculateRiskMetrics(): RiskMetrics = withContext(Dispatchers.IO) {
        val transactions = portfolioDao.getTransactionsByType(TransactionType.SELL)
        
        // 简化计算，实际应该基于收益率序列
        var winCount = 0
        var lossCount = 0
        var totalWin = 0.0
        var totalLoss = 0.0
        
        transactions.forEach { transaction ->
            // 获取对应的买入记录计算盈亏
            val holding = portfolioDao.getHoldingBySymbol(transaction.stockSymbol)
            val buyTransactions = portfolioDao.getTransactionsBySymbol(transaction.stockSymbol)
                .filter { it.type == TransactionType.BUY }
            
            if (buyTransactions.isNotEmpty()) {
                val avgBuyPrice = buyTransactions.sumOf { it.amount } / buyTransactions.sumOf { it.quantity }
                val profit = (transaction.price - avgBuyPrice) * transaction.quantity - transaction.totalFees
                
                if (profit > 0) {
                    winCount++
                    totalWin += profit
                } else {
                    lossCount++
                    totalLoss += abs(profit)
                }
            }
        }
        
        val totalTrades = winCount + lossCount
        val winRate = if (totalTrades > 0) (winCount.toDouble() / totalTrades) * 100 else null
        val profitFactor = if (totalLoss > 0) totalWin / totalLoss else null
        
        RiskMetrics(
            winRate = winRate,
            profitFactor = profitFactor,
            avgWin = if (winCount > 0) totalWin / winCount else null,
            avgLoss = if (lossCount > 0) totalLoss / lossCount else null
        )
    }
    
    // ==================== 快照功能 ====================
    
    /**
     * 创建每日快照
     */
    suspend fun createDailySnapshot(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 先刷新市值
            refreshHoldingsValue()
            
            val summary = getPortfolioSummary()
            val holdings = portfolioDao.getActiveHoldings()
            
            // 计算日收益率 (需要昨日快照)
            val today = Date()
            val yesterday = java.util.Calendar.getInstance().apply { 
                add(java.util.Calendar.DAY_OF_YEAR, -1) 
            }.time
            
            val yesterdaySnapshot = portfolioDao.getSnapshotByDate(yesterday)
            val dailyReturn = yesterdaySnapshot?.let {
                ((summary.totalAssets - it.totalAssets) / it.totalAssets) * 100
            }
            
            val snapshot = PortfolioSnapshot(
                snapshotDate = today,
                totalMarketValue = summary.totalMarketValue,
                totalCost = summary.totalCost,
                totalProfitLoss = summary.totalProfitLoss,
                totalProfitLossPercent = summary.totalProfitLossPercent,
                cashBalance = summary.cashBalance,
                totalAssets = summary.totalAssets,
                holdingCount = summary.holdingCount,
                stockCount = summary.stockCount,
                dailyReturn = dailyReturn
            )
            
            portfolioDao.insertSnapshot(snapshot)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 费用计算 ====================
    
    private fun calculateStampTax(amount: Double): Double {
        // A股卖出印花税 0.1%
        return amount * 0.001
    }
    
    private fun calculateTransferFee(amount: Double): Double {
        // A股过户费 0.001%
        return amount * 0.00001
    }
}
