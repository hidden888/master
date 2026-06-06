package com.iptv.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.iptv.player.core.util.StreamType
import com.iptv.player.data.local.entity.AccountEntity
import com.iptv.player.data.local.entity.CategoryEntity
import com.iptv.player.data.local.entity.ChannelEntity
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
