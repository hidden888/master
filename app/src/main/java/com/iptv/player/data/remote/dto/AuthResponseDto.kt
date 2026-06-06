package com.iptv.player.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    @SerialName("user_info") val userInfo: UserInfoDto? = null,
    @SerialName("server_info") val serverInfo: ServerInfoDto? = null,
)

@Serializable
data class UserInfoDto(
    @SerialName("username") val username: String? = null,
    @SerialName("auth") val auth: Int = 0,
    @SerialName("status") val status: String? = null,
    @SerialName("exp_date") val expDate: String? = null,
    @SerialName("is_trial") val isTrial: String? = null,
    @SerialName("active_cons") val activeConnections: String? = null,
    @SerialName("max_connections") val maxConnections: String? = null,
)

@Serializable
data class ServerInfoDto(
    @SerialName("url") val url: String? = null,
    @SerialName("port") val port: String? = null,
    @SerialName("https_port") val httpsPort: String? = null,
    @SerialName("server_protocol") val protocol: String? = null,
)
