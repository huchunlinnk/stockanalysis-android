package com.example.stockanalysis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 股票基础信息
 */
@Entity(tableName = "stocks")
data class Stock(
    @PrimaryKey
    val symbol: String,           // 股票代码，如 "600519"
    val name: String,             // 股票名称，如 "贵州茅台"
    val market: MarketType = MarketType.A_SHARE,  // 市场类型
    val exchange: String? = null, // 交易所
    val industry: String? = null, // 行业
    val addedTime: Date = Date(), // 添加时间
    val isFavorite: Boolean = false, // 是否收藏
    val sortOrder: Int = 0        // 排序顺序
) {
    /**
     * 获取完整代码（带市场前缀）
     */
    fun getFullSymbol(): String {
        return when (market) {
            MarketType.A_SHARE -> symbol
            MarketType.HK -> "hk$symbol"
            MarketType.US -> symbol
        }
    }
    
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return "$name ($symbol)"
    }
}

/**
 * 市场类型
 */
enum class MarketType {
    A_SHARE,    // A股
    HK,         // 港股
    US          // 美股
}

/**
 * 涨跌趋势
 */
enum class Trend {
    UP,     // 上涨
    DOWN,   // 下跌
    FLAT    // 平盘
}

/**
 * 股票实时行情
 */
data class StockQuote(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val prevClose: Double,
    val volume: Long,
    val turnover: Double? = null,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val updateTime: Date = Date(),
    
    // 扩展字段
    val peRatio: Double? = null,
    val pbRatio: Double? = null,
    val marketCap: Double? = null,
    val amplitude: Double? = null,
    val turnoverRate: Double? = null
) {
    /**
     * 判断涨跌状态
     */
    fun getTrend(): Trend {
        return when {
            change > 0 -> Trend.UP
            change < 0 -> Trend.DOWN
            else -> Trend.FLAT
        }
    }
    
    /**
     * 格式化涨跌幅显示
     */
    fun getChangePercentText(): String {
        val sign = if (changePercent >= 0) "+" else ""
        return "$sign${String.format("%.2f", changePercent)}%"
    }
    
    /**
     * 格式化价格显示
     */
    fun getPriceText(): String {
        return String.format("%.2f", currentPrice)
    }
}
