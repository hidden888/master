package com.iptv.player.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.iptv.player.core.util.AspectMode
import com.iptv.player.core.util.DecoderMode
import com.iptv.player.core.util.SessionManager
import com.iptv.player.core.util.SettingsStore
import com.iptv.player.core.util.StreamFormat
import com.iptv.player.core.util.StreamType
import com.iptv.player.core.util.UrlBuilder
import java.util.Locale
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.repository.AccountRepository
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.LiveRepository
import com.iptv.player.domain.repository.VodRepository
import com.iptv.player.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val title: String = "",
    val channelNumber: Int = 0,
    val isLive: Boolean = true,
    val isBuffering: Boolean = true,
    val error: String? = null,
    val showChannelList: Boolean = false,
    val showOptions: Boolean = false,
    val aspectMode: AspectMode = AspectMode.FIT,
    val playbackSpeed: Float = 1f,
)

/** One selectable entry in the player's track menus. */
data class TrackOption(val key: Int, val label: String, val selected: Boolean)

data class PlayerTracks(
    val audio: List<TrackOption> = emptyList(),
    val text: List<TrackOption> = emptyList(),
    val video: List<TrackOption> = emptyList(),
)

@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    dataSourceFactory: DataSource.Factory,
    private val liveRepository: LiveRepository,
    private val vodRepository: VodRepository,
    private val accountRepository: AccountRepository,
    private val settingsStore: SettingsStore,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val type: StreamType =
        runCatching { StreamType.valueOf(savedStateHandle[Screen.Player.ARG_TYPE] ?: "LIVE") }
            .getOrDefault(StreamType.LIVE)
    private var currentId: Int = savedStateHandle[Screen.Player.ARG_ID] ?: 0
    private val ext: String = savedStateHandle[Screen.Player.ARG_EXT] ?: "live"

    private val settings = settingsStore.snapshot

    private val _uiState = MutableStateFlow(
        PlayerUiState(isLive = type == StreamType.LIVE, aspectMode = settings.aspectMode),
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _tracks = MutableStateFlow(PlayerTracks())
    val tracks: StateFlow<PlayerTracks> = _tracks.asStateFlow()

    /** Flattened (group, indexInGroup) per track type, indexed by the keys exposed in [tracks]. */
    private val audioTracks = mutableListOf<Pair<TrackGroup, Int>>()
    private val textTracks = mutableListOf<Pair<TrackGroup, Int>>()
    private val videoTracks = mutableListOf<Pair<TrackGroup, Int>>()

    /** Live container order, with a fallback to the other format on the first failure. */
    private val liveFormats: List<String> = when (settings.streamFormat) {
        StreamFormat.HLS -> listOf("m3u8")
        StreamFormat.TS -> listOf("ts", "m3u8")
        StreamFormat.AUTO -> listOf("m3u8", "ts")
    }
    private var formatIndex = 0

    private var currentBase = ""
    private var currentUser = ""
    private var currentPass = ""

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _uiState.value = _uiState.value.copy(isBuffering = state == Player.STATE_BUFFERING)
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshTracks(tracks)
        }

        override fun onPlayerError(error: PlaybackException) {
            if (type == StreamType.LIVE && formatIndex < liveFormats.lastIndex) {
                formatIndex++
                play()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Wiedergabe fehlgeschlagen: ${error.errorCodeName}",
                    isBuffering = false,
                )
            }
        }
    }

    /** Prefer software decoders when requested; otherwise keep the platform default order. */
    private val codecSelector = MediaCodecSelector { mime, secure, tunneling ->
        val infos: List<MediaCodecInfo> = MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling)
        if (settings.decoderMode == DecoderMode.SOFTWARE) {
            infos.sortedByDescending { it.softwareOnly }
        } else {
            infos
        }
    }

    private val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(settings.decoderMode != DecoderMode.HARDWARE)
        .setMediaCodecSelector(codecSelector)

    val player: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    settings.bufferProfile.minMs,
                    settings.bufferProfile.maxMs,
                    settings.bufferProfile.playbackMs,
                    settings.bufferProfile.rebufferMs,
                )
                .build(),
        )
        .build()
        .apply {
            playWhenReady = true
            addListener(playerListener)
        }

    init {
        viewModelScope.launch {
            val accountId = sessionManager.activeAccountId.filterNotNull().first()
            val account = accountRepository.getAccount(accountId)
            if (account == null) {
                _uiState.value = _uiState.value.copy(error = "Konto nicht gefunden.", isBuffering = false)
                return@launch
            }
            currentBase = account.baseUrl
            currentUser = account.username
            currentPass = account.password

            if (type == StreamType.LIVE) {
                _channels.value = liveRepository.observeChannels(accountId, CATEGORY_ALL).first()
                updateLiveInfo()
                sessionManager.setLastChannel(currentId.toLong())
            } else if (type == StreamType.VOD) {
                val movie = vodRepository.getMovie(accountId, currentId)
                _uiState.value = _uiState.value.copy(title = movie?.name ?: "")
            }
            play()
        }
    }

    private fun updateLiveInfo() {
        val channel = _channels.value.firstOrNull { it.streamId == currentId }
        _uiState.value = _uiState.value.copy(
            title = channel?.name ?: _uiState.value.title,
            channelNumber = channel?.num ?: _uiState.value.channelNumber,
        )
    }

    private fun play() {
        val url = when (type) {
            StreamType.LIVE -> UrlBuilder.liveUrl(currentBase, currentUser, currentPass, currentId, liveFormats[formatIndex])
            StreamType.VOD -> UrlBuilder.vodUrl(currentBase, currentUser, currentPass, currentId, ext)
            StreamType.SERIES -> UrlBuilder.seriesUrl(currentBase, currentUser, currentPass, currentId, ext)
        }
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    fun tuneTo(streamId: Int) {
        if (type != StreamType.LIVE) return
        if (streamId == currentId && _uiState.value.error == null) {
            _uiState.value = _uiState.value.copy(showChannelList = false)
            return
        }
        currentId = streamId
        formatIndex = 0
        _uiState.value = _uiState.value.copy(error = null, isBuffering = true, showChannelList = false)
        updateLiveInfo()
        viewModelScope.launch { sessionManager.setLastChannel(streamId.toLong()) }
        play()
    }

    fun channelUp() = stepChannel(+1)

    fun channelDown() = stepChannel(-1)

    private fun stepChannel(delta: Int) {
        if (type != StreamType.LIVE) return
        val list = _channels.value
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.streamId == currentId }
        if (index < 0) return
        val next = ((index + delta) % list.size + list.size) % list.size
        tuneTo(list[next].streamId)
    }

    fun toggleChannelList() {
        if (type != StreamType.LIVE) return
        _uiState.value = _uiState.value.copy(showChannelList = !_uiState.value.showChannelList, showOptions = false)
    }

    fun hideChannelList() {
        if (_uiState.value.showChannelList) {
            _uiState.value = _uiState.value.copy(showChannelList = false)
        }
    }

    fun toggleOptions() {
        _uiState.value = _uiState.value.copy(
            showOptions = !_uiState.value.showOptions,
            showChannelList = false,
        )
    }

    fun hideOptions() {
        if (_uiState.value.showOptions) {
            _uiState.value = _uiState.value.copy(showOptions = false)
        }
    }

    fun cycleAspect() {
        val modes = AspectMode.entries
        val next = modes[(modes.indexOf(_uiState.value.aspectMode) + 1) % modes.size]
        _uiState.value = _uiState.value.copy(aspectMode = next)
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun selectAudio(key: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            if (key == AUTO) {
                clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            } else {
                audioTracks.getOrNull(key)?.let { setOverrideForType(TrackSelectionOverride(it.first, it.second)) }
            }
        }.build()
    }

    fun selectText(key: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            when (key) {
                OFF -> setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                else -> {
                    setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    textTracks.getOrNull(key)?.let { setOverrideForType(TrackSelectionOverride(it.first, it.second)) }
                }
            }
        }.build()
    }

    fun selectVideo(key: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            if (key == AUTO) {
                clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            } else {
                videoTracks.getOrNull(key)?.let { setOverrideForType(TrackSelectionOverride(it.first, it.second)) }
            }
        }.build()
    }

    private fun refreshTracks(current: Tracks) {
        audioTracks.clear(); textTracks.clear(); videoTracks.clear()
        val audio = mutableListOf<TrackOption>()
        val text = mutableListOf<TrackOption>()
        val video = mutableListOf<TrackOption>()
        var audioSel = false
        var textSel = false
        var videoSel = false

        for (group in current.groups) {
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val format = group.getTrackFormat(i)
                val selected = group.isTrackSelected(i)
                when (group.type) {
                    C.TRACK_TYPE_AUDIO -> {
                        audioTracks.add(group.mediaTrackGroup to i)
                        if (selected) audioSel = true
                        audio.add(TrackOption(audioTracks.lastIndex, audioLabel(format, audio.size), selected))
                    }
                    C.TRACK_TYPE_TEXT -> {
                        textTracks.add(group.mediaTrackGroup to i)
                        if (selected) textSel = true
                        text.add(TrackOption(textTracks.lastIndex, textLabel(format, text.size), selected))
                    }
                    C.TRACK_TYPE_VIDEO -> {
                        videoTracks.add(group.mediaTrackGroup to i)
                        if (selected) videoSel = true
                        video.add(TrackOption(videoTracks.lastIndex, videoLabel(format), selected))
                    }
                }
            }
        }

        _tracks.value = PlayerTracks(
            audio = if (audio.isEmpty()) emptyList() else listOf(TrackOption(AUTO, "Automatisch", !audioSel)) + audio,
            text = listOf(TrackOption(OFF, "Aus", !textSel)) + text,
            video = if (video.size <= 1) emptyList() else listOf(TrackOption(AUTO, "Auto (beste Qualität)", !videoSel)) + video,
        )
    }

    private fun audioLabel(format: Format, index: Int): String {
        val parts = listOfNotNull(
            format.label,
            format.language?.takeIf { it.isNotBlank() && it != "und" }
                ?.let { runCatching { Locale(it).displayLanguage }.getOrNull()?.ifBlank { it } ?: it },
            format.channelCount.takeIf { it > 0 }?.let { "${it}ch" },
            format.codecs?.substringBefore('.')?.uppercase(),
        ).distinct()
        return parts.joinToString(" · ").ifBlank { "Tonspur ${index + 1}" }
    }

    private fun textLabel(format: Format, index: Int): String {
        val lang = format.language?.takeIf { it.isNotBlank() && it != "und" }
            ?.let { runCatching { Locale(it).displayLanguage }.getOrNull()?.ifBlank { it } ?: it }
        return listOfNotNull(format.label, lang).distinct().joinToString(" · ").ifBlank { "Untertitel ${index + 1}" }
    }

    private fun videoLabel(format: Format): String {
        val parts = listOfNotNull(
            format.height.takeIf { it > 0 }?.let { "${it}p" },
            format.bitrate.takeIf { it > 0 }?.let { "${it / 1000} kbps" },
        )
        return parts.joinToString(" · ").ifBlank { "Video" }
    }

    fun retry() {
        formatIndex = 0
        _uiState.value = _uiState.value.copy(error = null, isBuffering = true)
        play()
    }

    override fun onCleared() {
        player.removeListener(playerListener)
        player.release()
    }

    private companion object {
        const val AUTO = -1
        const val OFF = -2
    }
}
