package com.uni.colabtasks.ui.util

import android.content.Context
import android.content.Intent
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskList

/**
 * Construye el texto a compartir de una lista (nombre + descripción + tareas).
 */
fun TaskList.toShareText(tasks: List<Task>): String = buildString {
    appendLine("📋 $name")
    if (!description.isNullOrBlank()) appendLine(description)
    appendLine()
    if (tasks.isEmpty()) {
        appendLine("Sin tareas todavía.")
    } else {
        tasks.forEach { task ->
            val checkbox = if (task.isCompleted) "✅" else "⬜"
            append("$checkbox ${task.title}")
            if (!task.category.isNullOrBlank()) append("  · ${task.category}")
            task.dueDate?.let { append("  · ${formatShortDate(it)}") }
            appendLine()
        }
    }
    if (contributors.isNotEmpty()) {
        appendLine()
        appendLine("Contribuyentes:")
        contributors.forEach { appendLine("  • $it") }
    }
}

/**
 * Lanza el chooser de Android Share con el texto de la lista.
 */
fun Context.shareList(list: TaskList, tasks: List<Task>) {
    val text = list.toShareText(tasks)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, list.name)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, list.name).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(chooser)
}
