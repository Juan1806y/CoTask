package com.uni.colabtasks.domain.model

data class TaskList(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val isFavorite: Boolean = false,
    val contributors: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)
