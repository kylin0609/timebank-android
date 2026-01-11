package com.timebank.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.timebank.app.data.local.datastore.PreferenceKeys
import com.timebank.app.domain.repository.ConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "timebank_prefs")

/**
 * 配置Repository实现
 */
@Singleton
class ConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ConfigRepository {

    override fun getExchangeRatio(): Flow<Double> {
        return context.dataStore.data.map { prefs ->
            prefs[PreferenceKeys.EXCHANGE_RATIO] ?: 1.0
        }
    }

    override suspend fun setExchangeRatio(ratio: Double) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.EXCHANGE_RATIO] = ratio
        }
    }

    override fun getCurrentBalance(): Flow<Long> {
        return context.dataStore.data.map { prefs ->
            prefs[PreferenceKeys.CURRENT_BALANCE] ?: 60L // 默认 1 分钟
        }
    }

    override suspend fun setCurrentBalance(balance: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.CURRENT_BALANCE] = balance.coerceAtLeast(0)
        }
    }

    override suspend fun addBalance(amount: Long) {
        val current = getCurrentBalance().first()
        setCurrentBalance(current + amount)
    }

    override suspend fun deductBalance(amount: Long): Boolean {
        val current = getCurrentBalance().first()
        return if (current >= amount) {
            setCurrentBalance(current - amount)
            true
        } else {
            false
        }
    }

    override fun isFirstLaunch(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[PreferenceKeys.IS_FIRST_LAUNCH] ?: true
        }
    }

    override suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.IS_FIRST_LAUNCH] = isFirst
        }
    }

    override fun isMonitorServiceEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[PreferenceKeys.MONITOR_SERVICE_ENABLED] ?: true
        }
    }

    override suspend fun setMonitorServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.MONITOR_SERVICE_ENABLED] = enabled
        }
    }

    override fun getLastResetDate(): Flow<Long> {
        return context.dataStore.data.map { prefs ->
            prefs[PreferenceKeys.LAST_RESET_DATE] ?: 0L
        }
    }

    override suspend fun setLastResetDate(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_RESET_DATE] = timestamp
        }
    }

    override fun getLastClearDate(): Flow<Long> {
        return context.dataStore.data.map { prefs ->
            prefs[PreferenceKeys.LAST_CLEAR_DATE] ?: 0L
        }
    }

    override suspend fun setLastClearDate(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_CLEAR_DATE] = timestamp
        }
    }
}
