package com.uni.colabtasks.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.uni.colabtasks.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

data class CountsRow(
    val total: Int,
    val done: Int
)

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY isCompleted ASC, updatedAt DESC")
    fun observeAll(listId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND isCompleted = 0 ORDER BY updatedAt DESC")
    fun observePending(listId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND isCompleted = 1 ORDER BY updatedAt DESC")
    fun observeDone(listId: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT
            COUNT(*) AS total,
            SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) AS done
        FROM tasks WHERE listId = :listId
    """)
    fun observeCounts(listId: String): Flow<CountsRow>

    /**
     * Devuelve TODAS las tareas cuya lista está cacheada en Room.
     * `task_lists` solo contiene listas accesibles para el usuario actual
     * (owned + shared sincronizadas via sharedListPointers), por lo que esto
     * cubre correctamente las tareas de listas compartidas también — sus
     * `ownerId` corresponde al dueño de la lista, no al usuario actual.
     */
    @Query("""
        SELECT t.* FROM tasks t
        INNER JOIN task_lists tl ON t.listId = tl.id
        WHERE t.dueDate IS NOT NULL
    """)
    fun observeAllAccessibleWithDueDate(): Flow<List<TaskEntity>>

    @Query("""
        SELECT t.* FROM tasks t
        INNER JOIN task_lists tl ON t.listId = tl.id
    """)
    fun observeAllAccessible(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(entity: TaskEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TaskEntity>)

    @Query("UPDATE tasks SET isCompleted = :completed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Ids de todas las listas cacheadas (owned + shared) — para filtrar tareas sincronizables. */
    @Query("SELECT id FROM task_lists")
    suspend fun knownListIds(): List<String>

    /**
     * Reconciliación por dueño: borra las tareas de `ownerId` que ya no existen en remoto.
     * Atómico sobre todas las listas del dueño, evitando carreras entre listas.
     */
    @Query("DELETE FROM tasks WHERE ownerId = :ownerId AND id NOT IN (:keepIds)")
    suspend fun deleteForOwnerExcept(ownerId: String, keepIds: List<String>)
}
