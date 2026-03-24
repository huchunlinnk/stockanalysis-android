package com.example.stockanalysis.data.backtest

import androidx.room.*
import java.util.Date

/**
 * 回测结果表
 */
@Entity(tableName = "backtest_results")
data class BacktestResult(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val stockSymbol: String,           // 股票代码
    val stockName: String,             // 股票名称
    
    // 回测配置
    val strategyId: String,            // 策略ID
    val strategyName: String,          // 策略名称
    val startDate: Date,               // 回测开始日期
    val endDate: Date,                 // 回测结束日期
    
    // 回测统计
    val totalSignals: Int = 0,         // 总信号数
    val correctSignals: Int = 0,       // 正确信号数
    val accuracy: Double = 0.0,        // 准确率
    
    // 收益统计
    val totalReturn: Double = 0.0,     // 总收益率
    val maxDrawdown: Double = 0.0,     // 最大回撤
    val sharpeRatio: Double = 0.0,     // 夏普比率
    val winRate: Double = 0.0,         // 胜率
    val profitFactor: Double = 0.0,    // 盈亏比
    
    // 交易统计
    val tradeCount: Int = 0,           // 交易次数
    val winCount: Int = 0,             // 盈利次数
    val lossCount: Int = 0,            // 亏损次数
    val avgWin: Double = 0.0,          // 平均盈利
    val avgLoss: Double = 0.0,         // 平均亏损
    
    // 详细数据 (JSON)
    val signalsData: String? = null,   // 信号详情JSON
    
    val createdAt: Date = Date()
)

/**
 * 回测信号详情
 */
@Entity(tableName = "backtest_signals")
data class BacktestSignal(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val backtestResultId: String,      // 关联回测结果ID
    
    val signalDate: Date,              // 信号日期
    val signalType: String,            // 信号类型 (BUY/SELL/HOLD)
    val confidence: Double = 0.0,      // 置信度
    
    // 价格信息
    val entryPrice: Double? = null,    // 入场价格
    val exitPrice: Double? = null,     // 出场价格
    
    // 预测与实际
    val predictedDirection: String,    // 预测方向 (UP/DOWN)
    val actualDirection: String? = null, // 实际方向
    val isCorrect: Boolean? = null,    // 是否预测正确
    
    // 收益
    val returnPercent: Double? = null, // 收益率
    val holdingDays: Int? = null,      // 持有天数
    
    // 原始分析结果ID
    val analysisResultId: String? = null
)

/**
 * 回测配置
 */
data class BacktestConfig(
    val stockSymbol: String,
    val strategyId: String,
    val startDate: Date,
    val endDate: Date,
    val initialCapital: Double = 100000.0,  // 初始资金
    val positionSize: PositionSizing = PositionSizing.FIXED_AMOUNT,
    val positionAmount: Double = 10000.0,   // 每次交易金额
    val stopLoss: Double? = null,           // 止损比例
    val takeProfit: Double? = null          // 止盈比例
)

/**
 * 仓位管理方式
 */
enum class PositionSizing {
    FIXED_AMOUNT,     // 固定金额
    FIXED_RATIO,      // 固定比例
    KELLY_CRITERION,  // 凯利公式
    EQUAL_WEIGHT      // 等权重
}

/**
 * 回测结果汇总
 */
data class BacktestSummary(
    val stockSymbol: String,
    val strategyId: String,
    val startDate: Date,
    val endDate: Date,
    
    // 收益指标
    val totalReturn: Double,
    val annualizedReturn: Double,
    val maxDrawdown: Double,
    val sharpeRatio: Double,
    
    // 胜率指标
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val profitFactor: Double,
    
    // 信号准确率
    val totalSignals: Int,
    val correctSignals: Int,
    val accuracy: Double
)

/**
 * 交易记录（回测用）
 */
data class BacktestTrade(
    val id: String = java.util.UUID.randomUUID().toString(),
    val entryDate: Date,
    val exitDate: Date? = null,
    val entryPrice: Double,
    val exitPrice: Double? = null,
    val quantity: Int,
    val direction: TradeDirection,
    val pnl: Double? = null,
    val pnlPercent: Double? = null,
    val exitReason: ExitReason? = null
)

/**
 * 交易方向
 */
enum class TradeDirection {
    LONG,    // 做多
    SHORT    // 做空
}

/**
 * 出场原因
 */
enum class ExitReason {
    STOP_LOSS,     // 止损
    TAKE_PROFIT,   // 止盈
    SIGNAL_REVERSE, // 信号反转
    END_OF_TEST    // 回测结束
}

/**
 * 回测信号评估
 */
data class SignalEvaluation(
    val analysisId: String,
    val signalDate: Date,
    val decision: String,              // 买入/卖出/持有
    val predictedDirection: String,    // 预测方向
    val actualDirection: String,       // 实际方向
    val isCorrect: Boolean,
    val priceAtSignal: Double,
    val priceAfterNDays: Map<Int, Double>,  // N天后的价格
    val returnAfterNDays: Map<Int, Double>  // N天后的收益率
)

/**
 * 时间段表现
 */
data class PeriodPerformance(
    val period: String,                // 时间段标识
    val startDate: Date,
    val endDate: Date,
    val signalCount: Int,
    val correctCount: Int,
    val accuracy: Double,
    val avgReturn: Double
)
