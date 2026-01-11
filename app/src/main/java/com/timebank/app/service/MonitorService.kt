package com.timebank.app.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.timebank.app.BlockActivity
import com.timebank.app.MainActivity
import com.timebank.app.R
import com.timebank.app.domain.model.AppCategory
import com.timebank.app.domain.repository.AppClassificationRepository
import com.timebank.app.domain.repository.ConfigRepository
import com.timebank.app.util.UsageStatsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 应用使用监控服务
 * 后台监控应用使用情况，计算时间余额
 */
@AndroidEntryPoint
class MonitorService : Service() {

    @Inject
    lateinit var usageStatsHelper: UsageStatsHelper

    @Inject
    lateinit var appClassificationRepository: AppClassificationRepository

    @Inject
    lateinit var configRepository: ConfigRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    // 上次检查的应用和时间戳
    private var lastApp: String? = null
    private var lastCheckTime: Long = 0

    // 防重复弹窗标志：记录上次显示拦截界面的时间
    private var lastBlockTime: Long = 0

    // 负向应用使用时长追踪（秒）
    private var negativeAppUsageSeconds: Long = 0
    private var lastNegativeApp: String? = null

    // 负向应用上次提示时间（用于每分钟提示）
    private var lastNegativeAppReminderTime: Long = 0

    // 屏幕关闭标志
    private var wasScreenOff = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val REMINDER_NOTIFICATION_ID = 1002  // 气泡提醒通知 ID
        private const val CHANNEL_ID = "timebank_monitor"
        private const val REMINDER_CHANNEL_ID = "timebank_reminder"  // 气泡提醒通知渠道
        private const val CHECK_INTERVAL = 100L // 每0.1秒检查一次（100毫秒）
        private const val BLOCK_COOLDOWN = 30000L // 拦截界面冷却时间：30秒内不重复弹窗
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("MonitorService", "服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("MonitorService", "服务启动")

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())

        // 开始监控
        startMonitoring()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("MonitorService", "服务销毁")
        stopMonitoring()
        serviceScope.cancel()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // 前台服务通知渠道
            val channel = NotificationChannel(
                CHANNEL_ID,
                "时间银行监控服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台监控应用使用时长"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)

