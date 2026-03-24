package com.example.stockanalysis.ui.chart

import android.graphics.Color
import android.graphics.Paint
import com.example.stockanalysis.data.model.KLineData
import com.example.stockanalysis.data.model.TechnicalIndicators
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * K线图表管理器
 * 负责K线图的配置、数据更新和技术指标显示
 */
class KLineChartManager(
    private val candleChart: CombinedChart
) {
    
    private val dateFormat = SimpleDateFormat("MM-dd", Locale.CHINA)
    
    /**
     * 初始化K线图配置
     */
    fun setupChart() {
        candleChart.apply {
            // 描述文本
            description.isEnabled = false
            
            // 背景
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            
            // 缩放和拖动
            setPinchZoom(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setDrawBorders(true
            )
            
            // 最大可见数据点数
            setMaxVisibleValueCount(60)
            
            // 动画
            animateX(500)
            
            // 图例
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
            
            // X轴配置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = Color.BLACK
                textSize = 10f
                labelRotationAngle = 0f
                
                // 自定义X轴标签格式（日期）
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        if (index >= 0 && index < klineData.size) {
                            return dateFormat.format(klineData[index].timestamp)
                        }
                        return ""
                    }
                }
            }
            
            // 左Y轴（价格）
            axisLeft.apply {
                setLabelCount(7, false)
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                setDrawAxisLine(true)
                textColor = Color.BLACK
                textSize = 10f
                
                // 不显示0值
                axisMinimum = 0f
            }
            
            // 右Y轴（不显示）
            axisRight.isEnabled = false
        }
    }
    
    private var klineData: List<KLineData> = emptyList()
    
    /**
     * 更新K线数据
     */
    fun updateKLineData(data: List<KLineData>, showIndicators: Boolean = true) {
        if (data.isEmpty()) return
        
        klineData = data
        
        // 创建K线数据
        val candleEntries = data.mapIndexed { index, kline ->
            CandleEntry(
                index.toFloat(),
                kline.high.toFloat(),
                kline.low.toFloat(),
                kline.open.toFloat(),
                kline.close.toFloat()
            )
        }
        
        val candleDataSet = CandleDataSet(candleEntries, "K线").apply {
            // 影线颜色
            shadowColor = Color.DKGRAY
            shadowWidth = 0.8f
            
            // 下跌颜色（绿色）
            decreasingColor = Color.GREEN
            decreasingPaintStyle = Paint.Style.FILL
            
            // 上涨颜色（红色）
            increasingColor = Color.RED
            increasingPaintStyle = Paint.Style.FILL
            
            // 中性颜色
            neutralColor = Color.GRAY
            
            // 高亮
            setDrawHorizontalHighlightIndicator(true)
            setDrawVerticalHighlightIndicator(true)
            highlightLineWidth = 1f
            highLightColor = Color.BLUE
        }
        
        val candleData = CandleData(candleDataSet)
        
        // 创建组合数据
        val combinedData = CombinedData()
        combinedData.setData(candleData)
        
        // 如果显示技术指标
        if (showIndicators) {
            val ma5Data = calculateMAData(data, 5, Color.YELLOW, "MA5")
            val ma10Data = calculateMAData(data, 10, Color.CYAN, "MA10")
            val ma20Data = calculateMAData(data, 20, Color.MAGENTA, "MA20")
            
            if (ma5Data != null) {
                val lineData = LineData()
                lineData.addDataSet(ma5Data)
                lineData.addDataSet(ma10Data)
                lineData.addDataSet(ma20Data)
                combinedData.setData(lineData)
            }
        }
        
        candleChart.data = combinedData
        candleChart.invalidate()
        
        // 移动到最后显示最新数据
        if (data.size > 60) {
            candleChart.moveViewToX(data.size - 60f)
        }
    }
    
    /**
     * 更新技术指标
     */
    fun updateIndicators(indicators: TechnicalIndicators) {
        // 更新图表上的指标线
        candleChart.invalidate()
    }
    
    /**
     * 计算均线数据
     */
    private fun calculateMAData(
        data: List<KLineData>,
        period: Int,
        color: Int,
        label: String
    ): LineDataSet? {
        if (data.size < period) return null
        
        val entries = mutableListOf<Entry>()
        
        for (i in period - 1 until data.size) {
            var sum = 0.0
            for (j in i - period + 1..i) {
                sum += data[j].close
            }
            val ma = sum / period
            entries.add(Entry(i.toFloat(), ma.toFloat()))
        }
        
        return LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 1f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
    }
    
    /**
     * 高亮显示特定数据点
     */
    fun highlightValue(index: Int) {
        candleChart.highlightValue(index.toFloat(), 0)
    }
    
    /**
     * 设置图表可见范围
     */
    fun setVisibleRange(min: Float, max: Float) {
        candleChart.setVisibleXRange(min, max)
    }
    
    /**
     * 移动到特定位置
     */
    fun moveTo(x: Float) {
        candleChart.moveViewToX(x)
    }
    
    /**
     * 获取当前显示的K线数据
     */
    fun getCurrentData(): List<KLineData> = klineData
}

/**
 * 图表类型
 */
enum class ChartType {
    KLINE,      // K线图
    LINE,       // 折线图
    BAR         // 柱状图
}

/**
 * 技术指标类型
 */
enum class IndicatorType {
    MA5,        // 5日均线
    MA10,       // 10日均线
    MA20,       // 20日均线
    MA60,       // 60日均线
    MACD,       // MACD
    RSI,        // RSI
    KDJ,        // KDJ
    BOLL        // 布林带
}
