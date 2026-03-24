package com.example.stockanalysis.data.model

import java.util.Date

/**
 * 大盘概览
 */
data class MarketOverview(
    val updateTime: Date = Date(),
    val marketType: MarketType,        // 市场类型
    
    // 主要指数
    val indices: List<MarketIndex>,
    
    // 市场统计
    val stats: MarketStats,
    
    // 板块表现
    val sectorPerformance: SectorPerformance
)

/**
 * 市场指数
 */
data class MarketIndex(
    val name: String,                  // 指数名称
    val symbol: String,                // 指数代码
    val currentValue: Double,          // 当前点数
    val change: Double,                // 涨跌点数
    val changePercent: Double,         // 涨跌幅
    val volume: Long?,                 // 成交量
    val turnover: Double?,             // 成交额
    val marketType: MarketType = MarketType.A_SHARE  // 所属市场
) {
    fun getTrend(): Trend {
        return when {
            change > 0 -> Trend.UP
            change < 0 -> Trend.DOWN
            else -> Trend.FLAT
        }
    }

    /**
     * 获取市场类型显示名称
     */
    fun getMarketTypeName(): String {
        return when (marketType) {
            MarketType.A_SHARE -> "A股"
            MarketType.HK -> "港股"
            MarketType.US -> "美股"
        }
    }
}

/**
 * 市场指数分组数据
 */
data class MarketIndexGroup(
    val marketType: MarketType,
    val marketName: String,
    val indices: List<MarketIndex>
)

/**
 * 市场统计
 */
data class MarketStats(
    val risingCount: Int,              // 上涨家数
    val fallingCount: Int,             // 下跌家数
    val flatCount: Int,                // 平盘家数
    val limitUpCount: Int,             // 涨停家数
    val limitDownCount: Int,           // 跌停家数
    
    // 涨跌分布
    val riseDistribution: Map<String, Int>,  // 涨幅分布
    val fallDistribution: Map<String, Int>   // 跌幅分布
)

/**
 * 板块表现
 */
data class SectorPerformance(
    val topRisers: List<SectorInfo>,   // 领涨板块
    val topFallers: List<SectorInfo>   // 领跌板块
)

/**
 * 板块信息
 */
data class SectorInfo(
    val name: String,                  // 板块名称
    val changePercent: Double,         // 涨跌幅
    val leadingStocks: List<String>    // 领涨股
)
