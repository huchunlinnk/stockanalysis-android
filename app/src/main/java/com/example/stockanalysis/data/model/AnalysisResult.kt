package com.example.stockanalysis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.stockanalysis.data.local.Converters
import java.util.Date

/**
 * AI分析结果
 */
@Entity(tableName = "analysis_results")
@TypeConverters(Converters::class)
data class AnalysisResult(
    @PrimaryKey
    val id: String,                    // 分析ID
    val stockSymbol: String,           // 股票代码
    val stockName: String,             // 股票名称
    val analysisTime: Date = Date(),   // 分析时间
    
    // 决策建议
    val decision: Decision,            // 决策类型
    val score: Int,                    // 评分 0-100
    val confidence: ConfidenceLevel,   // 置信度
    
    // 分析摘要
    val summary: String,               // 一句话总结
    val reasoning: String,             // 推理过程
    
    // 详细分析
    val technicalAnalysis: TechnicalAnalysis?,
    val fundamentalAnalysis: FundamentalAnalysis?,
    val sentimentAnalysis: SentimentAnalysis?,
    val riskAssessment: RiskAssessment?,
    
    // 操作建议
    val actionPlan: ActionPlan?,
    
    // 原始响应（用于调试）
    val rawResponse: String? = null,
    
    // 是否已同步到云端
    val isSynced: Boolean = false,
    // 是否已推送通知
    val isNotified: Boolean = false
)

/**
 * 决策类型
 */
enum class Decision {
    STRONG_BUY, // 强烈买入
    BUY,        // 买入
    HOLD,       // 持有/观望
    SELL,       // 卖出
    STRONG_SELL // 强烈卖出
}

/**
 * 置信度级别
 */
enum class ConfidenceLevel {
    HIGH,   // 高
    MEDIUM, // 中
    LOW     // 低
}

/**
 * 技术面分析
 */
data class TechnicalAnalysis(
    val trend: String,              // 趋势判断
    val maAlignment: String,        // 均线排列
    val supportLevel: Double?,      // 支撑位
    val resistanceLevel: Double?,   // 阻力位
    val volumeAnalysis: String,     // 量能分析
    val technicalScore: Int         // 技术面评分
)

/**
 * 基本面分析
 */
data class FundamentalAnalysis(
    val valuation: String,          // 估值分析
    val growth: String,             // 成长性
    val profitability: String,      // 盈利能力
    val financialHealth: String,    // 财务健康度
    val fundamentalScore: Int       // 基本面评分
)

/**
 * 舆情分析
 */
data class SentimentAnalysis(
    val overallSentiment: String,   // 整体情绪
    val sentimentScore: Double,     // 情绪分数 -1~1
    val keyNews: List<NewsItem>,    // 关键新闻
    val riskFactors: List<String>,  // 风险因素
    val catalysts: List<String>     // 催化剂
)

/**
 * 新闻条目
 */
data class NewsItem(
    val title: String,
    val source: String,
    val publishTime: Date,
    val sentiment: String,          // positive/negative/neutral
    val relevance: Double           // 相关度 0-1
)

/**
 * 风险评估
 */
data class RiskAssessment(
    val riskLevel: RiskLevel,       // 风险等级
    val volatility: String,         // 波动性
    val liquidityRisk: String,      // 流动性风险
    val marketRisk: String,         // 市场风险
    val specificRisks: List<String> // 具体风险点
)

/**
 * 风险等级
 */
enum class RiskLevel {
    LOW,      // 低
    MEDIUM,   // 中
    HIGH      // 高
}

/**
 * 行动计划
 */
data class ActionPlan(
    val entryPrice: Double?,        // 建议买入价
    val stopLossPrice: Double?,     // 止损价
    val targetPrice: Double?,       // 目标价
    val positionSize: String?,      // 仓位建议
    val timeHorizon: String?,       // 时间周期
    val checkList: List<CheckItem>  // 检查清单
)

/**
 * 检查项
 */
data class CheckItem(
    val item: String,
    val status: CheckStatus
)

/**
 * 检查状态
 */
enum class CheckStatus {
    PASSED,     // 满足
    WARNING,    // 注意
    FAILED      // 不满足
}

/**
 * 批量分析结果摘要
 */
data class AnalysisSummary(
    val totalCount: Int,
    val buyCount: Int,
    val holdCount: Int,
    val sellCount: Int,
    val averageScore: Int,
    val analysisTime: Date = Date()
)
