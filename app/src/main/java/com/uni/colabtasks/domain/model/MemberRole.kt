package com.uni.colabtasks.domain.model

/**
 * Rol de un usuario respecto a una lista:
 * - OWNER: control total (editar/borrar lista, gestionar miembros, todas las tareas)
 * - EDITOR: puede gestionar tareas, pero no la lista ni los miembros
 * - VIEWER: solo lectura
 */
enum class MemberRole { OWNER, EDITOR, VIEWER }

/** Miembro resuelto de una lista (para mostrar nombre/avatar y asignar tareas). */
data class ListMember(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val role: MemberRole
) {
    /** Nombre visible: displayName si existe, si no la parte local del email. */
    val label: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')
            ?: uid.take(6)
}
