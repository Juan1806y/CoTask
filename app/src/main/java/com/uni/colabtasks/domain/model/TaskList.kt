package com.uni.colabtasks.domain.model

data class TaskList(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val isFavorite: Boolean = false,
    // Emails introducidos por el usuario, segregados por rol (para mostrar/reinvitar)
    val contributors: List<String> = emptyList(),   // editores
    val viewerEmails: List<String> = emptyList(),    // solo lectura
    // Uids resueltos vía el directorio (para permisos y punteros de compartición)
    val memberIds: List<String> = emptyList(),       // editores
    val viewerIds: List<String> = emptyList(),       // solo lectura
    val createdAt: Long,
    val updatedAt: Long
) {
    /** Rol del usuario `uid` respecto a esta lista. */
    fun roleFor(uid: String?): MemberRole = when {
        uid == null -> MemberRole.VIEWER
        uid == ownerId -> MemberRole.OWNER
        uid in memberIds -> MemberRole.EDITOR
        uid in viewerIds -> MemberRole.VIEWER
        else -> MemberRole.VIEWER
    }

    fun canEditTasks(uid: String?): Boolean =
        roleFor(uid) == MemberRole.OWNER || roleFor(uid) == MemberRole.EDITOR

    fun isOwner(uid: String?): Boolean = uid != null && uid == ownerId
}
