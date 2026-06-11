package com.iptv.player.core.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Live-TV stream container the player should request. */
enum class StreamFormat(val label: String) {
    AUTO("Automatisch (HLS, dann TS)"),
    HLS("HLS (.m3u8)"),
    TS("MPEG-TS (.ts)"),
}

/** How the channel list is ordered. */
enum class ChannelSort(val label: String) {
    DEFAULT("Anbieter-Reihenfolge"),
    NUMBER("Nach Sendernummer"),
    NAME("Alphabetisch (A–Z)"),
}

/** Video scaling inside the player surface. */
enum class AspectMode(val label: String) {
    FIT("Anpassen (Original-Seitenverhältnis)"),
    FILL("Ausfüllen (verzerrt ggf.)"),
    ZOOM("Zoom (Ränder abschneiden)"),
}

/** ExoPlayer buffering profile. */
enum class BufferProfile(
    val label: String,
    val minMs: Int,
    val maxMs: Int,
    val playbackMs: Int,
    val rebufferMs: Int,
) {
    SMALL("Klein – schneller Start", 5_000, 15_000, 1_500, 3_000),
    NORMAL("Normal", 15_000, 50_000, 2_000, 5_000),
    LARGE("Groß – für instabile Netze", 30_000, 120_000, 3_000, 8_000),
}

/** Codec/decoder preference for the player. */
enum class DecoderMode(val label: String) {
    HARDWARE("Hardware (empfohlen)"),
    HW_FALLBACK("Hardware + Software-Fallback"),
    SOFTWARE("Software bevorzugen"),
}

data class AppSettings(
    /** Custom XMLTV EPG URL; blank means use {base}/xmltv.php. */
    val epgUrl: String = "",
    val streamFormat: StreamFormat = StreamFormat.AUTO,
    val channelSort: ChannelSort = ChannelSort.DEFAULT,
    val aspectMode: AspectMode = AspectMode.FIT,
    val bufferProfile: BufferProfile = BufferProfile.NORMAL,
    val decoderMode: DecoderMode = DecoderMode.HW_FALLBACK,
    val showChannelLogos: Boolean = true,
    val showChannelNumbers: Boolean = true,
    /** When on, hidden channels stay visible (so they can be un-hidden). */
    val showHiddenChannels: Boolean = false,
)

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { it.toAppSettings() }

    /** Synchronous snapshot for non-suspend call sites (e.g. building the ExoPlayer). */
    @Volatile
    var snapshot: AppSettings = AppSettings()
        private set

    init {
        // Prime the snapshot synchronously so the first player build sees stored values,
        // then keep it current for the rest of the process lifetime.
        runCatching {
            runBlocking { snapshot = context.settingsDataStore.data.first().toAppSettings() }
        }
        scope.launch {
            context.settingsDataStore.data.map { it.toAppSettings() }.collect { snapshot = it }
        }
    }

    suspend fun setEpgUrl(value: String) = put(Keys.EPG_URL, value.trim())
    suspend fun setStreamFormat(value: StreamFormat) = put(Keys.STREAM_FORMAT, value.name)
    suspend fun setChannelSort(value: ChannelSort) = put(Keys.CHANNEL_SORT, value.name)
    suspend fun setAspectMode(value: AspectMode) = put(Keys.ASPECT_MODE, value.name)
    suspend fun setBufferProfile(value: BufferProfile) = put(Keys.BUFFER_PROFILE, value.name)
    suspend fun setDecoderMode(value: DecoderMode) = put(Keys.DECODER_MODE, value.name)

    suspend fun setShowChannelLogos(value: Boolean) = putBool(Keys.SHOW_LOGOS, value)
    suspend fun setShowChannelNumbers(value: Boolean) = putBool(Keys.SHOW_NUMBERS, value)
    suspend fun setShowHiddenChannels(value: Boolean) = putBool(Keys.SHOW_HIDDEN, value)

    /** Hidden channel stream ids for a given account. */
    fun hiddenChannels(accountId: Long): Flow<Set<Int>> = context.settingsDataStore.data.map { prefs ->
        prefs[hiddenKey(accountId)]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun setChannelHidden(accountId: Long, streamId: Int, hidden: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val key = hiddenKey(accountId)
            val current = prefs[key]?.toMutableSet() ?: mutableSetOf()
            if (hidden) current.add(streamId.toString()) else current.remove(streamId.toString())
            prefs[key] = current
        }
    }

    private suspend fun put(key: Preferences.Key<String>, value: String) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private suspend fun putBool(key: Preferences.Key<Boolean>, value: Boolean) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private fun hiddenKey(accountId: Long) = stringSetPreferencesKey("hidden_channels_$accountId")

    private fun Preferences.toAppSettings() = AppSettings(
        epgUrl = this[Keys.EPG_URL] ?: "",
        streamFormat = enumOrDefault(this[Keys.STREAM_FORMAT], StreamFormat.AUTO),
        channelSort = enumOrDefault(this[Keys.CHANNEL_SORT], ChannelSort.DEFAULT),
        aspectMode = enumOrDefault(this[Keys.ASPECT_MODE], AspectMode.FIT),
        bufferProfile = enumOrDefault(this[Keys.BUFFER_PROFILE], BufferProfile.NORMAL),
        decoderMode = enumOrDefault(this[Keys.DECODER_MODE], DecoderMode.HW_FALLBACK),
        showChannelLogos = this[Keys.SHOW_LOGOS] ?: true,
        showChannelNumbers = this[Keys.SHOW_NUMBERS] ?: true,
        showHiddenChannels = this[Keys.SHOW_HIDDEN] ?: false,
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private object Keys {
        val EPG_URL = stringPreferencesKey("epg_url")
        val STREAM_FORMAT = stringPreferencesKey("stream_format")
        val CHANNEL_SORT = stringPreferencesKey("channel_sort")
        val ASPECT_MODE = stringPreferencesKey("aspect_mode")
        val BUFFER_PROFILE = stringPreferencesKey("buffer_profile")
        val DECODER_MODE = stringPreferencesKey("decoder_mode")
        val SHOW_LOGOS = booleanPreferencesKey("show_logos")
        val SHOW_NUMBERS = booleanPreferencesKey("show_numbers")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
    }
}
