package com.uni.colabtasks.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.uni.colabtasks.R
import com.uni.colabtasks.domain.model.Priority

/** Color semántico para cada prioridad (independiente del tema). */
fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.NONE -> Color(0xFF9E9E9E)
    Priority.LOW -> Color(0xFF4CAF50)
    Priority.MEDIUM -> Color(0xFFFFA000)
    Priority.HIGH -> Color(0xFFE53935)
}

@Composable
fun priorityLabel(priority: Priority): String = when (priority) {
    Priority.NONE -> stringResource(R.string.priority_none)
    Priority.LOW -> stringResource(R.string.priority_low)
    Priority.MEDIUM -> stringResource(R.string.priority_medium)
    Priority.HIGH -> stringResource(R.string.priority_high)
}
