package com.uni.colabtasks.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.uni.colabtasks.data.local.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {

    @Query("SELECT * FROM task_lists WHERE ownerId = :ownerId ORDER BY isFavorite DESC, updatedAt DESC")
    fun observeByOwner(ownerId: String): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE ownerId = :ownerId AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavoritesByOwner(ownerId: String): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TaskListEntity?

    @Query("SELECT * FROM task_lists WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TaskListEntity?>

    // @Upsert hace INSERT-o-UPDATE in-place. NO usar @Insert(REPLACE): Room implementa
    // REPLACE como DELETE+INSERT, y el DELETE dispara el ON DELETE CASCADE de la FK de tasks,
    // borrando todas las tareas de la lista al re-sincronizarla.
    @Upsert
    suspend fun upsert(entity: TaskListEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TaskListEntity>)

    @Query("UPDATE task_lists SET isFavorite = :favorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, updatedAt: Long)

    @Query("DELETE FROM task_lists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM task_lists WHERE ownerId = :ownerId AND id NOT IN (:keepIds)")
    suspend fun deleteByOwnerExcept(ownerId: String, keepIds: List<String>)
}
