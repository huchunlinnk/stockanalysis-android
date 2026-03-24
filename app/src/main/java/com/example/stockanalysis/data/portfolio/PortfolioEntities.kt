package com.example.stockanalysis.data.portfolio

import androidx.room.*
import java.util.Date

/**
 * 持仓记录表
 */
@Entity(tableName = "portfolio_holdings")
data class PortfolioHolding(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val stockSymbol: String,           // 股票代码
    val stockName: String,             // 股票名称
    val market: String = "CN",         // 市场
    
    // 持仓数量
    val totalQuantity: Int = 0,        // 总持仓
    val availableQuantity: Int = 0,    // 可用持仓
    val frozenQuantity: Int = 0,       // 冻结持仓
    
    // 成本
    val averageCost: Double = 0.0,     // 平均成本
    val totalCost: Double = 0.0,       // 总成本
    
    // 当前市值 (实时更新)
    val currentPrice: Double = 0.0,    // 当前价格
    val marketValue: Double = 0.0,     // 市值
    
    // 盈亏
    val profitLoss: Double = 0.0,      // 盈亏金额
    val profitLossPercent: Double = 0.0, // 盈亏比例
    
    // 交易记录汇总
    val firstBuyDate: Date? = null,    // 首次买入日期
    val lastTradeDate: Date? = null,   // 最后交易日期
    val tradeCount: Int = 0,           // 交易次数
    
    // 元数据
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

/**
 * 交易记录表
 */
@Entity(tableName = "portfolio_transactions")
data class PortfolioTransaction(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val stockSymbol: String,           // 股票代码
    val stockName: String,             // 股票名称
    
    val type: TransactionType,         // 交易类型
    val direction: TradeDirection,     // 买卖方向
    
    val quantity: Int,                 // 成交数量
    val price: Double,                 // 成交价格
    val amount: Double,                // 成交金额
    
    // 费用
    val commission: Double = 0.0,      // 佣金
    val stampTax: Double = 0.0,        // 印花税
    val transferFee: Double = 0.0,     // 过户费
    val otherFees: Double = 0.0,       // 其他费用
    val totalFees: Double = 0.0,       // 总费用
    
    // 交易详情
    val tradeDate: Date = Date(),      // 交易日期
    val tradeTime: String? = null,     // 交易时间
    val tradeNo: String? = null,       // 成交编号
    
    // 备注
    val notes: String? = null,
    val tags: String? = null,          // 标签，逗号分隔
    
    // 公司行为关联
    val corporateActionId: String? = null,
    
    val createdAt: Date = Date()
)

/**
 * 交易类型
 */
enum class TransactionType {
    BUY,           // 买入
    SELL,          // 卖出
    DIVIDEND,      // 分红
    BONUS,         // 送股
    SPLIT,         // 拆股
    MERGER,        // 合并
    SPINOFF,       // 分拆
    ADJUSTMENT     // 调整
}

/**
 * 买卖方向
 */
enum class TradeDirection {
    LONG,          // 做多
    SHORT          // 做空
}

/**
 * 资金流水表
 */
@Entity(tableName = "portfolio_cash")
data class CashFlow(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val type: CashFlowType,            // 流水类型
    val direction: CashDirection,      // 收支方向
    
    val amount: Double,                // 金额
    val currency: String = "CNY",      // 币种
    
    // 关联
    val transactionId: String? = null, // 关联交易
    val stockSymbol: String? = null,   // 关联股票
    
    // 详情
    val date: Date = Date(),
    val description: String? = null,
    val balanceAfter: Double? = null,  // 变动后余额
    
    val createdAt: Date = Date()
)

/**
 * 资金流水类型
 */
enum class CashFlowType {
    DEPOSIT,       // 入金
    WITHDRAWAL,    // 出金
    BUY,           // 买入
    SELL,          // 卖出
    DIVIDEND,      // 分红
    FEE,           // 费用
    ADJUSTMENT,    // 调整
    TRANSFER       // 转账
}

/**
 * 收支方向
 */
enum class CashDirection {
    IN,            // 收入
    OUT            // 支出
}

/**
 * 公司行为表（分红、拆股等）
 */
@Entity(tableName = "portfolio_corporate_actions")
data class CorporateAction(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val stockSymbol: String,           // 股票代码
    val stockName: String,             // 股票名称
    
    val type: CorporateActionType,     // 行为类型
    val exDate: Date,                  // 除权除息日
    val recordDate: Date? = null,      // 股权登记日
    val paymentDate: Date? = null,     // 派息日/到账日
    
    // 分红
    val dividendPerShare: Double? = null,  // 每股分红
    val dividendTotal: Double? = null,     // 总分红金额
    
    // 送股
    val bonusSharesRatio: Double? = null,  // 送股比例 (10送3 = 0.3)
    val bonusSharesTotal: Int? = null,     // 总送股数
    
    // 拆股
    val splitRatio: String? = null,        // 拆股比例 (2:1)
    val splitFrom: Int? = null,
    val splitTo: Int? = null,
    
    // 状态
    val isProcessed: Boolean = false,    // 是否已处理
    val processedAt: Date? = null,
    
    val description: String? = null,
    val createdAt: Date = Date()
)

/**
 * 公司行为类型
 */
enum class CorporateActionType {
    CASH_DIVIDEND,     // 现金分红
    STOCK_DIVIDEND,    // 股票分红
    BONUS_SHARES,      // 送股
    STOCK_SPLIT,       // 拆股
    REVERSE_SPLIT,     // 并股
    RIGHTS_ISSUE,      // 配股
    SPIN_OFF           // 分拆
}

/**
 * 每日持仓快照表
 */
@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshot(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val snapshotDate: Date,            // 快照日期
    
    // 汇总数据
    val totalMarketValue: Double = 0.0,    // 总市值
    val totalCost: Double = 0.0,           // 总成本
    val totalProfitLoss: Double = 0.0,     // 总盈亏
    val totalProfitLossPercent: Double = 0.0,
    
    val cashBalance: Double = 0.0,         // 现金余额
    val totalAssets: Double = 0.0,         // 总资产
    
    val holdingCount: Int = 0,             // 持仓数量
    val stockCount: Int = 0,               // 股票数量
    
    // 风险指标
    val dailyReturn: Double? = null,       // 日收益率
    val maxDrawdown: Double? = null,       // 最大回撤
    
    // 详细数据 (JSON格式存储)
    val holdingsData: String? = null,      // 持仓详情JSON
    
    val createdAt: Date = Date()
)

/**
 * 持仓与交易关联查询结果
 */
data class HoldingWithTransactions(
    @Embedded
    val holding: PortfolioHolding,
    
    @Relation(
        parentColumn = "stockSymbol",
        entityColumn = "stockSymbol"
    )
    val recentTransactions: List<PortfolioTransaction>
)

/**
 * 投资组合汇总
 */
data class PortfolioSummary(
    val totalMarketValue: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalProfitLoss: Double = 0.0,
    val totalProfitLossPercent: Double = 0.0,
    val cashBalance: Double = 0.0,
    val totalAssets: Double = 0.0,
    val holdingCount: Int = 0,
    val stockCount: Int = 0,
    val todayProfitLoss: Double? = null
)

/**
 * 风险指标
 */
data class RiskMetrics(
    val beta: Double? = null,              // Beta系数
    val sharpeRatio: Double? = null,       // 夏普比率
    val maxDrawdown: Double? = null,       // 最大回撤
    val volatility: Double? = null,        // 波动率
    val var95: Double? = null,             // 风险价值(95%)
    val winRate: Double? = null,           // 胜率
    val profitFactor: Double? = null,      // 盈亏比
    val avgWin: Double? = null,            // 平均盈利
    val avgLoss: Double? = null            // 平均亏损
)
