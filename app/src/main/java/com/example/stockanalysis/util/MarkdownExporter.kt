package com.example.stockanalysis.util

import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.Decision
import com.example.stockanalysis.data.model.RiskLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Markdown 导出工具
 * 将分析结果导出为Markdown格式
 */
class MarkdownExporter {

    /**
     * 导出为Markdown格式
     */
    fun exportToMarkdown(result: AnalysisResult): String {
        return buildString {
            // 标题
            appendLine("# ${result.stockName} (${result.stockSymbol}) 智能分析报告")
            appendLine()
            appendLine("---")
            appendLine()

            // 基本信息
            appendLine("## 📊 基本信息")
            appendLine()
            appendLine("- **分析时间**: ${formatDate(result.analysisTime)}")
            appendLine("- **决策建议**: ${formatDecision(result.decision)}")
            appendLine("- **评分**: ${result.score}分")
            appendLine("- **置信度**: ${formatConfidence(result.confidence)}")
            appendLine()

            // 分析摘要
            appendLine("## 💡 分析摘要")
            appendLine()
            appendLine(result.summary)
            appendLine()

            // 详细推理
            if (result.reasoning.isNotBlank()) {
                appendLine("## 🔍 推理过程")
                appendLine()
                appendLine(result.reasoning)
                appendLine()
            }

            // 技术面分析
            result.technicalAnalysis?.let { tech ->
                appendLine("## 📈 技术面分析")
                appendLine()
                appendLine("**趋势**: ${tech.trend}")
                appendLine()
                appendLine("**均线排列**: ${tech.maAlignment}")
                appendLine()
                tech.supportLevel?.let {
                    appendLine("**支撑位**: ¥${String.format("%.2f", it)}")
                }
                tech.resistanceLevel?.let {
                    appendLine("**阻力位**: ¥${String.format("%.2f", it)}")
                }
                appendLine()
                appendLine("**量能分析**: ${tech.volumeAnalysis}")
                appendLine()
                appendLine("**技术评分**: ${tech.technicalScore}分")
                appendLine()
            }

            // 基本面分析
            result.fundamentalAnalysis?.let { fund ->
                appendLine("## 💼 基本面分析")
                appendLine()
                appendLine("**估值评价**: ${fund.valuation}")
                appendLine()
                appendLine("**盈利能力**: ${fund.profitability}")
                appendLine()
                appendLine("**基本面评分**: ${fund.fundamentalScore}分")
                appendLine()
            }

            // 舆情分析
            result.sentimentAnalysis?.let { sentiment ->
                appendLine("## 📰 市场情绪")
                appendLine()
                appendLine("**整体情绪**: ${sentiment.overallSentiment}")
                appendLine()

                if (sentiment.keyNews.isNotEmpty()) {
                    appendLine("**关键新闻**:")
                    sentiment.keyNews.take(5).forEach { news ->
                        appendLine("- $news")
                    }
                    appendLine()
                }
            }

            // 风险评估
            result.riskAssessment?.let { risk ->
                appendLine("## ⚠️ 风险评估")
                appendLine()
                appendLine("**风险等级**: ${formatRiskLevel(risk.riskLevel)}")
                appendLine()
                appendLine("**波动性**: ${risk.volatility}")
                appendLine()
                appendLine("**流动性风险**: ${risk.liquidityRisk}")
                appendLine()
                appendLine("**市场风险**: ${risk.marketRisk}")
                appendLine()

                if (risk.specificRisks.isNotEmpty()) {
                    appendLine("**具体风险点**:")
                    risk.specificRisks.forEach { riskItem ->
                        appendLine("- $riskItem")
                    }
                    appendLine()
                }
            }

            // 免责声明
            appendLine("---")
            appendLine()
            appendLine("## 📌 免责声明")
            appendLine()
            appendLine("- 本报告由AI自动生成，仅供参考")
            appendLine("- 不构成任何投资建议")
            appendLine("- 投资有风险，入市需谨慎")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("*生成时间: ${formatDate(Date())}*")
        }
    }

    // ==================== 格式化工具方法 ====================

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    private fun formatDecision(decision: Decision): String {
        return when (decision) {
            Decision.STRONG_BUY -> "强烈买入"
            Decision.BUY -> "买入"
            Decision.HOLD -> "持有观望"
            Decision.SELL -> "卖出"
            Decision.STRONG_SELL -> "强烈卖出"
        }
    }

    private fun formatConfidence(confidence: com.example.stockanalysis.data.model.ConfidenceLevel): String {
        return when (confidence) {
            com.example.stockanalysis.data.model.ConfidenceLevel.HIGH -> "高"
            com.example.stockanalysis.data.model.ConfidenceLevel.MEDIUM -> "中"
            com.example.stockanalysis.data.model.ConfidenceLevel.LOW -> "低"
        }
    }

    private fun formatRiskLevel(level: RiskLevel): String {
        return when (level) {
            RiskLevel.LOW -> "低风险"
            RiskLevel.MEDIUM -> "中等风险"
            RiskLevel.HIGH -> "高风险"
        }
    }

    /**
     * 导出为简洁Markdown格式
     */
    fun exportCompactMarkdown(result: AnalysisResult): String {
        return buildString {
            appendLine("# ${result.stockName} (${result.stockSymbol}) 分析报告")
            appendLine()
            appendLine("**决策建议**: ${formatDecision(result.decision)}")
            appendLine("**评分**: ${result.score}分")
            appendLine()
            appendLine("## 摘要")
            appendLine(result.summary)
            appendLine()
            appendLine("---")
            appendLine("*生成时间: ${formatDate(Date())}*")
        }
    }
}
