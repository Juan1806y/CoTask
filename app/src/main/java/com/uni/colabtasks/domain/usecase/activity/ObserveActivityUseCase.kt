package com.uni.colabtasks.domain.usecase.activity

import com.uni.colabtasks.domain.model.ActivityEntry
import com.uni.colabtasks.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveActivityUseCase @Inject constructor(
    private val repository: ActivityRepository
) {
    operator fun invoke(listOwnerId: String, listId: String): Flow<List<ActivityEntry>> =
        repository.observeActivity(listOwnerId, listId)
}
