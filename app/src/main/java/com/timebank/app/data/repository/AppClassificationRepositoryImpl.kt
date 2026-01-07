package com.timebank.app.data.repository

import com.timebank.app.data.local.room.AppClassificationDao
import com.timebank.app.data.local.room.AppClassificationEntity
import com.timebank.app.domain.model.AppCategory
import com.timebank.app.domain.model.AppClassification
import com.timebank.app.domain.repository.AppClassificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用分类Repository实现
 */
@Singleton
class AppClassificationRepositoryImpl @Inject constructor(
    private val dao: AppClassificationDao
) : AppClassificationRepository {

    override fun getAllApps(): Flow<List<AppClassification>> {
        return dao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAppsByCategory(category: AppCategory): Flow<List<AppClassification>> {
        return dao.getByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAppByPackageName(packageName: String): AppClassification? {
        return dao.getByPackageName(packageName)?.toDomain()
    }

    override suspend fun addApp(app: AppClassification) {
        dao.insert(app.toEntity())
    }

    override suspend fun updateApp(app: AppClassification) {
        dao.update(app.toEntity())
    }

    override suspend fun deleteApp(packageName: String) {
        dao.deleteByPackageName(packageName)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun AppClassificationEntity.toDomain() = AppClassification(
        packageName = packageName,
        appName = appName,
        category = category,
        iconPath = iconPath,
        addedTime = addedTime,
        updatedTime = updatedTime
    )

    private fun AppClassification.toEntity() = AppClassificationEntity(
        packageName = packageName,
        appName = appName,
        category = category,
        iconPath = iconPath,
        addedTime = addedTime,
        updatedTime = updatedTime
    )
}
