package com.uni.colabtasks.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.uni.colabtasks.data.remote.dto.CommentDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseComment"

/**
 * Comentarios por tarea en RTDB:
 *   /users/{listOwnerId}/comments/{taskId}/{commentId}
 */
@Singleton
class FirebaseCommentDataSource @Inject constructor(
    private val rootRef: DatabaseReference
) {
    private fun ref(listOwnerId: String, taskId: String): DatabaseReference =
        rootRef.child("users").child(listOwnerId).child("comments").child(taskId)

    suspend fun add(listOwnerId: String, taskId: String, dto: CommentDto) {
        val node = ref(listOwnerId, taskId).push()
        dto.id = node.key.orEmpty()
        node.setValue(dto).await()
    }

    suspend fun delete(listOwnerId: String, taskId: String, commentId: String) {
        ref(listOwnerId, taskId).child(commentId).removeValue().await()
    }

    fun observe(listOwnerId: String, taskId: String): Flow<List<CommentDto>> = callbackFlow {
        val ref = ref(listOwnerId, taskId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children
                    .mapNotNull { it.getValue(CommentDto::class.java) }
                    .sortedBy { it.timestamp }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observe comments cancelled $listOwnerId/$taskId: ${error.message}")
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
