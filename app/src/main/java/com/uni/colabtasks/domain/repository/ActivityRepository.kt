package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.ActivityAction
import com.uni.colabtasks.domain.model.ActivityEntry
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeActivity(listOwnerId: String, listId: String): Flow<List<ActivityEntry>>
    suspend fun log(
        listOwnerId: String,
        listId: String,
        actorUid: String,
        actorName: String,
        action: ActivityAction,
        targetTitle: String
    )
}
