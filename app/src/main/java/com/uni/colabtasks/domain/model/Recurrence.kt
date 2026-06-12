package com.uni.colabtasks.domain.model

import java.util.Calendar

enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY }

/**
 * Avanza una fecha (epoch millis) según la recurrencia. Devuelve null si no hay recurrencia
 * o no había fecha base.
 */
fun nextOccurrence(dueDate: Long?, recurrence: Recurrence): Long? {
    if (dueDate == null || recurrence == Recurrence.NONE) return null
    val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
    when (recurrence) {
        Recurrence.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
        Recurrence.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 7)
        Recurrence.MONTHLY -> cal.add(Calendar.MONTH, 1)
        Recurrence.NONE -> return null
    }
    return cal.timeInMillis
}
