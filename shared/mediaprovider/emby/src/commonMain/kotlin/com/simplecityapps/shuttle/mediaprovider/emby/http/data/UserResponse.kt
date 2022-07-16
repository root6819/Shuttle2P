package com.simplecityapps.shuttle.mediaprovider.emby.http.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String
)