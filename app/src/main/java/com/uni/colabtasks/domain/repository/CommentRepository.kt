package com.uni.colabtasks.domain.repository

import com.uni.colabtasks.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun observeComments(listOwnerId: String, taskId: String): Flow<List<Comment>>
    suspend fun addComment(
        listOwnerId: String,
        taskId: String,
        authorUid: String,
        authorName: String,
        text: String
    )
    suspend fun deleteComment(listOwnerId: String, taskId: String, commentId: String)
}
