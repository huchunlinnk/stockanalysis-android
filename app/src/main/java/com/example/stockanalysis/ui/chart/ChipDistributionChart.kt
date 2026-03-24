package com.example.stockanalysis.ui.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.stockanalysis.data.model.ChipDistribution
import kotlin.math.max
import kotlin.math.min

/**
 * 筹码分布图表
 * 显示获利比例、成本分布、集中度等
 */
class ChipDistributionChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var chipData: ChipDistribution? = null
    private var currentPrice: Double = 0.0
    
    //  paints
    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    private val chipPaint = Paint().apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.FILL
        alpha = 180
    }
    
    private val chipBorderPaint = Paint().apply {
        color = Color.parseColor("#1976D2")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val currentPricePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    
    private val avgCostPaint = Paint().apply {
        color = Color.parseColor("#FF9800")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }
    
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isAntiAlias = true
    }
    
    private val labelPaint = Paint().apply {
        color = Color.GRAY
        textSize = 20f
        isAntiAlias = true
    }
    
    private val valuePaint = Paint().apply {
        color = Color.BLACK
        textSize = 28f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    // 边距
    private val padding = 40f
    private val topPadding = 120f
    private val bottomPadding = 60f
    
    fun setData(data: ChipDistribution, currentPrice: Double) {
        this.chipData = data
        this.currentPrice = currentPrice
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val data = chipData ?: return
        
        // 绘制背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // 绘制标题和统计信息
        drawStats(canvas, data)
        
        // 绘制筹码分布图
        drawChipDistribution(canvas, data)
    }
    
    private fun drawStats(canvas: Canvas, data: ChipDistribution) {
        val row1Y = 40f
        val row2Y = 80f
        val col1X = padding
        val col2X = width * 0.35f
        val col3X = width * 0.7f
        
        // 第一行: 获利比例、平均成本、集中度
        canvas.drawText("获利比例", col1X, row1Y, labelPaint)
        val profitRatioText = String.format("%.1f%%", data.profitRatio * 100)
        val profitColor = if (data.profitRatio > 0.5) Color.RED else Color.GREEN
        valuePaint.color = profitColor
        canvas.drawText(profitRatioText, col1X, row2Y, valuePaint)
        
        canvas.drawText("平均成本", col2X, row1Y, labelPaint)
        val avgCostText = String.format("¥%.2f", data.avgCost)
        valuePaint.color = Color.BLACK
        canvas.drawText(avgCostText, col2X, row2Y, valuePaint)
        
        canvas.drawText("90%集中度", col3X, row1Y, labelPaint)
        val concentrationText = String.format("%.1f%%", data.concentration90 * 100)
        valuePaint.color = Color.BLACK
        canvas.drawText(concentrationText, col3X, row2Y, valuePaint)
    }
    
    private fun drawChipDistribution(canvas: Canvas, data: ChipDistribution) {
        val chartTop = topPadding
        val chartBottom = height - bottomPadding
        val chartLeft = padding
        val chartRight = width - padding
        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft
        
        // 获取价格范围
        val allPrices = mutableListOf<Double>()
        allPrices.addAll(data.supportLevels)
        allPrices.addAll(data.resistanceLevels)
        allPrices.add(data.avgCost)
        allPrices.add(currentPrice)
        
        val minPrice = allPrices.minOrNull() ?: currentPrice * 0.9
        val maxPrice = allPrices.maxOrNull() ?: currentPrice * 1.1
        val priceRange = maxPrice - minPrice
        
        // 绘制价格轴
        drawPriceAxis(canvas, minPrice, maxPrice, chartLeft, chartRight, chartTop, chartBottom)
        
        // 绘制筹码分布柱状图 (简化版，模拟分布)
        drawChipBars(canvas, data, minPrice, maxPrice, chartLeft, chartWidth, chartTop, chartHeight)
        
        // 绘制当前价格线
        val currentPriceY = chartBottom - ((currentPrice - minPrice) / priceRange * chartHeight).toFloat()
        canvas.drawLine(chartLeft, currentPriceY, chartRight, currentPriceY, currentPricePaint)
        canvas.drawText("当前价: ¥${String.format("%.2f", currentPrice)}", chartRight - 150, currentPriceY - 10, textPaint)
        
        // 绘制平均成本线
        val avgCostY = chartBottom - ((data.avgCost - minPrice) / priceRange * chartHeight).toFloat()
        canvas.drawLine(chartLeft, avgCostY, chartRight, avgCostY, avgCostPaint)
        canvas.drawText("平均成本", chartLeft, avgCostY - 10, labelPaint)
    }
    
    private fun drawPriceAxis(
        canvas: Canvas,
        minPrice: Double,
        maxPrice: Double,
        chartLeft: Float,
        chartRight: Float,
        chartTop: Float,
        chartBottom: Float
    ) {
        // 绘制轴线
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, textPaint)
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, textPaint)
        
        // 绘制价格刻度
        val priceSteps = 5
        for (i in 0..priceSteps) {
            val price = minPrice + (maxPrice - minPrice) * i / priceSteps
            val y = chartBottom - ((price - minPrice) / (maxPrice - minPrice) * (chartBottom - chartTop)).toFloat()
            
            canvas.drawText(String.format("¥%.2f", price), 5f, y, labelPaint)
        }
    }
    
    private fun drawChipBars(
        canvas: Canvas,
        data: ChipDistribution,
        minPrice: Double,
        maxPrice: Double,
        chartLeft: Float,
        chartWidth: Float,
        chartTop: Float,
        chartHeight: Float
    ) {
        // 模拟筹码分布数据
        // 实际应该从数据源获取每个价位的筹码量
        val barCount = 20
        val barWidth = (chartWidth / barCount) * 0.8f
        val barSpacing = (chartWidth / barCount) * 0.2f
        
        // 在平均成本附近筹码最多
        val maxBarHeight = chartHeight * 0.8f
        
        for (i in 0 until barCount) {
            val priceRatio = i.toDouble() / barCount
            val price = minPrice + (maxPrice - minPrice) * priceRatio
            
            // 模拟筹码分布：在平均成本附近最多
            val distanceFromAvg = kotlin.math.abs(price - data.avgCost) / (maxPrice - minPrice)
            val chipAmount = kotlin.math.exp(-distanceFromAvg * 5)  // 高斯分布模拟
            
            val barHeight = (chipAmount * maxBarHeight).toFloat()
            val barLeft = chartLeft + i * (barWidth + barSpacing)
            val barTop = chartTop + chartHeight - barHeight
            val barRight = barLeft + barWidth
            val barBottom = chartTop + chartHeight
            
            // 根据价格位置设置颜色
            chipPaint.color = when {
                price < currentPrice -> Color.parseColor("#4CAF50")  // 获利区 - 绿色
                price > currentPrice -> Color.parseColor("#F44336")  // 亏损区 - 红色
                else -> Color.parseColor("#2196F3")  // 当前价 - 蓝色
            }
            
            canvas.drawRect(barLeft, barTop, barRight, barBottom, chipPaint)
        }
    }
}

/**
 * 筹码分布卡片View
 * 用于显示简化的筹码信息
 */
class ChipInfoCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.cardview.widget.CardView(context, attrs, defStyleAttr) {
    
    private val titleText: android.widget.TextView
    private val valueText: android.widget.TextView
    private val descText: android.widget.TextView
    
    init {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        
        val padding = 16
        setContentPadding(padding, padding, padding, padding)
        cardElevation = 4f
        radius = 8f
        
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        
        titleText = android.widget.TextView(context).apply {
            textSize = 12f
            setTextColor(Color.GRAY)
        }
        
        valueText = android.widget.TextView(context).apply {
            textSize = 20f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        descText = android.widget.TextView(context).apply {
            textSize = 11f
            setTextColor(Color.GRAY)
        }
        
        layout.addView(titleText)
        layout.addView(valueText)
        layout.addView(descText)
        
        addView(layout)
    }
    
    fun setData(title: String, value: String, description: String = "") {
        titleText.text = title
        valueText.text = value
        descText.text = description
        descText.visibility = if (description.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
