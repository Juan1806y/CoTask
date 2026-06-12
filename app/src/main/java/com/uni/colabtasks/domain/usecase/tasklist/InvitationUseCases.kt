package com.uni.colabtasks.domain.usecase.tasklist

import com.uni.colabtasks.domain.model.Invitation
import com.uni.colabtasks.domain.repository.TaskListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveInvitationsUseCase @Inject constructor(
    private val repository: TaskListRepository
) {
    operator fun invoke(uid: String): Flow<List<Invitation>> = repository.observeInvitations(uid)
}

class AcceptInvitationUseCase @Inject constructor(
    private val repository: TaskListRepository
) {
    suspend operator fun invoke(uid: String, invitation: Invitation) =
        repository.acceptInvitation(uid, invitation)
}

class RejectInvitationUseCase @Inject constructor(
    private val repository: TaskListRepository
) {
    suspend operator fun invoke(uid: String, invitation: Invitation) =
        repository.rejectInvitation(uid, invitation)
}
