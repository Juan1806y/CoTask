package com.uni.colabtasks.data.remote.dto

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class TaskDto(
    var id: String = "",
    var listId: String = "",
    var ownerId: String = "",
    var title: String = "",
    var description: String? = null,
    var category: String? = null,
    var isCompleted: Boolean = false,
    var dueDate: Long? = null,
    var priorityLevel: Int = 0,
    var assignedTo: String? = null,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)
