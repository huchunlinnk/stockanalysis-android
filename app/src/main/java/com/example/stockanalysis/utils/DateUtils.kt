package com.example.stockanalysis.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期工具类
 */
object DateUtils {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fullFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 格式化日期
     */
    fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }

    /**
     * 格式化时间
     */
    fun formatTime(date: Date): String {
        return timeFormat.format(date)
    }

    /**
     * 格式化日期时间
     */
    fun formatDateTime(date: Date): String {
        return dateTimeFormat.format(date)
    }

    /**
     * 格式化完整时间
     */
    fun formatFull(date: Date): String {
        return fullFormat.format(date)
    }

    /**
     * 获取相对时间描述
     */
    fun getRelativeTime(date: Date): String {
        val now = System.currentTimeMillis()
        val diff = now - date.time
        
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 604_800_000 -> "${diff / 86_400_000}天前"
            else -> formatDate(date)
        }
    }

    /**
     * 检查是否为交易时间（A股）
     */
    fun isTradingTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 周末不开盘
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        
        val time = hour * 60 + minute
        
        // 上午 9:30 - 11:30，下午 13:00 - 15:00
        return (time in 570..690) || (time in 780..900)
    }

    /**
     * 获取今天的日期字符串
     */
    fun getTodayString(): String {
        return dateFormat.format(Date())
    }

    /**
     * 解析时间字符串
     */
    fun parseTime(timeStr: String): Date? {
        return try {
            timeFormat.parse(timeStr)
        } catch (e: Exception) {
            null
        }
    }
}
