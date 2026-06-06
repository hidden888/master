package com.iptv.player.data.mapper

import android.util.Base64
import com.iptv.player.core.util.StreamType
import com.iptv.player.data.local.entity.AccountEntity
import com.iptv.player.data.local.entity.CategoryEntity
import com.iptv.player.data.local.entity.ChannelEntity
import com.iptv.player.data.remote.dto.CategoryDto
import com.iptv.player.data.remote.dto.EpgListingDto
import com.iptv.player.data.remote.dto.LiveStreamDto
import com.iptv.player.domain.model.Account
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.EpgProgram

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

fun AccountEntity.toDomain(decryptedPassword: String) = Account(
    id = id,
    name = name,
    baseUrl = baseUrl,
    username = username,
    password = decryptedPassword,
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
