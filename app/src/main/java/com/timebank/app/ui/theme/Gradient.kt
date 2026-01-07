package com.timebank.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * 渐变色辅助工具
 */

// 品牌渐变（深蓝紫）
fun brandGradient() = Brush.linearGradient(
    colors = listOf(GradientPrimaryStart, GradientPrimaryEnd),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

// 正向应用渐变（清新绿）
fun positiveGradient() = Brush.linearGradient(
    colors = listOf(GradientPositiveStart, GradientPositiveEnd),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

// 负向应用渐变（温暖粉橙）
fun negativeGradient() = Brush.linearGradient(
    colors = listOf(GradientNegativeStart, GradientNegativeEnd),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)
