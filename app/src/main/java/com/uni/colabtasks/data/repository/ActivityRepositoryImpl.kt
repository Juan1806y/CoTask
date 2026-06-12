package com.uni.colabtasks.data.repository

import com.uni.colabtasks.data.remote.FirebaseActivityDataSource
import com.uni.colabtasks.data.remote.dto.ActivityDto
import com.uni.colabtasks.domain.model.ActivityAction
import com.uni.colabtasks.domain.model.ActivityEntry
import com.uni.colabtasks.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseActivityDataSource
) : ActivityRepository {

    override fun observeActivity(listOwnerId: String, listId: String): Flow<List<ActivityEntry>> =
        dataSource.observe(listOwnerId, listId).map { dtos ->
            dtos.map { dto ->
                ActivityEntry(
                    id = dto.id,
                    actorUid = dto.actorUid,
                    actorName = dto.actorName,
                    action = runCatching { ActivityAction.valueOf(dto.action) }
                        .getOrDefault(ActivityAction.EDITED),
                    targetTitle = dto.targetTitle,
                    timestamp = dto.timestamp
                )
            }
        }

    override suspend fun log(
        listOwnerId: String,
        listId: String,
        actorUid: String,
        actorName: String,
        action: ActivityAction,
        targetTitle: String
    ) {
        dataSource.log(
            listOwnerId = listOwnerId,
            listId = listId,
            dto = ActivityDto(
                actorUid = actorUid,
                actorName = actorName,
                action = action.name,
                targetTitle = targetTitle,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
