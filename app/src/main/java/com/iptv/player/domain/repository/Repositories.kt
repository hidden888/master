package com.iptv.player.domain.repository

import com.iptv.player.core.network.NetworkResult
import com.iptv.player.domain.model.Account
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.NowNext
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun getAccount(id: Long): Account?
    suspend fun hasAccounts(): Boolean

    /** Authenticates against the Xtream server and, on success, persists the account. Returns its id. */
    suspend fun authenticateAndSave(
        name: String,
        baseUrl: String,
        username: String,
        password: String,
    ): NetworkResult<Long>
}

interface LiveRepository {
    fun observeCategories(accountId: Long): Flow<List<Category>>
    fun observeChannels(accountId: Long, categoryId: String?): Flow<List<Channel>>
    suspend fun getChannel(accountId: Long, streamId: Int): Channel?

    /** Fetches live categories + channels from the network and caches them in Room. */
    suspend fun syncLive(accountId: Long): NetworkResult<Unit>

    suspend fun getNowNext(accountId: Long, streamId: Int): NowNext
}

const val CATEGORY_ALL = "__all__"
