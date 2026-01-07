package com.timebank.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.timebank.app.presentation.viewmodel.HomeViewModel
import com.timebank.app.ui.theme.TimeBankTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用拦截界面 - 极简毛玻璃效果
 * 当负向应用余额不足时显示此全屏界面
 */
@AndroidEntryPoint
class BlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra("app_name") ?: "此应用"
        val packageName = intent.getStringExtra("package_name") ?: ""

        setContent {
            TimeBankTheme {
                BlockScreen(
                    appName = appName,
                    onClose = { finish() }
                )
            }
        }
    }
}

// 拦截页 - 极简毛玻璃效果
@Composable
fun BlockScreen(
    appName: String,
    onClose: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel()
    val balance by viewModel.balance.collectAsState()

    // 半透明黑色背景 + 模糊效果（通过深色背景模拟）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 锁图标 - 大而简洁
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 主标题
            Text(
                text = "时间不足",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 副标题
            Text(
                text = "无法打开 $appName",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 当前余额卡片 - 半透明白色
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = Color.White.copy(alpha = 0.1f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "当前余额",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$balance",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-2).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val minutes = (balance / 60).toInt()
                    val seconds = (balance % 60).toInt()
                    Text(
                        text = "秒 ($minutes 分 $seconds 秒)",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 提示文本 - 柔和透明
            Text(
                text = "💡 使用正向应用可以获得时间余额",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 关闭按钮 - 半透明白色边框
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "我知道了",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
