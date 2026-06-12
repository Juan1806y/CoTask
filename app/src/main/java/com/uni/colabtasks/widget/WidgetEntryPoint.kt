package com.uni.colabtasks.widget

import com.uni.colabtasks.data.local.dao.TaskDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Permite que el widget (que no es un componente Hilt) acceda al DAO de Room
 * vía EntryPointAccessors.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskDao(): TaskDao
}
