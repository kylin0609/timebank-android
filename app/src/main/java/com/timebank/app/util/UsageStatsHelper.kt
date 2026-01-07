package com.timebank.app.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UsageStats工具类
 */
@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val usageStatsManager: UsageStatsManager? by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    /**
     * 获取指定时间范围内的应用使用统计
     */
    fun getUsageStats(startTime: Long, endTime: Long): List<UsageStats> {
        return usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )?.filter {
            // 过滤掉使用时长为0的应用
            it.totalTimeInForeground > 0
        } ?: emptyList()
    }

    /**
     * 获取今日的应用使用统计
     */
    fun getTodayUsageStats(): List<UsageStats> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return getUsageStats(startTime, endTime)
    }

    /**
     * 获取指定日期的应用使用统计
     */
    fun getUsageStatsByDate(date: String): List<UsageStats> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        return try {
            calendar.time = dateFormat.parse(date) ?: return emptyList()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endTime = calendar.timeInMillis

            getUsageStats(startTime, endTime)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取指定应用的使用时长（秒）
     */
    fun getAppUsageTime(packageName: String, startTime: Long, endTime: Long): Long {
        val stats = getUsageStats(startTime, endTime)
        return stats.find { it.packageName == packageName }?.totalTimeInForeground?.div(1000) ?: 0
    }

    /**
     * 获取今日指定应用的使用时长（秒）
     */
    fun getTodayAppUsageTime(packageName: String): Long {
        val stats = getTodayUsageStats()
        return stats.find { it.packageName == packageName }?.totalTimeInForeground?.div(1000) ?: 0
    }

    /**
     * 获取当前前台应用包名
     */
    fun getForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000 // 最近10秒

        val stats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return null

        // 找到最近使用的应用
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}
