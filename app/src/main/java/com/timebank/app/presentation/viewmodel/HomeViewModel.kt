package com.timebank.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timebank.app.domain.repository.ConfigRepository
import com.timebank.app.util.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主页ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    // 当前余额
    val balance: StateFlow<Long> = configRepository.getCurrentBalance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // 权限状态
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    init {
        // 初始检查权限
        refreshPermission()
    }

    /**
     * 刷新权限状态
     */
    fun refreshPermission() {
        viewModelScope.launch {
            _hasPermission.value = permissionManager.hasUsageStatsPermission()
        }
    }

    /**
     * 添加余额（用于测试）
     */
    fun addTestBalance(amount: Long) {
        viewModelScope.launch {
            configRepository.addBalance(amount)
        }
    }

    /**
     * 扣除余额（用于测试）
     */
    fun deductTestBalance(amount: Long) {
        viewModelScope.launch {
            configRepository.deductBalance(amount)
        }
    }
}
