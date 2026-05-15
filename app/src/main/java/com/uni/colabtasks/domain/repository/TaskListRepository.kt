package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface TaskListRepository {
    fun observeLists(ownerId: String): Flow<List<TaskList>>
    fun observeFavorites(ownerId: String): Flow<List<TaskList>>
    fun observeList(id: String): Flow<TaskList?>
    suspend fun getList(id: String): TaskList?
    suspend fun createList(
        ownerId: String,
        name: String,
        description: String?,
        contributors: List<String>
    ): String
    suspend fun updateList(
        id: String,
        name: String,
        description: String?,
        contributors: List<String>
    )
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun deleteList(id: String)
    suspend fun syncFromRemote(ownerId: String)
}
