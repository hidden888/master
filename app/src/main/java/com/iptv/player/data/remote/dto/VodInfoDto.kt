package com.iptv.player.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VodInfoResponseDto(
    @SerialName("info") val info: VodInfoDto? = null,
    @SerialName("movie_data") val movieData: VodMovieDataDto? = null,
)

@Serializable
data class VodInfoDto(
    @SerialName("plot") val plot: String? = null,
    @SerialName("cast") val cast: String? = null,
    @SerialName("director") val director: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("releasedate") val releaseDate: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("duration") val duration: String? = null,
    @SerialName("movie_image") val movieImage: String? = null,
)

@Serializable
data class VodMovieDataDto(
    @SerialName("stream_id") @Serializable(StringAsIntSerializer::class) val streamId: Int = 0,
    @SerialName("name") val name: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
)
