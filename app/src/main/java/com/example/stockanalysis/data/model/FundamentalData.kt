package com.example.stockanalysis.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 基本面数据汇总
 * 存储股票的完整基本面信息
 */
@Entity(
    tableName = "fundamental_data",
    indices = [Index(value = ["symbol"], unique = true)]
)
data class FundamentalData(
    @PrimaryKey
    val symbol: String,                    // 股票代码
    val name: String = "",                 // 股票名称
    val updateTime: Date = Date(),         // 数据更新时间
    
    // 估值指标
    val peRatio: Double? = null,           // 市盈率（动态）
    val pbRatio: Double? = null,           // 市净率
    val psRatio: Double? = null,           // 市销率
    val pegRatio: Double? = null,          // PEG比率
    val enterpriseValue: Double? = null,   // 企业价值
    
    // 财务指标（JSON序列化存储）
    val financialIndicatorsJson: String? = null,
    
    // 成长性指标（JSON序列化存储）
    val growthMetricsJson: String? = null,
    
    // 分红信息（JSON序列化存储）
    val dividendInfoJson: String? = null,
    
    // 机构持仓（JSON序列化存储）
    val institutionalHoldingJson: String? = null,
    
    // 数据来源
    val source: String = "akshare",        // 数据来源
    val isCacheValid: Boolean = true       // 缓存是否有效
) {
    /**
     * 获取缓存状态
     */
    fun isExpired(cacheDays: Int = 1): Boolean {
        val now = Date()
        val diffMillis = now.time - updateTime.time
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)
        return diffDays >= cacheDays || !isCacheValid
    }
}

/**
 * 财务指标
 * 包含盈利能力、偿债能力、运营效率等
 */
data class FinancialIndicators(
    // 盈利能力
    val roe: Double? = null,               // 净资产收益率 (%)
    val roa: Double? = null,               // 总资产收益率 (%)
    val grossMargin: Double? = null,       // 毛利率 (%)
    val netMargin: Double? = null,         // 净利率 (%)
    val operatingMargin: Double? = null,   // 营业利润率 (%)
    
    // 偿债能力
    val debtToEquity: Double? = null,      // 资产负债率 (%)
    val currentRatio: Double? = null,      // 流动比率
    val quickRatio: Double? = null,        // 速动比率
    val interestCoverage: Double? = null,  // 利息保障倍数
    
    // 运营效率
    val inventoryTurnover: Double? = null, // 存货周转率
    val receivablesTurnover: Double? = null, // 应收账款周转率
    val assetTurnover: Double? = null,     // 总资产周转率
    
    // 现金流
    val operatingCashFlow: Double? = null, // 经营活动现金流（亿元）
    val freeCashFlow: Double? = null,      // 自由现金流（亿元）
    val cashFlowPerShare: Double? = null,  // 每股现金流（元）
    
    // 最新报告期
    val reportDate: String? = null,        // 报告期（YYYYMMDD）
    val reportType: String? = null         // 报告类型：年报/季报
) {
    /**
     * 获取盈利能力评级
     */
    fun getProfitabilityRating(): String {
        return when {
            (roe ?: 0.0) > 15 && (grossMargin ?: 0.0) > 30 -> "优秀"
            (roe ?: 0.0) > 10 && (grossMargin ?: 0.0) > 20 -> "良好"
            (roe ?: 0.0) > 5 && (grossMargin ?: 0.0) > 10 -> "一般"
            else -> "较弱"
        }
    }
    
    /**
     * 获取财务健康度评级
     */
    fun getFinancialHealthRating(): String {
        return when {
            (debtToEquity ?: 0.0) < 40 && (currentRatio ?: 0.0) > 1.5 -> "健康"
            (debtToEquity ?: 0.0) < 60 && (currentRatio ?: 0.0) > 1.0 -> "一般"
            (debtToEquity ?: 0.0) < 80 -> "偏紧"
            else -> "高风险"
        }
    }
    
    /**
     * 获取现金流评级
     */
    fun getCashFlowRating(): String {
        return when {
            (operatingCashFlow ?: 0.0) > 0 && (freeCashFlow ?: 0.0) > 0 -> "充裕"
            (operatingCashFlow ?: 0.0) > 0 -> "正常"
            (operatingCashFlow ?: 0.0) < 0 -> "紧张"
            else -> "未知"
        }
    }
}

/**
 * 成长性指标
 */
