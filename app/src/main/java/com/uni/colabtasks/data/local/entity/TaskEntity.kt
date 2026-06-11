package com.uni.colabtasks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index("ownerId")]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val ownerId: String,
    val title: String,
    val description: String?,
    val category: String?,
    val isCompleted: Boolean,
    val dueDate: Long?,
    val priorityLevel: Int = 0,
    val assignedTo: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
