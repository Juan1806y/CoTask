package com.uni.colabtasks.data.remote.dto

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class InvitationDto(
    var listId: String = "",
    var ownerId: String = "",
    var listName: String = "",
    var inviterName: String = "",
    var role: String = "EDITOR",
    var timestamp: Long = 0L
)
