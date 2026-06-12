package com.uni.colabtasks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_lists",
    indices = [Index("ownerId")]
)
data class TaskListEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val description: String?,
    val isFavorite: Boolean,
    val contributors: List<String>,
    val viewerEmails: List<String>,
    val memberIds: List<String>,
    val viewerIds: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)
