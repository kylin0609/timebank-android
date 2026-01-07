package com.timebank.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 配置Repository接口
 */
interface ConfigRepository {

    fun getExchangeRatio(): Flow<Double>

    suspend fun setExchangeRatio(ratio: Double)

    fun getCurrentBalance(): Flow<Long>

    suspend fun setCurrentBalance(balance: Long)

    suspend fun addBalance(amount: Long)

    suspend fun deductBalance(amount: Long): Boolean

    fun isFirstLaunch(): Flow<Boolean>

    suspend fun setFirstLaunch(isFirst: Boolean)

    fun isMonitorServiceEnabled(): Flow<Boolean>

    suspend fun setMonitorServiceEnabled(enabled: Boolean)

    fun getLastResetDate(): Flow<Long>

    suspend fun setLastResetDate(timestamp: Long)

    fun getLastClearDate(): Flow<Long>

    suspend fun setLastClearDate(timestamp: Long)
}
