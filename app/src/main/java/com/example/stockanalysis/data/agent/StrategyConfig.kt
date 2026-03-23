package com.example.stockanalysis.data.agent

import java.util.Date

/**
 * 策略配置
 */
data class StrategyConfig(
    val id: String,
    val name: String,
    val description: String,
    val category: StrategyCategory,
    val systemPrompt: String,
    val tools: List<String>,
    val indicators: List<String>,
    val maxIterations: Int = 5,
    val temperature: Double = 0.7,
    val isEnabled: Boolean = true
)

/**
 * 策略分类
 */
enum class StrategyCategory {
    TECHNICAL,      // 技术分析
    FUNDAMENTAL,    // 基本面分析
    SENTIMENT,      // 情绪分析
    COMPREHENSIVE   // 综合分析
}

/**
 * 策略加载器
 */
interface StrategyLoader {
    fun loadAllStrategies(): List<StrategyConfig>
    fun loadStrategy(id: String): StrategyConfig?
    fun getEnabledStrategies(): List<StrategyConfig>
}

/**
 * 内置策略加载器
 */
class BuiltInStrategyLoader : StrategyLoader {
    
    override fun loadAllStrategies(): List<StrategyConfig> {
        return getBuiltInStrategies()
    }
    
    override fun loadStrategy(id: String): StrategyConfig? {
        return getBuiltInStrategies().find { it.id == id }
    }
    
    override fun getEnabledStrategies(): List<StrategyConfig> {
        return getBuiltInStrategies().filter { it.isEnabled }
    }
    
