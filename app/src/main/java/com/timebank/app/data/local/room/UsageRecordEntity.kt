package com.timebank.app.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.timebank.app.domain.model.AppCategory

/**
 * 使用记录表
 */
@Entity(
    tableName = "usage_record",
    indices = [Index(value = ["packageName", "date"])]
)
data class UsageRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val category: AppCategory
)
