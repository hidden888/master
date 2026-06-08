package com.iptv.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iptv.player.core.util.StreamType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val username: String,
    /** Stored encrypted via CredentialCrypto. */
    val password: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "categories",
    primaryKeys = ["accountId", "type", "categoryId"],
    indices = [Index("accountId", "type")],
)
data class CategoryEntity(
    val accountId: Long,
    val type: StreamType,
    val categoryId: String,
    val name: String,
    val parentId: Int = 0,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "channels",
    primaryKeys = ["accountId", "streamId"],
    indices = [Index("accountId", "categoryId")],
)
data class ChannelEntity(
    val accountId: Long,
    val streamId: Int,
    val num: Int,
    val name: String,
    val icon: String? = null,
    val epgChannelId: String? = null,
    val categoryId: String? = null,
    val tvArchive: Boolean = false,
)

@Entity(
    tableName = "vod",
    primaryKeys = ["accountId", "streamId"],
    indices = [Index("accountId", "categoryId")],
)
data class VodEntity(
    val accountId: Long,
    val streamId: Int,
    val name: String,
    val cover: String? = null,
    val rating: String? = null,
    val containerExtension: String? = null,
    val categoryId: String? = null,
)

@Entity(
    tableName = "series",
    primaryKeys = ["accountId", "seriesId"],
    indices = [Index("accountId", "categoryId")],
)
data class SeriesEntity(
    val accountId: Long,
    val seriesId: Int,
    val name: String,
    val cover: String? = null,
    val plot: String? = null,
    val genre: String? = null,
    val categoryId: String? = null,
)

@Entity(
    tableName = "epg_programs",
    indices = [Index("accountId", "epgChannelId", "startUtc")],
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val epgChannelId: String,
    val title: String,
    val description: String,
    val startUtc: Long,
    val endUtc: Long,
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB color used for the avatar tile. */
    val avatarColor: Long,
    val isKids: Boolean = false,
    /** Salted hash ("salt:hash") for the parental PIN, or null when the profile is unlocked. */
    val pinHash: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["accountId", "profileId", "type", "itemId"],
    indices = [Index("accountId", "profileId", "type")],
)
data class FavoriteEntity(
    val accountId: Long,
    val profileId: Long,
    val type: StreamType,
    /** streamId for LIVE/VOD, seriesId for SERIES. */
    val itemId: Int,
    val addedAt: Long = System.currentTimeMillis(),
)
