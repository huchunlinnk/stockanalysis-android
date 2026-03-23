package com.example.stockanalysis.data.agent.tools

import com.example.stockanalysis.data.agent.AgentTool
import com.example.stockanalysis.data.agent.ParameterProperty
import com.example.stockanalysis.data.agent.ToolParameters
import com.example.stockanalysis.data.datasource.DataSourceManager
import com.example.stockanalysis.data.news.NewsSearchManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 获取实时行情工具
 */
@Singleton
class GetRealtimeQuoteTool @Inject constructor(
    private val dataSourceManager: DataSourceManager
) : AgentTool {
    
    override val name = "get_realtime_quote"
    override val description = "获取股票实时行情数据，包括当前价格、涨跌幅、成交量等"
    
    override val parameters = ToolParameters(
        properties = mapOf(
            "symbol" to ParameterProperty(
                type = "string",
                description = "股票代码，如 600519 或 000001"
            )
        ),
        required = listOf("symbol")
    )
    
    override suspend fun execute(params: Map<String, Any>): Result<String> {
        val symbol = params["symbol"] as? String
            ?: return Result.failure(IllegalArgumentException("Missing required parameter: symbol"))
        
        return dataSourceManager.fetchQuote(symbol).map { quote ->
            """
            【实时行情】$symbol
            股票名称: ${quote.name}
            当前价格: ¥${String.format("%.2f", quote.price)}
            涨跌幅: ${String.format("%.2f", quote.changePercent)}%
            涨跌额: ${String.format("%.2f", quote.change)}
            开盘价: ¥${String.format("%.2f", quote.open)}
            最高价: ¥${String.format("%.2f", quote.high)}
            最低价: ¥${String.format("%.2f", quote.low)}
            昨收: ¥${String.format("%.2f", quote.preClose)}
            成交量: ${formatVolume(quote.volume)}
            成交额: ${formatAmount(quote.amount)}
            换手率: ${String.format("%.2f", quote.turnoverRate ?: 0.0)}%
            市盈率: ${String.format("%.2f", quote.peRatio ?: 0.0)}
            市净率: ${String.format("%.2f", quote.pbRatio ?: 0.0)}
            """.trimIndent()
        }
    }
    
    private fun formatVolume(volume: Long): String {
        return when {
            volume >= 100000000 -> String.format("%.2f亿手", volume / 100000000.0)
            volume >= 10000 -> String.format("%.2f万手", volume / 10000.0)
            else -> "$volume 手"
        }
    }
    
    private fun formatAmount(amount: Double): String {
        return when {
            amount >= 100000000 -> String.format("%.2f亿", amount / 100000000.0)
            amount >= 10000 -> String.format("%.2f万", amount / 10000.0)
            else -> String.format("%.2f", amount)
        }
    }
}

/**
 * 获取K线数据工具
 */
@Singleton
class GetKLineDataTool @Inject constructor(
    private val dataSourceManager: DataSourceManager
) : AgentTool {
    
    override val name = "get_kline_data"
    override val description = "获取股票K线数据，用于技术分析和趋势判断"
    
    override val parameters = ToolParameters(
        properties = mapOf(
            "symbol" to ParameterProperty(
                type = "string",
                description = "股票代码，如 600519"
            ),
            "days" to ParameterProperty(
                type = "integer",
                description = "获取天数，默认90天"
            )
        ),
        required = listOf("symbol")
    )
    
    override suspend fun execute(params: Map<String, Any>): Result<String> {
        val symbol = params["symbol"] as? String
            ?: return Result.failure(IllegalArgumentException("Missing required parameter: symbol"))
        val days = (params["days"] as? Number)?.toInt() ?: 90
        
        return dataSourceManager.fetchKLineData(symbol, days).map { data ->
            val recentData = data.takeLast(10)
            val first = data.firstOrNull()
            val last = data.lastOrNull()
            
            val priceChange = if (first != null && last != null) {
                ((last.close - first.close) / first.close * 100)
            } else 0.0
            
            buildString {
                appendLine("【K线数据】$symbol (最近${days}天)")
                appendLine()
                appendLine("区间涨跌: ${String.format("%.2f", priceChange)}%")
                appendLine()
                appendLine("最近10个交易日:")
                recentData.forEach { kline ->
                    appendLine("${formatDate(kline.timestamp)} 开:${kline.open} 高:${kline.high} 低:${kline.low} 收:${kline.close} 量:${kline.volume}")
                }
            }
        }
    }
    
    private fun formatDate(timestamp: java.util.Date): String {
        return java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA).format(timestamp)
    }
}

