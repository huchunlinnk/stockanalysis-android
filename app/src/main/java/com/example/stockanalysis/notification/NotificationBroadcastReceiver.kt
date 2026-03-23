package com.example.stockanalysis.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val helper = NotificationHelper(context)
        
        when (intent.action) {
            "ACTION_DAILY_REMINDER" -> {
                helper.showMarketUpdateNotification("每日提醒", "查看今日股票分析")
            }
            "ACTION_MARKET_OPEN" -> {
                helper.showMarketUpdateNotification("市场开盘", "股市已开盘")
            }
            "ACTION_MARKET_CLOSE" -> {
                helper.showMarketUpdateNotification("市场收盘", "股市已收盘")
            }
        }
    }
}
