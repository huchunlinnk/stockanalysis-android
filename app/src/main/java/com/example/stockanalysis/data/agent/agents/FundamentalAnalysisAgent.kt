package com.example.stockanalysis.data.agent.agents

import android.util.Log
import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.data.llm.LLMService
import org.json.JSONObject

/**
 * 基本面分析 Agent
 * 负责：估值分析、成长性分析、盈利能力分析、财务健康度评估
 */
class FundamentalAnalysisAgent(
    llmService: LLMService,
    toolRegistry: AgentToolRegistry
) : BaseAgent(llmService, toolRegistry) {
    
    companion object {
        private const val TAG = "FundamentalAnalysisAgent"
    }
    
    override val agentType: AgentType = AgentType.FUNDAMENTAL
    override val agentName: String = "基本面分析Agent"
    override val description: String = "评估公司估值、财务状况和成长潜力"
    override val maxSteps: Int = 4
    
    override fun getAvailableTools(): List<String> = listOf(
        "get_realtime_quote",
        "get_stock_info",
        "get_financial_data"
    )
    
    override fun getSystemPrompt(context: AgentContext): String {
        return """你是一个专业的**基本面分析Agent**，专注于公司财务分析和估值评估。

## 分析维度

### 1. 估值分析
- **PE（市盈率）**：与行业平均和历史水平比较
- **PB（市净率）**：评估资产溢价程度
- **PS（市销率）**：评估收入质量
- **PEG**：考虑成长的估值合理性
- 估值结论：低估/合理/高估

### 2. 成长性分析
- **营收增长**：收入同比增长率
- **利润增长**：净利润同比增长率
- **增长质量**：增长是否伴随现金流改善
- **增长可持续性**：增长驱动因素分析

### 3. 盈利能力
- **毛利率**：产品竞争力
- **净利率**：运营效率
- **ROE（净资产收益率）**：股东回报能力
- **ROA（总资产收益率）**：资产使用效率

### 4. 财务健康度
- **资产负债率**：财务杠杆风险
- **流动比率**：短期偿债能力
- **速动比率**：流动性评估
- **现金流状况**：经营现金流健康度

### 5. 行业地位
- **市场份额**：行业排名
- **竞争格局**：护城河分析
- **行业景气度**：所处行业周期位置

## 输出格式（必须严格遵循JSON格式）
```json
{
  "signal": "strong_buy|buy|hold|sell|strong_sell",
  "confidence": 0.0-1.0,
  "reasoning": "2-3句话的基本面分析结论",
  "fundamental_score": 0-100,
  "valuation": {
    "pe_ratio": 当前PE,
    "pb_ratio": 当前PB,
    "valuation_conclusion": "undervalued|fair|overvalued",
    "valuation_reasoning": "估值判断理由"
  },
  "growth": {
    "revenue_growth": "营收增长情况",
    "profit_growth": "利润增长情况",
    "growth_score": 0-100
  },
  "profitability": {
    "gross_margin": "毛利率",
    "net_margin": "净利率",
    "roe": "ROE",
    "profitability_score": 0-100
  },
  "financial_health": {
    "debt_ratio": "资产负债率",
    "current_ratio": "流动比率",
    "cash_flow_status": "现金流状况",
    "health_score": 0-100
  },
  "risk_factors": ["风险因素1", "风险因素2"]
}
```

## 信号规则
- **strong_buy**：估值合理偏低，盈利能力强，成长性好
- **buy**：基本面健康，估值合理
- **hold**：基本面一般，或估值偏高
- **sell**：基本面恶化，或估值过高
- **strong_sell**：基本面严重恶化，或估值极度泡沫

## 数据限制说明
如果财务数据不可用，请基于已有信息（如PE/PB）进行分析，并明确标注数据来源限制。

请基于提供的数据进行专业的基本面分析。"""
    }
    
    override fun getUserPrompt(context: AgentContext): String {
        val symbol = context.stockSymbol
        val name = context.stockName
        
        val fundamentalData = buildString {
            context.getData<String>("quote")?.let {
                appendLine("=== 实时行情（含基础估值指标）===")
                appendLine(it)
                appendLine()
            }
            context.getData<String>("stock_info")?.let {
                appendLine("=== 股票基本信息 ===")
                appendLine(it)
                appendLine()
            }
            context.getData<String>("financial")?.let {
                appendLine("=== 财务数据 ===")
                appendLine(it)
                appendLine()
            }
        }
        
        return """请对以下股票进行基本面分析：

股票：$name ($symbol)

$fundamentalData

请基于以上数据进行基本面分析，输出符合指定格式的 JSON 结果。
注意：
1. 严格使用上述JSON格式
2. 如数据不完整，请在分析中明确说明
3. 信号判断需考虑数据的完整性和时效性
4. 重点关注估值合理性和财务健康度"""
    }
    
    override suspend fun postProcess(context: AgentContext, rawResponse: String): AgentOpinion? {
        return try {
            val jsonStr = extractJson(rawResponse)
            val json = JSONObject(jsonStr)
            
            val signal = parseSignal(extractString(json, "signal"))
            val confidence = validateConfidence(extractDouble(json, "confidence", 0.5).toFloat())
            val reasoning = extractString(json, "reasoning")
            val fundamentalScore = extractInt(json, "fundamental_score", 50)
            
            // 构建详细数据
            val rawData = mutableMapOf<String, Any>(
                "fundamental_score" to fundamentalScore
            )
            
            extractJSONObject(json, "valuation")?.let {
                rawData["valuation"] = it.toString()
            }
            extractJSONObject(json, "growth")?.let {
                rawData["growth"] = it.toString()
            }
            extractJSONObject(json, "profitability")?.let {
                rawData["profitability"] = it.toString()
            }
            extractJSONObject(json, "financial_health")?.let {
                rawData["financial_health"] = it.toString()
            }
            
            // 提取风险因素
            val riskFactors = mutableListOf<String>()
            json.optJSONArray("risk_factors")?.let { array ->
                for (i in 0 until array.length()) {
                    riskFactors.add(array.getString(i))
                }
            }
            rawData["risk_factors"] = riskFactors
            
            AgentOpinion(
                agentType = agentType,
                agentName = agentName,
                signal = signal,
                confidence = confidence,
                reasoning = reasoning,
                rawData = rawData
            ).also {
                context.setData("fundamental_opinion", it)
                context.setData("fundamental_score", fundamentalScore)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析基本面Agent响应失败", e)
            null
        }
    }
    
    override suspend fun prefetchData(context: AgentContext) {
        try {
            val symbol = context.stockSymbol
            
            executeTool("get_stock_info", mapOf("symbol" to symbol), context)
                .onSuccess { context.setData("stock_info", it) }
            
            executeTool("get_financial_data", mapOf("symbol" to symbol), context)
                .onSuccess { context.setData("financial", it) }
                
        } catch (e: Exception) {
            Log.w(TAG, "预取基本面数据失败: ${e.message}")
        }
    }
    
    override fun shouldExecute(context: AgentContext): Boolean {
        // 基本面分析是可选项，如果没有数据也可以跳过
        return true
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
