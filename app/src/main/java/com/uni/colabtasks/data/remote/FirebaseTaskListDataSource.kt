package com.uni.colabtasks.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.uni.colabtasks.data.remote.dto.SharedListPointerDto
import com.uni.colabtasks.data.remote.dto.TaskListDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseTaskList"

@Singleton
class FirebaseTaskListDataSource @Inject constructor(
    private val rootRef: DatabaseReference
) {
    private fun listsRef(ownerId: String): DatabaseReference =
        rootRef.child("users").child(ownerId).child("lists")

    private fun sharedPointersRef(memberUid: String): DatabaseReference =
        rootRef.child("users").child(memberUid).child("sharedListPointers")

    fun observeLists(ownerId: String): Flow<List<TaskListDto>> = callbackFlow {
        val ref = listsRef(ownerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(TaskListDto::class.java) }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                // IMPORTANTE: NO emitimos emptyList() porque eso provocaría que el sync
                // limpie Room (deleteByOwnerExcept con lista vacía borra todo). Solo cerramos.
                Log.w(TAG, "observeLists cancelled for $ownerId: ${error.message}")
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun fetchLists(ownerId: String): List<TaskListDto> {
        val snapshot = listsRef(ownerId).get().await()
        return snapshot.children.mapNotNull { it.getValue(TaskListDto::class.java) }
    }

    suspend fun fetchList(ownerId: String, listId: String): TaskListDto? {
        val snapshot = listsRef(ownerId).child(listId).get().await()
        return snapshot.getValue(TaskListDto::class.java)
    }

    fun observeList(ownerId: String, listId: String): Flow<TaskListDto?> = callbackFlow {
        val ref = listsRef(ownerId).child(listId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(TaskListDto::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                // Emit null para no bloquear combine(...) en combineLatestPerPointer
                Log.w(TAG, "observeList cancelled $ownerId/$listId: ${error.message}")
                trySend(null)
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun upsert(dto: TaskListDto) {
        listsRef(dto.ownerId).child(dto.id).setValue(dto).await()
    }

    suspend fun delete(ownerId: String, listId: String) {
        listsRef(ownerId).child(listId).removeValue().await()
    }

    // ---------- Shared list pointers ----------

    /**
     * Observa los punteros a listas compartidas con el usuario `memberUid`.
     * Cada puntero indica en qué tree de RTDB vive la lista original (`ownerId`).
     */
    fun observeSharedPointers(memberUid: String): Flow<List<SharedListPointerDto>> = callbackFlow {
        val ref = sharedPointersRef(memberUid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    val ownerId = child.child("ownerId").getValue(String::class.java)
                    val listId = child.key
                    if (ownerId.isNullOrEmpty() || listId.isNullOrEmpty()) null
                    else SharedListPointerDto(listId = listId, ownerId = ownerId)
                }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observeSharedPointers cancelled for $memberUid: ${error.message}")
                // Empty es seguro aquí — no toca Room directamente; lo consume flatMapLatest.
                trySend(emptyList())
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addSharedPointer(memberUid: String, listId: String, ownerId: String) {
        sharedPointersRef(memberUid).child(listId)
            .setValue(mapOf("ownerId" to ownerId)).await()
    }

    suspend fun removeSharedPointer(memberUid: String, listId: String) {
        sharedPointersRef(memberUid).child(listId).removeValue().await()
    }
}
