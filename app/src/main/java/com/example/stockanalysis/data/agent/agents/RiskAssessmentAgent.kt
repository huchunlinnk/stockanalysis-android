package com.example.stockanalysis.data.agent.agents

import android.util.Log
import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.data.llm.LLMService
import org.json.JSONObject

/**
 * 风险评估 Agent
 * 负责：风险识别、风险等级评估、风险因素监控
 */
class RiskAssessmentAgent(
    llmService: LLMService,
    toolRegistry: AgentToolRegistry
) : BaseAgent(llmService, toolRegistry) {
    
    companion object {
        private const val TAG = "RiskAssessmentAgent"
    }
    
    override val agentType: AgentType = AgentType.RISK
    override val agentName: String = "风险评估Agent"
    override val description: String = "识别投资风险，评估风险等级和影响"
    override val maxSteps: Int = 4
    
    override fun getAvailableTools(): List<String> = listOf(
        "get_realtime_quote",
        "get_stock_info",
        "search_news"
    )
    
    override fun getSystemPrompt(context: AgentContext): String {
        return """你是一个专业的**风险评估Agent**，专注于识别和评估股票投资的各类风险。

## 风险检查清单

### 1. 内部人/大股东风险
- [ ] 大股东减持（特别是5%以上股东）
- [ ] 高管减持
- [ ] 股权质押比例过高（>50%）
- [ ] 解禁风险（近期有大额解禁）

### 2. 业绩风险
- [ ] 业绩预亏或预减
- [ ] 业绩变脸（前期预告与实际差异大）
- [ ] 营收下滑
- [ ] 利润率持续下降

### 3. 监管风险
- [ ] 监管问询函
- [ ] 立案调查
- [ ] 行政处罚
- [ ] 信息披露违规

### 4. 估值风险
- [ ] PE > 100 或 PE < 0（亏损）
- [ ] PB > 10
- [ ] 估值显著高于行业平均
- [ ] 估值与成长性不匹配

### 5. 流动性风险
- [ ] 日均成交额过低（<5000万）
- [ ] 换手率异常
- [ ] 大股东持股比例过高（>70%）

### 6. 行业风险
- [ ] 行业政策不利变化
- [ ] 行业景气度下降
- [ ] 技术替代风险
- [ ] 原材料价格大幅波动

### 7. 技术风险
- [ ] 股价大幅偏离均线（乖离率过大）
- [ ] 成交量异常放大
- [ ] 关键支撑位跌破
- [ ] 趋势明显走弱

### 8. 市场系统性风险
- [ ] 大盘处于下跌趋势
- [ ] 市场恐慌情绪
- [ ] 流动性收紧预期

## 风险等级定义

### High（高风险）
- 存在严重违法违规行为
- 重大业绩造假嫌疑
- 面临退市风险
- 大股东恶意减持
- 重大诉讼/仲裁

### Medium（中风险）
- 业绩明显下滑
- 估值偏高
- 行业政策不利
- 解禁压力
- 监管关注

### Low（低风险）
- 正常市场波动
- 估值合理偏高
- 一般性负面新闻
- 技术性调整

## 输出格式（必须严格遵循JSON格式）
```json
{
  "signal": "strong_buy|buy|hold|sell|strong_sell",
  "confidence": 0.0-1.0,
  "reasoning": "2-3句话的风险评估结论",
  "risk_level": "high|medium|low|none",
  "risk_score": 0-100,
  "overall_assessment": "整体风险描述",
  "flags": [
    {
      "category": "insider|earnings|regulatory|valuation|liquidity|industry|technical|market",
      "severity": "high|medium|low",
      "description": "风险描述",
      "recommendation": "应对建议"
    }
  ],
  "risk_breakdown": {
    "insider_risk": 0-100,
    "earnings_risk": 0-100,
    "regulatory_risk": 0-100,
    "valuation_risk": 0-100,
    "liquidity_risk": 0-100,
    "industry_risk": 0-100,
    "technical_risk": 0-100
  },
  "veto_buy": true|false,
  "signal_adjustment": "none|downgrade_one|downgrade_two|veto"
}
```

## 信号映射（风险信号与交易信号相反）
- **risk_level = none** → signal = strong_buy
- **risk_level = low** → signal = buy
- **risk_level = medium** → signal = hold
- **risk_level = high** → signal = sell 或 strong_sell

## 否决规则
当存在以下情况时，veto_buy = true：
- 监管立案调查
- 业绩预亏
- 大股东大额减持进行中
- 估值严重泡沫（PE>200）
- 退市风险警示

请基于所有可用信息进行全面的风险评估。务必客观、谨慎，宁可过度预警也不可遗漏重大风险。"""
    }
    
    override fun getUserPrompt(context: AgentContext): String {
        val symbol = context.stockSymbol
        val name = context.stockName
        
        val riskData = buildString {
            // 包含其他 Agent 的意见作为参考
            context.getOpinion(AgentType.TECHNICAL)?.let {
                appendLine("=== 技术面分析意见 ===")
                appendLine("信号: ${it.signal.value}")
                appendLine("置信度: ${it.confidence}")
                appendLine("理由: ${it.reasoning}")
                appendLine()
            }
            context.getOpinion(AgentType.FUNDAMENTAL)?.let {
                appendLine("=== 基本面分析意见 ===")
                appendLine("信号: ${it.signal.value}")
                appendLine("置信度: ${it.confidence}")
                appendLine("理由: ${it.reasoning}")
                appendLine()
            }
            context.getOpinion(AgentType.NEWS)?.let {
                appendLine("=== 舆情分析意见 ===")
                appendLine("信号: ${it.signal.value}")
                appendLine("置信度: ${it.confidence}")
                appendLine("理由: ${it.reasoning}")
                it.rawData["risk_factors"]?.let { risks ->
                    appendLine("识别的风险: $risks")
                }
                appendLine()
            }
            
            context.getData<String>("quote")?.let {
                appendLine("=== 实时行情（估值信息）===")
                appendLine(it)
                appendLine()
            }
            context.getData<String>("news")?.let {
                appendLine("=== 相关新闻 ===")
                appendLine(it)
                appendLine()
            }
        }
        
        return """请对以下股票进行全面的风险评估：

股票：$name ($symbol)

$riskData

请基于以上信息进行全面的风险评估，特别关注：
1. 监管风险和法律风险
2. 业绩风险和财务风险
3. 大股东减持和解禁风险
4. 估值合理性
5. 技术面风险信号

输出符合指定格式的 JSON 结果。
注意：
1. 严格使用上述JSON格式
2. 每个识别的风险都要明确严重等级
3. 如果存在否决买入的条件，务必设置 veto_buy = true
4. signal_adjustment 表示对最终决策的建议调整"""
    }
    
    override suspend fun postProcess(context: AgentContext, rawResponse: String): AgentOpinion? {
        return try {
            val jsonStr = extractJson(rawResponse)
            val json = JSONObject(jsonStr)
            
            val signal = parseSignal(extractString(json, "signal"))
            val confidence = validateConfidence(extractDouble(json, "confidence", 0.5).toFloat())
            val reasoning = extractString(json, "reasoning")
            val riskLevel = extractString(json, "risk_level", "medium")
            val riskScore = extractInt(json, "risk_score", 50)
            val vetoBuy = extractBoolean(json, "veto_buy", false)
            
            // 处理风险标记
            val flags = mutableListOf<Map<String, Any>>()
            json.optJSONArray("flags")?.let { array ->
                for (i in 0 until array.length()) {
                    val flag = array.getJSONObject(i)
                    val flagMap = mutableMapOf<String, Any>(
                        "category" to flag.optString("category", "unknown"),
                        "severity" to flag.optString("severity", "medium"),
                        "description" to flag.optString("description", "")
                    )
                    flag.optString("recommendation", "").takeIf { it.isNotEmpty() }?.let {
                        flagMap["recommendation"] = it
                    }
                    flags.add(flagMap)
                    
                    // 向上下文添加风险告警
                    val severity = when (flag.optString("severity", "medium")) {
                        "high" -> AgentMessage.RiskAlert.RiskSeverity.HIGH
                        "low" -> AgentMessage.RiskAlert.RiskSeverity.LOW
                        else -> AgentMessage.RiskAlert.RiskSeverity.MEDIUM
                    }
                    context.addRiskAlert(
                        AgentMessage.RiskAlert(
                            fromAgent = agentType,
                            toAgent = AgentType.DECISION,
                            severity = severity,
                            category = flag.optString("category", "unknown"),
                            description = flag.optString("description", ""),
                            impact = flag.optString("recommendation", "")
                        )
                    )
                }
            }
            
            // 构建详细数据
            val rawData = mutableMapOf<String, Any>(
                "risk_level" to riskLevel,
                "risk_score" to riskScore,
                "veto_buy" to vetoBuy,
                "flags" to flags,
                "overall_assessment" to extractString(json, "overall_assessment", "")
            )
            
            extractJSONObject(json, "risk_breakdown")?.let {
                rawData["risk_breakdown"] = it.toString()
            }
            
            AgentOpinion(
                agentType = agentType,
                agentName = agentName,
                signal = signal,
                confidence = confidence,
                reasoning = reasoning,
                rawData = rawData
            ).also {
                context.setData("risk_opinion", it)
                context.setData("risk_score", riskScore)
                context.setData("veto_buy", vetoBuy)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析风险Agent响应失败", e)
            null
        }
    }
    
    override suspend fun prefetchData(context: AgentContext) {
        // 风险评估主要依赖其他 Agent 的结果，不需要额外预取
    }
    
    override fun getDependencies(): List<AgentType> {
        // 风险评估依赖其他分析 Agent 的结果
        return listOf(AgentType.TECHNICAL, AgentType.FUNDAMENTAL, AgentType.NEWS)
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
