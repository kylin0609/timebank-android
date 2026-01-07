package com.timebank.app.domain.repository

import com.timebank.app.domain.model.AppCategory
import com.timebank.app.domain.model.AppClassification
import kotlinx.coroutines.flow.Flow

/**
 * 应用分类Repository接口
 */
interface AppClassificationRepository {

    fun getAllApps(): Flow<List<AppClassification>>

    fun getAppsByCategory(category: AppCategory): Flow<List<AppClassification>>

    suspend fun getAppByPackageName(packageName: String): AppClassification?

    suspend fun addApp(app: AppClassification)

    suspend fun updateApp(app: AppClassification)

    suspend fun deleteApp(packageName: String)

    suspend fun deleteAll()
}
