package com.timebank.app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
     * 获取所有已安装的用户应用（排除系统应用）
     * 优化版本：先返回基本信息，图标延迟加载
     */
    suspend fun getInstalledUserApps(): List<InstalledAppInfo> = withContext(Dispatchers.Default) {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        android.util.Log.d("AppInfoHelper", "总共安装的应用: ${packages.size}")

        val userApps = packages.filter { isUserApp(it) }
        android.util.Log.d("AppInfoHelper", "过滤后用户应用: ${userApps.size}")

        // 第一阶段：快速返回基本信息（不加载图标）
        val basicInfo = userApps.map { appInfo ->
            InstalledAppInfo(
                packageName = appInfo.packageName,
                appName = getAppName(appInfo, packageManager),
                icon = null  // 暂时不加载图标
            )
        }.sortedBy { it.appName }

        android.util.Log.d("AppInfoHelper", "基本信息加载完成: ${basicInfo.size} 个应用")

        // 第二阶段：异步加载图标（不阻塞返回）
        val withIcons = basicInfo.map { appInfo ->
            val icon = try {
                appInfo.packageName.let { pkg ->
                    packageManager.getApplicationInfo(pkg, 0).loadIcon(packageManager)
                }
            } catch (e: Exception) {
                null
            }
            appInfo.copy(icon = icon)
        }

        android.util.Log.d("AppInfoHelper", "图标加载完成: ${withIcons.size} 个应用")
        withIcons
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