data class GrowthMetrics(
    // 营收增长
    val revenueGrowthYoY: Double? = null,      // 营收同比增长率 (%)
    val revenueGrowthQoQ: Double? = null,      // 营收环比增长率 (%)
    val revenueGrowth3Y: Double? = null,       // 3年营收复合增长率 (%)
    val revenueGrowth5Y: Double? = null,       // 5年营收复合增长率 (%)
    
    // 净利润增长
    val netProfitGrowthYoY: Double? = null,    // 净利润同比增长率 (%)
    val netProfitGrowthQoQ: Double? = null,    // 净利润环比增长率 (%)
    val netProfitGrowth3Y: Double? = null,     // 3年净利润复合增长率 (%)
    val netProfitGrowth5Y: Double? = null,     // 5年净利润复合增长率 (%)
    
    // 利润增长
    val grossProfitGrowthYoY: Double? = null,  // 毛利润同比增长率 (%)
    val operatingProfitGrowthYoY: Double? = null, // 营业利润同比增长率 (%)
    
    // 资产增长
    val totalAssetGrowthYoY: Double? = null,   // 总资产同比增长率 (%)
    val equityGrowthYoY: Double? = null,       // 净资产同比增长率 (%)
    
    // 最新报告期
    val reportDate: String? = null
) {
    /**
     * 获取成长性评级
     */
    fun getGrowthRating(): String {
        return when {
            (revenueGrowthYoY ?: 0.0) > 30 && (netProfitGrowthYoY ?: 0.0) > 30 -> "高速增长"
            (revenueGrowthYoY ?: 0.0) > 15 && (netProfitGrowthYoY ?: 0.0) > 15 -> "稳健增长"
            (revenueGrowthYoY ?: 0.0) > 0 && (netProfitGrowthYoY ?: 0.0) > 0 -> "缓慢增长"
            (revenueGrowthYoY ?: 0.0) < 0 || (netProfitGrowthYoY ?: 0.0) < 0 -> "负增长"
            else -> "未知"
        }
    }
    
    /**
     * 获取综合成长评分 (0-100)
     */
    fun getGrowthScore(): Int {
        var score = 50
        
        // 营收增长评分
        score += when {
            (revenueGrowthYoY ?: 0.0) > 30 -> 20
            (revenueGrowthYoY ?: 0.0) > 15 -> 15
            (revenueGrowthYoY ?: 0.0) > 0 -> 5
            (revenueGrowthYoY ?: 0.0) < 0 -> -10
            else -> 0
        }
        
        // 利润增长评分
        score += when {
            (netProfitGrowthYoY ?: 0.0) > 30 -> 20
            (netProfitGrowthYoY ?: 0.0) > 15 -> 15
            (netProfitGrowthYoY ?: 0.0) > 0 -> 5
            (netProfitGrowthYoY ?: 0.0) < 0 -> -10
            else -> 0
        }
        
        return score.coerceIn(0, 100)
    }
}

/**
 * 分红信息
 */
data class DividendInfo(
    val symbol: String = "",
    val dividendPerShare: Double? = null,      // 每股分红（元）
    val dividendYield: Double? = null,         // 股息率 (%)
    val payoutRatio: Double? = null,           // 分红率 (%)
    val exDividendDate: String? = null,        // 除权除息日
    val recordDate: String? = null,            // 股权登记日
    val dividendDate: String? = null,          // 分红日
    val isPreTax: Boolean = true,              // 是否税前
    
    // 历史分红记录
    val dividendHistory: List<DividendEvent> = emptyList(),
    
    // TTM分红统计
    val ttmDividendPerShare: Double? = null,   // 近12个月每股分红
    val ttmDividendCount: Int = 0,             // 近12个月分红次数
    
    // 分红持续性
    val consecutiveYears: Int = 0,             // 连续分红年数
    val dividendStability: String = "未知"      // 分红稳定性
) {
    /**
     * 获取分红评级
     */
    fun getDividendRating(): String {
        return when {
            (dividendYield ?: 0.0) > 4.0 && consecutiveYears >= 5 -> "高股息"
            (dividendYield ?: 0.0) > 2.0 && consecutiveYears >= 3 -> "稳定分红"
            (dividendYield ?: 0.0) > 0 -> "有分红"
            else -> "无分红"
        }
    }
}

/**
 * 分红事件
 */
data class DividendEvent(
    val eventDate: String,                     // 事件日期
    val exDividendDate: String?,               // 除权除息日
    val recordDate: String?,                   // 股权登记日
    val announcementDate: String?,             // 公告日
    val cashDividendPerShare: Double,          // 每股现金分红
    val isPreTax: Boolean = true,              // 是否税前
    val stockDividend: Double? = null,         // 每股送股
    val capitalIncrease: Double? = null        // 每股转增
)

/**
 * 机构持仓
 */
