package com.iptv.player.data.repository

import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.CredentialCrypto
import com.iptv.player.core.util.StreamType
import com.iptv.player.core.util.UrlBuilder
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.dao.CategoryDao
import com.iptv.player.data.local.dao.VodDao
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.data.mapper.toEntity
import com.iptv.player.data.remote.api.XtreamApiService
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Movie
import com.iptv.player.domain.model.MovieDetail
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.VodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VodRepositoryImpl @Inject constructor(
    private val api: XtreamApiService,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val vodDao: VodDao,
    private val crypto: CredentialCrypto,
) : VodRepository {

    override fun observeCategories(accountId: Long): Flow<List<Category>> =
        categoryDao.observe(accountId, StreamType.VOD).map { list -> list.map { it.toDomain() } }

    override fun observeMovies(accountId: Long, categoryId: String?): Flow<List<Movie>> {
        val source = if (categoryId == null || categoryId == CATEGORY_ALL) {
            vodDao.observeAll(accountId)
        } else {
            vodDao.observeByCategory(accountId, categoryId)
        }
        return source.map { list -> list.map { it.toDomain() } }
    }

    override fun observeRecentMovies(accountId: Long, limit: Int): Flow<List<Movie>> =
        vodDao.observeRecent(accountId, limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getMovie(accountId: Long, streamId: Int): Movie? =
        vodDao.getById(accountId, streamId)?.toDomain()

    override suspend fun syncVod(accountId: Long): NetworkResult<Unit> {
        val account = accountDao.getById(accountId)
            ?: return NetworkResult.Error(404, "Konto nicht gefunden.")
        val pass = crypto.decrypt(account.password)
        val url = UrlBuilder.playerApi(account.baseUrl)

        return try {
            val categoriesResp = api.getVodCategories(url, account.username, pass)
            val streamsResp = api.getVodStreams(url, account.username, pass)
            if (!categoriesResp.isSuccessful || !streamsResp.isSuccessful) {
                return NetworkResult.Error(categoriesResp.code(), "Filme konnten nicht geladen werden.")
            }
            val categories = categoriesResp.body().orEmpty()
                .mapIndexed { index, dto -> dto.toEntity(accountId, StreamType.VOD, index) }
            val movies = streamsResp.body().orEmpty().map { it.toEntity(accountId) }

            categoryDao.clear(accountId, StreamType.VOD)
            categoryDao.upsertAll(categories)
            vodDao.clear(accountId)
            vodDao.upsertAll(movies)
            NetworkResult.Success(Unit)
        } catch (t: Throwable) {
            NetworkResult.Exception(t)
        }
    }

    override suspend fun getMovieDetail(accountId: Long, streamId: Int): NetworkResult<MovieDetail> {
        val account = accountDao.getById(accountId)
            ?: return NetworkResult.Error(404, "Konto nicht gefunden.")
        val pass = crypto.decrypt(account.password)
        val url = UrlBuilder.playerApi(account.baseUrl)
        val fallbackExt = vodDao.getById(accountId, streamId)?.containerExtension

        return try {
            val response = api.getVodInfo(url, account.username, pass, streamId)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                NetworkResult.Error(response.code(), "Details konnten nicht geladen werden.")
            } else {
                NetworkResult.Success(body.toDomain(fallbackExt))
            }
        } catch (t: Throwable) {
            NetworkResult.Exception(t)
        }
    }
}
