package com.uni.colabtasks.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.uni.colabtasks.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agenda/cancela recordatorios locales para tareas con fecha límite usando WorkManager.
 * El trabajo se identifica de forma única por `reminder_{taskId}` para poder reemplazarlo
 * o cancelarlo de forma idempotente.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun schedule(task: Task) {
        val dueDate = task.dueDate
        // Solo agendamos si hay fecha futura y la tarea no está completada.
        if (dueDate == null || task.isCompleted) {
            cancel(task.id)
            return
        }
        val delay = dueDate - System.currentTimeMillis()
        if (delay <= 0) {
            // Fecha ya pasada: no agendamos nada (evita notificación inmediata molesta).
            cancel(task.id)
            return
        }

        val data = Data.Builder()
            .putString(ReminderWorker.KEY_TASK_ID, task.id)
            .putString(ReminderWorker.KEY_TASK_TITLE, task.title)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(
            workName(task.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(taskId: String) {
        workManager.cancelUniqueWork(workName(taskId))
    }

    private fun workName(taskId: String) = "reminder_$taskId"
}