data class InstitutionalHolding(
    val symbol: String = "",
    val institutionCount: Int? = null,         // 机构数量
    val holdingRatio: Double? = null,          // 机构持仓比例 (%)
    val holdingChange: Double? = null,         // 持仓变动（万股）
    val holdingChangeRatio: Double? = null,    // 持仓变动比例 (%)
    
    // 前十大流通股东
    val top10Holders: List<ShareholderInfo> = emptyList(),
    val top10HoldingRatio: Double? = null,     // 前十大股东持股比例
    
    // 基金持仓
    val fundHoldingRatio: Double? = null,      // 基金持仓比例
    val fundHoldingChange: Double? = null,     // 基金持仓变动
    
    // 北向资金/南向资金
    val northboundHolding: Double? = null,     // 北向资金持仓（万股）
    val northboundChange: Double? = null,      // 北向资金变动（万股）
    
    // 报告期
    val reportDate: String? = null,            // 报告期
    val updateTime: Date = Date()
) {
    /**
     * 获取机构关注度评级
     */
    fun getInstitutionRating(): String {
        return when {
            (institutionCount ?: 0) > 100 && (holdingRatio ?: 0.0) > 30 -> "高度关注"
            (institutionCount ?: 0) > 50 && (holdingRatio ?: 0.0) > 15 -> "关注"
            (institutionCount ?: 0) > 10 && (holdingRatio ?: 0.0) > 5 -> "一般关注"
            else -> "关注较少"
        }
    }
    
    /**
     * 判断机构是否在增持
     */
    fun isIncreasingHolding(): Boolean {
        return (holdingChange ?: 0.0) > 0 || (holdingChangeRatio ?: 0.0) > 0
    }
}

/**
 * 股东信息
 */
data class ShareholderInfo(
    val name: String,                          // 股东名称
    val holdingShares: Double,                 // 持股数量（万股）
    val holdingRatio: Double,                  // 持股比例 (%)
    val holdingChange: Double?,                // 持股变动（万股）
    val shareholderType: String                // 股东类型：机构/个人/基金等
)

/**
 * 估值指标
 */
data class ValuationMetrics(
    val symbol: String = "",
    val peTtm: Double? = null,                 // 市盈率TTM
    val peStatic: Double? = null,              // 静态市盈率
    val peForward: Double? = null,             // 预测市盈率
    val pbRatio: Double? = null,               // 市净率
    val psRatio: Double? = null,               // 市销率
    val pcRatio: Double? = null,               // 市现率
    val evEbitda: Double? = null,              // 企业价值/EBITDA
    
    // 行业对比
    val industryPeMedian: Double? = null,      // 行业中位数PE
    val industryPbMedian: Double? = null,      // 行业中位数PB
    val pePercentile: Double? = null,          // PE历史百分位
    val pbPercentile: Double? = null,          // PB历史百分位
    
    // 估值判断
    val updateTime: Date = Date()
) {
    /**
     * 获取估值评级
     */
    fun getValuationRating(): String {
        return when {
            isUndervalued() -> "低估"
            isFairValued() -> "合理"
            isOvervalued() -> "高估"
            else -> "未知"
        }
    }
    
    /**
     * 是否低估
     */
    fun isUndervalued(): Boolean {
        return (peTtm ?: 999.0) < 15 || 
               (pbRatio ?: 999.0) < 1.5 ||
               (pePercentile ?: 100.0) < 30
    }
    
    /**
     * 是否高估
     */
    fun isOvervalued(): Boolean {
        return (peTtm ?: 0.0) > 50 || 
               (pbRatio ?: 0.0) > 5 ||
               (pePercentile ?: 0.0) > 70
    }
    
    /**
     * 估值是否合理
     */
    fun isFairValued(): Boolean {
        return !isUndervalued() && !isOvervalued()
    }
    
    /**
     * 相对于行业的估值水平
     */
    fun getRelativeToIndustry(): String {
        val pe = peTtm
        val industryPe = industryPeMedian
        
        if (pe == null || industryPe == null || industryPe == 0.0) {
            return "无法比较"
        }
        
        val ratio = pe / industryPe
        return when {
            ratio < 0.8 -> "低于行业平均"
            ratio > 1.2 -> "高于行业平均"
            else -> "与行业持平"
        }
    }
}

/**
 * 基本面分析结果
 * 用于在UI层展示
 */
data class FundamentalAnalysisResult(
    val symbol: String,
    val name: String,
    val updateTime: Date,
    
    // 综合评分
    val overallScore: Int,                     // 综合评分 0-100
    
    // 各维度评分
    val valuationScore: Int,                   // 估值评分
    val profitabilityScore: Int,               // 盈利能力评分
    val growthScore: Int,                      // 成长性评分
    val financialHealthScore: Int,             // 财务健康度评分
    val dividendScore: Int,                    // 分红评分
    val institutionScore: Int,                 // 机构关注度评分
    
    // 分析结论
    val valuationConclusion: String,           // 估值结论
    val profitabilityConclusion: String,       // 盈利能力结论
    val growthConclusion: String,              // 成长性结论
    val financialHealthConclusion: String,     // 财务健康度结论
    val dividendConclusion: String,            // 分红结论
    val institutionConclusion: String,         // 机构持仓结论
    
    // 风险提示
    val riskFactors: List<String>,             // 风险因素
    
    // 投资建议
    val investmentAdvice: String               // 投资建议
)
