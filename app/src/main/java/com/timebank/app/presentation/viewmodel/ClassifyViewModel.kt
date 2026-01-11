package com.timebank.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebank.app.domain.model.AppCategory
import com.timebank.app.domain.model.AppClassification
import com.timebank.app.domain.repository.AppClassificationRepository
import com.timebank.app.util.AppInfoHelper
import com.timebank.app.util.InstalledAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用分类ViewModel
 */
@HiltViewModel
class ClassifyViewModel @Inject constructor(
    private val repository: AppClassificationRepository,
    private val appInfoHelper: AppInfoHelper
) : ViewModel() {

    // 所有已安装的应用
    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())

    // 已分类的应用（使用 Eagerly 确保数据不会被清除）
    private val _classifiedApps = repository.getAllApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // 正向应用
    val positiveApps: StateFlow<List<AppClassification>> = _classifiedApps
        .map { apps -> apps.filter { it.category == AppCategory.POSITIVE } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // 负向应用
    val negativeApps: StateFlow<List<AppClassification>> = _classifiedApps
        .map { apps -> apps.filter { it.category == AppCategory.NEGATIVE } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // 未分类应用
    val unclassifiedApps: StateFlow<List<InstalledAppInfo>> = combine(
        _installedApps,
        _classifiedApps
    ) { installed, classified ->
        android.util.Log.d("ClassifyViewModel", "combine: installed=${installed.size}, classified=${classified.size}")
        val classifiedPackages = classified.map { it.packageName }.toSet()
        val result = installed.filter { it.packageName !in classifiedPackages }
        android.util.Log.d("ClassifyViewModel", "未分类应用数量: ${result.size}")
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // 加载状态
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 图标加载进度 (当前/总数)
    private val _loadingProgress = MutableStateFlow(0 to 0)
    val loadingProgress: StateFlow<Pair<Int, Int>> = _loadingProgress.asStateFlow()

    // 是否正在加载图标
    private val _isLoadingIcons = MutableStateFlow(false)
    val isLoadingIcons: StateFlow<Boolean> = _isLoadingIcons.asStateFlow()

    init {
        loadInstalledApps()
    }

    /**
     * 分阶段加载已安装的应用列表
     * 第一阶段：快速加载基本信息（0.1秒内）
     * 第二阶段：并行加载图标（1-2秒）
     */
    fun loadInstalledApps() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            try {
                // 第一阶段：快速加载基本信息（不含图标）
                android.util.Log.d("ClassifyViewModel", "第一阶段：快速加载基本信息")
                val basicApps = appInfoHelper.getInstalledUserAppsBasicInfo()
                android.util.Log.d("ClassifyViewModel", "已加载 ${basicApps.size} 个应用（基本信息）")

                // 立即更新列表，用户可以看到应用列表（默认图标）
                _installedApps.value = basicApps
                _isLoading.value = false

                // 第二阶段：后台并行加载图标
                if (basicApps.isNotEmpty()) {
                    _isLoadingIcons.value = true
                    _loadingProgress.value = 0 to basicApps.size

                    android.util.Log.d("ClassifyViewModel", "第二阶段：并行加载图标")
                    val appsWithIcons = appInfoHelper.loadIconsAsync(basicApps) { current, total ->
                        // 实时更新进度
                        _loadingProgress.value = current to total
                    }

                    // 图标加载完成，更新列表
                    _installedApps.value = appsWithIcons
                    _isLoadingIcons.value = false
                    android.util.Log.d("ClassifyViewModel", "图标加载完成: ${appsWithIcons.size} 个应用")
                }
            } catch (e: Exception) {
                android.util.Log.e("ClassifyViewModel", "加载应用列表失败", e)
                _installedApps.value = emptyList()
                _isLoading.value = false
                _isLoadingIcons.value = false
            }
        }
    }

    /**
     * 添加应用到分类
     */
    fun addAppToCategory(packageName: String, appName: String, category: AppCategory) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.addApp(
                AppClassification(
                    packageName = packageName,
                    appName = appName,
                    category = category,
                    addedTime = now,
                    updatedTime = now
                )
            )
        }
    }

    /**
     * 移除应用分类
     */
    fun removeApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteApp(packageName)
        }
    }

    /**
     * 切换应用分类
     */
    fun switchCategory(packageName: String, appName: String, newCategory: AppCategory) {
        viewModelScope.launch {
            val existing = repository.getAppByPackageName(packageName)
            if (existing != null) {
                repository.updateApp(
                    existing.copy(
                        category = newCategory,
                        updatedTime = System.currentTimeMillis()
                    )
                )
            } else {
                addAppToCategory(packageName, appName, newCategory)
            }
        }
    }
}
