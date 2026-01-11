package com.timebank.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.timebank.app.presentation.ui.classify.ClassifyScreen
import com.timebank.app.presentation.viewmodel.ClassifyViewModel
import com.timebank.app.presentation.viewmodel.HomeViewModel
import com.timebank.app.presentation.viewmodel.SettingsViewModel
import com.timebank.app.service.MonitorService
import com.timebank.app.ui.theme.TimeBankTheme
import com.timebank.app.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主Activity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启动监控服务
        android.util.Log.d("MainActivity", "启动 MonitorService")
        val serviceIntent = Intent(this, MonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        setContent {
            TimeBankTheme {
                MainScreen(permissionManager)
            }
        }
    }
}

/**
 * 导航项
 */
sealed class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : NavigationItem("home", "主页", Icons.Default.Home)
    object Classify : NavigationItem("classify", "分类", Icons.Default.List)
    object Settings : NavigationItem("settings", "设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(permissionManager: PermissionManager) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Classify,
        NavigationItem.Settings
    )

    Scaffold(
        bottomBar = {
            // 轻盈透明底部导航栏
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedItem) {
                0 -> {
                    val viewModel: HomeViewModel = viewModel()
                    val context = androidx.compose.ui.platform.LocalContext.current
                    HomeScreen(
                        viewModel = viewModel,
                        onRequestPermission = {
                            context.startActivity(permissionManager.openUsageStatsSettings())
                        }
                    )
                }
                1 -> {
                    val viewModel: ClassifyViewModel = viewModel()
                    ClassifyScreen(viewModel = viewModel)
                }
                2 -> {
                    val viewModel: SettingsViewModel = viewModel()
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRequestPermission: () -> Unit
) {
    val balance by viewModel.balance.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var hasOverlayPermission by remember {
        mutableStateOf(android.provider.Settings.canDrawOverlays(context))
    }

    // 快速轮询检测权限变化，提供即时反馈
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(200) // 0.2 秒轮询
            hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)
            viewModel.refreshPermission() // 同时刷新使用情况权限
        }
    }

    // 极简主页设计
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 应用标题 - 小而精致
            Text(
                text = "时间银行",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 超大余额显示
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 余额数字 - 96sp 超大
                Text(
                    text = "$balance",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-2).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 单位和换算
                val minutes = (balance / 60).toInt()
                val seconds = (balance % 60).toInt()
                Text(
                    text = "秒 ($minutes 分 $seconds 秒)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.weight(2f))

            // 权限提示 - 极简风格，始终显示所有权限状态
            if (!hasPermission || !hasOverlayPermission) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 使用情况访问权限
                    PermissionChip(
                        icon = if (hasPermission) "✅" else "⚠️",
                        text = if (hasPermission) "使用情况访问权限（已授予）" else "需要使用情况访问权限",
                        isGranted = hasPermission,
                        onClick = onRequestPermission
                    )

                    // 悬浮窗权限
                    PermissionChip(
                        icon = if (hasOverlayPermission) "✅" else "⚠️",
                        text = if (hasOverlayPermission) "悬浮窗权限（已授予）" else "需要悬浮窗权限",
                        isGranted = hasOverlayPermission,
                        onClick = {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// 极简权限提示芯片（支持状态显示）
@Composable
fun PermissionChip(
    icon: String,
    text: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = if (!isGranted) onClick else {{}},
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = MaterialTheme.shapes.large,
        color = if (isGranted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        },
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.weight(1f)
            )
            if (!isGranted) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 设置页 - 极简分组列表
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val exchangeRatio by viewModel.exchangeRatio.collectAsState()
    val currentBalance by viewModel.currentBalance.collectAsState()
    val lastClearDate by viewModel.lastClearDate.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var showRatioDialog by remember { mutableStateOf(false) }
    var showClearLimitDialog by remember { mutableStateOf(false) }
    var tempRatio by remember { mutableStateOf(exchangeRatio.toFloat()) }

    // 计算是否可以清除
    val canClear = remember(lastClearDate) {
        viewModel.canClearData()
    }

    // 计算剩余时间
    val timeUntilNextClear = remember(lastClearDate) {
        viewModel.getTimeUntilNextClear()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 当前状态卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "当前状态",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 余额
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "当前余额",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(currentBalance),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 兑换比例
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "兑换比例",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "1:${String.format("%.1f", exchangeRatio)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 功能设置
            Text(
                text = "功能设置",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 兑换比例设置
            Surface(
                onClick = {
                    tempRatio = exchangeRatio.toFloat()
                    showRatioDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "兑换比例",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "使用 1 分钟正向 = ${String.format("%.1f", exchangeRatio)} 分钟负向",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 数据管理
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 清除数据
            Surface(
                onClick = {
                    if (canClear) {
                        showClearDialog = true
                    } else {
                        showClearLimitDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "清除所有数据",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (canClear) "重置余额为 5 分钟（每周限1次）" else "每周限1次（已使用）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 关于
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "时间银行",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        }

        // 清除数据确认对话框
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("确认清除所有数据？") },
                text = { Text("将重置余额为 5 分钟，但保留应用分类设置。此操作不可恢复，且每周只能执行一次。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllDataAndResetToDefault()
                            showClearDialog = false
                        }
                    ) {
                        Text("确认", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // 清除限制提示对话框
        if (showClearLimitDialog) {
            val daysRemaining = (timeUntilNextClear / (24 * 60 * 60 * 1000L)).toInt()
            val hoursRemaining = ((timeUntilNextClear % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L)).toInt()

            AlertDialog(
                onDismissRequest = { showClearLimitDialog = false },
                title = { Text("每周限制") },
                text = {
                    Text("每周只能清除数据一次。\n\n距离下次可清除还需要：\n$daysRemaining 天 $hoursRemaining 小时")
                },
                confirmButton = {
                    TextButton(onClick = { showClearLimitDialog = false }) {
                        Text("知道了")
                    }
                }
            )
        }

        // 兑换比例设置对话框
        if (showRatioDialog) {
            AlertDialog(
                onDismissRequest = { showRatioDialog = false },
                title = { Text("设置兑换比例") },
                text = {
                    Column {
                        Text("使用 1 分钟正向应用可获得多少分钟负向应用时长？")
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "当前比例：1 : ${String.format("%.1f", tempRatio)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = tempRatio,
                            onValueChange = { tempRatio = it },
                            valueRange = 0.1f..2.0f,
                            steps = 18
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0.1", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("2.0", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "说明：比例越大，使用正向应用获得的时间越多",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setExchangeRatio(tempRatio.toDouble())
                            showRatioDialog = false
                        }
                    ) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRatioDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 格式化时间（秒 -> 小时:分钟:秒）
 */
fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "${hours}小时 ${minutes}分钟 ${secs}秒"
    } else if (minutes > 0) {
        "${minutes}分钟 ${secs}秒"
    } else {
        "${secs}秒"
    }
}
