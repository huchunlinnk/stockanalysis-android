package com.example.stockanalysis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.stockanalysis.data.local.Converters

/**
 * 用户记忆数据模型
 *
 * 用于存储用户的偏好、常用操作、历史决策等信息
 * 帮助 Agent 更好地理解用户习惯，提供个性化服务
 */
@Entity(tableName = "user_memory")
@TypeConverters(Converters::class)
data class UserMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * 记忆类型
     */
    val type: MemoryType,

    /**
     * 记忆键（用于快速查询）
     */
    val key: String,

    /**
     * 记忆值
     */
    val value: String,

    /**
     * 记忆描述
     */
    val description: String = "",

    /**
     * 置信度 (0.0 - 1.0)
     * 表示这条记忆的可靠程度
     */
    val confidence: Float = 1.0f,

    /**
     * 访问次数（越多说明越重要）
     */
    val accessCount: Int = 0,

    /**
     * 创建时间
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * 最后访问时间
     */
    val lastAccessedAt: Long = System.currentTimeMillis(),

    /**
     * 过期时间（可选，null 表示永不过期）
     */
    val expiresAt: Long? = null
)

/**
 * 记忆类型
 */
enum class MemoryType {
    /**
     * 用户偏好
     * 例如：喜欢的分析维度、风险偏好等
     */
    PREFERENCE,

    /**
     * 常用股票
     * 用户经常分析的股票代码
     */
    FREQUENT_STOCK,

    /**
     * 历史决策
     * 用户过去的买卖决策
     */
    HISTORICAL_DECISION,

    /**
     * 分析模式
     * 用户常用的分析参数和配置
     */
    ANALYSIS_PATTERN,

    /**
     * 市场观点
     * 用户对某个股票或行业的看法
     */
    MARKET_VIEW,

    /**
     * 风险承受能力
     * 用户的风险评级和偏好
     */
    RISK_TOLERANCE,

    /**
     * 投资目标
     * 用户的投资目标和期望收益
     */
    INVESTMENT_GOAL,

    /**
     * 自定义标签
     * 用户为股票添加的自定义标签
     */
    CUSTOM_TAG,

    /**
     * 学习内容
     * Agent 从用户行为中学到的模式
     */
    LEARNED_PATTERN,

    /**
     * 其他
     */
    OTHER
}

/**
 * 记忆查询结果
 */
data class MemoryQueryResult(
    val memories: List<UserMemory>,
    val totalCount: Int,
    val averageConfidence: Float
)

/**
 * 用户偏好数据类
 */
data class UserPreference(
    val key: String,
    val value: String,
    val description: String = ""
) {
    companion object {
        // 预定义的偏好键
        const val PREF_RISK_LEVEL = "risk_level" // 风险偏好: conservative/moderate/aggressive
        const val PREF_INVESTMENT_STYLE = "investment_style" // 投资风格: value/growth/dividend
        const val PREF_TIME_HORIZON = "time_horizon" // 投资期限: short/medium/long
        const val PREF_ANALYSIS_DEPTH = "analysis_depth" // 分析深度: basic/detailed/comprehensive
        const val PREF_NOTIFICATION_FREQ = "notification_freq" // 通知频率: realtime/daily/weekly
        const val PREF_FAVORITE_INDICATORS = "favorite_indicators" // 喜欢的技术指标
        const val PREF_AUTO_ANALYSIS = "auto_analysis" // 是否自动分析
    }
}

/**
 * 股票访问记录
 */
data class StockAccessRecord(
    val symbol: String,
    val name: String,
    val accessCount: Int,
    val lastAccessTime: Long,
    val avgScore: Float?, // 平均评分
    val lastDecision: String? // 最后决策：buy/sell/hold
)

/**
 * 决策历史
 */
data class DecisionHistory(
    val stockSymbol: String,
    val stockName: String,
    val decision: String, // buy/sell/hold
    val reason: String,
    val timestamp: Long,
    val actualOutcome: String? = null // 实际结果（可选）
)
