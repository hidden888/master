package com.iptv.player.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    @SerialName("category_id") val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("parent_id") @Serializable(StringAsIntSerializer::class) val parentId: Int = 0,
)

@Serializable
data class LiveStreamDto(
    @SerialName("num") @Serializable(StringAsIntSerializer::class) val num: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("stream_id") @Serializable(StringAsIntSerializer::class) val streamId: Int = 0,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("tv_archive") @Serializable(StringAsIntSerializer::class) val tvArchive: Int = 0,
)

@Serializable
data class VodStreamDto(
    @SerialName("stream_id") @Serializable(StringAsIntSerializer::class) val streamId: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("cover") val cover: String? = null,
    @SerialName("rating") val rating: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
data class SeriesDto(
    @SerialName("series_id") @Serializable(StringAsIntSerializer::class) val seriesId: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("cover") val cover: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)
