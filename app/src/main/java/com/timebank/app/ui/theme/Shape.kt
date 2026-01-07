package com.timebank.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 时间银行应用形状系统
 * 定义统一的圆角样式
 */
val TimeBankShapes = Shapes(
    // 小圆角 - 用于按钮、小卡片
    small = RoundedCornerShape(8.dp),
    
    // 中等圆角 - 用于卡片
    medium = RoundedCornerShape(16.dp),
    
    // 大圆角 - 用于对话框、底部抽屉
    large = RoundedCornerShape(24.dp),
    
    // 超大圆角 - 用于特殊组件
    extraLarge = RoundedCornerShape(32.dp)
)
