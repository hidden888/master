package com.iptv.player.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShortEpgResponseDto(
    @SerialName("epg_listings") val listings: List<EpgListingDto> = emptyList(),
)

/** title/description are base64-encoded in the Xtream short-EPG response. */
@Serializable
data class EpgListingDto(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("start") val start: String? = null,
    @SerialName("end") val end: String? = null,
    @SerialName("start_timestamp") @Serializable(StringAsLongSerializer::class) val startTimestamp: Long = 0,
    @SerialName("stop_timestamp") @Serializable(StringAsLongSerializer::class) val stopTimestamp: Long = 0,
    @SerialName("channel_id") val channelId: String? = null,
)
