package com.example.stockanalysis.data.agent.agents

import android.util.Log
import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.data.llm.LLMService
import org.json.JSONObject
import java.util.Date

/**
 * 新闻情报分析 Agent
 * 负责：新闻搜索、舆情分析、市场情绪、催化剂识别
 */
class NewsAnalysisAgent(
    llmService: LLMService,
    toolRegistry: AgentToolRegistry
) : BaseAgent(llmService, toolRegistry) {
    
    companion object {
        private const val TAG = "NewsAnalysisAgent"
    }
    
    override val agentType: AgentType = AgentType.NEWS
    override val agentName: String = "舆情分析Agent"
    override val description: String = "收集新闻情报，分析市场情绪和催化剂"
    override val maxSteps: Int = 4
    
    override fun getAvailableTools(): List<String> = listOf(
        "search_news",
        "search_stock_news",
        "get_realtime_quote"
    )
    
    override fun getSystemPrompt(context: AgentContext): String {
        return """你是一个专业的**舆情分析Agent**，专注于收集和分析股票市场相关的新闻、公告和市场情绪。

## 分析维度

### 1. 新闻收集
- **公司公告**：业绩公告、重大合同、股权变动
- **行业动态**：行业政策、竞争格局变化
- **市场新闻**：机构评级、研报观点
- **风险事件**：监管处罚、诉讼纠纷、业绩预警

### 2. 舆情分析
- **整体情绪**：积极/中性/消极
- **情绪强度**：情绪的程度评估
- **情绪一致性**：不同来源的情绪是否一致
- **情绪变化趋势**：相比前期的情绪变化

### 3. 催化剂识别
- **业绩催化**：超预期业绩、业绩拐点
- **事件催化**：重大合同、并购重组
- **政策催化**：行业政策利好
- **市场催化**：机构增持、研报推荐

### 4. 风险预警
- **减持风险**：大股东/高管减持
- **业绩风险**：业绩不及预期、预亏
- **监管风险**：监管问询、处罚
- **行业风险**：行业政策变化、景气度下降

## 输出格式（必须严格遵循JSON格式）
```json
{
  "signal": "strong_buy|buy|hold|sell|strong_sell",
  "confidence": 0.0-1.0,
  "reasoning": "2-3句话的舆情分析结论",
  "sentiment_score": -1.0到1.0,
  "sentiment_label": "very_positive|positive|neutral|negative|very_negative",
  "news_summary": {
    "total_count": 新闻总数,
    "positive_count": 积极新闻数,
    "negative_count": 消极新闻数,
    "latest_news": "最新重要新闻摘要"
  },
  "catalysts": [
    {
      "type": "earnings|event|policy|market",
      "description": "催化剂描述",
      "impact": "high|medium|low",
      "timeframe": "immediate|short|medium"
    }
  ],
  "risk_alerts": [
    {
      "type": "insider|earnings|regulatory|industry",
      "description": "风险描述",
      "severity": "high|medium|low"
    }
  ],
  "key_news": [
    {
      "title": "新闻标题",
      "source": "来源",
      "sentiment": "positive|negative|neutral",
      "relevance": 0.0-1.0
    }
  ]
}
```

## 信号规则
- **strong_buy**：强烈的积极催化剂，情绪非常乐观
- **buy**：有积极催化剂，情绪积极
- **hold**：无明显催化剂，情绪中性或矛盾
- **sell**：有负面因素，情绪消极
- **strong_sell**：严重的负面事件，情绪极度悲观

## 新闻相关性判断
- 评估新闻与目标股票的相关性（0-1）
- 优先分析与主营业务直接相关的信息
- 区分事实性新闻和观点性分析

## 时效性考量
- 优先分析最近7天内的新闻
- 重大事件（如业绩公告）权重更高
- 过期新闻（超过30天）应降低权重

请基于搜索到的新闻进行专业的舆情分析。"""
    }
    
    override fun getUserPrompt(context: AgentContext): String {
        val symbol = context.stockSymbol
        val name = context.stockName
        
        val newsData = buildString {
            context.getData<String>("news")?.let {
                appendLine("=== 相关新闻 ===")
                appendLine(it)
                appendLine()
            }
        }
        
        return """请对以下股票进行舆情分析：

股票：$name ($symbol)

$newsData

请基于以上新闻数据进行舆情分析，输出符合指定格式的 JSON 结果。
注意：
1. 严格使用上述JSON格式
2. 区分事实和观点
3. 评估新闻的时效性和相关性
4. 识别真正的催化剂，而非噪音
5. 对风险因素给予足够重视"""
    }
    
    override suspend fun postProcess(context: AgentContext, rawResponse: String): AgentOpinion? {
        return try {
            val jsonStr = extractJson(rawResponse)
            val json = JSONObject(jsonStr)
            
            val signal = parseSignal(extractString(json, "signal"))
            val confidence = validateConfidence(extractDouble(json, "confidence", 0.5).toFloat())
            val reasoning = extractString(json, "reasoning")
            val sentimentScore = extractDouble(json, "sentiment_score", 0.0)
            val sentimentLabel = extractString(json, "sentiment_label", "neutral")
            
            // 处理风险告警
            json.optJSONArray("risk_alerts")?.let { array ->
                for (i in 0 until array.length()) {
                    val alert = array.getJSONObject(i)
                    val severity = when (alert.optString("severity", "medium")) {
                        "high" -> AgentMessage.RiskAlert.RiskSeverity.HIGH
                        "low" -> AgentMessage.RiskAlert.RiskSeverity.LOW
                        else -> AgentMessage.RiskAlert.RiskSeverity.MEDIUM
                    }
                    context.addRiskAlert(
                        AgentMessage.RiskAlert(
                            fromAgent = agentType,
                            toAgent = null,
                            severity = severity,
                            category = alert.optString("type", "news"),
                            description = alert.optString("description", "")
                        )
                    )
                }
            }
            
            // 构建详细数据
            val rawData = mutableMapOf<String, Any>(
                "sentiment_score" to sentimentScore,
                "sentiment_label" to sentimentLabel
            )
            
            extractJSONObject(json, "news_summary")?.let {
                rawData["news_summary"] = it.toString()
            }
            
            // 处理催化剂
            val catalysts = mutableListOf<Map<String, String>>()
            json.optJSONArray("catalysts")?.let { array ->
                for (i in 0 until array.length()) {
                    val catalyst = array.getJSONObject(i)
                    catalysts.add(mapOf(
                        "type" to catalyst.optString("type", ""),
                        "description" to catalyst.optString("description", ""),
                        "impact" to catalyst.optString("impact", ""),
                        "timeframe" to catalyst.optString("timeframe", "")
                    ))
                }
            }
            rawData["catalysts"] = catalysts
            
            // 处理关键新闻
            val keyNews = mutableListOf<Map<String, Any>>()
            json.optJSONArray("key_news")?.let { array ->
                for (i in 0 until array.length()) {
                    val news = array.getJSONObject(i)
                    keyNews.add(mapOf(
                        "title" to news.optString("title", ""),
                        "source" to news.optString("source", ""),
                        "sentiment" to news.optString("sentiment", "neutral"),
                        "relevance" to news.optDouble("relevance", 0.5)
                    ))
                }
            }
            rawData["key_news"] = keyNews
            
            AgentOpinion(
                agentType = agentType,
                agentName = agentName,
                signal = signal,
                confidence = confidence,
                reasoning = reasoning,
                rawData = rawData
            ).also {
                context.setData("news_opinion", it)
                context.setData("sentiment_score", sentimentScore)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析舆情Agent响应失败", e)
            null
        }
    }
    
    override suspend fun prefetchData(context: AgentContext) {
        try {
            val symbol = context.stockSymbol
            val name = context.stockName
            
            // 搜索新闻
            executeTool("search_news", mapOf("symbol" to symbol, "name" to name), context)
                .onSuccess { context.setData("news", it) }
                
        } catch (e: Exception) {
            Log.w(TAG, "预取新闻数据失败: ${e.message}")
        }
    }
    
    private fun extractJson(response: String): String {
        val jsonPattern = """```(?:json)?\s*([\s\S]*?)```""".toRegex()
        val match = jsonPattern.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        val start = response.indexOf('{')
        val end = response.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1)
        }
        
        return response
    }
}
