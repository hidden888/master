package com.iptv.player.data.repository

import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.AccountType
import com.iptv.player.core.util.CredentialCrypto
import com.iptv.player.core.util.SettingsStore
import com.iptv.player.core.util.StreamType
import com.iptv.player.core.util.UrlBuilder
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.CategoryDao
import com.iptv.player.data.local.dao.ChannelDao
import com.iptv.player.data.local.entity.AccountEntity
import com.iptv.player.data.local.entity.CategoryEntity
import com.iptv.player.data.local.entity.ChannelEntity
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.data.mapper.toEntity
import com.iptv.player.data.remote.api.XtreamApiService
import com.iptv.player.data.remote.m3u.M3uParser
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.NowNext
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.LiveRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LiveRepositoryImpl @Inject constructor(
    private val api: XtreamApiService,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val channelDao: ChannelDao,
    private val crypto: CredentialCrypto,
    private val settingsStore: SettingsStore,
    private val m3uParser: M3uParser,
) : LiveRepository {

    override fun observeCategories(accountId: Long): Flow<List<Category>> =
        categoryDao.observe(accountId, StreamType.LIVE).map { list -> list.map { it.toDomain() } }

    override fun observeChannels(accountId: Long, categoryId: String?): Flow<List<Channel>> {
        val source = if (categoryId == null || categoryId == CATEGORY_ALL) {
            channelDao.observeAll(accountId)
        } else {
            channelDao.observeByCategory(accountId, categoryId)
        }
        return source.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getChannel(accountId: Long, streamId: Int): Channel? =
        channelDao.getById(accountId, streamId)?.toDomain()

    override suspend fun syncLive(accountId: Long): NetworkResult<Unit> {
        val account = accountDao.getById(accountId)
            ?: return NetworkResult.Error(404, "Konto nicht gefunden.")
        return when (account.type) {
            AccountType.M3U -> runCatching { syncM3u(accountId, account) }
                .getOrElse { NetworkResult.Exception(it) }
            AccountType.XTREAM -> syncXtream(accountId, account)
        }
    }

    private suspend fun syncXtream(accountId: Long, account: AccountEntity): NetworkResult<Unit> {
        val pass = crypto.decrypt(account.password)
        val url = UrlBuilder.playerApi(account.baseUrl)

        return try {
            val categoriesResp = api.getLiveCategories(url, account.username, pass)
            val streamsResp = api.getLiveStreams(url, account.username, pass)
            if (!categoriesResp.isSuccessful || !streamsResp.isSuccessful) {
                return NetworkResult.Error(
                    categoriesResp.code(),
                    "Synchronisierung fehlgeschlagen.",
                )
            }
            val categories = categoriesResp.body().orEmpty()
                .mapIndexed { index, dto -> dto.toEntity(accountId, StreamType.LIVE, index) }
            val channels = streamsResp.body().orEmpty().map { it.toEntity(accountId) }

            categoryDao.clear(accountId, StreamType.LIVE)
            categoryDao.upsertAll(categories)
            channelDao.clear(accountId)
            channelDao.upsertAll(channels)
            accountDao.touch(accountId)
            NetworkResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkResult.Exception(t)
        }
    }

    private suspend fun syncM3u(accountId: Long, account: AccountEntity): NetworkResult<Unit> {
        val response = api.getXmltvRaw(account.baseUrl)
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            return NetworkResult.Error(response.code(), "Playlist konnte nicht geladen werden.")
        }
        val text = body.byteStream().bufferedReader().use { it.readText() }
        val playlist = m3uParser.parse(text)
        if (playlist.entries.isEmpty()) {
            return NetworkResult.Error(204, "Playlist enthält keine Sender.")
        }

        val groups = playlist.entries.mapNotNull { it.group?.takeIf { g -> g.isNotBlank() } }.distinct()
        val categories = groups.mapIndexed { index, group ->
            CategoryEntity(accountId, StreamType.LIVE, group, group, 0, index)
        }
        val channels = playlist.entries.mapIndexed { index, entry ->
            ChannelEntity(
                accountId = accountId,
                streamId = index + 1,
                num = index + 1,
                name = entry.name,
                icon = entry.logo,
                epgChannelId = entry.tvgId,
                categoryId = entry.group,
                tvArchive = false,
                streamUrl = entry.url,
            )
        }

        categoryDao.clear(accountId, StreamType.LIVE)
        categoryDao.upsertAll(categories)
        channelDao.clear(accountId)
        channelDao.upsertAll(channels)
        accountDao.touch(accountId)

        // Adopt the playlist's EPG URL if the user hasn't configured one yet.
        val playlistEpg = playlist.epgUrl
        if (!playlistEpg.isNullOrBlank() && settingsStore.settings.first().epgUrl.isBlank()) {
            settingsStore.setEpgUrl(playlistEpg)
        }
        return NetworkResult.Success(Unit)
    }

    override suspend fun getNowNext(accountId: Long, streamId: Int): NowNext {
        val account = accountDao.getById(accountId) ?: return NowNext(null, null)
        val pass = crypto.decrypt(account.password)
        val url = UrlBuilder.playerApi(account.baseUrl)
        return try {
            val response = api.getShortEpg(url, account.username, pass, streamId, limit = 2)
            val offset = settingsStore.settings.first().epgOffsetHours * 3_600_000L
            val now = System.currentTimeMillis()
            val programs = response.body()?.listings?.map { it.toDomain() }.orEmpty()
                .map { if (offset == 0L) it else it.copy(startUtc = it.startUtc + offset, endUtc = it.endUtc + offset) }
                .sortedBy { it.startUtc }
            val current = programs.firstOrNull { now in it.startUtc until it.endUtc }
                ?: programs.firstOrNull()
            val upcoming = programs.firstOrNull { it.startUtc > (current?.startUtc ?: 0) }
            NowNext(current, upcoming)
        } catch (_: Throwable) {
            NowNext(null, null)
        }
    }
}
