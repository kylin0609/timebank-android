package com.timebank.app.presentation.ui.classify

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.timebank.app.domain.model.AppCategory
import com.timebank.app.domain.model.AppClassification
import com.timebank.app.presentation.viewmodel.ClassifyViewModel
import com.timebank.app.util.InstalledAppInfo

/**
 * 应用分类管理页面 - 极简网格设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassifyScreen(
    viewModel: ClassifyViewModel
) {
    val positiveApps by viewModel.positiveApps.collectAsState()
    val negativeApps by viewModel.negativeApps.collectAsState()
    val unclassifiedApps by viewModel.unclassifiedApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedFilter by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部间距
            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            Text(
                text = "应用分类",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 筛选条件 - FilterChip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("正向 ${positiveApps.size}") },
                    leadingIcon = if (selectedFilter == 0) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("负向 ${negativeApps.size}") },
                    leadingIcon = if (selectedFilter == 1) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("未分类 ${unclassifiedApps.size}") },
                    leadingIcon = if (selectedFilter == 2) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 应用网格
            when (selectedFilter) {
                0 -> ClassifiedAppGrid(
                    apps = positiveApps,
                    isLoading = isLoading,
                    onRemove = { viewModel.removeApp(it) },
                    context = context
                )
                1 -> ClassifiedAppGrid(
                    apps = negativeApps,
                    isLoading = isLoading,
                    onRemove = { viewModel.removeApp(it) },
                    context = context
                )
                2 -> UnclassifiedAppGrid(
                    apps = unclassifiedApps,
                    isLoading = isLoading,
                    onAddToPositive = { app ->
                        viewModel.addAppToCategory(app.packageName, app.appName, AppCategory.POSITIVE)
                    },
                    onAddToNegative = { app ->
                        viewModel.addAppToCategory(app.packageName, app.appName, AppCategory.NEGATIVE)
                    }
                )
            }
        }

        // 添加按钮 - 仅在已分类页面显示
        if (selectedFilter < 2) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, "添加应用")
            }
        }

        // 添加应用对话框
        if (showAddDialog) {
            AddAppDialog(
                apps = unclassifiedApps,
                targetCategory = if (selectedFilter == 0) AppCategory.POSITIVE else AppCategory.NEGATIVE,
                onDismiss = { showAddDialog = false },
                onAdd = { app, category ->
                    viewModel.addAppToCategory(app.packageName, app.appName, category)
                    showAddDialog = false
                }
            )
        }
    }
}

// 已分类应用 - 3列网格
@Composable
fun ClassifiedAppGrid(
    apps: List<AppClassification>,
    isLoading: Boolean,
    onRemove: (String) -> Unit,
    context: android.content.Context
) {
    when {
        isLoading && apps.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        apps.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无应用\n点击右下角 + 添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center
                )
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(apps) { app ->
                    ClassifiedAppItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        onRemove = { onRemove(app.packageName) },
                        context = context
                    )
                }
            }
        }
    }
}

// 已分类应用卡片
@Composable
fun ClassifiedAppItem(
    appName: String,
    packageName: String,
    onRemove: () -> Unit,
    context: android.content.Context
) {
    var showMenu by remember { mutableStateOf(false) }

    // 获取应用图标
    val appIcon = remember(packageName) {
        try {
            context.packageManager.getApplicationInfo(packageName, 0).loadIcon(context.packageManager)
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { showMenu = true }
        ) {
            // 80dp 圆形容器 + 应用图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = appName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 应用名称
            Text(
                text = appName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 长按菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("移除") },
                onClick = {
                    onRemove()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

// 未分类应用 - 3列网格
@Composable
fun UnclassifiedAppGrid(
    apps: List<InstalledAppInfo>,
    isLoading: Boolean,
    onAddToPositive: (InstalledAppInfo) -> Unit,
    onAddToNegative: (InstalledAppInfo) -> Unit
) {
    when {
        isLoading && apps.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        apps.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "所有应用都已分类",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center
                )
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(apps) { app ->
                    UnclassifiedAppItem(
                        app = app,
                        onAddToPositive = { onAddToPositive(app) },
                        onAddToNegative = { onAddToNegative(app) }
                    )
                }
            }
        }
    }
}

// 未分类应用卡片
@Composable
fun UnclassifiedAppItem(
    app: InstalledAppInfo,
    onAddToPositive: () -> Unit,
    onAddToNegative: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { showMenu = true }
        ) {
            // 80dp 圆形容器 + 应用图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon.toBitmap().asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 应用名称
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 分类菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("正向应用") },
                onClick = {
                    onAddToPositive()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("负向应用") },
                onClick = {
                    onAddToNegative()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppDialog(
    apps: List<InstalledAppInfo>,
    targetCategory: AppCategory,
    onDismiss: () -> Unit,
    onAdd: (InstalledAppInfo, AppCategory) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (targetCategory == AppCategory.POSITIVE) "添加正向应用" else "添加负向应用"
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 搜索框
                if (apps.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索应用名称或包名") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "清除"
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 应用列表
                if (apps.isEmpty()) {
                    Text("所有应用都已分类")
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到匹配的应用",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(400.dp)
                    ) {
                        items(filteredApps) { app ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    onAdd(app, targetCategory)
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 应用图标
                                    if (app.icon != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = app.icon.toBitmap().asImageBitmap(),
                                            contentDescription = app.appName,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    } else {
                                        // 默认图标占位
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    shape = MaterialTheme.shapes.small
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Build,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // 应用信息
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = app.packageName,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
