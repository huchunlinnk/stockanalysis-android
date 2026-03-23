package com.example.stockanalysis.data.market

import com.example.stockanalysis.data.api.LLMApiService
import com.example.stockanalysis.data.datasource.DataSourceManager
import com.example.stockanalysis.data.local.StockDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 市场复盘服务实现
 * 完全在Android端运行
 */
@Singleton
class MarketReviewServiceImpl @Inject constructor(
    private val dataSourceManager: DataSourceManager,
    private val llmService: LLMApiService,
    private val stockDao: StockDao
) : MarketReviewService {
    
    companion object {
        const val TAG = "MarketReviewService"
    }
    
    override suspend fun generateDailyReview(date: Date): Result<MarketReview> = withContext(Dispatchers.IO) {
        try {
            // 1. 获取市场数据
            val marketData = fetchMarketData()
            
            // 2. 生成AI复盘报告
            val aiReport = generateAIReport(marketData)
            
            // 3. 构建复盘对象
            val review = MarketReview(
                reviewDate = date,
                marketType = "CN",
                
                // 指数数据
                shIndexValue = marketData.indices.find { it.symbol == "000001" }?.value,
                shIndexChange = marketData.indices.find { it.symbol == "000001" }?.changePercent,
                szIndexValue = marketData.indices.find { it.symbol == "399001" }?.value,
                szIndexChange = marketData.indices.find { it.symbol == "399001" }?.changePercent,
                cyIndexValue = marketData.indices.find { it.symbol == "399006" }?.value,
                cyIndexChange = marketData.indices.find { it.symbol == "399006" }?.changePercent,
                
                // 市场统计
                upCount = marketData.marketStats.upCount,
                downCount = marketData.marketStats.downCount,
                flatCount = marketData.marketStats.flatCount,
                limitUpCount = marketData.marketStats.limitUpCount,
                limitDownCount = marketData.marketStats.limitDownCount,
                totalVolume = marketData.marketStats.totalVolume,
                totalAmount = marketData.marketStats.totalAmount,
                
                // 板块数据
                topSectors = marketData.topSectors.joinToString(",") { it.name },
                bottomSectors = marketData.bottomSectors.joinToString(",") { it.name },
                
                // AI报告
                summary = aiReport.summary,
                technicalAnalysis = aiReport.technicalAnalysis,
                sentimentAnalysis = aiReport.sentimentAnalysis,
                strategy = aiReport.strategy,
                action = aiReport.action.name,
                riskLevel = aiReport.riskLevel
            )
            
            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getReviewHistory(days: Int): List<MarketReview> {
        // 从数据库获取历史复盘
        // 简化实现
        return emptyList()
    }
    
    override suspend fun getLatestReview(): MarketReview? {
        // 从数据库获取最新复盘
        // 简化实现
        return null
    }
    
    /**
     * 获取市场数据
     */
    private suspend fun fetchMarketData(): MarketOverviewData {
        // 获取主要指数
        val indexSymbols = listOf("000001", "399001", "399006") // 上证、深证、创业板
        val indices = mutableListOf<IndexData>()
        
        indexSymbols.forEach { symbol ->
            val result = dataSourceManager.fetchQuote(symbol)
            result.onSuccess { quote ->
                indices.add(IndexData(
                    name = quote.name,
                    symbol = symbol,
                    value = quote.price,
                    change = quote.change,
                    changePercent = quote.changePercent,
                    volume = quote.volume,
                    turnover = quote.amount
                ))
            }
        }
        
        // 获取市场概览
        val overview = dataSourceManager.fetchMarketOverview()
        
        // 构建统计数据
        val marketStats = if (overview.isSuccess) {
            val data = overview.getOrNull()
            MarketStatsData(
                upCount = data?.stats?.risingCount ?: 0,
                downCount = data?.stats?.fallingCount ?: 0,
                flatCount = data?.stats?.flatCount ?: 0,
                limitUpCount = data?.stats?.limitUpCount ?: 0,
                limitDownCount = data?.stats?.limitDownCount ?: 0,
                totalVolume = 0L,
                totalAmount = 0.0
            )
        } else {
            MarketStatsData(0, 0, 0, 0, 0, 0L, 0.0)
        }
        
        // 计算市场情绪
        val sentiment = calculateSentiment(indices, marketStats)
        
        // 构建板块数据
        val topSectors = overview.getOrNull()?.sectorPerformance?.topRisers?.map { sector ->
            SectorData(
                name = sector.name,
                changePercent = sector.changePercent,
                leadingStock = sector.leadingStocks.firstOrNull()
            )
        } ?: emptyList()
        
        val bottomSectors = overview.getOrNull()?.sectorPerformance?.topFallers?.map { sector ->
            SectorData(
                name = sector.name,
                changePercent = sector.changePercent,
                leadingStock = sector.leadingStocks.firstOrNull()
            )
        } ?: emptyList()
        
        return MarketOverviewData(
            indices = indices,
            marketStats = marketStats,
            topSectors = topSectors,
            bottomSectors = bottomSectors,
            capitalFlow = CapitalFlowData(0.0, 0.0, 0.0, 0.0, 0.0),
            sentiment = sentiment
        )
    }
    
    /**
     * 计算市场情绪
     */
    private fun calculateSentiment(indices: List<IndexData>, stats: MarketStatsData): MarketSentiment {
        // 基于涨跌比和指数表现计算情绪
        val total = stats.upCount + stats.downCount
        val upRatio = if (total > 0) stats.upCount.toDouble() / total else 0.5
        
        // 指数平均涨跌幅
        val avgIndexChange = indices.map { it.changePercent }.average()
        
        // 计算恐惧贪婪指数 (0-100)
        val fearGreedIndex = when {
            upRatio > 0.7 && avgIndexChange > 1.5 -> 80  // 极度贪婪
            upRatio > 0.6 && avgIndexChange > 0.5 -> 60  // 贪婪
            upRatio in 0.4..0.6 -> 50                      // 中性
            upRatio < 0.3 && avgIndexChange < -1.5 -> 20  // 极度恐惧
            upRatio < 0.4 && avgIndexChange < -0.5 -> 40  // 恐惧
            else -> 50
        }.toInt()
        
        val (sentiment, description) = when (fearGreedIndex) {
            in 0..20 -> SentimentType.EXTREME_FEAR to "市场恐慌，大量抛售"
            in 21..40 -> SentimentType.FEAR to "市场情绪低迷，谨慎观望"
            in 41..60 -> SentimentType.NEUTRAL to "市场情绪中性，震荡为主"
            in 61..80 -> SentimentType.GREED to "市场情绪乐观，资金活跃"
            else -> SentimentType.EXTREME_GREED to "市场过热，注意风险"
        }
        
        return MarketSentiment(fearGreedIndex, sentiment, description)
    }
    
    /**
     * 生成AI复盘报告
     */
    private suspend fun generateAIReport(marketData: MarketOverviewData): AIReport {
        val prompt = buildPrompt(marketData)
        
        return try {
            val request = com.example.stockanalysis.data.api.ChatCompletionRequest(
                model = "gpt-4",
                messages = listOf(
                    com.example.stockanalysis.data.api.Message(
                        role = "system",
                        content = "你是一个专业的市场分析师，擅长每日市场复盘和策略制定。"
                    ),
                    com.example.stockanalysis.data.api.Message(
                        role = "user",
                        content = prompt
                    )
                ),
                temperature = 0.7,
                maxTokens = 1500
            )
            
            val response = llmService.chatCompletion(request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                parseAIResponse(content ?: "")
            } else {
                generateFallbackReport(marketData)
            }
        } catch (e: Exception) {
            generateFallbackReport(marketData)
        }
    }
    
    /**
     * 构建提示词
     */
    private fun buildPrompt(data: MarketOverviewData): String {
        return """
            请根据以下市场数据进行今日复盘分析：
            
            【指数表现】
            ${data.indices.joinToString("\n") { "- ${it.name}: ${it.value} (${it.changePercent}%)" }}
            
            【市场统计】
            - 上涨: ${data.marketStats.upCount}家
            - 下跌: ${data.marketStats.downCount}家
            - 涨停: ${data.marketStats.limitUpCount}家
            - 跌停: ${data.marketStats.limitDownCount}家
            
            【领涨板块】
            ${data.topSectors.take(5).joinToString("\n") { "- ${it.name}: +${it.changePercent}%" }}
            
            【领跌板块】
            ${data.bottomSectors.take(5).joinToString("\n") { "- ${it.name}: ${it.changePercent}%" }}
            
            【市场情绪】
            恐惧贪婪指数: ${data.sentiment.fearGreedIndex}/100 - ${data.sentiment.description}
            
            请输出以下格式：
            1. 一句话总结今日市场
            2. 技术面分析
            3. 情绪面分析
            4. 明日操作策略 (进攻/均衡/防守)
            5. 风险等级 (1-5)
        """.trimIndent()
    }
    
    /**
     * 解析AI响应
     */
    private fun parseAIResponse(content: String): AIReport {
        // 简化解析，实际应该用更鲁棒的解析方式
        val action = when {
            content.contains("进攻") -> ActionType.OFFENSIVE
            content.contains("防守") -> ActionType.DEFENSIVE
            else -> ActionType.BALANCED
        }
        
        val riskLevel = content.lines()
            .firstOrNull { it.contains("风险等级") }
            ?.filter { it.isDigit() }
            ?.toIntOrNull() ?: 3
        
        return AIReport(
            summary = content.lines().firstOrNull() ?: "今日市场震荡整理",
            technicalAnalysis = "技术面分析见详细报告",
            sentimentAnalysis = "情绪面分析见详细报告",
            strategy = content,
            action = action,
            riskLevel = riskLevel
        )
    }
    
    /**
     * 生成备用报告（当AI调用失败时）
     */
    private fun generateFallbackReport(data: MarketOverviewData): AIReport {
        val shChange = data.indices.find { it.symbol == "000001" }?.changePercent ?: 0.0
        
        val (action, risk) = when {
            shChange > 1.0 -> ActionType.OFFENSIVE to 2
            shChange < -1.0 -> ActionType.DEFENSIVE to 4
            else -> ActionType.BALANCED to 3
        }
        
        return AIReport(
            summary = "今日市场${if (shChange > 0) "上涨" else "下跌"}，${data.sentiment.description}",
            technicalAnalysis = "指数${if (shChange > 0) "突破" else "跌破"}关键位，${data.sentiment.sentiment}",
            sentimentAnalysis = data.sentiment.description,
            strategy = "建议${if (action == ActionType.OFFENSIVE) "积极布局" else if (action == ActionType.DEFENSIVE) "控制仓位" else "均衡配置"}",
            action = action,
            riskLevel = risk
        )
    }
}

/**
 * AI报告
 */
private data class AIReport(
    val summary: String,
    val technicalAnalysis: String,
    val sentimentAnalysis: String,
    val strategy: String,
    val action: ActionType,
    val riskLevel: Int
)
