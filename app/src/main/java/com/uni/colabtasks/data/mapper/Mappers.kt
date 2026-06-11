package com.uni.colabtasks.data.mapper

import com.uni.colabtasks.data.local.entity.TaskEntity
import com.uni.colabtasks.data.local.entity.TaskListEntity
import com.uni.colabtasks.data.remote.dto.TaskDto
import com.uni.colabtasks.data.remote.dto.TaskListDto
import com.uni.colabtasks.domain.model.Priority
import com.uni.colabtasks.domain.model.Task
import com.uni.colabtasks.domain.model.TaskList

// ---- TaskList ----
fun TaskListEntity.toDomain() = TaskList(
    id = id,
    ownerId = ownerId,
    name = name,
    description = description,
    isFavorite = isFavorite,
    contributors = contributors,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskList.toEntity() = TaskListEntity(
    id = id,
    ownerId = ownerId,
    name = name,
    description = description,
    isFavorite = isFavorite,
    contributors = contributors,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskList.toDto(memberIds: List<String> = emptyList()) = TaskListDto(
    id = id,
    ownerId = ownerId,
    name = name,
    description = description,
    isFavorite = isFavorite,
    contributors = contributors,
    memberIds = memberIds,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskListDto.toEntity() = TaskListEntity(
    id = id,
    ownerId = ownerId,
    name = name,
    description = description,
    isFavorite = isFavorite,
    contributors = contributors,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ---- Task ----
fun TaskEntity.toDomain() = Task(
    id = id,
    listId = listId,
    ownerId = ownerId,
    title = title,
    description = description,
    category = category,
    isCompleted = isCompleted,
    dueDate = dueDate,
    priority = Priority.fromLevel(priorityLevel),
    assignedTo = assignedTo,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Task.toEntity() = TaskEntity(
    id = id,
    listId = listId,
    ownerId = ownerId,
    title = title,
    description = description,
    category = category,
    isCompleted = isCompleted,
    dueDate = dueDate,
    priorityLevel = priority.level,
    assignedTo = assignedTo,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Task.toDto() = TaskDto(
    id = id,
    listId = listId,
    ownerId = ownerId,
    title = title,
    description = description,
    category = category,
    isCompleted = isCompleted,
    dueDate = dueDate,
    priorityLevel = priority.level,
    assignedTo = assignedTo,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TaskDto.toEntity() = TaskEntity(
    id = id,
    listId = listId,
    ownerId = ownerId,
    title = title,
    description = description,
    category = category,
    isCompleted = isCompleted,
    dueDate = dueDate,
    priorityLevel = priorityLevel,
    assignedTo = assignedTo,
    createdAt = createdAt,
    updatedAt = updatedAt
)
