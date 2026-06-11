package com.iptv.player.data.repository

import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.CredentialCrypto
import com.iptv.player.core.util.SettingsStore
import com.iptv.player.core.util.UrlBuilder
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.ChannelDao
import com.iptv.player.data.local.dao.EpgDao
import com.iptv.player.data.local.entity.ChannelEntity
import com.iptv.player.data.local.entity.EpgProgramEntity
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.data.remote.api.XtreamApiService
import com.iptv.player.data.remote.xmltv.XmltvParser
import com.iptv.player.domain.model.EpgProgram
import com.iptv.player.domain.repository.EpgRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class EpgRepositoryImpl @Inject constructor(
    private val api: XtreamApiService,
    private val accountDao: AccountDao,
    private val channelDao: ChannelDao,
    private val epgDao: EpgDao,
    private val crypto: CredentialCrypto,
    private val xmltvParser: XmltvParser,
    private val settingsStore: SettingsStore,
) : EpgRepository {

    override fun observeCurrentByChannel(accountId: Long): Flow<Map<String, EpgProgram>> =
        // Re-evaluate "now" every minute so the current program advances even without DB changes.
        combine(ticker(60_000), settingsStore.settings) { now, s -> now to s.epgOffsetHours * 3_600_000L }
            .flatMapLatest { (now, offset) ->
                epgDao.observeCurrent(accountId, now - offset).map { programs ->
                    programs.associate { it.epgChannelId to it.toDomain().shift(offset) }
                }
            }

    private fun EpgProgram.shift(offsetMs: Long): EpgProgram =
        if (offsetMs == 0L) this else copy(startUtc = startUtc + offsetMs, endUtc = endUtc + offsetMs)

    override suspend fun hasEpg(accountId: Long): Boolean = epgDao.count(accountId) > 0

    override suspend fun refreshEpg(accountId: Long): NetworkResult<Unit> {
        val account = accountDao.getById(accountId)
            ?: return NetworkResult.Error(404, "Konto nicht gefunden.")
        val pass = crypto.decrypt(account.password)

        val customEpgUrl = settingsStore.settings.first().epgUrl.trim()

        // 1) Try the full XMLTV dump (custom URL if set, else {base}/xmltv.php).
        val xmltvEntities = runCatching {
            downloadXmltv(accountId, account.baseUrl, account.username, pass, customEpgUrl)
        }.getOrElse { emptyList() }

        val entities = xmltvEntities.ifEmpty {
            // 2) Fallback: many providers serve no usable xmltv.php but do answer get_short_epg
            //    per stream. Pull a few upcoming entries per channel instead.
            runCatching { downloadShortEpg(accountId, account.baseUrl, account.username, pass) }
                .getOrElse { return NetworkResult.Exception(it) }
        }

        if (entities.isEmpty()) {
            return NetworkResult.Error(204, "Anbieter liefert kein Programm.")
        }

        return try {
            epgDao.clear(accountId)
            entities.chunked(500).forEach { epgDao.insertAll(it) }
            epgDao.deleteOlderThan(System.currentTimeMillis() - PRUNE_BEFORE_MS)
            NetworkResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkResult.Exception(t)
        }
    }

    private suspend fun downloadXmltv(
        accountId: Long,
        baseUrl: String,
        username: String,
        pass: String,
        customUrl: String,
    ): List<EpgProgramEntity> {
        val response = if (customUrl.isNotBlank()) {
            api.getXmltvRaw(customUrl)
        } else {
            api.getXmltv(UrlBuilder.xmltv(baseUrl), username, pass)
        }
        val body = response.body() ?: return emptyList()
        if (!response.isSuccessful) return emptyList()
        val parsed = body.byteStream().use { xmltvParser.parse(it) }
        return parsed.map {
            EpgProgramEntity(
                accountId = accountId,
                epgChannelId = it.channelId,
                title = it.title,
                description = it.description,
                startUtc = it.startUtc,
                endUtc = it.endUtc,
            )
        }
    }

    private suspend fun downloadShortEpg(
        accountId: Long,
        baseUrl: String,
        username: String,
        pass: String,
    ): List<EpgProgramEntity> = coroutineScope {
        val url = UrlBuilder.playerApi(baseUrl)
        // Only channels with an EPG id can be matched in the guide; cap the request count.
        val channels = channelDao.observeAll(accountId).first()
            .filter { !it.epgChannelId.isNullOrBlank() }
            .take(MAX_SHORT_EPG_CHANNELS)

        channels.chunked(SHORT_EPG_BATCH).flatMap { batch ->
            batch.map { channel ->
                async { shortEpgForChannel(accountId, url, username, pass, channel) }
            }.awaitAll().flatten()
        }
    }

    private suspend fun shortEpgForChannel(
        accountId: Long,
        playerApiUrl: String,
        username: String,
        pass: String,
        channel: ChannelEntity,
    ): List<EpgProgramEntity> = runCatching {
        val response = api.getShortEpg(playerApiUrl, username, pass, channel.streamId)
        val listings = response.body()?.listings.orEmpty()
        listings.mapNotNull { listing ->
            val program = listing.toDomain()
            if (program.startUtc <= 0L || program.endUtc <= program.startUtc) return@mapNotNull null
            EpgProgramEntity(
                accountId = accountId,
                epgChannelId = channel.epgChannelId ?: return@mapNotNull null,
                title = program.title,
                description = program.description,
                startUtc = program.startUtc,
                endUtc = program.endUtc,
            )
        }
    }.getOrElse { emptyList() }

    override suspend fun getProgramsInWindow(
        accountId: Long,
        start: Long,
        end: Long,
    ): Map<String, List<EpgProgram>> {
        val offset = settingsStore.settings.first().epgOffsetHours * 3_600_000L
        return epgDao.getProgramsInWindow(accountId, start - offset, end - offset)
            .groupBy({ it.epgChannelId }, { it.toDomain().shift(offset) })
    }

    private fun ticker(periodMs: Long): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(periodMs)
        }
    }

    private companion object {
        const val PRUNE_BEFORE_MS = 6L * 60 * 60 * 1000 // keep last 6h of history
        const val MAX_SHORT_EPG_CHANNELS = 200
        const val SHORT_EPG_BATCH = 8
    }
}
