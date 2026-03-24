package com.example.stockanalysis.ui.chart

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.stockanalysis.data.model.KLineData
import com.example.stockanalysis.data.model.TechnicalIndicators
import com.example.stockanalysis.databinding.ViewCombinedChartBinding
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.data.Entry

/**
 * 组合图表视图
 * 包含K线图和成交量图，支持联动
 */
class CombinedChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewCombinedChartBinding
    private lateinit var klineChartManager: KLineChartManager
    private lateinit var volumeChartManager: VolumeChartManager
    
    var onChartTouchListener: ((KLineData?) -> Unit)? = null
    
    init {
        orientation = VERTICAL
        binding = ViewCombinedChartBinding.inflate(LayoutInflater.from(context), this, true)
        initCharts()
    }
    
    private fun initCharts() {
        // 初始化K线图管理器
        klineChartManager = KLineChartManager(binding.candleChart)
        klineChartManager.setupChart()
        
        // 初始化成交量图管理器
        volumeChartManager = VolumeChartManager(binding.volumeChart)
        volumeChartManager.setupChart()
        
        // 设置K线图手势监听，实现联动
        binding.candleChart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartGestureStart(
                me: android.view.MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {}
            
            override fun onChartGestureEnd(
                me: android.view.MotionEvent?,
                lastPerformedGesture: ChartTouchListener.ChartGesture?
            ) {}
            
            override fun onChartLongPressed(me: android.view.MotionEvent?) {}
            
            override fun onChartDoubleTapped(me: android.view.MotionEvent?) {}
            
            override fun onChartSingleTapped(me: android.view.MotionEvent?) {}
            
            override fun onChartFling(
                me1: android.view.MotionEvent?,
                me2: android.view.MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ) {}
            
            override fun onChartScale(me: android.view.MotionEvent?, scaleX: Float, scaleY: Float) {
                // 同步缩放
                syncCharts()
            }
            
            override fun onChartTranslate(me: android.view.MotionEvent?, dX: Float, dY: Float) {
                // 同步移动
                syncCharts()
            }
        }
        
        // 设置K线图值选择监听
        binding.candleChart.setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val index = e?.x?.toInt() ?: return
                val data = klineChartManager.getCurrentData()
                if (index >= 0 && index < data.size) {
                    onChartTouchListener?.invoke(data[index])
                }
            }
            
            override fun onNothingSelected() {
                onChartTouchListener?.invoke(null)
            }
        })
    }
    
    /**
     * 同步K线图和成交量图
     */
    private fun syncCharts() {
        val lowestVisibleX = binding.candleChart.lowestVisibleX
        volumeChartManager.syncPosition(lowestVisibleX, binding.candleChart.visibleXRange)
    }
    
    /**
     * 设置数据
     */
    fun setData(klineData: List<KLineData>, indicators: TechnicalIndicators? = null) {
        klineChartManager.updateKLineData(klineData, indicators != null)
        volumeChartManager.updateVolumeData(klineData)
        
        indicators?.let {
            klineChartManager.updateIndicators(it)
        }
    }
    
    /**
     * 设置是否显示技术指标
     */
    fun showIndicators(show: Boolean) {
        val data = klineChartManager.getCurrentData()
        if (data.isNotEmpty()) {
            klineChartManager.updateKLineData(data, show)
        }
    }
    
    /**
     * 移动到最后
     */
    fun moveToEnd() {
        val count = klineChartManager.getCurrentData().size
        if (count > 60) {
            klineChartManager.moveTo(count - 60f)
        }
    }
    
    /**
     * 高亮特定索引
     */
    fun highlightIndex(index: Int) {
        klineChartManager.highlightValue(index)
    }
    
    /**
     * 获取当前K线数据
     */
    fun getCurrentData(): List<KLineData> {
        return klineChartManager.getCurrentData()
    }
}
