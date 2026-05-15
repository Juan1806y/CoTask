package com.uni.colabtasks.domain.model

/**
 * Categorías sugeridas en el dropdown. El campo `Task.category` sigue siendo `String?` libre,
 * así que el usuario puede introducir cualquier valor; estas son únicamente sugerencias.
 */
enum class TaskCategory(val key: String) {
    PERSONAL("personal"),
    WORK("trabajo"),
    STUDY("estudio"),
    HOME("hogar"),
    HEALTH("salud"),
    OTHER("otros");

    companion object {
        fun matchKey(category: String?): TaskCategory? =
            category?.let { value -> entries.firstOrNull { it.key.equals(value, ignoreCase = true) } }
    }
}
