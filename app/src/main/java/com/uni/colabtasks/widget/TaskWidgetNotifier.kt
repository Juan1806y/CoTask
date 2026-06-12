package com.uni.colabtasks.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refresca el widget de "Mis tareas de hoy" cuando cambian las tareas.
 * No-op seguro si no hay ningún widget colocado en la pantalla.
 */
@Singleton
class TaskWidgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun refresh() {
        runCatching { MyTasksWidget().updateAll(context) }
    }
}