    private fun getBuiltInStrategies(): List<StrategyConfig> {
        return listOf(
            // 1. 均线金叉策略
            StrategyConfig(
                id = "ma_golden_cross",
                name = "均线金叉策略",
                description = "基于均线金叉死叉判断买卖点",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个专业的股票技术分析师，擅长使用均线系统判断趋势和买卖点。

分析要点：
1. 识别均线金叉（短期均线上穿长期均线）- 买入信号
2. 识别均线死叉（短期均线下穿长期均线）- 卖出信号
3. 判断均线排列形态（多头排列/空头排列）
4. 结合量能确认信号有效性
5. 给出明确的操作建议

请使用get_technical_indicators工具获取技术指标数据，然后进行分析。
输出格式：
1. 当前均线状态
2. 金叉/死叉信号判断
3. 操作建议（买入/卖出/观望）
4. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators", "get_kline_data"),
                indicators = listOf("MA5", "MA10", "MA20", "MA60")
            ),
            
            // 2. MACD背离策略
            StrategyConfig(
                id = "macd_divergence",
                name = "MACD背离策略",
                description = "基于MACD背离判断趋势反转",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个专业的技术分析专家，擅长使用MACD指标判断趋势反转。

分析要点：
1. 识别MACD顶背离（价格新高，MACD未新高）- 卖出信号
2. 识别MACD底背离（价格新低，MACD未新低）- 买入信号
3. 观察DIF与DEA的金叉死叉
4. 结合MACD柱状图变化判断动能

请使用get_technical_indicators工具获取MACD数据，然后进行分析。
输出格式：
1. MACD当前状态
2. 背离信号判断
3. 操作建议
4. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators", "get_kline_data"),
                indicators = listOf("MACD")
            ),
            
            // 3. KDJ超买超卖策略
            StrategyConfig(
                id = "kdj_overbought",
                name = "KDJ超买超卖策略",
                description = "基于KDJ指标判断超买超卖",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个专业的短线交易专家，擅长使用KDJ指标判断超买超卖。

分析要点：
1. KDJ > 80 为超买区域，考虑卖出
2. KDJ < 20 为超卖区域，考虑买入
3. K上穿D为金叉买入信号
4. K下穿D为死叉卖出信号
5. J值 > 100 严重超买，J值 < 0 严重超卖

请使用get_technical_indicators工具获取KDJ数据，然后进行分析。
输出格式：
1. KDJ当前数值
2. 超买超卖判断
3. 操作建议
4. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators"),
                indicators = listOf("KDJ")
            ),
            
            // 4. 布林带突破策略
            StrategyConfig(
                id = "bollinger_breakout",
                name = "布林带突破策略",
                description = "基于布林带判断支撑阻力",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个专业的技术分析专家，擅长使用布林带判断支撑阻力和波动性。

分析要点：
1. 价格触及上轨，可能回调
2. 价格触及下轨，可能反弹
3. 布林带收口，预示即将选择方向
4. 布林带开口，趋势可能延续
5. 中轨作为多空分界线

请使用get_technical_indicators工具获取布林带数据，然后进行分析。
输出格式：
1. 布林带当前状态
2. 价格位置判断
3. 操作建议
4. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators"),
                indicators = listOf("BOLL")
            ),
            
            // 5. 多头趋势判断
            StrategyConfig(
                id = "bull_trend",
                name = "多头趋势判断",
                description = "综合判断多头趋势是否确立",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个专业的趋势跟踪专家，擅长判断多头趋势是否确立。

多头趋势特征：
1. 均线多头排列（MA5 > MA10 > MA20 > MA60）
2. 价格在所有均线上方运行
3. MACD在零轴上方，DIF > DEA
4. 成交量配合上涨，缩量回调
5. 回调不破关键均线支撑

请使用相关工具获取数据，综合判断。
输出格式：
1. 趋势状态判断
2. 确认度评分
3. 操作建议
4. 止损位设置建议
5. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators", "get_trend_analysis", "get_kline_data"),
                indicators = listOf("MA5", "MA10", "MA20", "MA60", "MACD", "Volume")
            ),
            
            // 6. 综合分析策略
            StrategyConfig(
                id = "comprehensive_analysis",
                name = "综合分析",
                description = "技术面、消息面、基本面综合分析",
                category = StrategyCategory.COMPREHENSIVE,
                systemPrompt = """你是一个全面的股票分析师，综合考虑技术面、消息面和趋势进行判断。

分析维度：
1. 技术面：均线、MACD、KDJ、布林带等指标
2. 消息面：最新新闻、风险事件、业绩预期
3. 趋势面：当前趋势状态、强弱度
4. 量能：成交量变化、量比

请使用所有可用工具获取数据，进行全面分析。
输出格式：
1. 技术面总结
2. 消息面总结
3. 趋势判断
4. 综合评分
5. 操作建议
6. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators", "get_trend_analysis", "get_kline_data", "search_news"),
                indicators = listOf("MA5", "MA10", "MA20", "MACD", "KDJ", "RSI", "BOLL")
            ),
            
            // 7. 短线交易策略
            StrategyConfig(
                id = "short_term",
                name = "短线交易策略",
                description = "适合1-5天的短线操作",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个短线交易专家，专注于1-5天的短线操作。

短线要点：
1. 关注5日、10日均线
2. 重视KDJ和RSI超买超卖
3. 关注成交量突变
4. 严格止损止盈
5. 快进快出，不恋战

请使用相关工具获取数据，进行短线分析。
输出格式：
1. 短线技术指标
2. 入场点建议
3. 止损位设置
4. 目标位预期
5. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators", "get_kline_data"),
                indicators = listOf("MA5", "MA10", "KDJ", "RSI")
            ),
            
            // 8. 中线趋势策略
            StrategyConfig(
                id = "mid_term",
                name = "中线趋势策略",
                description = "适合1-3个月的中线持有",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个中线投资专家，专注于1-3个月的趋势跟踪。

中线要点：
1. 关注20日、60日均线
2. 重视MACD和趋势判断
3. 关注基本面变化
4. 分批建仓，逢低加仓
5. 趋势改变才离场

请使用相关工具获取数据，进行中线分析。
输出格式：
1. 中期趋势判断
2. 建仓策略
3. 加仓/减仓点
4. 离场信号
5. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators", "get_trend_analysis", "get_kline_data", "search_news"),
                indicators = listOf("MA20", "MA60", "MACD", "BOLL")
            ),
            
            // 9. 量价分析策略
            StrategyConfig(
                id = "volume_price",
                name = "量价分析策略",
                description = "基于成交量和价格关系分析",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个量价分析专家，擅长通过成交量和价格关系判断市场意图。

量价关系：
1. 价涨量增 - 健康上涨
2. 价涨量缩 - 上涨乏力
3. 价跌量增 - 恐慌抛售
4. 价跌量缩 - 洗盘或无人问津
5. 放量突破 - 有效突破
6. 缩量回调 - 正常调整

请使用get_kline_data工具获取量价数据，然后进行分析。
输出格式：
1. 量价关系分析
2. 主力意图判断
3. 操作建议
4. 风险提示""",
                tools = listOf("get_realtime_quote", "get_kline_data"),
                indicators = listOf("Volume")
            ),
            
            // 10. 资金流向策略
            StrategyConfig(
                id = "capital_flow",
                name = "资金流向策略",
                description = "分析主力资金流向",
                category = StrategyCategory.TECHNICAL,
                systemPrompt = """你是一个资金流向分析专家，擅长判断主力资金进出。

分析要点：
1. 大单净流入/流出
2. 主力资金持续流入
3. 散户资金追涨杀跌
4. 量价配合验证

请使用相关工具获取数据进行分析。
输出格式：
1. 资金流向判断
2. 主力意图分析
3. 操作建议
4. 风险提示""",
                tools = listOf("get_realtime_quote", "get_technical_indicators", "get_kline_data"),
                indicators = listOf("Volume", "MACD")
            ),
            
            // 11. 事件驱动策略
            StrategyConfig(
                id = "event_driven",
                name = "事件驱动策略",
                description = "基于新闻事件和业绩公告",
                category = StrategyCategory.SENTIMENT,
                systemPrompt = """你是一个事件驱动型分析师，擅长根据新闻事件和业绩公告判断投资机会。

分析要点：
1. 业绩超预期/低于预期
2. 重大合同、并购消息
3. 行业政策变化
4. 风险事件警示
5. 市场情绪变化

请使用search_news工具获取相关新闻，结合技术面进行分析。
输出格式：
1. 重要事件梳理
2. 事件影响分析
3. 技术面配合度
4. 操作建议
5. 风险提示""",
                tools = listOf("get_realtime_quote", "search_news", "get_technical_indicators"),
                indicators = listOf("MA5", "MA10", "Volume")
            )
        )
    }
}

/**
 * 消息角色
 */
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/**
 * 聊天消息
 */
data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val timestamp: Date = Date()
)

/**
 * 工具调用
 */
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>
)

/**
 * Agent响应事件
 */
sealed class AgentEvent {
    data class Thinking(val content: String) : AgentEvent()
    data class ToolCall(val toolName: String, val params: Map<String, Any>) : AgentEvent()
    data class ToolResult(val toolName: String, val result: Result<String>) : AgentEvent()
    data class Progress(val step: Int, val total: Int, val message: String) : AgentEvent()
    data class Complete(val content: String, val summary: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}
