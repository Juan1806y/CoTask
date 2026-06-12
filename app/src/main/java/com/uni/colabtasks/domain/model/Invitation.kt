package com.uni.colabtasks.domain.model

/**
 * Invitación a colaborar en una lista. Vive en el árbol del invitado
 * (`/users/{inviteeUid}/invitations/{listId}`) hasta que la acepta o rechaza.
 */
data class Invitation(
    val listId: String,
    val ownerId: String,
    val listName: String,
    val inviterName: String,
    val role: MemberRole,
    val timestamp: Long
)