/**
 * 获取技术指标工具
 */
@Singleton
class GetTechnicalIndicatorsTool @Inject constructor(
    private val dataSourceManager: DataSourceManager
) : AgentTool {
    
    override val name = "get_technical_indicators"
    override val description = "获取股票技术指标，包括MACD、KDJ、RSI、均线等"
    
    override val parameters = ToolParameters(
        properties = mapOf(
            "symbol" to ParameterProperty(
                type = "string",
                description = "股票代码"
            )
        ),
        required = listOf("symbol")
    )
    
    override suspend fun execute(params: Map<String, Any>): Result<String> {
        val symbol = params["symbol"] as? String
            ?: return Result.failure(IllegalArgumentException("Missing required parameter: symbol"))
        
        return dataSourceManager.fetchTechnicalIndicators(symbol).map { indicators ->
            buildString {
                appendLine("【技术指标】$symbol")
                appendLine()
                
                // 均线
                indicators.movingAverages?.let { ma ->
                    appendLine("【均线系统】")
                    ma.ma5?.let { appendLine("  MA5:  ${String.format("%.2f", it)}") }
                    ma.ma10?.let { appendLine("  MA10: ${String.format("%.2f", it)}") }
                    ma.ma20?.let { appendLine("  MA20: ${String.format("%.2f", it)}") }
                    ma.ma60?.let { appendLine("  MA60: ${String.format("%.2f", it)}") }
                    
                    // 均线排列判断
                    if (ma.ma5 != null && ma.ma10 != null && ma.ma20 != null) {
                        when {
                            ma.ma5 > ma.ma10 && ma.ma10 > ma.ma20 -> {
                                appendLine("  均线排列: 多头排列（强势）")
                            }
                            ma.ma5 < ma.ma10 && ma.ma10 < ma.ma20 -> {
                                appendLine("  均线排列: 空头排列（弱势）")
                            }
                            else -> {
                                appendLine("  均线排列: 震荡整理")
                            }
                        }
                    }
                    appendLine()
                }
                
                // MACD
                indicators.macd?.let { macd ->
                    appendLine("【MACD指标】")
                    appendLine("  DIF: ${String.format("%.3f", macd.dif)}")
                    appendLine("  DEA: ${String.format("%.3f", macd.dea)}")
                    appendLine("  MACD柱状: ${String.format("%.3f", macd.macd)}")
                    
                    when {
                        macd.dif > macd.dea && macd.macd > 0 -> appendLine("  信号: DIF上穿DEA，多头信号")
                        macd.dif < macd.dea && macd.macd < 0 -> appendLine("  信号: DIF下穿DEA，空头信号")
                        else -> appendLine("  信号: 趋势不明")
                    }
                    appendLine()
                }
                
                // KDJ
                indicators.kdj?.let { kdj ->
                    appendLine("【KDJ指标】")
                    appendLine("  K值: ${String.format("%.2f", kdj.k)}")
                    appendLine("  D值: ${String.format("%.2f", kdj.d)}")
                    appendLine("  J值: ${String.format("%.2f", kdj.j)}")
                    
                    when {
                        kdj.j > 100 -> appendLine("  信号: J值超买区域")
                        kdj.j < 0 -> appendLine("  信号: J值超卖区域")
                        kdj.k > kdj.d -> appendLine("  信号: K上穿D，买入信号")
                        kdj.k < kdj.d -> appendLine("  信号: K下穿D，卖出信号")
                    }
                    appendLine()
                }
                
                // RSI
                indicators.rsi6?.let { rsi ->
                    appendLine("【RSI指标】")
                    appendLine("  RSI(6):  ${String.format("%.2f", rsi)}")
                    indicators.rsi12?.let { appendLine("  RSI(12): ${String.format("%.2f", it)}") }
                    indicators.rsi24?.let { appendLine("  RSI(24): ${String.format("%.2f", it)}") }
                    
                    when {
                        rsi > 80 -> appendLine("  信号: 超买")
                        rsi < 20 -> appendLine("  信号: 超卖")
                        rsi > 50 -> appendLine("  信号: 偏多")
                        else -> appendLine("  信号: 偏空")
                    }
                    appendLine()
                }
                
                // BOLL
                indicators.boll?.let { boll ->
                    appendLine("【布林带】")
                    appendLine("  上轨: ${String.format("%.2f", boll.upper)}")
                    appendLine("  中轨: ${String.format("%.2f", boll.middle)}")
                    appendLine("  下轨: ${String.format("%.2f", boll.lower)}")
                }
            }
        }
    }
}

