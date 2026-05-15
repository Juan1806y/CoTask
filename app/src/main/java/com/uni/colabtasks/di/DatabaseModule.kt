package com.uni.colabtasks.di

import android.content.Context
import androidx.room.Room
import com.uni.colabtasks.data.local.ColabTasksDatabase
import com.uni.colabtasks.data.local.dao.TaskDao
import com.uni.colabtasks.data.local.dao.TaskListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ColabTasksDatabase =
        Room.databaseBuilder(
            context,
            ColabTasksDatabase::class.java,
            ColabTasksDatabase.DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTaskListDao(database: ColabTasksDatabase): TaskListDao = database.taskListDao()

    @Provides
    fun provideTaskDao(database: ColabTasksDatabase): TaskDao = database.taskDao()
}
