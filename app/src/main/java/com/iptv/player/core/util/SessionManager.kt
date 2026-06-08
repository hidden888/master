package com.iptv.player.core.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/** Holds the currently selected account / profile so repositories can scope their queries. */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val activeAccountKey = longPreferencesKey("active_account_id")
    private val activeProfileKey = longPreferencesKey("active_profile_id")
    private val lastChannelKey = longPreferencesKey("last_channel_id")

    val activeAccountId: Flow<Long?> = context.sessionDataStore.data
        .map { it[activeAccountKey] }

    val activeProfileId: Flow<Long?> = context.sessionDataStore.data
        .map { it[activeProfileKey] }

    /** Active profile id, falling back to the default profile (0) when none is selected yet. */
    val activeProfileIdOrDefault: Flow<Long> = context.sessionDataStore.data
        .map { it[activeProfileKey] ?: DEFAULT_PROFILE_ID }

    val lastChannelId: Flow<Long?> = context.sessionDataStore.data
        .map { it[lastChannelKey] }

    suspend fun setActiveAccount(id: Long) {
        context.sessionDataStore.edit { it[activeAccountKey] = id }
    }

    suspend fun setActiveProfile(id: Long) {
        context.sessionDataStore.edit { it[activeProfileKey] = id }
    }

    suspend fun setLastChannel(id: Long) {
        context.sessionDataStore.edit { it[lastChannelKey] = id }
    }

    companion object {
        const val DEFAULT_PROFILE_ID = 0L
    }
}
