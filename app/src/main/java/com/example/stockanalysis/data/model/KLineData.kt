package com.example.stockanalysis.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * K线数据 - 本地存储的历史行情数据
 */
@Entity(
    tableName = "kline_data",
    indices = [Index(value = ["symbol", "timestamp"], unique = true)]
)
data class KLineData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,           // 股票代码
    val timestamp: Date,          // 时间戳
    val open: Double,             // 开盘价
    val high: Double,             // 最高价
    val low: Double,              // 最低价
    val close: Double,            // 收盘价
    val volume: Long,             // 成交量
    val amount: Double,           // 成交额
    val change: Double = 0.0,     // 涨跌额
    val changePercent: Double = 0.0, // 涨跌幅%
    val period: String = "daily", // 周期：daily, weekly, monthly
    val source: String = "local"  // 数据来源
) {
    /**
     * 获取价格变动趋势
     */
    fun getTrend(): Trend {
        return when {
            change > 0 -> Trend.UP
            change < 0 -> Trend.DOWN
            else -> Trend.FLAT
        }
    }
    
    /**
     * 是否为阳线
     */
    fun isBullish(): Boolean = close >= open
    
    /**
     * 是否为阴线
     */
    fun isBearish(): Boolean = close < open
    
    /**
     * 振幅
     */
    fun getAmplitude(): Double = if (open > 0) ((high - low) / open * 100) else 0.0
}

/**
 * 筹码分布数据
 */
data class ChipDistribution(
    val symbol: String,
    val avgCost: Double,              // 平均成本
    val profitRatio: Double,          // 获利比例 0-1
    val concentration90: Double,      // 90%筹码集中度
    val concentration70: Double,      // 70%筹码集中度
    val peakPrice: Double?,           // 筹码峰值价格
    val supportLevels: List<Double>,  // 支撑位
    val resistanceLevels: List<Double>, // 阻力位
    val timestamp: Date = Date()
) {
    /**
     * 获取筹码状态
     */
    fun getChipStatus(currentPrice: Double): String {
        return when {
            profitRatio > 0.7 && currentPrice > avgCost -> "筹码集中，主力控盘"
            profitRatio < 0.3 && currentPrice < avgCost -> "筹码分散，套牢盘多"
            concentration90 < 0.1 -> "高度集中"
            concentration90 > 0.3 -> "较为分散"
            else -> "筹码正常"
        }
    }
}

/**
 * 实时行情数据
 */
data class RealtimeQuote(
    val symbol: String,
    val name: String,
    val price: Double,                // 当前价格
    val open: Double,                 // 开盘价
    val high: Double,                 // 最高价
    val low: Double,                  // 最低价
    val preClose: Double,             // 昨收
    val volume: Long,                 // 成交量
    val amount: Double,               // 成交额
    val change: Double,               // 涨跌额
    val changePercent: Double,        // 涨跌幅%
    val volumeRatio: Double? = null,  // 量比
    val turnoverRate: Double? = null, // 换手率%
    val peRatio: Double? = null,      // 市盈率
    val pbRatio: Double? = null,      // 市净率
    val marketCap: Double? = null,    // 总市值
    val circMarketCap: Double? = null,// 流通市值
    val bidPrice: Double? = null,     // 买一价
    val bidVolume: Long? = null,      // 买一量
    val askPrice: Double? = null,     // 卖一价
    val askVolume: Long? = null,      // 卖一量
    val updateTime: Date = Date()
) {
    /**
     * 获取涨跌趋势
     */
    fun getTrend(): Trend = when {
        changePercent > 0 -> Trend.UP
        changePercent < 0 -> Trend.DOWN
        else -> Trend.FLAT
    }
    
    /**
     * 格式化涨跌幅显示
     */
    fun getChangePercentText(): String {
        val sign = if (changePercent >= 0) "+" else ""
        return "$sign${String.format("%.2f", changePercent)}%"
    }
    
    /**
     * 格式化价格
     */
    fun getPriceText(): String = String.format("%.2f", price)
    
    /**
     * 量比描述
     */
    fun getVolumeRatioDesc(): String {
        if (volumeRatio == null) return "无数据"
        return when {
            volumeRatio < 0.5 -> "极度萎缩"
            volumeRatio < 0.8 -> "明显萎缩"
            volumeRatio < 1.2 -> "正常"
            volumeRatio < 2.0 -> "温和放量"
            volumeRatio < 3.0 -> "明显放量"
            else -> "巨量"
        }
    }
}

/**
 * 均线数据
 */
