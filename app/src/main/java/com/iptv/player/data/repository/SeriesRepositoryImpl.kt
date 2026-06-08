package com.iptv.player.data.repository

import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.CredentialCrypto
import com.iptv.player.core.util.StreamType
import com.iptv.player.core.util.UrlBuilder
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.CategoryDao
import com.iptv.player.data.local.dao.SeriesDao
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.data.mapper.toEntity
import com.iptv.player.data.remote.api.XtreamApiService
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Series
import com.iptv.player.domain.model.SeriesDetail
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.SeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SeriesRepositoryImpl @Inject constructor(
    private val api: XtreamApiService,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val seriesDao: SeriesDao,
    private val crypto: CredentialCrypto,
) : SeriesRepository {

    override fun observeCategories(accountId: Long): Flow<List<Category>> =
        categoryDao.observe(accountId, StreamType.SERIES).map { list -> list.map { it.toDomain() } }

    override fun observeSeries(accountId: Long, categoryId: String?): Flow<List<Series>> {
        val source = if (categoryId == null || categoryId == CATEGORY_ALL) {
            seriesDao.observeAll(accountId)
        } else {
            seriesDao.observeByCategory(accountId, categoryId)
        }
        return source.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSeries(accountId: Long, seriesId: Int): Series? =
        seriesDao.getById(accountId, seriesId)?.toDomain()

    override suspend fun syncSeries(accountId: Long): NetworkResult<Unit> {
        val account = accountDao.getById(accountId)
            ?: return NetworkResult.Error(404, "Konto nicht gefunden.")
        val pass = crypto.decrypt(account.password)
        val url = UrlBuilder.playerApi(account.baseUrl)

        return try {
            val categoriesResp = api.getSeriesCategories(url, account.username, pass)
            val seriesResp = api.getSeries(url, account.username, pass)
            if (!categoriesResp.isSuccessful || !seriesResp.isSuccessful) {
                return NetworkResult.Error(categoriesResp.code(), "Serien konnten nicht geladen werden.")
            }
            val categories = categoriesResp.body().orEmpty()
                .mapIndexed { index, dto -> dto.toEntity(accountId, StreamType.SERIES, index) }
            val series = seriesResp.body().orEmpty().map { it.toEntity(accountId) }

            categoryDao.clear(accountId, StreamType.SERIES)
            categoryDao.upsertAll(categories)
            seriesDao.clear(accountId)
            seriesDao.upsertAll(series)
            NetworkResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkResult.Exception(t)
        }
    }

    override suspend fun getSeriesDetail(accountId: Long, seriesId: Int): NetworkResult<SeriesDetail> {
        val account = accountDao.getById(accountId)
            ?: return NetworkResult.Error(404, "Konto nicht gefunden.")
        val pass = crypto.decrypt(account.password)
        val url = UrlBuilder.playerApi(account.baseUrl)
        val fallbackCover = seriesDao.getById(accountId, seriesId)?.cover

        return try {
            val response = api.getSeriesInfo(url, account.username, pass, seriesId)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                NetworkResult.Error(response.code(), "Details konnten nicht geladen werden.")
            } else {
                NetworkResult.Success(body.toDomain(fallbackCover))
            }
        } catch (t: Throwable) {
            NetworkResult.Exception(t)
        }
    }
}
