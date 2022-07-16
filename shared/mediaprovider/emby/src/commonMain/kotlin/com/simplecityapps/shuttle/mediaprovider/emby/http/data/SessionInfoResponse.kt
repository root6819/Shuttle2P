package com.simplecityapps.shuttle.mediaprovider.emby.http.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionInfoResponse(
    @SerialName("DeviceId") val deviceId: String
)