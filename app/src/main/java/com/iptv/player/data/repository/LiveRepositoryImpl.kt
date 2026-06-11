package com.iptv.player.data.repository

import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.CredentialCrypto
import com.iptv.player.core.util.SettingsStore
import com.iptv.player.core.util.StreamType
import com.iptv.player.core.util.UrlBuilder
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.CategoryDao
import com.iptv.player.data.local.dao.ChannelDao
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.data.mapper.toEntity
import com.iptv.player.data.remote.api.XtreamApiService
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
