package com.uni.colabtasks.domain.model

enum class ActivityAction { CREATED, COMPLETED, REOPENED, DELETED, EDITED, ASSIGNED }

data class ActivityEntry(
    val id: String,
    val actorUid: String,
    val actorName: String,
    val action: ActivityAction,
    val targetTitle: String,
    val timestamp: Long
)
