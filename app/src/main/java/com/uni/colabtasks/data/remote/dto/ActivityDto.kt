package com.uni.colabtasks.data.remote.dto

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ActivityDto(
    var id: String = "",
    var actorUid: String = "",
    var actorName: String = "",
    var action: String = "",
    var targetTitle: String = "",
    var timestamp: Long = 0L
)