data class MovingAverages(
    val symbol: String,
    val ma5: Double? = null,
    val ma10: Double? = null,
    val ma20: Double? = null,
    val ma30: Double? = null,
    val ma60: Double? = null,
    val ma120: Double? = null,
    val ma250: Double? = null
) {
    /**
     * 均线多头排列检查 (MA5 > MA10 > MA20)
     */
    fun isBullishAlignment(): Boolean {
        return ma5 != null && ma10 != null && ma20 != null &&
               ma5 > ma10 && ma10 > ma20
    }
    
    /**
     * 均线空头排列检查 (MA5 < MA10 < MA20)
     */
    fun isBearishAlignment(): Boolean {
        return ma5 != null && ma10 != null && ma20 != null &&
               ma5 < ma10 && ma10 < ma20
    }
    
    /**
     * 获取均线状态描述
     */
    fun getAlignmentStatus(currentPrice: Double): String {
        return when {
            currentPrice > (ma5 ?: 0.0) && 
            (ma5 ?: 0.0) > (ma10 ?: 0.0) && 
            (ma10 ?: 0.0) > (ma20 ?: 0.0) -> "多头排列"
            currentPrice < (ma5 ?: Double.MAX_VALUE) && 
            (ma5 ?: Double.MAX_VALUE) < (ma10 ?: Double.MAX_VALUE) && 
            (ma10 ?: Double.MAX_VALUE) < (ma20 ?: Double.MAX_VALUE) -> "空头排列"
            currentPrice > (ma5 ?: 0.0) && (ma5 ?: 0.0) > (ma10 ?: 0.0) -> "短期向好"
            currentPrice < (ma5 ?: Double.MAX_VALUE) && (ma5 ?: Double.MAX_VALUE) < (ma10 ?: Double.MAX_VALUE) -> "短期走弱"
            else -> "震荡整理"
        }
    }
}

/**
 * MACD数据
 */
data class MacdData(
    val dif: Double,
    val dea: Double,
    val macd: Double
)

/**
 * KDJ数据
 */
data class KdjData(
    val k: Double,
    val d: Double,
    val j: Double
)

/**
 * BOLL数据
 */
data class BollData(
    val upper: Double,
    val middle: Double,
    val lower: Double
)

/**
 * 技术指标计算结果
 */
data class TechnicalIndicators(
    val symbol: String,
    val timestamp: Date = Date(),
    
    // 均线
    val movingAverages: MovingAverages? = null,
    
    // MACD
    val macd: MacdData? = null,
    
    // KDJ
    val kdj: KdjData? = null,
    
    // RSI
    val rsi6: Double? = null,
    val rsi12: Double? = null,
    val rsi24: Double? = null,
    
    // BOLL
    val boll: BollData? = null,
    
    // 成交量均线
    val volumeMa5: Double? = null,
    val volumeMa10: Double? = null,
    
    // 其他指标
    val bias24: Double? = null,       // 24日乖离率
    val cci: Double? = null,          // CCI指标
    val dmi: DmiData? = null,         // DMI指标
    val obv: Double? = null,          // OBV能量潮
    val arbr: ArBrData? = null        // ARBR人气意愿指标
) {
    /**
     * 获取RSI信号
     */
    fun getRsiSignal(): String {
        val rsi = rsi6 ?: return "无数据"
        return when {
            rsi > 80 -> "超买"
            rsi < 20 -> "超卖"
            rsi > 50 -> "强势"
            else -> "弱势"
        }
    }
    
    /**
     * 获取MACD信号
     */
    fun getMacdSignal(): String {
        val m = macd ?: return "无数据"
        return when {
            m.dif > m.dea && m.macd > 0 -> "金叉上涨"
            m.dif < m.dea && m.macd < 0 -> "死叉下跌"
            m.dif > m.dea -> "多头排列"
            else -> "空头排列"
        }
    }
    
    /**
     * 获取KDJ信号
     */
    fun getKdjSignal(): String {
        val k = kdj ?: return "无数据"
        return when {
            k.j > 100 -> "超买区"
            k.j < 0 -> "超卖区"
            k.k > k.d -> "金叉"
            else -> "死叉"
        }
    }
}

data class DmiData(
    val pdi: Double,      // +DI
    val mdi: Double,      // -DI
    val adx: Double,      // ADX
    val adxr: Double      // ADXR
)

data class ArBrData(
    val ar: Double,       // AR人气指标
    val br: Double        // BR意愿指标
)

/**
 * 趋势分析结果
 */
data class TrendAnalysis(
    val symbol: String,
    val trendStatus: TrendStatus,     // 趋势状态
    val trendStrength: Double,        // 趋势强度 0-100
    val buySignal: BuySignal,         // 买入信号
    val signalScore: Int,             // 信号评分 0-100
    val signalReasons: List<String>,  // 信号理由
    val riskFactors: List<String>,    // 风险因素
    val biasMa5: Double? = null,      // 相对于MA5的乖离率
    val biasMa10: Double? = null,     // 相对于MA10的乖离率
    val volumeStatus: VolumeStatus,   // 量能状态
    val supportLevel: Double? = null, // 支撑位
    val resistanceLevel: Double? = null, // 阻力位
    val timestamp: Date = Date()
)

enum class TrendStatus {
    STRONG_UP,    // 强势上涨
    UP,           // 上涨
    WEAK_UP,      // 弱势上涨
    SIDEWAYS,     // 横盘震荡
    WEAK_DOWN,    // 弱势下跌
    DOWN,         // 下跌
    STRONG_DOWN   // 强势下跌
}

enum class BuySignal {
    STRONG_BUY,   // 强烈买入
    BUY,          // 买入
    WEAK_BUY,     // 弱势买入
    NEUTRAL,      // 中性
    WEAK_SELL,    // 弱势卖出
    SELL,         // 卖出
    STRONG_SELL   // 强烈卖出
}

enum class VolumeStatus {
    EXPANDING,    // 放量
    SHRINKING,    // 缩量
    NORMAL,       // 正常
    EXTREME       // 异常
}