/**
 * 获取趋势分析工具
 */
@Singleton
class GetTrendAnalysisTool @Inject constructor(
    private val dataSourceManager: DataSourceManager
) : AgentTool {
    
    override val name = "get_trend_analysis"
    override val description = "获取股票趋势分析报告"
    
    override val parameters = ToolParameters(
        properties = mapOf(
            "symbol" to ParameterProperty(
                type = "string",
                description = "股票代码"
            )
        ),
        required = listOf("symbol")
    )
    
    override suspend fun execute(params: Map<String, Any>): Result<String> {
        val symbol = params["symbol"] as? String
            ?: return Result.failure(IllegalArgumentException("Missing required parameter: symbol"))
        
        return dataSourceManager.fetchTrendAnalysis(symbol).map { trend ->
            """
            【趋势分析】$symbol
            
            趋势状态: ${trend.trendStatus}
            趋势强度: ${trend.trendStrength}/100
            
            买卖信号: ${trend.buySignal}
            信号评分: ${trend.signalScore}/100
            
            偏离度:
            - 偏离MA5: ${String.format("%.2f", trend.biasMa5)}%
            - 偏离MA10: ${String.format("%.2f", trend.biasMa10)}%
            
            量能状态: ${trend.volumeStatus}
            """.trimIndent()
        }
    }
}

/**
 * 搜索新闻工具
 */
@Singleton
class SearchNewsTool @Inject constructor(
    private val newsSearchManager: NewsSearchManager
) : AgentTool {
    
    override val name = "search_news"
    override val description = "搜索股票相关新闻和资讯"
    
    override val parameters = ToolParameters(
        properties = mapOf(
            "symbol" to ParameterProperty(
                type = "string",
                description = "股票代码"
            ),
            "name" to ParameterProperty(
                type = "string",
                description = "股票名称"
            )
        ),
        required = listOf("symbol", "name")
    )
    
    override suspend fun execute(params: Map<String, Any>): Result<String> {
        val symbol = params["symbol"] as? String
            ?: return Result.failure(IllegalArgumentException("Missing required parameter: symbol"))
        val name = params["name"] as? String
            ?: return Result.failure(IllegalArgumentException("Missing required parameter: name"))
        
        val result = newsSearchManager.searchMultiDimension(symbol, name)
        
        return Result.success(buildString {
            appendLine("【新闻情报】$name ($symbol)")
            appendLine()
            
            if (result.latestNews.isNotEmpty()) {
                appendLine("【最新消息】")
                result.latestNews.take(5).forEach { news ->
                    appendLine("- ${news.title}")
                    appendLine("  来源: ${news.source} | ${formatDate(news.publishedDate)}")
                    news.summary?.let { appendLine("  摘要: $it") }
                    appendLine()
                }
            }
            
            if (result.riskNews.isNotEmpty()) {
                appendLine("【风险警示】")
                result.riskNews.take(3).forEach { news ->
                    appendLine("⚠️ ${news.title}")
                }
                appendLine()
            }
            
            if (result.performanceNews.isNotEmpty()) {
                appendLine("【业绩资讯】")
                result.performanceNews.take(3).forEach { news ->
                    appendLine("📊 ${news.title}")
                }
            }
            
            if (result.latestNews.isEmpty() && result.riskNews.isEmpty() && result.performanceNews.isEmpty()) {
                appendLine("暂无相关新闻")
            }
        })
    }
    
    private fun formatDate(date: java.util.Date): String {
        return java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA).format(date)
    }
}
