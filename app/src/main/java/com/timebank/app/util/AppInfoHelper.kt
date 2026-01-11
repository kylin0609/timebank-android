package com.timebank.app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 已安装应用信息
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

/**
 * 应用信息工具类
 */
@Singleton
class AppInfoHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 快速获取所有已安装的用户应用（仅基本信息，不含图标）
     * 优化版本：0.1秒内返回，图标后续异步加载
     */
    suspend fun getInstalledUserAppsBasicInfo(): List<InstalledAppInfo> = withContext(Dispatchers.Default) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        android.util.Log.d("AppInfoHelper", "总共安装的应用: ${packages.size}")

        val userApps = packages.filter { isUserApp(it) }
        android.util.Log.d("AppInfoHelper", "过滤后用户应用: ${userApps.size}")

        // 快速返回基本信息（不加载图标）
        val basicInfo = userApps.map { appInfo ->
            InstalledAppInfo(
                packageName = appInfo.packageName,
                appName = getAppName(appInfo, packageManager),
                icon = null  // 不加载图标，极速返回
            )
        }.sortedBy { it.appName }

        android.util.Log.d("AppInfoHelper", "基本信息加载完成: ${basicInfo.size} 个应用")
        basicInfo
    }

    /**
     * 并行加载应用图标（10个并发协程）
     * @param apps 应用基本信息列表
     * @param onProgress 进度回调 (当前进度, 总数)
     * @return 包含图标的应用信息列表
     */
    suspend fun loadIconsAsync(
        apps: List<InstalledAppInfo>,
        onProgress: (Int, Int) -> Unit = { _: Int, _: Int -> }
    ): List<InstalledAppInfo> {
        return withContext(Dispatchers.Default) {
            val packageManager = context.packageManager
            val total = apps.size
            var completed = 0
            val lock = Any()

            android.util.Log.d("AppInfoHelper", "开始并行加载 $total 个应用图标")

            // 分批处理，每批10个并行加载
            val result = apps.chunked(10).flatMap { batch ->
                batch.map { appInfo ->
                    async(Dispatchers.Default) {
                        val icon = try {
                            packageManager.getApplicationInfo(appInfo.packageName, 0)
                                .loadIcon(packageManager)
                        } catch (e: Exception) {
                            android.util.Log.w("AppInfoHelper", "加载图标失败: ${appInfo.packageName}")
                            null
                        }

                        // 更新进度
                        synchronized(lock) {
                            completed++
                            if (completed % 10 == 0 || completed == total) {
                                onProgress(completed, total)
                                android.util.Log.d("AppInfoHelper", "图标加载进度: $completed/$total")
                            }
                        }

                        appInfo.copy(icon = icon)
                    }
                }.awaitAll()
            }

            android.util.Log.d("AppInfoHelper", "图标加载完成: ${result.size} 个应用")
            result
        }
    }

    /**
     * 兼容旧方法（已废弃，建议使用 getInstalledUserAppsBasicInfo + loadIconsAsync）
     */
    @Deprecated("使用 getInstalledUserAppsBasicInfo() 和 loadIconsAsync() 代替")
    suspend fun getInstalledUserApps(): List<InstalledAppInfo> {
        return getInstalledUserAppsBasicInfo()
    }

    /**
     * 获取应用名称
     */
    fun getAppName(packageName: String): String {
        val packageManager = context.packageManager
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            getAppName(appInfo, packageManager)
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * 获取应用图标
     */
    fun getAppIcon(packageName: String): Drawable? {
        val packageManager = context.packageManager
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            null
        }
    }

    private fun getAppName(appInfo: ApplicationInfo, packageManager: PackageManager): String {
        return try {
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            appInfo.packageName
        }
    }

    /**
     * 判断是否为用户应用（非系统应用）
     */
    private fun isUserApp(appInfo: ApplicationInfo): Boolean {
        // 排除系统应用
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

        // 排除自己
        val isSelf = appInfo.packageName == context.packageName

        return !isSelf && (!isSystemApp || isUpdatedSystemApp)
    }
}
