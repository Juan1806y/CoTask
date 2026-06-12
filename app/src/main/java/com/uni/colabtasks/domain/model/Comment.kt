package com.uni.colabtasks.domain.model

data class Comment(
    val id: String,
    val authorUid: String,
    val authorName: String,
    val text: String,
    val timestamp: Long
)
