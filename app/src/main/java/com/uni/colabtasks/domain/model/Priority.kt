package com.uni.colabtasks.domain.model

/**
 * Prioridad de una tarea. Se persiste como `level` (Int) para que el ORDER BY
 * en SQL y las comparaciones sean triviales (mayor nivel = más urgente).
 */
enum class Priority(val level: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    companion object {
        fun fromLevel(level: Int): Priority = entries.firstOrNull { it.level == level } ?: NONE
    }
}
