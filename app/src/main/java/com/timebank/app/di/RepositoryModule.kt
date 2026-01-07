package com.timebank.app.di

import com.timebank.app.data.repository.AppClassificationRepositoryImpl
import com.timebank.app.data.repository.ConfigRepositoryImpl
import com.timebank.app.domain.repository.AppClassificationRepository
import com.timebank.app.domain.repository.ConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppClassificationRepository(
        impl: AppClassificationRepositoryImpl
    ): AppClassificationRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(
        impl: ConfigRepositoryImpl
    ): ConfigRepository
}
