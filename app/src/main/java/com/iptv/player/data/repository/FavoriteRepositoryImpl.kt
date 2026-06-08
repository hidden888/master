package com.iptv.player.data.repository

import com.iptv.player.core.util.StreamType
import com.iptv.player.data.local.dao.FavoriteDao
import com.iptv.player.data.local.entity.FavoriteEntity
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.Movie
import com.iptv.player.domain.model.Series
import com.iptv.player.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
) : FavoriteRepository {

    override fun observeFavoriteIds(accountId: Long, profileId: Long, type: StreamType): Flow<Set<Int>> =
        favoriteDao.observeIds(accountId, profileId, type).map { it.toSet() }

    override fun observeFavoriteChannels(accountId: Long, profileId: Long): Flow<List<Channel>> =
        favoriteDao.observeFavoriteChannels(accountId, profileId).map { list -> list.map { it.toDomain() } }

    override fun observeFavoriteMovies(accountId: Long, profileId: Long): Flow<List<Movie>> =
        favoriteDao.observeFavoriteMovies(accountId, profileId).map { list -> list.map { it.toDomain() } }

    override fun observeFavoriteSeries(accountId: Long, profileId: Long): Flow<List<Series>> =
        favoriteDao.observeFavoriteSeries(accountId, profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun setFavorite(
        accountId: Long,
        profileId: Long,
        type: StreamType,
        itemId: Int,
        favorite: Boolean,
    ) {
        if (favorite) {
            favoriteDao.insert(
                FavoriteEntity(accountId = accountId, profileId = profileId, type = type, itemId = itemId),
            )
        } else {
            favoriteDao.delete(accountId, profileId, type, itemId)
        }
    }
}
