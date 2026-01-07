package com.timebank.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebank.app.domain.repository.AppClassificationRepository
import com.timebank.app.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val appClassificationRepository: AppClassificationRepository
) : ViewModel() {

    // 兑换比例
    val exchangeRatio: StateFlow<Double> = configRepository.getExchangeRatio()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0
        )

    // 当前余额
    val currentBalance: StateFlow<Long> = configRepository.getCurrentBalance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // 监控服务状态
    val monitorEnabled: StateFlow<Boolean> = configRepository.isMonitorServiceEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // 上次清除日期
    val lastClearDate: StateFlow<Long> = configRepository.getLastClearDate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    /**
     * 检查是否可以清除数据（每周只能清除一次）
     */
    fun canClearData(): Boolean {
        val lastClear = lastClearDate.value
        if (lastClear == 0L) return true

        val currentTime = System.currentTimeMillis()
        val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L // 7天

        return (currentTime - lastClear) >= oneWeekInMillis
    }

    /**
     * 获取距离下次可清除的剩余时间（毫秒）
     */
    fun getTimeUntilNextClear(): Long {
        val lastClear = lastClearDate.value
        if (lastClear == 0L) return 0L

        val currentTime = System.currentTimeMillis()
        val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L
        val nextClearTime = lastClear + oneWeekInMillis

        return (nextClearTime - currentTime).coerceAtLeast(0L)
    }

    /**
     * 设置兑换比例
     */
    fun setExchangeRatio(ratio: Double) {
        viewModelScope.launch {
            configRepository.setExchangeRatio(ratio)
        }
    }

    /**
     * 清除所有数据并重置为默认余额（5分钟）
     * 每周只能执行一次
     */
    fun clearAllDataAndResetToDefault() {
        viewModelScope.launch {
            // 重置余额为 5 分钟（300 秒）
            configRepository.setCurrentBalance(300L)
            // 更新上次清除时间
            configRepository.setLastClearDate(System.currentTimeMillis())
        }
    }

    /**
     * 重置余额
     */
    fun resetBalance() {
        viewModelScope.launch {
            configRepository.setCurrentBalance(0L)
        }
    }

    /**
     * 清除所有数据
     */
    fun clearAllData() {
        viewModelScope.launch {
            // 重置余额
            configRepository.setCurrentBalance(0L)
            // 不清除应用分类，只重置余额
        }
    }

    /**
     * 切换监控服务状态
     */
    fun toggleMonitor(enabled: Boolean) {
        viewModelScope.launch {
            configRepository.setMonitorServiceEnabled(enabled)
        }
    }
}