            // 气泡提醒通知渠道（高优先级，显示在顶部）
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "使用时长提醒",
                NotificationManager.IMPORTANCE_HIGH  // 高优先级，会显示横幅通知
            ).apply {
                description = "负向应用使用时长提醒"
                setShowBadge(false)
                enableVibration(false)  // 不震动
                setSound(null, null)     // 不播放声音
            }
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("时间银行正在运行")
            .setContentText("正在监控应用使用时长")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * 开始监控
     */
    private fun startMonitoring() {
        if (monitorJob?.isActive == true) {
            android.util.Log.d("MonitorService", "监控已在运行")
            return
        }

        lastCheckTime = System.currentTimeMillis()

        monitorJob = serviceScope.launch {
            // 启动时检查是否需要每日重置
            checkDailyReset()

            while (isActive) {
                try {
                    checkCurrentApp()
                    delay(CHECK_INTERVAL)
                } catch (e: Exception) {
                    android.util.Log.e("MonitorService", "监控出错: ${e.message}", e)
                }
            }
        }
    }

    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * 检查是否需要每日重置
     */
    private suspend fun checkDailyReset() {
        val currentTime = System.currentTimeMillis()
        val lastResetDate = configRepository.getLastResetDate().first()

        // 获取今天的开始时间（0点）
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        // 如果上次重置时间是昨天或更早，执行重置
        if (lastResetDate < todayStart) {
            android.util.Log.d("MonitorService", "检测到新的一天，执行每日重置")
            configRepository.setCurrentBalance(60L) // 重置为默认的 1 分钟 (60秒)
            configRepository.setLastResetDate(currentTime)
            android.util.Log.d("MonitorService", "每日重置完成，余额重置为 60 秒")
        }
    }

    /**
     * 检查当前前台应用
     */
    private suspend fun checkCurrentApp() {
        val currentApp = usageStatsHelper.getForegroundApp()
        val currentTime = System.currentTimeMillis()

        // 检测屏幕是否关闭（锁屏或黑屏）
        val powerManager = getSystemService(PowerManager::class.java)
        val isScreenOn = powerManager?.isInteractive ?: true
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val isLocked = keyguardManager?.isDeviceLocked ?: false

        // 如果屏幕关闭或锁屏，不计算时间
        if (!isScreenOn || isLocked) {
            if (!wasScreenOff) {
                android.util.Log.d("MonitorService", "检测到屏幕关闭/锁屏，暂停时间计算")
                wasScreenOff = true
            }
            // 重置检查时间，避免恢复时计算累积时间
            lastCheckTime = currentTime
            return
        }

        // 从锁屏恢复
        if (wasScreenOff) {
            android.util.Log.d("MonitorService", "屏幕恢复，重新开始计算")
            wasScreenOff = false
            lastCheckTime = currentTime
            return
        }

        if (currentApp != null && currentApp != packageName) {
            // 如果是新应用
            if (currentApp != lastApp) {
                android.util.Log.d("MonitorService", "检测到切换到新应用: $currentApp")

                // 检查是否是负向应用，如果是且余额不足则立即拦截
                val classification = appClassificationRepository.getAppByPackageName(currentApp)
                android.util.Log.d("MonitorService", "应用分类: ${classification?.appName} - ${classification?.category}")

                if (classification != null && classification.category == AppCategory.NEGATIVE) {
                    val currentBalance = configRepository.getCurrentBalance().first()
                    android.util.Log.d("MonitorService", "负向应用，当前余额: $currentBalance")

                    // 切换应用时重置负向应用计数
                    if (currentApp != lastNegativeApp) {
                        lastNegativeApp = currentApp
                        negativeAppUsageSeconds = 0
                        lastNegativeAppReminderTime = currentTime
                    }

                    if (currentBalance <= 0) {
                        // 检查是否在冷却期内，避免重复弹窗
                        if (currentTime - lastBlockTime < BLOCK_COOLDOWN) {
                            android.util.Log.d("MonitorService", "拦截界面冷却期内，跳过弹窗")
                            return
                        }

                        android.util.Log.d("MonitorService", "余额不足，立即拦截负向应用: ${classification.appName}")
                        launchBlockActivity(classification.appName, currentApp)
                        lastBlockTime = currentTime
                        // 不更新lastApp和lastCheckTime，下次检查时仍会认为是新应用并再次拦截
                        return
                    }
                }

                lastApp = currentApp
                lastCheckTime = currentTime
            } else {
                // 同一个应用，计算使用时长
                val duration = (currentTime - lastCheckTime) / 1000 // 秒
                if (duration >= 1) { // 至少使用了1秒
                    android.util.Log.d("MonitorService", "同一应用 $currentApp 使用了 $duration 秒")
                    handleAppUsage(currentApp, duration, currentTime)
                    lastCheckTime = currentTime
                }
            }
        }
    }

    /**
     * 处理应用使用情况
     */
    private suspend fun handleAppUsage(packageName: String, duration: Long, currentTime: Long) {
        // 获取应用分类
        val classification = appClassificationRepository.getAppByPackageName(packageName)

        if (classification != null) {
            val exchangeRatio = configRepository.getExchangeRatio().first()

            when (classification.category) {
                AppCategory.POSITIVE -> {
                    // 正向应用：增加余额
                    val earned = (duration * exchangeRatio).toLong()
                    configRepository.addBalance(earned)
                    android.util.Log.d("MonitorService", "正向应用 ${classification.appName} 使用 ${duration}秒，获得 ${earned}秒")

                    // 更新通知
                    updateNotification("使用正向应用，已获得 ${earned}秒")
                }

                AppCategory.NEGATIVE -> {
                    // 负向应用：先检查余额是否充足
                    val currentBalance = configRepository.getCurrentBalance().first()
                    val cost = duration

                    // 追踪负向应用使用时长
                    if (packageName == lastNegativeApp) {
                        negativeAppUsageSeconds += duration
                        android.util.Log.d("MonitorService", "负向应用累计使用: ${negativeAppUsageSeconds}秒，距上次提示: ${currentTime - lastNegativeAppReminderTime}ms")

                        // 每5秒（5000毫秒）显示一次气泡提醒（测试用）
                        if (currentTime - lastNegativeAppReminderTime >= 5000) {
                            showBubbleReminder(negativeAppUsageSeconds)
                            lastNegativeAppReminderTime = currentTime
                            android.util.Log.d("MonitorService", "已显示气泡提醒，累计使用: ${negativeAppUsageSeconds}秒")
                        }
                    } else {
                        // 切换到新的负向应用，重置计数器
                        lastNegativeApp = packageName
                        negativeAppUsageSeconds = duration
                        lastNegativeAppReminderTime = currentTime
                    }

                    if (currentBalance <= 0) {
                        // 检查是否在冷却期内，避免重复弹窗
                        if (currentTime - lastBlockTime < BLOCK_COOLDOWN) {
                            android.util.Log.d("MonitorService", "拦截界面冷却期内，跳过弹窗")
                            return
                        }

                        // 余额不足或为0，立即拦截
                        android.util.Log.d("MonitorService", "余额不足(当前: ${currentBalance})，拦截负向应用: ${classification.appName}")
                        launchBlockActivity(classification.appName, packageName)
                        lastBlockTime = currentTime
                        // 重置负向应用计数器
                        negativeAppUsageSeconds = 0
                        lastNegativeApp = null
                        lastNegativeAppReminderTime = 0
                    } else {
                        // 尝试扣除余额
                        val success = configRepository.deductBalance(cost)

                        if (success) {
                            android.util.Log.d("MonitorService", "负向应用 ${classification.appName} 使用 ${duration}秒，扣除 ${cost}秒")
                            updateNotification("使用负向应用，已扣除 ${cost}秒")
                        } else {
                            // 检查是否在冷却期内，避免重复弹窗
                            if (currentTime - lastBlockTime < BLOCK_COOLDOWN) {
                                android.util.Log.d("MonitorService", "拦截界面冷却期内，跳过弹窗")
                                return
                            }

                            // 扣除失败（余额变为负），拦截
                            android.util.Log.d("MonitorService", "余额不足，拦截负向应用: ${classification.appName}")
                            launchBlockActivity(classification.appName, packageName)
                            lastBlockTime = currentTime
                            // 重置负向应用计数器
                            negativeAppUsageSeconds = 0
                            lastNegativeApp = null
                            lastNegativeAppReminderTime = 0
                        }
                    }
                }

                AppCategory.NONE -> {
                    // 未分类应用，不处理
                }
            }
        }
    }

    /**
     * 更新通知内容
     */
    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("时间银行正在运行")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 显示气泡提醒
     * 每5秒在负向应用上显示已使用时间（测试用）
     * 使用 Notification 实现，保证完全不阻挡触摸
     */
    private fun showBubbleReminder(usedSeconds: Long) {
        val usedMinutes = (usedSeconds / 60).toInt()

        android.util.Log.d("MonitorService", "显示气泡提醒: 已连续使用 ${usedSeconds}秒 (${usedMinutes}分钟)")

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post {
            try {
                val notificationManager = getSystemService(NotificationManager::class.java)

                // 构建提醒消息
                val message = if (usedSeconds < 60) {
                    "已连续使用 ${usedSeconds}秒"
                } else {
                    "已连续使用 ${usedMinutes}分钟"
                }

                // 创建通知
                val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("⏱️ 使用时长提醒")
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setAutoCancel(true)
                    .setTimeoutAfter(2000)
                    .setOnlyAlertOnce(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(0)
                    .build()

                // 显示通知
                notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)

                // 2秒后自动取消通知
                handler.postDelayed({
                    notificationManager.cancel(REMINDER_NOTIFICATION_ID)
                }, 2000)

            } catch (e: Exception) {
                android.util.Log.e("MonitorService", "通知显示失败: ${e.message}", e)
            }
        }
    }

    /**
     * 启动拦截界面
     */
    private fun launchBlockActivity(appName: String, packageName: String) {
        val intent = Intent(this, BlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("app_name", appName)
            putExtra("package_name", packageName)
        }
        startActivity(intent)
    }
}
