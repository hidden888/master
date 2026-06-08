package com.iptv.player.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesInfoResponseDto(
    @SerialName("info") val info: SeriesInfoDto? = null,
    // Keyed by season number as a string, e.g. "1" -> [episodes].
    @SerialName("episodes") val episodes: Map<String, List<EpisodeDto>> = emptyMap(),
)

@Serializable
data class SeriesInfoDto(
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("director") val director: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("cover") val cover: String? = null,
)

@Serializable
data class EpisodeDto(
    @SerialName("id") val id: String = "",
    @SerialName("episode_num") @Serializable(StringAsIntSerializer::class) val episodeNum: Int = 0,
    @SerialName("season") @Serializable(StringAsIntSerializer::class) val season: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("info") val info: EpisodeInfoDto? = null,
)

@Serializable
data class EpisodeInfoDto(
    @SerialName("movie_image") val movieImage: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("duration") val duration: String? = null,
)
