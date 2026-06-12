package com.uni.colabtasks.data.remote.dto

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class CommentDto(
    var id: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var text: String = "",
    var timestamp: Long = 0L
)
