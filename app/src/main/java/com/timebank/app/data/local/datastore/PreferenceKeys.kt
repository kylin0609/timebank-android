package com.timebank.app.data.local.datastore

import androidx.datastore.preferences.core.*

/**
 * DataStore配置键
 */
object PreferenceKeys {
    val EXCHANGE_RATIO = doublePreferencesKey("exchange_ratio")
    val RESET_DAY_OF_WEEK = intPreferencesKey("reset_day_of_week")
    val RESET_HOUR = intPreferencesKey("reset_hour")
    val RESET_MINUTE = intPreferencesKey("reset_minute")
    val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
    val MONITOR_SERVICE_ENABLED = booleanPreferencesKey("monitor_enabled")
    val CURRENT_BALANCE = longPreferencesKey("current_balance")
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val MANUAL_RESET_COUNT = intPreferencesKey("manual_reset_count")
    val LAST_RESET_WEEK = intPreferencesKey("last_reset_week")
    val LAST_RESET_DATE = longPreferencesKey("last_reset_date")
    val LAST_CLEAR_DATE = longPreferencesKey("last_clear_date")
}
