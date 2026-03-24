package com.example.stockanalysis.data.model

import java.util.Date

/**
 * 策略配置数据类
 * 支持YAML配置的策略定义
 */
data class StrategyConfig(
    val id: String,                       // 策略ID
    val name: String,                     // 策略名称
    val description: String,              // 策略描述
    val category: StrategyCategory,       // 策略类别
    val version: String = "1.0",          // 版本
    val author: String? = null,           // 作者
    
    // 策略参数
    val parameters: Map<String, StrategyParameter>,
    
    // 策略规则（YAML格式）
    val rulesYaml: String,
    
    // 适用市场
    val supportedMarkets: List<MarketType>,
    
    // 风险等级
    val riskLevel: RiskLevel,
    
    // 时间周期
    val timeframes: List<TimeFrame>,
    
    // 元数据
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    /**
     * 获取策略参数默认值
     */
    fun getDefaultParams(): Map<String, Any> {
        return parameters.mapValues { it.value.defaultValue }
    }
    
    /**
     * 验证参数值
     */
    fun validateParams(values: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()
        
        parameters.forEach { (key, param) ->
            val value = values[key]
            if (value == null && param.required) {
                errors.add("必填参数 $key 未提供")
                return@forEach
            }
            
            value?.let {
                when (param.type) {
                    ParameterType.INT -> {
                        val intValue = (it as? Number)?.toInt()
                        if (intValue == null) {
                            errors.add("参数 $key 必须是整数")
                        } else {
                            param.min?.let { min ->
                                if (intValue < min.toInt()) errors.add("参数 $key 不能小于 $min")
                            }
                            param.max?.let { max ->
                                if (intValue > max.toInt()) errors.add("参数 $key 不能大于 $max")
                            }
                        }
                    }
                    ParameterType.FLOAT -> {
                        val floatValue = (it as? Number)?.toDouble()
                        if (floatValue == null) {
                            errors.add("参数 $key 必须是数值")
                        } else {
                            param.min?.let { min ->
                                if (floatValue < min) errors.add("参数 $key 不能小于 $min")
                            }
                            param.max?.let { max ->
                                if (floatValue > max) errors.add("参数 $key 不能大于 $max")
                            }
                        }
                    }
                    ParameterType.BOOLEAN -> {
                        if (it !is Boolean) {
                            errors.add("参数 $key 必须是布尔值")
                        }
                    }
                    ParameterType.STRING -> {
                        if (it !is String) {
                            errors.add("参数 $key 必须是字符串")
                        } else {
                            param.allowedValues?.let { allowed ->
                                if (it !in allowed) errors.add("参数 $key 必须是以下值之一: $allowed")
                            }
                        }
                    }
                    ParameterType.STRING_LIST -> {
                        if (it !is List<*>) {
                            errors.add("参数 $key 必须是列表")
                        }
                    }
                }
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
}

/**
 * 策略类别
 */
enum class StrategyCategory {
    TREND_FOLLOWING,      // 趋势跟踪
    MEAN_REVERSION,       // 均值回归
    BREAKOUT,             // 突破策略
    MOMENTUM,             // 动量策略
    VALUE,                // 价值投资
    TECHNICAL,            // 纯技术策略
    FUNDAMENTAL,          // 基本面策略
    MULTI_FACTOR,         // 多因子策略
    ARBITRAGE,            // 套利策略
    CUSTOM                // 自定义策略
}

/**
 * 时间周期
 */
enum class TimeFrame {
    M1, M5, M15, M30,    // 分钟
    H1, H4,              // 小时
    D1,                  // 日线
    W1,                  // 周线
    MN1                  // 月线
}

/**
 * 策略参数定义
 */
data class StrategyParameter(
    val name: String,                     // 参数名称
    val description: String,              // 参数描述
    val type: ParameterType,              // 参数类型
    val defaultValue: Any,                // 默认值
    val required: Boolean = true,         // 是否必填
    val min: Double? = null,              // 最小值（数值类型）
    val max: Double? = null,              // 最大值（数值类型）
    val allowedValues: List<String>? = null,  // 允许的字符串值
    val step: Double? = null              // 步长（用于UI滑块）
)

/**
 * 参数类型
 */
enum class ParameterType {
    INT,          // 整数
    FLOAT,        // 浮点数
    BOOLEAN,      // 布尔值
    STRING,       // 字符串
    STRING_LIST   // 字符串列表
}

/**
 * 验证结果
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val messages: List<String>) : ValidationResult()
}

/**
 * 策略实例（运行时配置）
 */
data class StrategyInstance(
    val id: String,
    val configId: String,                 // 引用的策略配置ID
    val name: String,                     // 实例名称（可自定义）
    val params: Map<String, Any>,         // 运行时参数值
    val isActive: Boolean = true,         // 是否激活
    val createdAt: Date = Date(),
    val lastRunAt: Date? = null,
    val runCount: Int = 0,
    val successCount: Int = 0
)

/**
 * 策略执行结果
 */
data class StrategyExecutionResult(
    val instanceId: String,
    val symbol: String,
    val timestamp: Date,
    val signal: StrategySignal,           // 交易信号
    val confidence: Double,               // 置信度 0-1
    val reason: String,                   // 信号原因
    val metrics: Map<String, Any>,        // 执行指标
    val indicators: Map<String, Double>? = null  // 相关指标值
)

/**
 * 策略信号
 */
enum class StrategySignal {
    STRONG_BUY,
    BUY,
    HOLD,
    SELL,
    STRONG_SELL,
    NO_SIGNAL
}

/**
 * 预置策略配置
 * 11种策略配置（对应Python项目）
 */
object PresetStrategies {
    
    /**
     * 获取所有预置策略
     */
    fun getAll(): List<StrategyConfig> {
        return listOf(
            quickAnalysis(),
            standardAnalysis(),
            fullAnalysis(),
            votingAnalysis(),
            trendFollowing(),
            meanReversion(),
            breakoutStrategy(),
            momentumStrategy(),
            valueInvesting(),
            technicalMultiFactor(),
            fundamentalAnalysis()
        )
    }
    
    /**
     * 快速分析策略
     */
    fun quickAnalysis(): StrategyConfig = StrategyConfig(
        id = "quick_analysis",
        name = "快速分析",
        description = "使用基础技术指标进行快速分析，适合日内短线",
        category = StrategyCategory.TECHNICAL,
        version = "1.0",
        parameters = mapOf(
            "lookback_days" to StrategyParameter(
                name = "回看天数",
                description = "分析历史数据的天数",
                type = ParameterType.INT,
                defaultValue = 5,
                min = 1.0,
                max = 30.0
            ),
            "min_confidence" to StrategyParameter(
                name = "最小置信度",
                description = "生成信号所需的最小置信度",
                type = ParameterType.FLOAT,
                defaultValue = 0.6,
                min = 0.0,
                max = 1.0,
                step = 0.05
            )
        ),
        rulesYaml = """
            indicators:
              - type: MA
                periods: [5, 10]
              - type: RSI
                period: 6
              - type: MACD
            
            conditions:
              buy:
                - rsi < 30
                - close > ma5
              sell:
                - rsi > 70
                - close < ma5
            
            scoring:
              weights:
                trend: 0.4
                momentum: 0.4
                volume: 0.2
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.MEDIUM,
        timeframes = listOf(TimeFrame.D1, TimeFrame.H1),
        isDefault = true
    )
    
    /**
     * 标准分析策略
     */
    fun standardAnalysis(): StrategyConfig = StrategyConfig(
        id = "standard_analysis",
        name = "标准分析",
        description = "综合分析技术面和基础面，适合波段操作",
        category = StrategyCategory.MULTI_FACTOR,
        version = "1.0",
        parameters = mapOf(
            "lookback_days" to StrategyParameter(
                name = "回看天数",
                description = "分析历史数据的天数",
                type = ParameterType.INT,
                defaultValue = 20,
                min = 5.0,
                max = 60.0
            ),
            "ma_periods" to StrategyParameter(
                name = "均线周期",
                description = "分析的均线周期",
                type = ParameterType.STRING_LIST,
                defaultValue = listOf("5", "10", "20", "60")
            ),
            "fundamental_weight" to StrategyParameter(
                name = "基本面权重",
                description = "基本面分析在总评分中的权重",
                type = ParameterType.FLOAT,
                defaultValue = 0.3,
                min = 0.0,
                max = 1.0,
                step = 0.1
            )
        ),
        rulesYaml = """
            indicators:
              - type: MA
                periods: [5, 10, 20, 60]
              - type: RSI
                period: 14
              - type: MACD
              - type: BOLL
              - type: KDJ
            
            fundamental:
              - pe_ratio
              - pb_ratio
              - roe
            
            conditions:
              buy:
                - trend == "up" and rsi < 70
                - ma5 > ma20 and volume > avg_volume
              sell:
                - trend == "down" or rsi > 80
                - close < ma60
            
            scoring:
              technical: 0.7
              fundamental: 0.3
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.MEDIUM,
        timeframes = listOf(TimeFrame.D1, TimeFrame.W1)
    )
    
    /**
     * 完整分析策略
     */
    fun fullAnalysis(): StrategyConfig = StrategyConfig(
        id = "full_analysis",
        name = "完整分析",
        description = "全面深入的多维度分析，适合中长期投资决策",
        category = StrategyCategory.MULTI_FACTOR,
        version = "1.0",
        parameters = mapOf(
            "lookback_days" to StrategyParameter(
                name = "回看天数",
                description = "分析历史数据的天数",
                type = ParameterType.INT,
                defaultValue = 90,
                min = 30.0,
                max = 252.0
            ),
            "use_fundamental" to StrategyParameter(
                name = "启用基本面分析",
                description = "是否包含基本面分析",
                type = ParameterType.BOOLEAN,
                defaultValue = true
            ),
            "use_sentiment" to StrategyParameter(
                name = "启用情绪分析",
                description = "是否包含市场情绪分析",
                type = ParameterType.BOOLEAN,
                defaultValue = true
            )
        ),
        rulesYaml = """
            indicators:
              technical:
                - type: MA
                  periods: [5, 10, 20, 60, 120]
                - type: RSI
                  periods: [6, 12, 24]
                - type: MACD
                - type: BOLL
                - type: KDJ
                - type: CCI
              
              pattern:
                - support_resistance
                - trend_lines
                - chart_patterns
            
            fundamental:
              - valuation: [pe, pb, ps]
              - profitability: [roe, roa, margin]
              - growth: [revenue_growth, profit_growth]
              - financial_health: [debt_ratio, current_ratio]
            
            sentiment:
              - news_sentiment
              - market_sentiment
            
            scoring:
              technical: 0.5
              fundamental: 0.35
              sentiment: 0.15
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.HIGH,
        timeframes = listOf(TimeFrame.D1, TimeFrame.W1, TimeFrame.MN1)
    )
    
    /**
     * 投票分析策略
     */
    fun votingAnalysis(): StrategyConfig = StrategyConfig(
        id = "voting_analysis",
        name = "投票分析",
        description = "多策略投票机制，综合多个子策略的信号",
        category = StrategyCategory.MULTI_FACTOR,
        version = "1.0",
        parameters = mapOf(
            "strategies" to StrategyParameter(
                name = "参与投票的策略",
                description = "参与投票的子策略列表",
                type = ParameterType.STRING_LIST,
                defaultValue = listOf("trend", "momentum", "mean_reversion", "breakout")
            ),
            "min_agreement" to StrategyParameter(
                name = "最小一致度",
                description = "生成信号所需的最小策略一致度",
                type = ParameterType.FLOAT,
                defaultValue = 0.6,
                min = 0.5,
                max = 1.0,
                step = 0.1
            )
        ),
        rulesYaml = """
            sub_strategies:
              - trend_following
              - momentum
              - mean_reversion
              - breakout
              - value
            
            voting:
              method: weighted
              weights:
                trend_following: 0.25
                momentum: 0.25
                mean_reversion: 0.2
                breakout: 0.2
                value: 0.1
            
            consensus:
              strong_buy: >= 0.8
              buy: >= 0.6
              hold: < 0.6 and > 0.4
              sell: <= 0.4
              strong_sell: <= 0.2
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.MEDIUM,
        timeframes = listOf(TimeFrame.D1)
    )
    
    /**
     * 趋势跟踪策略
     */
    fun trendFollowing(): StrategyConfig = StrategyConfig(
        id = "trend_following",
        name = "趋势跟踪",
        description = "顺势而为，跟随主要趋势进行交易",
        category = StrategyCategory.TREND_FOLLOWING,
        version = "1.0",
        parameters = mapOf(
            "fast_ma" to StrategyParameter(
                name = "快速均线",
                description = "快速移动平均线周期",
                type = ParameterType.INT,
                defaultValue = 10,
                min = 5.0,
                max = 30.0
            ),
            "slow_ma" to StrategyParameter(
                name = "慢速均线",
                description = "慢速移动平均线周期",
                type = ParameterType.INT,
                defaultValue = 30,
                min = 20.0,
                max = 120.0
            ),
            "trend_strength_threshold" to StrategyParameter(
                name = "趋势强度阈值",
                description = "确认趋势所需的最小强度",
                type = ParameterType.FLOAT,
                defaultValue = 0.3,
                min = 0.1,
                max = 1.0,
                step = 0.1
            )
        ),
        rulesYaml = """
            indicators:
              - type: MA
                periods: [${'$'}{fast_ma}, ${'$'}{slow_ma}]
              - type: ADX
                period: 14
            
            entry:
              long:
                - ma${'$'}{fast_ma} crosses_above ma${'$'}{slow_ma}
                - adx > 25
              short:
                - ma${'$'}{fast_ma} crosses_below ma${'$'}{slow_ma}
                - adx > 25
            
            exit:
              long:
                - ma${'$'}{fast_ma} crosses_below ma${'$'}{slow_ma}
              short:
                - ma${'$'}{fast_ma} crosses_above ma${'$'}{slow_ma}
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.MEDIUM,
        timeframes = listOf(TimeFrame.D1, TimeFrame.W1)
    )
    
    /**
     * 均值回归策略
     */
    fun meanReversion(): StrategyConfig = StrategyConfig(
        id = "mean_reversion",
        name = "均值回归",
        description = "利用价格偏离均线的回调进行交易",
        category = StrategyCategory.MEAN_REVERSION,
        version = "1.0",
        parameters = mapOf(
            "ma_period" to StrategyParameter(
                name = "均线周期",
                description = "参考移动平均线周期",
                type = ParameterType.INT,
                defaultValue = 20,
                min = 10.0,
                max = 60.0
            ),
            "deviation_threshold" to StrategyParameter(
                name = "偏离阈值",
                description = "开仓所需的偏离程度(%)",
                type = ParameterType.FLOAT,
                defaultValue = 5.0,
                min = 2.0,
                max = 15.0,
                step = 0.5
            )
        ),
        rulesYaml = """
            indicators:
              - type: MA
                period: ${'$'}{ma_period}
              - type: BOLL
                period: 20
                std_dev: 2
            
            entry:
              long:
                - close < lower_bollinger
                - (ma${'$'}{ma_period} - close) / ma${'$'}{ma_period} > ${'$'}{deviation_threshold}%
              short:
                - close > upper_bollinger
                - (close - ma${'$'}{ma_period}) / ma${'$'}{ma_period} > ${'$'}{deviation_threshold}%
            
            exit:
              long: close >= ma${'$'}{ma_period}
              short: close <= ma${'$'}{ma_period}
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.MEDIUM,
        timeframes = listOf(TimeFrame.D1, TimeFrame.H4)
    )
    
    /**
     * 突破策略
     */
    fun breakoutStrategy(): StrategyConfig = StrategyConfig(
        id = "breakout",
        name = "突破策略",
        description = "捕捉价格突破关键阻力或支撑的交易机会",
        category = StrategyCategory.BREAKOUT,
        version = "1.0",
        parameters = mapOf(
            "lookback_period" to StrategyParameter(
                name = "回看周期",
                description = "识别支撑阻力的回看周期",
                type = ParameterType.INT,
                defaultValue = 20,
                min = 10.0,
                max = 60.0
            ),
            "volume_confirm" to StrategyParameter(
                name = "成交量确认",
                description = "是否需要成交量确认",
                type = ParameterType.BOOLEAN,
                defaultValue = true
            )
        ),
        rulesYaml = """
            indicators:
              - type: DonchianChannel
                period: ${'$'}{lookback_period}
              - type: ATR
                period: 14
            
            entry:
              long:
                - close > highest_high(${'$'}{lookback_period})
                - volume > avg_volume * 1.5 if ${'$'}{volume_confirm}
              short:
                - close < lowest_low(${'$'}{lookback_period})
                - volume > avg_volume * 1.5 if ${'$'}{volume_confirm}
            
            stop_loss:
              long: entry - atr * 2
              short: entry + atr * 2
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.HIGH,
        timeframes = listOf(TimeFrame.D1, TimeFrame.H1)
    )
    
    /**
     * 动量策略
     */
    fun momentumStrategy(): StrategyConfig = StrategyConfig(
        id = "momentum",
        name = "动量策略",
        description = "追踪价格动量变化，捕捉加速上涨或下跌机会",
        category = StrategyCategory.MOMENTUM,
        version = "1.0",
        parameters = mapOf(
            "momentum_period" to StrategyParameter(
                name = "动量周期",
                description = "计算价格动量的周期",
                type = ParameterType.INT,
                defaultValue = 12,
                min = 5.0,
                max = 30.0
            ),
            "rsi_period" to StrategyParameter(
                name = "RSI周期",
                description = "RSI指标周期",
                type = ParameterType.INT,
                defaultValue = 14,
                min = 7.0,
                max = 21.0
            )
        ),
        rulesYaml = """
            indicators:
              - type: Momentum
                period: ${'$'}{momentum_period}
              - type: RSI
                period: ${'$'}{rsi_period}
              - type: ROC
                period: 10
            
            entry:
              long:
                - momentum > 0 and momentum > momentum[1]
                - rsi > 50 and rsi < 70
                - roc > 0
              short:
                - momentum < 0 and momentum < momentum[1]
                - rsi < 50 and rsi > 30
                - roc < 0
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.HIGH,
        timeframes = listOf(TimeFrame.D1, TimeFrame.H1)
    )
    
    /**
     * 价值投资策略
     */
    fun valueInvesting(): StrategyConfig = StrategyConfig(
        id = "value_investing",
        name = "价值投资",
        description = "基于基本面估值的安全边际投资策略",
        category = StrategyCategory.VALUE,
        version = "1.0",
        parameters = mapOf(
            "max_pe" to StrategyParameter(
                name = "最大市盈率",
                description = "允许的最大PE值",
                type = ParameterType.FLOAT,
                defaultValue = 20.0,
                min = 5.0,
                max = 50.0
            ),
            "max_pb" to StrategyParameter(
                name = "最大市净率",
                description = "允许的最大PB值",
                type = ParameterType.FLOAT,
                defaultValue = 3.0,
                min = 0.5,
                max = 10.0
            ),
            "min_roe" to StrategyParameter(
                name = "最小ROE",
                description = "要求的最小净资产收益率(%)",
                type = ParameterType.FLOAT,
                defaultValue = 10.0,
                min = 5.0,
                max = 30.0
            )
        ),
        rulesYaml = """
            filters:
              - pe < ${'$'}{max_pe}
              - pb < ${'$'}{max_pb}
              - roe > ${'$'}{min_roe}%
              - debt_to_equity < 60%
            
            scoring:
              valuation: 0.4
              profitability: 0.35
              stability: 0.25
            
            ranking:
              by: composite_score
              top_n: 20
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.LOW,
        timeframes = listOf(TimeFrame.W1, TimeFrame.MN1)
    )
    
    /**
     * 技术多因子策略
     */
    fun technicalMultiFactor(): StrategyConfig = StrategyConfig(
        id = "technical_multifactor",
        name = "技术多因子",
        description = "综合多种技术指标的多因子评分系统",
        category = StrategyCategory.TECHNICAL,
        version = "1.0",
        parameters = mapOf(
            "factors" to StrategyParameter(
                name = "因子列表",
                description = "参与评分的因子",
                type = ParameterType.STRING_LIST,
                defaultValue = listOf("trend", "momentum", "volatility", "volume", "support_resistance")
            ),
            "min_score" to StrategyParameter(
                name = "最小分数",
                description = "开仓所需的最小分数",
                type = ParameterType.FLOAT,
                defaultValue = 70.0,
                min = 50.0,
                max = 90.0
            )
        ),
        rulesYaml = """
            factors:
              trend:
                indicators: [ma_alignment, adx]
                weight: 0.25
              momentum:
                indicators: [rsi, macd, momentum]
                weight: 0.25
              volatility:
                indicators: [atr, boll_width]
                weight: 0.15
              volume:
                indicators: [volume_trend, obv]
                weight: 0.2
              support_resistance:
                indicators: [pivot_points, fibonacci]
                weight: 0.15
            
            scoring:
              scale: 0-100
              buy_threshold: ${'$'}{min_score}
              sell_threshold: ${'$'}{min_score} # 使用相同阈值
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.MEDIUM,
        timeframes = listOf(TimeFrame.D1)
    )
    
    /**
     * 基本面分析策略
     */
    fun fundamentalAnalysis(): StrategyConfig = StrategyConfig(
        id = "fundamental_analysis",
        name = "基本面分析",
        description = "深度基本面分析，关注公司财务健康度和成长性",
        category = StrategyCategory.FUNDAMENTAL,
        version = "1.0",
        parameters = mapOf(
            "analysis_depth" to StrategyParameter(
                name = "分析深度",
                description = "分析的详细程度",
                type = ParameterType.STRING,
                defaultValue = "standard",
                allowedValues = listOf("basic", "standard", "comprehensive")
            ),
            "compare_with_industry" to StrategyParameter(
                name = "行业对比",
                description = "是否进行行业对比分析",
                type = ParameterType.BOOLEAN,
                defaultValue = true
            )
        ),
        rulesYaml = """
            categories:
              valuation:
                metrics: [pe, pb, ps, peg]
                weight: 0.25
              profitability:
                metrics: [roe, roa, gross_margin, net_margin]
                weight: 0.25
              growth:
                metrics: [revenue_growth, profit_growth, cagr_3y]
                weight: 0.2
              financial_health:
                metrics: [current_ratio, debt_to_equity, interest_coverage]
                weight: 0.2
              quality:
                metrics: [earnings_quality, cash_flow_quality]
                weight: 0.1
            
            evaluation:
              industry_comparison: ${'$'}{compare_with_industry}
              historical_comparison: true
              peer_ranking: true
        """.trimIndent(),
        supportedMarkets = listOf(MarketType.A_SHARE, MarketType.HK, MarketType.US),
        riskLevel = RiskLevel.LOW,
        timeframes = listOf(TimeFrame.MN1)
    )
}
