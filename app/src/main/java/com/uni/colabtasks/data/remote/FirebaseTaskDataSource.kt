package com.uni.colabtasks.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.uni.colabtasks.data.remote.dto.TaskDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseTask"

@Singleton
class FirebaseTaskDataSource @Inject constructor(
    private val rootRef: DatabaseReference
) {
    private fun tasksRef(ownerId: String): DatabaseReference =
        rootRef.child("users").child(ownerId).child("tasks")

    fun observeTasks(ownerId: String, listId: String): Flow<List<TaskDto>> = callbackFlow {
        val query: Query = tasksRef(ownerId).orderByChild("listId").equalTo(listId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(TaskDto::class.java) }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                // NO emitimos emptyList() — `syncFromRemote` haría deleteByListExcept(listId, [])
                // que borra TODAS las tareas locales. Solo cerramos.
                Log.w(TAG, "observeTasks cancelled $ownerId/$listId: ${error.message}")
                close()
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun fetchTasks(ownerId: String, listId: String): List<TaskDto> {
        val snapshot = tasksRef(ownerId).orderByChild("listId").equalTo(listId).get().await()
        return snapshot.children.mapNotNull { it.getValue(TaskDto::class.java) }
    }

    suspend fun upsert(dto: TaskDto) {
        tasksRef(dto.ownerId).child(dto.id).setValue(dto).await()
    }

    suspend fun setCompletion(ownerId: String, taskId: String, completed: Boolean, updatedAt: Long) {
        val ref = tasksRef(ownerId).child(taskId)
        // OJO: Firebase serializa `var isCompleted` (Kotlin) como "completed" (sin prefijo `is`).
        // Si escribimos "isCompleted" creamos un campo nuevo y dejamos el original intacto,
        // y al deserializar el setter `setCompleted(...)` recibe el valor viejo → flicker.
        val updates = mapOf<String, Any>(
            "completed" to completed,
            "updatedAt" to updatedAt
        )
        ref.updateChildren(updates).await()
    }

    suspend fun delete(ownerId: String, taskId: String) {
        tasksRef(ownerId).child(taskId).removeValue().await()
    }

    suspend fun deleteForList(ownerId: String, listId: String) {
        val snapshot = tasksRef(ownerId).orderByChild("listId").equalTo(listId).get().await()
        snapshot.children.forEach { it.ref.removeValue().await() }
    }
}
