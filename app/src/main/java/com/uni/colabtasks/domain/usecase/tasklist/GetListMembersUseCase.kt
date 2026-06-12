package com.uni.colabtasks.domain.usecase.tasklist

import com.uni.colabtasks.domain.model.ListMember
import com.uni.colabtasks.domain.model.TaskList
import com.uni.colabtasks.domain.repository.TaskListRepository
import javax.inject.Inject

class GetListMembersUseCase @Inject constructor(
    private val repository: TaskListRepository
) {
    suspend operator fun invoke(list: TaskList): List<ListMember> = repository.getListMembers(list)
}
