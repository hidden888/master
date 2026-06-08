package com.iptv.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.iptv.player.core.util.StreamType
import com.iptv.player.data.local.entity.AccountEntity
import com.iptv.player.data.local.entity.CategoryEntity
import com.iptv.player.data.local.entity.ChannelEntity
import com.iptv.player.data.local.entity.EpgProgramEntity
import com.iptv.player.data.local.entity.FavoriteEntity
import com.iptv.player.data.local.entity.SeriesEntity
import com.iptv.player.data.local.entity.VodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Upsert
    suspend fun upsert(account: AccountEntity): Long

    @Query("SELECT * FROM accounts ORDER BY lastUsed DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("UPDATE accounts SET lastUsed = :ts WHERE id = :id")
    suspend fun touch(id: Long, ts: Long = System.currentTimeMillis())
}

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE accountId = :accountId AND type = :type ORDER BY sortOrder, name")
    fun observe(accountId: Long, type: StreamType): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories WHERE accountId = :accountId AND type = :type")
    suspend fun clear(accountId: Long, type: StreamType)
}

@Dao
interface ChannelDao {
    @Upsert
    suspend fun upsertAll(channels: List<ChannelEntity>)

    @Query("SELECT * FROM channels WHERE accountId = :accountId ORDER BY num")
    fun observeAll(accountId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND categoryId = :categoryId ORDER BY num")
    fun observeByCategory(accountId: Long, categoryId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId AND streamId = :streamId")
    suspend fun getById(accountId: Long, streamId: Int): ChannelEntity?

    @Query("DELETE FROM channels WHERE accountId = :accountId")
    suspend fun clear(accountId: Long)
}

@Dao
interface VodDao {
    @Upsert
    suspend fun upsertAll(movies: List<VodEntity>)

    @Query("SELECT * FROM vod WHERE accountId = :accountId ORDER BY name")
    fun observeAll(accountId: Long): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod WHERE accountId = :accountId AND categoryId = :categoryId ORDER BY name")
    fun observeByCategory(accountId: Long, categoryId: String): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod WHERE accountId = :accountId AND streamId = :streamId")
    suspend fun getById(accountId: Long, streamId: Int): VodEntity?

    @Query("SELECT COUNT(*) FROM vod WHERE accountId = :accountId")
    suspend fun count(accountId: Long): Int

    @Query("DELETE FROM vod WHERE accountId = :accountId")
    suspend fun clear(accountId: Long)
}

@Dao
interface SeriesDao {
    @Upsert
    suspend fun upsertAll(series: List<SeriesEntity>)

    @Query("SELECT * FROM series WHERE accountId = :accountId ORDER BY name")
    fun observeAll(accountId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE accountId = :accountId AND categoryId = :categoryId ORDER BY name")
    fun observeByCategory(accountId: Long, categoryId: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE accountId = :accountId AND seriesId = :seriesId")
    suspend fun getById(accountId: Long, seriesId: Int): SeriesEntity?

    @Query("SELECT COUNT(*) FROM series WHERE accountId = :accountId")
    suspend fun count(accountId: Long): Int

    @Query("DELETE FROM series WHERE accountId = :accountId")
    suspend fun clear(accountId: Long)
}

@Dao
interface EpgDao {
    @Insert
    suspend fun insertAll(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE accountId = :accountId")
    suspend fun clear(accountId: Long)

    @Query("DELETE FROM epg_programs WHERE endUtc < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM epg_programs WHERE accountId = :accountId")
    suspend fun count(accountId: Long): Int

    /** All programs currently on air for the account (one per channel). */
    @Query(
        "SELECT * FROM epg_programs WHERE accountId = :accountId " +
            "AND startUtc <= :now AND endUtc > :now",
    )
    fun observeCurrent(accountId: Long, now: Long): Flow<List<EpgProgramEntity>>

    /** Now + next for a single channel. */
    @Query(
        "SELECT * FROM epg_programs WHERE accountId = :accountId " +
            "AND epgChannelId = :channelId AND endUtc > :now ORDER BY startUtc LIMIT 2",
    )
    suspend fun getNowNext(accountId: Long, channelId: String, now: Long): List<EpgProgramEntity>

    /** Programs overlapping a time window, for the guide grid. */
    @Query(
        "SELECT * FROM epg_programs WHERE accountId = :accountId " +
            "AND endUtc > :start AND startUtc < :end ORDER BY epgChannelId, startUtc",
    )
    suspend fun getProgramsInWindow(accountId: Long, start: Long, end: Long): List<EpgProgramEntity>
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query(
        "DELETE FROM favorites WHERE accountId = :accountId AND profileId = :profileId " +
            "AND type = :type AND itemId = :itemId",
    )
    suspend fun delete(accountId: Long, profileId: Long, type: StreamType, itemId: Int)

    /** Favorited item ids for one content type, to drive the star toggles. */
    @Query(
        "SELECT itemId FROM favorites WHERE accountId = :accountId " +
            "AND profileId = :profileId AND type = :type",
    )
    fun observeIds(accountId: Long, profileId: Long, type: StreamType): Flow<List<Int>>

    @Query(
        "SELECT c.* FROM channels c INNER JOIN favorites f " +
            "ON f.accountId = c.accountId AND f.itemId = c.streamId AND f.type = 'LIVE' " +
            "WHERE f.accountId = :accountId AND f.profileId = :profileId ORDER BY c.num",
    )
    fun observeFavoriteChannels(accountId: Long, profileId: Long): Flow<List<ChannelEntity>>

    @Query(
        "SELECT v.* FROM vod v INNER JOIN favorites f " +
            "ON f.accountId = v.accountId AND f.itemId = v.streamId AND f.type = 'VOD' " +
            "WHERE f.accountId = :accountId AND f.profileId = :profileId ORDER BY v.name",
    )
    fun observeFavoriteMovies(accountId: Long, profileId: Long): Flow<List<VodEntity>>

    @Query(
        "SELECT s.* FROM series s INNER JOIN favorites f " +
            "ON f.accountId = s.accountId AND f.itemId = s.seriesId AND f.type = 'SERIES' " +
            "WHERE f.accountId = :accountId AND f.profileId = :profileId ORDER BY s.name",
    )
    fun observeFavoriteSeries(accountId: Long, profileId: Long): Flow<List<SeriesEntity>>
}
