package com.example.stockanalysis.data.model

import androidx.room.ColumnInfo

/**
 * 决策统计结果
 */
data class DecisionCount(
    @ColumnInfo(name = "decision")
    val decision: String,
    
    @ColumnInfo(name = "count")
    val count: Int
)
