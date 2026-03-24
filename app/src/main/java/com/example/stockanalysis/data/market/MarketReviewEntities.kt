package com.example.stockanalysis.data.market

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 市场复盘报告表
 */
@Entity(tableName = "market_reviews")
data class MarketReview(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val reviewDate: Date,              // 复盘日期
    val marketType: String = "CN",     // 市场类型 (CN/US/HK)
    
    // 指数数据
    val shIndexValue: Double? = null,  // 上证指数
    val shIndexChange: Double? = null,
    val szIndexValue: Double? = null,  // 深证成指
    val szIndexChange: Double? = null,
    val cyIndexValue: Double? = null,  // 创业板指
    val cyIndexChange: Double? = null,
    
    // 市场统计
    val upCount: Int = 0,              // 上涨家数
    val downCount: Int = 0,            // 下跌家数
    val flatCount: Int = 0,            // 平盘家数
    val limitUpCount: Int = 0,         // 涨停数
    val limitDownCount: Int = 0,       // 跌停数
    val totalVolume: Long? = null,     // 总成交量
    val totalAmount: Double? = null,   // 总成交额
    
    // 板块数据 (JSON)
    val topSectors: String? = null,    // 领涨板块
    val bottomSectors: String? = null, // 领跌板块
    
    // AI复盘报告
    val summary: String? = null,       // 一句话总结
    val technicalAnalysis: String? = null,  // 技术面分析
    val sentimentAnalysis: String? = null,  // 情绪面分析
    val strategy: String? = null,      // 操作策略
    
    // 策略建议
    val action: String? = null,        // 建议动作 (进攻/均衡/防守)
    val riskLevel: Int = 3,            // 风险等级 1-5
    
    val createdAt: Date = Date()
)

/**
 * 板块表现
 */
@Entity(tableName = "sector_performance")
data class SectorPerformance(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    
    val date: Date,
    val sectorName: String,            // 板块名称
    val changePercent: Double,         // 涨跌幅
    val leadingStocks: String? = null, // 领涨股
    val volume: Long? = null,          // 成交量
    val amount: Double? = null,        // 成交额
    val rank: Int = 0                  // 排名
)

/**
 * 市场复盘服务接口
 */
interface MarketReviewService {
    
    /**
     * 生成每日复盘
     */
    suspend fun generateDailyReview(date: Date = Date()): Result<MarketReview>
    
    /**
     * 获取历史复盘
     */
    suspend fun getReviewHistory(days: Int = 30): List<MarketReview>
    
    /**
     * 获取最新复盘
     */
    suspend fun getLatestReview(): MarketReview?
}

/**
 * 市场数据概览
 */
data class MarketOverviewData(
    // 主要指数
    val indices: List<IndexData>,
    
    // 市场统计
    val marketStats: MarketStatsData,
    
    // 板块表现
    val topSectors: List<SectorData>,
    val bottomSectors: List<SectorData>,
    
    // 资金流向
    val capitalFlow: CapitalFlowData,
    
    // 市场情绪
    val sentiment: MarketSentiment
)

/**
 * 指数数据
 */
data class IndexData(
    val name: String,
    val symbol: String,
    val value: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long? = null,
    val turnover: Double? = null
)

/**
 * 市场统计数据
 */
data class MarketStatsData(
    val upCount: Int,
    val downCount: Int,
    val flatCount: Int,
    val limitUpCount: Int,
    val limitDownCount: Int,
    val totalVolume: Long,
    val totalAmount: Double
)

/**
 * 板块数据
 */
data class SectorData(
    val name: String,
    val changePercent: Double,
    val leadingStock: String? = null,
    val leadingStockChange: Double? = null
)

/**
 * 资金流向
 */
data class CapitalFlowData(
    val mainNetInflow: Double,         // 主力净流入
    val superLargeNetInflow: Double,   // 超大单净流入
    val largeNetInflow: Double,        // 大单净流入
    val mediumNetInflow: Double,       // 中单净流入
    val smallNetInflow: Double         // 小单净流入
)

/**
 * 市场情绪
 */
data class MarketSentiment(
    val fearGreedIndex: Int,           // 恐惧贪婪指数 0-100
    val sentiment: SentimentType,
    val description: String
)

enum class SentimentType {
    EXTREME_FEAR,  // 极度恐惧
    FEAR,          // 恐惧
    NEUTRAL,       // 中性
    GREED,         // 贪婪
    EXTREME_GREED  // 极度贪婪
}

/**
 * 操作策略建议
 */
data class StrategyAdvice(
    val action: ActionType,
    val title: String,
    val description: String,
    val positionSuggestion: String,    // 仓位建议
    val focusAreas: List<String>       // 关注方向
)

enum class ActionType {
    OFFENSIVE,    // 进攻
    BALANCED,     // 均衡
    DEFENSIVE     // 防守
}
