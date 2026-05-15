package com.uni.colabtasks.di

import com.uni.colabtasks.data.preferences.PreferencesRepositoryImpl
import com.uni.colabtasks.data.repository.AuthRepositoryImpl
import com.uni.colabtasks.data.repository.TaskListRepositoryImpl
import com.uni.colabtasks.data.repository.TaskRepositoryImpl
import com.uni.colabtasks.data.repository.UserDirectoryRepositoryImpl
import com.uni.colabtasks.domain.repository.AuthRepository
import com.uni.colabtasks.domain.repository.PreferencesRepository
import com.uni.colabtasks.domain.repository.TaskListRepository
import com.uni.colabtasks.domain.repository.TaskRepository
import com.uni.colabtasks.domain.repository.UserDirectoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTaskListRepository(impl: TaskListRepositoryImpl): TaskListRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindUserDirectoryRepository(impl: UserDirectoryRepositoryImpl): UserDirectoryRepository
}
