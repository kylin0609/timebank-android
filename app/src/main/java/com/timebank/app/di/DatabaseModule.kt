package com.timebank.app.di

import android.content.Context
import androidx.room.Room
import com.timebank.app.data.local.room.AppClassificationDao
import com.timebank.app.data.local.room.TimeBankDatabase
import com.timebank.app.data.local.room.UsageRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TimeBankDatabase {
        return Room.databaseBuilder(
            context,
            TimeBankDatabase::class.java,
            TimeBankDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAppClassificationDao(database: TimeBankDatabase): AppClassificationDao {
        return database.appClassificationDao()
    }

    @Provides
    fun provideUsageRecordDao(database: TimeBankDatabase): UsageRecordDao {
        return database.usageRecordDao()
    }
}
