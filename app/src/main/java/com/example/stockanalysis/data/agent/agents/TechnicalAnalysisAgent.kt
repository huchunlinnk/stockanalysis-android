package com.example.stockanalysis.data.agent.agents

import android.util.Log
import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.data.llm.LLMService
import com.example.stockanalysis.utils.TechnicalIndicatorCalculator
import org.json.JSONObject

/**
 * 技术面分析 Agent
 * 负责：均线分析、MACD、KDJ、布林带、趋势判断、支撑阻力位
 */
class TechnicalAnalysisAgent(
    llmService: LLMService,
    toolRegistry: AgentToolRegistry
) : BaseAgent(llmService, toolRegistry) {
    
    companion object {
        private const val TAG = "TechnicalAnalysisAgent"
    }
    
    override val agentType: AgentType = AgentType.TECHNICAL
    override val agentName: String = "技术面分析Agent"
    override val description: String = "分析技术指标、趋势状态和关键价格水平"
    override val maxSteps: Int = 6
    
    override fun getAvailableTools(): List<String> = listOf(
        "get_realtime_quote",
        "get_kline_data",
        "get_technical_indicators",
        "get_trend_analysis",
        "get_chip_distribution"
    )
    
    override fun getSystemPrompt(context: AgentContext): String {
        return """你是一个专业的**技术分析Agent**，专注于中国A股、港股和美股的图表分析。

## 分析流程（按顺序执行）
1. 获取实时行情和历史K线数据
2. 计算技术指标（MA、MACD、KDJ、RSI、布林带）
3. 分析趋势状态（上涨/下跌/震荡）
4. 识别关键价格水平（支撑/阻力/止损）
5. 评估量价关系

## 分析维度
- **均线分析**：MA5/MA10/MA20/MA60的排列和交叉
- **MACD分析**：DIF/DEA位置、金叉死叉、柱状图变化
- **KDJ分析**：超买超卖、金叉死叉
- **布林带**：价格位置、带宽变化
- **趋势判断**：多时间周期趋势一致性
- **支撑阻力**：近期高低点、密集成交区

## 输出格式（必须严格遵循JSON格式）
```json
{
  "signal": "strong_buy|buy|hold|sell|strong_sell",
  "confidence": 0.0-1.0,
  "reasoning": "2-3句话的技术分析结论",
  "technical_score": 0-100,
  "key_levels": {
    "support": 支撑位价格,
    "resistance": 阻力位价格,
    "stop_loss": 建议止损位,
    "take_profit": 建议目标位
  },
  "indicators": {
    "ma_alignment": "bullish|neutral|bearish",
    "macd_signal": "bullish|neutral|bearish",
    "kdj_status": "overbought|neutral|oversold",
    "bollinger_position": "upper|middle|lower"
  },
  "trend": {
    "short_term": "up|down|sideways",
    "medium_term": "up|down|sideways",
    "strength": 0-100
  },
  "volume_analysis": "量价配合情况简述"
}
```

## 信号定义
- **strong_buy**：多头排列，指标共振，趋势强劲
- **buy**：趋势向上，指标积极
- **hold**：震荡整理，信号矛盾
- **sell**：趋势向下，指标消极
- **strong_sell**：空头排列，指标共振，趋势疲软

## 置信度评估
- 0.9-1.0：指标高度一致，信号明确
- 0.7-0.9：指标多数一致，信号较明确
- 0.5-0.7：指标部分矛盾，需要谨慎
- 0.3-0.5：指标矛盾较多，信号模糊
- 0.0-0.3：指标高度矛盾，无法判断

请基于提供的数据进行专业分析，给出明确的技术面结论。"""
    }
    
    override fun getUserPrompt(context: AgentContext): String {
        val symbol = context.stockSymbol
        val name = context.stockName
        
        val technicalData = buildString {
            context.getData<String>("quote")?.let {
                appendLine("=== 实时行情 ===")
                appendLine(it)
                appendLine()
            }
            context.getData<String>("indicators")?.let {
                appendLine("=== 技术指标 ===")
                appendLine(it)
                appendLine()
            }
            context.getData<String>("trend")?.let {
                appendLine("=== 趋势分析 ===")
                appendLine(it)
                appendLine()
            }
            context.getData<String>("chip")?.let {
                appendLine("=== 筹码分布 ===")
                appendLine(it)
                appendLine()
            }
        }
        
        return """请对以下股票进行技术面分析：

股票：$name ($symbol)

$technicalData

请基于以上数据进行技术分析，输出符合指定格式的 JSON 结果。
注意：
1. 严格使用上述JSON格式
2. signal 必须是 strong_buy/buy/hold/sell/strong_sell 之一
3. confidence 必须是 0.0-1.0 之间的数值
4. 推理过程必须简洁专业"""
    }
    
    override suspend fun postProcess(context: AgentContext, rawResponse: String): AgentOpinion? {
        return try {
            // 提取 JSON 部分
            val jsonStr = extractJson(rawResponse)
            val json = JSONObject(jsonStr)
            
            val signal = parseSignal(extractString(json, "signal"))
            val confidence = validateConfidence(extractDouble(json, "confidence", 0.5).toFloat())
            val reasoning = extractString(json, "reasoning")
            val technicalScore = extractInt(json, "technical_score", 50)
            
            // 提取关键价格水平
            val keyLevelsJson = extractJSONObject(json, "key_levels")
            val keyLevels = keyLevelsJson?.let {
                AgentOpinion.KeyLevels(
                    support = if (it.has("support")) it.getDouble("support") else null,
                    resistance = if (it.has("resistance")) it.getDouble("resistance") else null,
                    stopLoss = if (it.has("stop_loss")) it.getDouble("stop_loss") else null,
                    takeProfit = if (it.has("take_profit")) it.getDouble("take_profit") else null
                )
            }
            
            // 提取指标数据
            val indicatorsJson = extractJSONObject(json, "indicators")
            val trendJson = extractJSONObject(json, "trend")
            
            val rawData = mutableMapOf<String, Any>(
                "technical_score" to technicalScore,
                "volume_analysis" to extractString(json, "volume_analysis")
            )
            indicatorsJson?.let { rawData["indicators"] = it.toString() }
            trendJson?.let { rawData["trend"] = it.toString() }
            
            AgentOpinion(
                agentType = agentType,
                agentName = agentName,
                signal = signal,
                confidence = confidence,
                reasoning = reasoning,
                keyLevels = keyLevels,
                rawData = rawData
            ).also {
                // 将技术面数据存入上下文
                context.setData("technical_opinion", it)
                context.setData("technical_score", technicalScore)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析技术Agent响应失败", e)
            null
        }
    }
    
    override suspend fun prefetchData(context: AgentContext) {
        // 预取技术指标数据
        try {
            val symbol = context.stockSymbol
            
            // 获取实时行情
            executeTool("get_realtime_quote", mapOf("symbol" to symbol), context)
                .onSuccess { context.setData("quote", it) }
            
            // 获取技术指标
            executeTool("get_technical_indicators", mapOf("symbol" to symbol), context)
                .onSuccess { context.setData("indicators", it) }
            
            // 获取趋势分析
            executeTool("get_trend_analysis", mapOf("symbol" to symbol), context)
                .onSuccess { context.setData("trend", it) }
            
            // 获取筹码分布
            executeTool("get_chip_distribution", mapOf("symbol" to symbol), context)
                .onSuccess { context.setData("chip", it) }
                
        } catch (e: Exception) {
            Log.w(TAG, "预取数据失败: ${e.message}")
        }
    }
    
    /**
     * 从响应中提取 JSON
     */
    private fun extractJson(response: String): String {
        // 尝试找到 JSON 代码块
        val jsonPattern = """```(?:json)?\s*([\s\S]*?)```""".toRegex()
        val match = jsonPattern.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // 尝试找到大括号包裹的内容
        val start = response.indexOf('{')
        val end = response.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1)
        }
        
        return response
    }
}
