package com.uni.colabtasks.data.remote.dto

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfileDto(
    var uid: String = "",
    var email: String = "",
    var displayName: String? = null,
    var photoUrl: String? = null,
    var updatedAt: Long = 0L
)
