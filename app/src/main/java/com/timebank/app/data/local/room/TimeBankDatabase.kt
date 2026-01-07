package com.timebank.app.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 时间银行数据库
 */
@Database(
    entities = [
        AppClassificationEntity::class,
        UsageRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TimeBankDatabase : RoomDatabase() {

    abstract fun appClassificationDao(): AppClassificationDao
    abstract fun usageRecordDao(): UsageRecordDao

    companion object {
        const val DATABASE_NAME = "timebank.db"
    }
}
