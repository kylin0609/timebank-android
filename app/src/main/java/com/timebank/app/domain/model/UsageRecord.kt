package com.timebank.app.domain.model

/**
 * 应用使用记录
 */
data class UsageRecord(
    val id: Long = 0,
    val packageName: String,         // 包名
    val date: String,                // 日期 (yyyy-MM-dd)
    val startTime: Long,             // 开始时间戳
    val endTime: Long,               // 结束时间戳
    val duration: Long,              // 使用时长（秒）
    val category: AppCategory        // 分类快照
)
