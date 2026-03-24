package com.example.stockanalysis.ui.chart

import android.graphics.Color
import com.example.stockanalysis.data.model.KLineData
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 成交量图表管理器
 */
class VolumeChartManager(
    private val volumeChart: BarChart
) {
    
    private val dateFormat = SimpleDateFormat("MM-dd", Locale.CHINA)
    private var klineData: List<KLineData> = emptyList()
    
    /**
     * 初始化成交量图表
     */
    fun setupChart() {
        volumeChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            setPinchZoom(true)
            isDragEnabled = true
            setScaleEnabled(true)
            
            // X轴
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.BLACK
                textSize = 10f
                
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return ""  // 成交量图不显示X轴标签
                    }
                }
            }
            
            // 左Y轴
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = Color.BLACK
                textSize = 10f
                
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatVolume(value.toLong())
                    }
                }
            }
            
            // 右Y轴
            axisRight.isEnabled = false
            
            // 图例
            legend.isEnabled = false
            
            // 动画
            animateY(500)
        }
    }
    
    /**
     * 更新成交量数据
     */
    fun updateVolumeData(data: List<KLineData>) {
        if (data.isEmpty()) return
        
        klineData = data
        
        val entries = data.mapIndexed { index, kline ->
            BarEntry(
                index.toFloat(),
                kline.volume.toFloat(),
                kline  // 存储K线数据用于颜色判断
            )
        }
        
        val dataSet = BarDataSet(entries, "成交量").apply {
            // 根据涨跌设置颜色
            val colors = data.map { kline ->
                if (kline.close >= kline.open) {
                    Color.rgb(239, 83, 80)  // 上涨 - 红色
                } else {
                    Color.rgb(38, 166, 154)  // 下跌 - 绿色
                }
            }
            setColors(colors)
            
            setDrawValues(false)
        }
        
        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        
        volumeChart.data = barData
        volumeChart.invalidate()
        
        // 同步K线图的位置
        if (data.size > 60) {
            volumeChart.moveViewToX(data.size - 60f)
        }
    }
    
    /**
     * 格式化成交量显示
     */
    private fun formatVolume(volume: Long): String {
        return when {
            volume >= 100000000 -> String.format("%.1f亿", volume / 100000000.0)
            volume >= 10000 -> String.format("%.1f万", volume / 10000.0)
            else -> volume.toString()
        }
    }
    
    /**
     * 同步X轴位置（与K线图联动）
     */
    fun syncPosition(lowestVisibleX: Float, visibleXRange: Float) {
        volumeChart.moveViewToX(lowestVisibleX)
    }
    
    /**
     * 获取当前数据
     */
    fun getCurrentData(): List<KLineData> = klineData
}
