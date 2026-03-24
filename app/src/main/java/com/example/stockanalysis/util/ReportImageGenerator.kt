package com.example.stockanalysis.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.format.DateFormat
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.example.stockanalysis.R
import com.example.stockanalysis.data.model.ActionPlan
import com.example.stockanalysis.data.model.AnalysisResult
import com.example.stockanalysis.data.model.CheckStatus
import com.example.stockanalysis.data.model.ConfidenceLevel
import com.example.stockanalysis.data.model.Decision
import com.example.stockanalysis.data.model.RiskLevel
import java.util.Date

/**
 * 报告图片生成器
 * 生成专业美观的分析报告分享图片
 */
class ReportImageGenerator(private val context: Context) {

    companion object {
        const val IMAGE_WIDTH = 1080
        const val PADDING = 60
        const val CORNER_RADIUS = 24f
        
        // 颜色配置 - Material Design 风格
        val COLOR_BACKGROUND = Color.parseColor("#0F1419")  // 深色背景
        val COLOR_CARD_BG = Color.parseColor("#1A1F2E")     // 卡片背景
        val COLOR_CARD_BG_SECONDARY = Color.parseColor("#242B3D")  // 次要卡片背景
        val COLOR_TEXT_PRIMARY = Color.parseColor("#FFFFFF")       // 主文字
        val COLOR_TEXT_SECONDARY = Color.parseColor("#8B95A5")     // 次要文字
        val COLOR_ACCENT = Color.parseColor("#4A90E2")             // 强调色
        val COLOR_GRADIENT_START = Color.parseColor("#667EEA")     // 渐变开始
        val COLOR_GRADIENT_END = Color.parseColor("#764BA2")       // 渐变结束
        
        // 决策颜色
        val COLOR_STRONG_BUY = Color.parseColor("#00C853")
        val COLOR_BUY = Color.parseColor("#64DD17")
        val COLOR_HOLD = Color.parseColor("#FFD600")
        val COLOR_SELL = Color.parseColor("#FF9100")
        val COLOR_STRONG_SELL = Color.parseColor("#FF1744")
        
        // 风险颜色
        val COLOR_RISK_LOW = Color.parseColor("#00C853")
        val COLOR_RISK_MEDIUM = Color.parseColor("#FFD600")
        val COLOR_RISK_HIGH = Color.parseColor("#FF1744")
    }

    /**
     * 生成分析报告分享图片
     */
    fun generateReportImage(result: AnalysisResult): Bitmap {
        // 计算所需高度
        val calculator = HeightCalculator()
        val height = calculator.calculate(result)
        
        // 创建 Bitmap
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制背景
        drawBackground(canvas)
        
        // 渲染内容
        val renderer = ReportRenderer(canvas)
        renderer.render(result)
        
        return bitmap
    }

    /**
     * 生成简洁版分享图片（适合社交媒体）
     */
    fun generateCompactReportImage(result: AnalysisResult): Bitmap {
        val calculator = CompactHeightCalculator()
        val height = calculator.calculate(result)
        
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        drawBackground(canvas)
        
        val renderer = CompactReportRenderer(canvas)
        renderer.render(result)
        
        return bitmap
    }

