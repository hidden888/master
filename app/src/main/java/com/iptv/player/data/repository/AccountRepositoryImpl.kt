package com.iptv.player.data.repository

import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.CredentialCrypto
import com.iptv.player.core.util.UrlBuilder
import com.iptv.player.data.local.dao.AccountDao
import com.iptv.player.data.local.entity.AccountEntity
import com.iptv.player.data.mapper.toDomain
import com.iptv.player.data.remote.api.XtreamApiService
import com.iptv.player.domain.model.Account
import com.iptv.player.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val api: XtreamApiService,
    private val accountDao: AccountDao,
    private val crypto: CredentialCrypto,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain(crypto.decrypt(it.password)) } }

    override suspend fun getAccount(id: Long): Account? =
        accountDao.getById(id)?.let { it.toDomain(crypto.decrypt(it.password)) }

    override suspend fun hasAccounts(): Boolean = accountDao.count() > 0

    override suspend fun authenticateAndSave(
        name: String,
        baseUrl: String,
        username: String,
        password: String,
    ): NetworkResult<Long> {
        val normalized = UrlBuilder.normalizeBaseUrl(baseUrl)
        val url = UrlBuilder.playerApi(normalized)

        val result = try {
            val response = api.authenticate(url, username, password)
            val body = response.body()
            when {
                !response.isSuccessful ->
                    return NetworkResult.Error(response.code(), "HTTP ${response.code()}")
                body?.userInfo?.auth != 1 ->
                    return NetworkResult.Error(401, "Anmeldedaten ungültig oder Konto inaktiv.")
                body.userInfo?.status.equals("Expired", ignoreCase = true) ->
                    return NetworkResult.Error(403, "Konto abgelaufen.")
                else -> body
            }
        } catch (t: Throwable) {
            return NetworkResult.Exception(t)
        }

        val id = accountDao.upsert(
            AccountEntity(
                name = name.ifBlank { result.userInfo?.username ?: username },
                baseUrl = normalized,
                username = username,
                password = crypto.encrypt(password),
            ),
        )
        return NetworkResult.Success(id)
    }
}
