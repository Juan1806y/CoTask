package com.uni.colabtasks.data.remote.dto

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class TaskListDto(
    var id: String = "",
    var ownerId: String = "",
    var name: String = "",
    var description: String? = null,
    var isFavorite: Boolean = false,
    var contributors: List<String> = emptyList(),   // editor emails
    var viewerEmails: List<String> = emptyList(),    // viewer emails
    var memberIds: List<String> = emptyList(),       // editor uids
    var viewerIds: List<String> = emptyList(),       // viewer uids
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)

@IgnoreExtraProperties
data class SharedListPointerDto(
    var listId: String = "",
    var ownerId: String = ""
)
