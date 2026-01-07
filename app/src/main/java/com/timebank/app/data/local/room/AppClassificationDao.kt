package com.timebank.app.data.local.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 应用分类DAO
 */
@Dao
interface AppClassificationDao {

    @Query("SELECT * FROM app_classification")
    fun getAllFlow(): Flow<List<AppClassificationEntity>>

    @Query("SELECT * FROM app_classification WHERE category = :category")
    fun getByCategory(category: com.timebank.app.domain.model.AppCategory): Flow<List<AppClassificationEntity>>

    @Query("SELECT * FROM app_classification WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): AppClassificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppClassificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppClassificationEntity>)

    @Update
    suspend fun update(app: AppClassificationEntity)

    @Delete
    suspend fun delete(app: AppClassificationEntity)

    @Query("DELETE FROM app_classification WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM app_classification")
    suspend fun deleteAll()
}
