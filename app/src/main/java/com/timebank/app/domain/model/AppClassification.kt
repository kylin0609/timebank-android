package com.timebank.app.domain.model

/**
 * 应用分类信息
 */
data class AppClassification(
    val packageName: String,        // 包名
    val appName: String,             // 应用名称
    val category: AppCategory,       // 分类
    val iconPath: String? = null,    // 图标路径
    val addedTime: Long,             // 添加时间戳
    val updatedTime: Long            // 更新时间戳
)
