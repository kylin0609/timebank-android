package com.timebank.app.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.timebank.app.domain.model.AppCategory

/**
 * 应用分类表
 */
@Entity(tableName = "app_classification")
data class AppClassificationEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val iconPath: String? = null,
    val addedTime: Long,
    val updatedTime: Long
)
