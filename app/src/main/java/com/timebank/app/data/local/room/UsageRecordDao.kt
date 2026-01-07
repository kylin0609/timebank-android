package com.timebank.app.data.local.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 使用记录DAO
 */
@Dao
interface UsageRecordDao {

    @Query("SELECT * FROM usage_record WHERE date = :date ORDER BY startTime DESC")
    fun getByDate(date: String): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_record WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, startTime DESC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_record WHERE packageName = :packageName ORDER BY date DESC, startTime DESC LIMIT :limit")
    fun getByPackageName(packageName: String, limit: Int = 100): Flow<List<UsageRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: UsageRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<UsageRecordEntity>)

    @Query("DELETE FROM usage_record WHERE date < :beforeDate")
    suspend fun deleteOldRecords(beforeDate: String)

    @Query("DELETE FROM usage_record")
    suspend fun deleteAll()

    @Query("SELECT SUM(duration) FROM usage_record WHERE date = :date AND category = :category")
    suspend fun getTotalDurationByCategory(date: String, category: com.timebank.app.domain.model.AppCategory): Long?
}