    /**
     * 绘制渐变背景
     */
    private fun drawBackground(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = COLOR_BACKGROUND
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), canvas.height.toFloat(), paint)
    }

    // ==================== 高度计算器 ====================
    
    private class HeightCalculator {
        private var height = 0
        
        fun calculate(result: AnalysisResult): Int {
            height = PADDING  // 顶部边距
            
            // 头部区域
            height += 200  // Logo和标题
            height += 40
            
            // 股票信息卡片
            height += 180
            height += 40
            
            // 决策结果区域
            height += 200
            height += 40
            
            // 评分区域
            height += 160
            height += 40
            
            // 分析摘要
            height += calculateTextHeight(result.summary, 900, 16f) + 80
            height += 40
            
            // 技术面分析
            result.technicalAnalysis?.let {
                height += 60  // 标题
                height += calculateTextHeight(it.trend, 900, 14f)
                height += 120  // 支撑/阻力
                height += 40
            }
            
            // 基本面分析
            result.fundamentalAnalysis?.let {
                height += 60
                height += calculateTextHeight(it.valuation, 900, 14f)
                height += 40
            }
            
            // 风险评估
            result.riskAssessment?.let {
                height += 60
                height += 100
                height += 40
            }
            
            // 行动计划
            result.actionPlan?.let {
                height += 60
                height += 200
                it.checkList.take(3).forEach { _ ->
                    height += 50
                }
                height += 40
            }
            
            // 页脚
            height += 120
            height += PADDING  // 底部边距
            
            return height
        }
        
        private fun calculateTextHeight(text: String, width: Int, textSize: Float): Int {
            val paint = TextPaint().apply {
                this.textSize = textSize
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width).build()
            return layout.height
        }
    }

    private class CompactHeightCalculator {
        fun calculate(result: AnalysisResult): Int {
            var height = PADDING
            height += 180  // 头部
            height += 160  // 股票信息
            height += 180  // 决策
            height += 120  // 评分
            height += 100  // 页脚
            height += PADDING
            return height
        }
    }

    // ==================== 完整报告渲染器 ====================
    
    private class ReportRenderer(private val canvas: Canvas) {
        private var currentY = PADDING.toFloat()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        
        fun render(result: AnalysisResult) {
            renderHeader()
            renderStockInfo(result)
            renderDecision(result)
            renderScore(result)
            renderSummary(result)
            result.technicalAnalysis?.let { renderTechnicalAnalysis(it) }
            result.fundamentalAnalysis?.let { renderFundamentalAnalysis(it) }
            result.riskAssessment?.let { renderRiskAssessment(it) }
            result.actionPlan?.let { renderActionPlan(it) }
            renderFooter()
        }
        
        private fun renderHeader() {
            // 应用 Logo 背景渐变
            val gradient = LinearGradient(
                PADDING.toFloat(), currentY,
                (PADDING + 120).toFloat(), currentY + 120,
                COLOR_GRADIENT_START, COLOR_GRADIENT_END,
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            
            // Logo 圆形背景
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (PADDING + 120).toFloat(), currentY + 120
            )
            canvas.drawRoundRect(rect, 24f, 24f, paint)
            paint.shader = null
            
            // Logo 文字
            textPaint.apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 56f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("S", PADDING + 40f, currentY + 80f, textPaint)
            
            // 应用名称
            textPaint.apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 48f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("StockAnalysis", PADDING + 140f, currentY + 70f, textPaint)
            
            // 副标题
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 28f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText("AI 智能分析报告", PADDING + 140f, currentY + 110f, textPaint)
            
            currentY += 200
        }
        
        private fun renderStockInfo(result: AnalysisResult) {
            // 卡片背景
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 180
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            // 股票代码
            textPaint.apply {
                color = COLOR_ACCENT
                textSize = 36f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(result.stockSymbol, (PADDING + 40).toFloat(), currentY + 60f, textPaint)
            
            // 股票名称
            textPaint.apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 48f
            }
            canvas.drawText(result.stockName, (PADDING + 40).toFloat(), currentY + 120f, textPaint)
            
            // 分析时间
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 24f
            }
            val dateStr = DateFormat.format("yyyy-MM-dd HH:mm", result.analysisTime).toString()
            canvas.drawText("分析时间: $dateStr", (PADDING + 40).toFloat(), currentY + 155f, textPaint)
            
            currentY += 220
        }
        
        private fun renderDecision(result: AnalysisResult) {
            val decisionColor = when (result.decision) {
                Decision.STRONG_BUY -> COLOR_STRONG_BUY
                Decision.BUY -> COLOR_BUY
                Decision.HOLD -> COLOR_HOLD
                Decision.SELL -> COLOR_SELL
                Decision.STRONG_SELL -> COLOR_STRONG_SELL
            }
            
            val decisionText = when (result.decision) {
                Decision.STRONG_BUY -> "强烈买入"
                Decision.BUY -> "买入"
                Decision.HOLD -> "持有观望"
                Decision.SELL -> "卖出"
                Decision.STRONG_SELL -> "强烈卖出"
            }
            
            // 卡片背景
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 200
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            // 决策标签
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 28f
            }
            canvas.drawText("决策建议", (PADDING + 40).toFloat(), currentY + 50f, textPaint)
            
            // 决策结果（带背景）
            textPaint.apply {
                color = decisionColor
                textSize = 72f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(decisionText, (PADDING + 40).toFloat(), currentY + 130f, textPaint)
            
            // 置信度
            val confidenceText = when (result.confidence) {
                ConfidenceLevel.HIGH -> "高置信度"
                ConfidenceLevel.MEDIUM -> "中等置信度"
                ConfidenceLevel.LOW -> "低置信度"
            }
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 24f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText(confidenceText, (PADDING + 40).toFloat(), currentY + 170f, textPaint)
            
            currentY += 240
        }
        
        private fun renderScore(result: AnalysisResult) {
            // 卡片背景
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 160
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            // 评分标题
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 28f
            }
            canvas.drawText("综合评分", (PADDING + 40).toFloat(), currentY + 50f, textPaint)
            
            // 评分数值
            val scoreColor = when {
                result.score >= 80 -> COLOR_STRONG_BUY
                result.score >= 60 -> COLOR_BUY
                result.score >= 40 -> COLOR_HOLD
                result.score >= 20 -> COLOR_SELL
                else -> COLOR_STRONG_SELL
            }
            
            textPaint.apply {
                color = scoreColor
                textSize = 80f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("${result.score}", (PADDING + 40).toFloat(), currentY + 125f, textPaint)
            
            // /100
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 36f
            }
            canvas.drawText("/100", (PADDING + 140).toFloat(), currentY + 125f, textPaint)
            
            // 评分条
            val barY = currentY + 145f
            paint.color = COLOR_CARD_BG_SECONDARY
            canvas.drawRoundRect(
                (PADDING + 250).toFloat(), barY - 20,
                (IMAGE_WIDTH - PADDING - 40).toFloat(), barY + 10,
                15f, 15f, paint
            )
            
            // 评分进度
            paint.color = scoreColor
            val progressWidth = (IMAGE_WIDTH - PADDING - 40 - PADDING - 250) * result.score / 100
            canvas.drawRoundRect(
                (PADDING + 250).toFloat(), barY - 20,
                (PADDING + 250 + progressWidth).toFloat(), barY + 10,
                15f, 15f, paint
            )
            
            currentY += 200
        }
        
        private fun renderSummary(result: AnalysisResult) {
            // 卡片背景
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 100
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            // 标题
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 24f
            }
            canvas.drawText("📋 分析摘要", (PADDING + 30).toFloat(), currentY + 45f, textPaint)
            
            // 内容
            textPaint.apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 28f
            }
            
            val layout = StaticLayout.Builder.obtain(
                result.summary, 0, result.summary.length, textPaint, IMAGE_WIDTH - PADDING * 2 - 60
            ).setLineSpacing(0f, 1.3f).build()
            
            canvas.save()
            canvas.translate((PADDING + 30).toFloat(), currentY + 60f)
            layout.draw(canvas)
            canvas.restore()
            
            currentY += 80 + layout.height + 40
        }
        
        private fun renderTechnicalAnalysis(technical: com.example.stockanalysis.data.model.TechnicalAnalysis) {
            // 卡片背景
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 250
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            // 标题
            textPaint.apply {
                color = COLOR_ACCENT
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("📈 技术面分析", (PADDING + 30).toFloat(), currentY + 50f, textPaint)
            
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 24f
            }
            
            var y = currentY + 90f
            
            // 趋势
            canvas.drawText("趋势: ${technical.trend}", (PADDING + 30).toFloat(), y, textPaint)
            y += 40f
            
            // 均线
            canvas.drawText("均线: ${technical.maAlignment}", (PADDING + 30).toFloat(), y, textPaint)
            y += 40f
            
            // 支撑/阻力
            textPaint.apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 24f
            }
            val supportText = technical.supportLevel?.let { "支撑: ¥%.2f".format(it) } ?: "支撑: 暂不明确"
            val resistanceText = technical.resistanceLevel?.let { "阻力: ¥%.2f".format(it) } ?: "阻力: 暂不明确"
            canvas.drawText(supportText, (PADDING + 30).toFloat(), y, textPaint)
            canvas.drawText(resistanceText, (PADDING + 300).toFloat(), y, textPaint)
            
            currentY += 290
        }
        
        private fun renderFundamentalAnalysis(fundamental: com.example.stockanalysis.data.model.FundamentalAnalysis) {
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 180
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            textPaint.apply {
                color = COLOR_ACCENT
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("📊 基本面分析", (PADDING + 30).toFloat(), currentY + 50f, textPaint)
            
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 24f
            }
            
            var y = currentY + 90f
            canvas.drawText("估值: ${fundamental.valuation}", (PADDING + 30).toFloat(), y, textPaint)
            y += 35f
            canvas.drawText("成长性: ${fundamental.growth}", (PADDING + 30).toFloat(), y, textPaint)
            
            currentY += 220
        }
        
        private fun renderRiskAssessment(risk: com.example.stockanalysis.data.model.RiskAssessment) {
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 160
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            textPaint.apply {
                color = COLOR_ACCENT
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("⚠️ 风险评估", (PADDING + 30).toFloat(), currentY + 50f, textPaint)
            
            val riskColor = when (risk.riskLevel) {
                RiskLevel.LOW -> COLOR_RISK_LOW
                RiskLevel.MEDIUM -> COLOR_RISK_MEDIUM
                RiskLevel.HIGH -> COLOR_RISK_HIGH
            }
            
            val riskText = when (risk.riskLevel) {
                RiskLevel.LOW -> "低风险"
                RiskLevel.MEDIUM -> "中等风险"
                RiskLevel.HIGH -> "高风险"
            }
            
            // 风险等级标签
            paint.color = riskColor and 0x20FFFFFF
            val tagRect = RectF(
                (PADDING + 30).toFloat(), currentY + 70f,
                (PADDING + 180).toFloat(), currentY + 115f
            )
            canvas.drawRoundRect(tagRect, 20f, 20f, paint)
            
            textPaint.apply {
                color = riskColor
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(riskText, (PADDING + 50).toFloat(), currentY + 102f, textPaint)
            
            currentY += 200
        }
        
        private fun renderActionPlan(actionPlan: ActionPlan) {
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 300
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            textPaint.apply {
                color = COLOR_ACCENT
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("🎯 操作建议", (PADDING + 30).toFloat(), currentY + 50f, textPaint)
            
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 24f
                typeface = Typeface.DEFAULT
            }
            
            var y = currentY + 90f
            
            actionPlan.entryPrice?.let {
                canvas.drawText("建议买入价: ¥%.2f".format(it), (PADDING + 30).toFloat(), y, textPaint)
                y += 35f
            }
            
            actionPlan.targetPrice?.let {
                canvas.drawText("目标价位: ¥%.2f".format(it), (PADDING + 30).toFloat(), y, textPaint)
                y += 35f
            }
            
            actionPlan.stopLossPrice?.let {
                canvas.drawText("止损价位: ¥%.2f".format(it), (PADDING + 30).toFloat(), y, textPaint)
                y += 45f
            }
            
            // 检查清单
            actionPlan.checkList.take(3).forEach { item ->
                val statusColor = when (item.status) {
                    CheckStatus.PASSED -> COLOR_STRONG_BUY
                    CheckStatus.WARNING -> COLOR_HOLD
                    CheckStatus.FAILED -> COLOR_STRONG_SELL
                }
                
                val statusIcon = when (item.status) {
                    CheckStatus.PASSED -> "✓"
                    CheckStatus.WARNING -> "!"
                    CheckStatus.FAILED -> "✗"
                }
                
                textPaint.color = statusColor
                canvas.drawText("$statusIcon ${item.item}", (PADDING + 30).toFloat(), y, textPaint)
                y += 40f
            }
            
            currentY += 340
        }
        
        private fun renderFooter() {
            // 分隔线
            paint.color = COLOR_CARD_BG
            canvas.drawRect(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 2f, paint
            )
            currentY += 30f
            
            // Logo
            textPaint.apply {
                color = COLOR_ACCENT
                textSize = 36f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("StockAnalysis", (PADDING + 30).toFloat(), currentY + 40f, textPaint)
            
            // 免责声明
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 20f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText(
                "本分析仅供参考，不构成投资建议。投资有风险，入市需谨慎。",
                (PADDING + 30).toFloat(), currentY + 75f, textPaint
            )
            
            // 生成时间
            val timeStr = DateFormat.format("yyyy-MM-dd HH:mm", Date()).toString()
            canvas.drawText("生成于 $timeStr", (PADDING + 30).toFloat(), currentY + 100f, textPaint)
        }
    }

    // ==================== 简洁版报告渲染器 ====================
    
    private class CompactReportRenderer(private val canvas: Canvas) {
        private var currentY = PADDING.toFloat()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        
        fun render(result: AnalysisResult) {
            renderHeader()
            renderCompactStockInfo(result)
            renderCompactDecision(result)
            renderCompactScore(result)
            renderCompactFooter()
        }
        
        private fun renderHeader() {
            // 渐变背景
            val gradient = LinearGradient(
                0f, currentY, IMAGE_WIDTH.toFloat(), currentY + 120,
                COLOR_GRADIENT_START, COLOR_GRADIENT_END,
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, currentY, IMAGE_WIDTH.toFloat(), currentY + 120, paint)
            paint.shader = null
            
            // 标题
            textPaint.apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 48f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("StockAnalysis", PADDING.toFloat(), currentY + 70f, textPaint)
            
            currentY += 140
        }
        
        private fun renderCompactStockInfo(result: AnalysisResult) {
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 140
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            textPaint.apply {
                color = COLOR_TEXT_PRIMARY
                textSize = 48f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(result.stockName, (PADDING + 30).toFloat(), currentY + 60f, textPaint)
            
            textPaint.apply {
                color = COLOR_ACCENT
                textSize = 32f
            }
            canvas.drawText(result.stockSymbol, (PADDING + 30).toFloat(), currentY + 100f, textPaint)
            
            currentY += 160
        }
        
        private fun renderCompactDecision(result: AnalysisResult) {
            val (decisionText, decisionColor) = when (result.decision) {
                Decision.STRONG_BUY -> "强烈买入" to COLOR_STRONG_BUY
                Decision.BUY -> "买入" to COLOR_BUY
                Decision.HOLD -> "持有观望" to COLOR_HOLD
                Decision.SELL -> "卖出" to COLOR_SELL
                Decision.STRONG_SELL -> "强烈卖出" to COLOR_STRONG_SELL
            }
            
            // 大卡片背景
            paint.color = decisionColor and 0x15FFFFFF
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 180
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            textPaint.apply {
                color = decisionColor
                textSize = 72f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(decisionText, (PADDING + 40).toFloat(), currentY + 110f, textPaint)
            
            currentY += 200
        }
        
        private fun renderCompactScore(result: AnalysisResult) {
            paint.color = COLOR_CARD_BG
            val rect = RectF(
                PADDING.toFloat(), currentY,
                (IMAGE_WIDTH - PADDING).toFloat(), currentY + 120
            )
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
            
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 28f
            }
            canvas.drawText("综合评分", (PADDING + 30).toFloat(), currentY + 45f, textPaint)
            
            val scoreColor = when {
                result.score >= 80 -> COLOR_STRONG_BUY
                result.score >= 60 -> COLOR_BUY
                result.score >= 40 -> COLOR_HOLD
                result.score >= 20 -> COLOR_SELL
                else -> COLOR_STRONG_SELL
            }
            
            textPaint.apply {
                color = scoreColor
                textSize = 64f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("${result.score}", (PADDING + 30).toFloat(), currentY + 100f, textPaint)
            
            currentY += 140
        }
        
        private fun renderCompactFooter() {
            textPaint.apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 22f
            }
            canvas.drawText(
                "⚠️ 本分析仅供参考，不构成投资建议",
                PADDING.toFloat(), currentY + 40f, textPaint
            )
            
            val timeStr = DateFormat.format("yyyy-MM-dd HH:mm", Date()).toString()
            canvas.drawText("生成于 $timeStr", PADDING.toFloat(), currentY + 70f, textPaint)
        }
    }
}
