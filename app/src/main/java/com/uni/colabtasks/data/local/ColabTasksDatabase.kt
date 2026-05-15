package com.uni.colabtasks.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.uni.colabtasks.data.local.converters.Converters
import com.uni.colabtasks.data.local.dao.TaskDao
import com.uni.colabtasks.data.local.dao.TaskListDao
import com.uni.colabtasks.data.local.entity.TaskEntity
import com.uni.colabtasks.data.local.entity.TaskListEntity

@Database(
    entities = [TaskListEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ColabTasksDatabase : RoomDatabase() {
    abstract fun taskListDao(): TaskListDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val DB_NAME = "colabtasks.db"
    }
}
