package com.timebank.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 时间银行应用程序入口
 * 使用Hilt进行依赖注入
 */
@HiltAndroidApp
class TimeBankApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化应用
    }
}
