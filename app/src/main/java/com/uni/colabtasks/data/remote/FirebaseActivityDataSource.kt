package com.uni.colabtasks.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.uni.colabtasks.data.remote.dto.ActivityDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseActivity"

/**
 * Registro de actividad por lista en RTDB:
 *   /users/{listOwnerId}/activity/{listId}/{activityId}
 */
@Singleton
class FirebaseActivityDataSource @Inject constructor(
    private val rootRef: DatabaseReference
) {
    private fun ref(listOwnerId: String, listId: String): DatabaseReference =
        rootRef.child("users").child(listOwnerId).child("activity").child(listId)

    suspend fun log(listOwnerId: String, listId: String, dto: ActivityDto) {
        val node = ref(listOwnerId, listId).push()
        dto.id = node.key.orEmpty()
        node.setValue(dto).await()
    }

    fun observe(listOwnerId: String, listId: String): Flow<List<ActivityDto>> = callbackFlow {
        val ref = ref(listOwnerId, listId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children
                    .mapNotNull { it.getValue(ActivityDto::class.java) }
                    .sortedByDescending { it.timestamp }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "observe activity cancelled $listOwnerId/$listId: ${error.message}")
                close()
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
