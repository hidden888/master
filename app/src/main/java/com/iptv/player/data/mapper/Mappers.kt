package com.iptv.player.data.mapper

import android.util.Base64
import com.iptv.player.core.util.StreamType
import com.iptv.player.data.local.entity.AccountEntity
import com.iptv.player.data.local.entity.CategoryEntity
import com.iptv.player.data.local.entity.ChannelEntity
import com.iptv.player.data.local.entity.EpgProgramEntity
import com.iptv.player.data.local.entity.ProfileEntity
import com.iptv.player.data.local.entity.SeriesEntity
import com.iptv.player.data.local.entity.VodEntity
import com.iptv.player.data.remote.dto.CategoryDto
import com.iptv.player.data.remote.dto.EpgListingDto
import com.iptv.player.data.remote.dto.LiveStreamDto
import com.iptv.player.data.remote.dto.SeriesDto
import com.iptv.player.data.remote.dto.SeriesInfoResponseDto
import com.iptv.player.data.remote.dto.VodInfoResponseDto
import com.iptv.player.data.remote.dto.VodStreamDto
import com.iptv.player.domain.model.Account
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.EpgProgram
import com.iptv.player.domain.model.Episode
import com.iptv.player.domain.model.Movie
import com.iptv.player.domain.model.MovieDetail
import com.iptv.player.domain.model.Profile
import com.iptv.player.domain.model.Series
import com.iptv.player.domain.model.SeriesDetail

fun CategoryDto.toEntity(accountId: Long, type: StreamType, order: Int) = CategoryEntity(
    accountId = accountId,
    type = type,
    categoryId = categoryId,
    name = categoryName,
    parentId = parentId,
    sortOrder = order,
)

fun LiveStreamDto.toEntity(accountId: Long) = ChannelEntity(
    accountId = accountId,
    streamId = streamId,
    num = num,
    name = name,
    icon = streamIcon,
    epgChannelId = epgChannelId,
    categoryId = categoryId,
    tvArchive = tvArchive == 1,
)

fun CategoryEntity.toDomain() = Category(id = categoryId, name = name, type = type)

fun ChannelEntity.toDomain() = Channel(
    streamId = streamId,
    num = num,
    name = name,
    icon = icon,
    epgChannelId = epgChannelId,
    categoryId = categoryId,
    tvArchive = tvArchive,
)

fun ProfileEntity.toDomain() = Profile(
    id = id,
    name = name,
    avatarColor = avatarColor,
    isKids = isKids,
    hasPin = pinHash != null,
)

fun AccountEntity.toDomain(decryptedPassword: String) = Account(
    id = id,
    name = name,
    baseUrl = baseUrl,
    username = username,
    password = decryptedPassword,
)

fun VodStreamDto.toEntity(accountId: Long) = VodEntity(
    accountId = accountId,
    streamId = streamId,
    name = name,
    cover = cover ?: streamIcon,
    rating = rating,
    containerExtension = containerExtension,
    categoryId = categoryId,
)

fun VodEntity.toDomain() = Movie(
    streamId = streamId,
    name = name,
    cover = cover,
    rating = rating,
    containerExtension = containerExtension,
    categoryId = categoryId,
)

fun VodInfoResponseDto.toDomain(fallbackExtension: String?) = MovieDetail(
    plot = info?.plot,
    cast = info?.cast,
    director = info?.director,
    genre = info?.genre,
    rating = info?.rating,
    duration = info?.duration,
    cover = info?.movieImage,
    containerExtension = movieData?.containerExtension ?: fallbackExtension,
)

fun SeriesDto.toEntity(accountId: Long) = SeriesEntity(
    accountId = accountId,
    seriesId = seriesId,
    name = name,
    cover = cover,
    plot = plot,
    genre = genre,
    categoryId = categoryId,
)

fun SeriesEntity.toDomain() = Series(
    seriesId = seriesId,
    name = name,
    cover = cover,
    plot = plot,
    genre = genre,
    categoryId = categoryId,
)

fun SeriesInfoResponseDto.toDomain(fallbackCover: String?): SeriesDetail {
    val episodesBySeason = episodes
        .mapNotNull { (seasonKey, list) ->
            val season = seasonKey.toIntOrNull() ?: return@mapNotNull null
            season to list.map { dto ->
                Episode(
                    id = dto.id.toIntOrNull() ?: 0,
                    title = dto.title.ifBlank { "Folge ${dto.episodeNum}" },
                    season = if (dto.season > 0) dto.season else season,
                    episodeNum = dto.episodeNum,
                    cover = dto.info?.movieImage,
                    containerExtension = dto.containerExtension,
                )
            }.sortedBy { it.episodeNum }
        }
        .toMap()
    return SeriesDetail(
        plot = info?.plot,
        cast = info?.cast,
        director = info?.director,
        genre = info?.genre,
        rating = info?.rating,
        cover = info?.cover ?: fallbackCover,
        seasons = episodesBySeason.keys.sorted(),
        episodesBySeason = episodesBySeason,
    )
}

fun EpgProgramEntity.toDomain(): EpgProgram = EpgProgram(
    title = title,
    description = description,
    startUtc = startUtc,
    endUtc = endUtc,
)

fun EpgListingDto.toDomain(): EpgProgram = EpgProgram(
    title = decodeBase64(title),
    description = decodeBase64(description),
    startUtc = startTimestamp * 1000,
    endUtc = stopTimestamp * 1000,
)

private fun decodeBase64(value: String): String = try {
    if (value.isBlank()) "" else String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)
} catch (_: IllegalArgumentException) {
    value
}
