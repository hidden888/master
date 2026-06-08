package com.iptv.player.data.repository

import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.CredentialCrypto
import com.iptv.player.core.util.UrlBuilder
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.EpgDao
import com.iptv.player.data.local.entity.EpgProgramEntity
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.data.remote.api.XtreamApiService
import com.iptv.player.data.remote.xmltv.XmltvParser
import com.iptv.player.domain.model.EpgProgram
import com.iptv.player.domain.repository.EpgRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class EpgRepositoryImpl @Inject constructor(
    private val api: XtreamApiService,
    private val accountDao: AccountDao,
    private val epgDao: EpgDao,
    private val crypto: CredentialCrypto,
    private val xmltvParser: XmltvParser,
) : EpgRepository {

    override fun observeCurrentByChannel(accountId: Long): Flow<Map<String, EpgProgram>> =
        // Re-evaluate "now" every minute so the current program advances even without DB changes.
        ticker(60_000)
            .flatMapLatest { now -> epgDao.observeCurrent(accountId, now) }
            .map { programs ->
                programs.associate { it.epgChannelId to it.toDomain() }
            }

    override suspend fun hasEpg(accountId: Long): Boolean = epgDao.count(accountId) > 0

    override suspend fun refreshEpg(accountId: Long): NetworkResult<Unit> {
        val account = accountDao.getById(accountId)
            ?: return NetworkResult.Error(404, "Konto nicht gefunden.")
        val pass = crypto.decrypt(account.password)
        val url = UrlBuilder.xmltv(account.baseUrl)

        return try {
            val response = api.getXmltv(url, account.username, pass)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return NetworkResult.Error(response.code(), "EPG konnte nicht geladen werden.")
            }
            val parsed = body.byteStream().use { xmltvParser.parse(it) }
            val entities = parsed.map {
                EpgProgramEntity(
                    accountId = accountId,
                    epgChannelId = it.channelId,
                    title = it.title,
                    description = it.description,
                    startUtc = it.startUtc,
                    endUtc = it.endUtc,
                )
            }
            epgDao.clear(accountId)
            entities.chunked(500).forEach { epgDao.insertAll(it) }
            epgDao.deleteOlderThan(System.currentTimeMillis() - PRUNE_BEFORE_MS)
            NetworkResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkResult.Exception(t)
        }
    }

    override suspend fun getProgramsInWindow(
        accountId: Long,
        start: Long,
        end: Long,
    ): Map<String, List<EpgProgram>> =
        epgDao.getProgramsInWindow(accountId, start, end)
            .groupBy({ it.epgChannelId }, { it.toDomain() })

    private fun ticker(periodMs: Long): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(periodMs)
        }
    }

    private companion object {
        const val PRUNE_BEFORE_MS = 6L * 60 * 60 * 1000 // keep last 6h of history
    }
}
