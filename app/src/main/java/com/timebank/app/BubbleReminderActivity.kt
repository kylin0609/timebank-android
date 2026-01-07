package com.timebank.app

import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timebank.app.ui.theme.TimeBankTheme
import kotlinx.coroutines.delay

/**
 * 气泡提示 Activity
 * 每分钟在负向应用右上角显示一个非侵入式提示
 */
class BubbleReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置窗口为透明、非聚焦、可穿透
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)

            // 设置背景为透明
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT

            // 设置重力为右上角
            val params = attributes
            params.gravity = Gravity.TOP or Gravity.END
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
        }

        val remainingMinutes = intent.getIntExtra("remaining_minutes", 0)
        val remainingSeconds = intent.getIntExtra("remaining_seconds", 0)

        setContent {
            TimeBankTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    contentAlignment = Alignment.TopEnd
                ) {
                    BubbleReminderContent(
                        remainingMinutes = remainingMinutes,
                        remainingSeconds = remainingSeconds,
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun BubbleReminderContent(
    remainingMinutes: Int,
    remainingSeconds: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    // 延迟启动动画
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    // 2秒后自动消失
    LaunchedEffect(Unit) {
        delay(2000)
        visible = false
        delay(300) // 等待动画完成
        onDismiss()
    }

    // 淡入淡出+缩放动画
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut(
            animationSpec = tween(300)
        ) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(300)
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(end = 16.dp, top = 16.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 时钟图标
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                // 文字提示
                Column {
                    Text(
                        text = "剩余时间",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = if (remainingMinutes > 0) {
                            "${remainingMinutes}分${remainingSeconds}秒"
                        } else {
                            "${remainingSeconds}秒"
                        },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
