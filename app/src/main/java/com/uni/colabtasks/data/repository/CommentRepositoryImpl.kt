package com.uni.colabtasks.data.repository

import com.uni.colabtasks.data.remote.FirebaseCommentDataSource
import com.uni.colabtasks.data.remote.dto.CommentDto
import com.uni.colabtasks.domain.model.Comment
import com.uni.colabtasks.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseCommentDataSource
) : CommentRepository {

    override fun observeComments(listOwnerId: String, taskId: String): Flow<List<Comment>> =
        dataSource.observe(listOwnerId, taskId).map { dtos ->
            dtos.map { Comment(it.id, it.authorUid, it.authorName, it.text, it.timestamp) }
        }

    override suspend fun addComment(
        listOwnerId: String,
        taskId: String,
        authorUid: String,
        authorName: String,
        text: String
    ) {
        dataSource.add(
            listOwnerId = listOwnerId,
            taskId = taskId,
            dto = CommentDto(
                authorUid = authorUid,
                authorName = authorName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteComment(listOwnerId: String, taskId: String, commentId: String) {
        dataSource.delete(listOwnerId, taskId, commentId)
    }
}
