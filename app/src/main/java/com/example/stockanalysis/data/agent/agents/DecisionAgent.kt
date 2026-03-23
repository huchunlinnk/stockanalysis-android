package com.example.stockanalysis.data.agent.agents

import android.util.Log
import com.example.stockanalysis.data.agent.*
import com.example.stockanalysis.data.llm.LLMService
import org.json.JSONObject

/**
 * 决策建议 Agent
 * 负责：综合所有 Agent 意见、生成最终决策、行动计划
 */
class DecisionAgent(
    llmService: LLMService,
    toolRegistry: AgentToolRegistry
) : BaseAgent(llmService, toolRegistry) {
    
    companion object {
        private const val TAG = "DecisionAgent"
        
        // 默认权重配置
        private val DEFAULT_WEIGHTS = mapOf(
            AgentType.TECHNICAL to 0.35,
            AgentType.FUNDAMENTAL to 0.25,
            AgentType.NEWS to 0.20,
            AgentType.RISK to 0.20
        )
    }
    
    override val agentType: AgentType = AgentType.DECISION
    override val agentName: String = "决策建议Agent"
    override val description: String = "综合所有分析结果，生成最终投资建议"
    override val maxSteps: Int = 3
    
    override fun getAvailableTools(): List<String> = emptyList()
    
    override fun getSystemPrompt(context: AgentContext): String {
        return """你是一个专业的**投资决策Agent**，负责综合各 Agent 的分析意见，生成最终的决策建议和行动计划。

## 核心职责
1. **综合判断**：整合技术、基本面、舆情、风险四个维度的分析
2. **决策制定**：给出明确的买入/持有/卖出建议
3. **行动规划**：具体的买入/卖出价位、仓位建议
4. **风险控制**：明确的止损止盈位

## 权重分配（参考）
- 技术面分析：35%
- 基本面分析：25%
- 舆情分析：20%
- 风险评估：20%（负向权重，高风险会拉低评分）

## 决策逻辑

### 评分计算
1. 计算加权平均分（考虑各 Agent 置信度）
2. 风险调整：高风险降低评分，低风险提升评分
3. 一致性检查：各 Agent 信号一致性影响最终置信度

### 信号判定
- **Strong Buy（强烈买入）**：综合评分 >= 80，无重大风险
- **Buy（买入）**：综合评分 60-79，风险可控
- **Hold（持有/观望）**：综合评分 40-59，或存在分歧
- **Sell（卖出）**：综合评分 20-39，或风险较高
- **Strong Sell（强烈卖出）**：综合评分 < 20，或存在严重风险

## 输出格式（必须严格遵循JSON格式）
```json
{
  "signal": "strong_buy|buy|hold|sell|strong_sell",
  "confidence": 0.0-1.0,
  "reasoning": "综合分析结论，包含主要依据",
  "final_score": 0-100,
  "consensus_level": "high|medium|low",
  "weighted_opinions": {
    "technical": {"signal": "...", "weight": 0.35, "contribution": 评分贡献},
    "fundamental": {"signal": "...", "weight": 0.25, "contribution": 评分贡献},
    "news": {"signal": "...", "weight": 0.20, "contribution": 评分贡献},
    "risk": {"signal": "...", "weight": 0.20, "contribution": 评分贡献}
  },
  "action_plan": {
    "no_position": {
      "action": "买入/观望",
      "position_size": "建议仓位",
      "entry_price": "建议买入价位",
      "reasoning": "操作建议理由"
    },
    "has_position": {
      "action": "持有/加仓/减仓/清仓",
      "position_adjustment": "仓位调整建议",
      "reasoning": "操作建议理由"
    }
  },
  "risk_management": {
    "stop_loss": "止损价位",
    "take_profit": "目标价位",
    "max_position": "最大仓位限制",
    "time_horizon": "建议持有周期"
  },
  "checklist": [
    {"item": "检查项1", "status": "pass|warning|fail", "note": "说明"},
    {"item": "检查项2", "status": "pass|warning|fail", "note": "说明"}
  ],
  "key_points": ["核心观点1", "核心观点2", "核心观点3"],
  "risk_warning": "风险提示汇总"
}
```

## 一致性判定
- **High（高度一致）**：所有 Agent 信号方向一致
- **Medium（中等一致）**：大部分 Agent 信号方向一致
- **Low（存在分歧）**：Agent 信号存在明显分歧

## 行动建议要求
1. **具体明确**：给出具体的价位，而非模糊区间
2. **区分场景**：区分无持仓和已有持仓的不同策略
3. **可操作性**：建议必须可以立即执行
4. **风险控制**：每个建议都必须伴随风险控制措施

## 风险提示要求
1. 列出所有识别的重大风险
2. 说明风险对投资决策的影响
3. 给出风险发生时的应对建议

请基于各 Agent 的分析意见，做出客观、专业的投资决策。"""
    }
    
    override fun getUserPrompt(context: AgentContext): String {
        val symbol = context.stockSymbol
        val name = context.stockName
        
        // 计算加权评分
        val weightedScore = context.calculateWeightedScore()
        
        // 收集各 Agent 意见
        val opinionsData = buildString {
            AgentType.values().filter { it != AgentType.DECISION }.forEach { type ->
                context.getOpinion(type)?.let { opinion ->
                    appendLine("=== ${type.displayName()} ===")
                    appendLine("信号: ${opinion.signal.value}")
                    appendLine("置信度: ${String.format("%.2f", opinion.confidence)}")
                    appendLine("评分: ${opinion.signal.score}")
                    appendLine("理由: ${opinion.reasoning}")
                    opinion.keyLevels?.let {
                        appendLine("关键价位: 支撑=${it.support}, 阻力=${it.resistance}")
                    }
                    appendLine()
                }
            }
        }
        
        // 风险告警
        val riskData = buildString {
            if (context.riskAlerts.isNotEmpty()) {
                appendLine("=== 风险告警 ===")
                context.riskAlerts.forEach { alert ->
                    appendLine("[${alert.severity.name}] ${alert.category}: ${alert.description}")
                }
                appendLine()
            }
        }
        
        // 预计算数据
        val hasVetoBuy = context.getData<Boolean>("veto_buy") == true
        val riskScore = context.getData<Int>("risk_score") ?: 50
        
        return """请综合以下分析意见，生成最终的投资决策建议：

股票：$name ($symbol)

=== 预计算数据 ===
加权平均评分: $weightedScore
是否有否决买入风险: $hasVetoBuy
风险评分: $riskScore

$opinionsData
$riskData

请基于以上信息：
1. 计算最终评分和决策信号
2. 评估各 Agent 意见的一致性
3. 制定具体的行动计划（分无持仓和已持仓场景）
4. 设定明确的止损止盈位
5. 列出检查清单
6. 总结核心观点

输出符合指定格式的 JSON 结果。
注意：
1. 如果 veto_buy = true，最终信号不能是 strong_buy 或 buy
2. 一致性判定要客观
3. 行动计划必须具体可执行
4. 风险提示要完整"""
    }
    
    override suspend fun postProcess(context: AgentContext, rawResponse: String): AgentOpinion? {
        return try {
            val jsonStr = extractJson(rawResponse)
            val json = JSONObject(jsonStr)
            
            val signal = parseSignal(extractString(json, "signal"))
            var confidence = validateConfidence(extractDouble(json, "confidence", 0.5).toFloat())
            val reasoning = extractString(json, "reasoning")
            val finalScore = extractInt(json, "final_score", 50)
            
            // 如果有否决买入风险，调整信号
            val hasVetoBuy = context.getData<Boolean>("veto_buy") == true
            val adjustedSignal = if (hasVetoBuy && signal in listOf(Signal.STRONG_BUY, Signal.BUY)) {
                // 降级信号
                Signal.HOLD
            } else {
                signal
            }
            
            // 如果有高风险告警，降低置信度
            if (context.hasHighRiskAlert()) {
                confidence *= 0.7f
            }
            
            // 提取行动计划
            val actionPlan = extractJSONObject(json, "action_plan")
            val riskManagement = extractJSONObject(json, "risk_management")
            
            // 提取关键价位
            val keyLevels = riskManagement?.let { rm ->
                AgentOpinion.KeyLevels(
                    stopLoss = if (rm.has("stop_loss")) rm.getDouble("stop_loss") else null,
                    takeProfit = if (rm.has("take_profit")) rm.getDouble("take_profit") else null,
                    entryPrice = actionPlan?.let { ap ->
                        ap.optJSONObject("no_position")?.optDouble("entry_price")
                    }
                )
            }
            
            // 构建详细数据
            val rawData = mutableMapOf<String, Any>(
                "final_score" to finalScore,
                "consensus_level" to extractString(json, "consensus_level", "medium"),
                "veto_adjusted" to (adjustedSignal != signal)
            )
            
            actionPlan?.let { rawData["action_plan"] = it.toString() }
            riskManagement?.let { rawData["risk_management"] = it.toString() }
            
            // 提取核心观点
            val keyPoints = mutableListOf<String>()
            json.optJSONArray("key_points")?.let { array ->
                for (i in 0 until array.length()) {
                    keyPoints.add(array.getString(i))
                }
            }
            rawData["key_points"] = keyPoints
            rawData["risk_warning"] = extractString(json, "risk_warning", "")
            
            // 提取检查清单
            val checklist = mutableListOf<Map<String, String>>()
            json.optJSONArray("checklist")?.let { array ->
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    checklist.add(mapOf(
                        "item" to item.optString("item", ""),
                        "status" to item.optString("status", ""),
                        "note" to item.optString("note", "")
                    ))
                }
            }
            rawData["checklist"] = checklist
            
            AgentOpinion(
                agentType = agentType,
                agentName = agentName,
                signal = adjustedSignal,
                confidence = validateConfidence(confidence),
                reasoning = reasoning,
                keyLevels = keyLevels,
                rawData = rawData
            ).also {
                context.setData("final_decision", it)
                context.setData("final_score", finalScore)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析决策Agent响应失败", e)
            // 如果解析失败，基于现有意见生成默认决策
            generateFallbackDecision(context)
        }
    }
    
    /**
     * 生成降级决策（当 LLM 解析失败时使用）
     */
    private fun generateFallbackDecision(context: AgentContext): AgentOpinion? {
        val weightedScore = context.calculateWeightedScore()
        val signal = Signal.fromScore(weightedScore)
        
        // 检查风险
        val hasVetoBuy = context.getData<Boolean>("veto_buy") == true
        val finalSignal = if (hasVetoBuy && signal in listOf(Signal.STRONG_BUY, Signal.BUY)) {
            Signal.HOLD
        } else {
            signal
        }
        
        return AgentOpinion(
            agentType = agentType,
            agentName = agentName,
            signal = finalSignal,
            confidence = 0.6f,
            reasoning = "基于各 Agent 加权评分自动生成的决策建议（LLM解析失败后的降级方案）",
            rawData = mapOf(
                "final_score" to weightedScore,
                "fallback" to true,
                "weighted_calculation" to true
            )
        )
    }
    
    override suspend fun prefetchData(context: AgentContext) {
        // 决策 Agent 不需要预取数据，依赖其他 Agent 的结果
    }
    
    override fun getDependencies(): List<AgentType> {
        // 决策 Agent 依赖所有分析 Agent
        return listOf(AgentType.TECHNICAL, AgentType.FUNDAMENTAL, AgentType.NEWS, AgentType.RISK)
    }
    
    override fun shouldExecute(context: AgentContext): Boolean {
        // 至少有一个分析 Agent 完成了分析
        return context.getAnalysisOpinions().isNotEmpty()
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
