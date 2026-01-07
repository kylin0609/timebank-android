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

    init {
        loadInstalledApps()
    }

    /**
     * 加载已安装的应用列表
     * 优化：先标记为加载中，加载完成后标记为完成
     */
    private fun loadInstalledApps() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            try {
                val apps = appInfoHelper.getInstalledUserApps()
                android.util.Log.d("ClassifyViewModel", "已加载 ${apps.size} 个应用")
                _installedApps.value = apps
            } catch (e: Exception) {
                android.util.Log.e("ClassifyViewModel", "加载应用列表失败", e)
                _installedApps.value = emptyList()
            } finally {
                _isLoading.value = false
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
