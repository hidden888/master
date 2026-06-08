package com.iptv.player.domain.model

import com.iptv.player.core.util.StreamType

data class Account(
    val id: Long,
    val name: String,
    val baseUrl: String,
    val username: String,
    val password: String,
)

data class Category(
    val id: String,
    val name: String,
    val type: StreamType,
)

data class Channel(
    val streamId: Int,
    val num: Int,
    val name: String,
    val icon: String?,
    val epgChannelId: String?,
    val categoryId: String?,
    val tvArchive: Boolean,
)

/** Currently airing / upcoming program for a channel. */
data class NowNext(
    val now: EpgProgram?,
    val next: EpgProgram?,
)

data class EpgProgram(
    val title: String,
    val description: String,
    val startUtc: Long,
    val endUtc: Long,
)

data class Movie(
    val streamId: Int,
    val name: String,
    val cover: String?,
    val rating: String?,
    val containerExtension: String?,
    val categoryId: String?,
)

data class MovieDetail(
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val rating: String?,
    val duration: String?,
    val cover: String?,
    val containerExtension: String?,
)

data class Series(
    val seriesId: Int,
    val name: String,
    val cover: String?,
    val plot: String?,
    val genre: String?,
    val categoryId: String?,
)

data class Episode(
    val id: Int,
    val title: String,
    val season: Int,
    val episodeNum: Int,
    val cover: String?,
    val containerExtension: String?,
)

data class SeriesDetail(
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val rating: String?,
    val cover: String?,
    val seasons: List<Int>,
    val episodesBySeason: Map<Int, List<Episode>>,
)
