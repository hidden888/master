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
