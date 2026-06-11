package com.uni.colabtasks.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.uni.colabtasks.data.remote.dto.TaskDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseTask"

/**
 * Acceso a las tareas en RTDB bajo `/users/{ownerId}/tasks/{taskId}`.
 *
 * NOTA de diseño: no usamos `orderByChild("listId").equalTo(...)` porque eso exige
 * declarar `".indexOn": "listId"` en las reglas de seguridad (y crashea si falta).
 * En su lugar leemos todo el nodo de tareas del usuario y filtramos por `listId` en
 * el cliente. Para una app de tareas personal el volumen es pequeño, así que es
 * simple y robusto, sin depender de índices remotos.
 */
@Singleton
class FirebaseTaskDataSource @Inject constructor(
    private val rootRef: DatabaseReference
) {
    private fun tasksRef(ownerId: String): DatabaseReference =
        rootRef.child("users").child(ownerId).child("tasks")

    /**
     * Observa TODAS las tareas de un dueño (`/users/{ownerId}/tasks`). El repositorio
     * reconcilia luego contra las listas conocidas localmente. Un solo listener por dueño
     * (en vez de uno por lista) evita carreras de reconciliación entre listas.
     */
    fun observeOwnerTasks(ownerId: String): Flow<List<TaskDto>> = callbackFlow {
        val ref = tasksRef(ownerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(TaskDto::class.java) }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                // NO emitimos emptyList(): la reconciliación borraría tareas locales válidas.
                Log.w(TAG, "observeOwnerTasks cancelled $ownerId: ${error.message}")
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
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

    /**
     * Borra todas las tareas de una lista. Resiliente: si falla la lectura remota
     * (permisos, red), loguea y no propaga para no tumbar el flujo de borrado de lista.
     */
    suspend fun deleteForList(ownerId: String, listId: String) {
        runCatching {
            val snapshot = tasksRef(ownerId).get().await()
            snapshot.children
                .filter { it.getValue(TaskDto::class.java)?.listId == listId }
                .forEach { it.ref.removeValue().await() }
        }.onFailure { e ->
            Log.w(TAG, "deleteForList failed for $ownerId/$listId: ${e.message}")
        }
    }
}
