package com.iptv.player.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.iptv.player.core.util.SessionManager
import com.iptv.player.core.util.StreamType
import com.iptv.player.core.util.UrlBuilder
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
)

@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    dataSourceFactory: DataSource.Factory,
    private val liveRepository: LiveRepository,
    private val vodRepository: VodRepository,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val type: StreamType =
        runCatching { StreamType.valueOf(savedStateHandle[Screen.Player.ARG_TYPE] ?: "LIVE") }
            .getOrDefault(StreamType.LIVE)
    private var currentId: Int = savedStateHandle[Screen.Player.ARG_ID] ?: 0
    private val ext: String = savedStateHandle[Screen.Player.ARG_EXT] ?: "live"

    private val _uiState = MutableStateFlow(PlayerUiState(isLive = type == StreamType.LIVE))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    /** Live format fallback: prefer HLS, fall back to raw TS on the first failure. */
    private val liveFormats = listOf("m3u8", "ts")
    private var formatIndex = 0

    private var currentBase = ""
    private var currentUser = ""
    private var currentPass = ""

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _uiState.value = _uiState.value.copy(isBuffering = state == Player.STATE_BUFFERING)
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

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 50_000, 2_000, 5_000)
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
        _uiState.value = _uiState.value.copy(showChannelList = !_uiState.value.showChannelList)
    }

    fun hideChannelList() {
        if (_uiState.value.showChannelList) {
            _uiState.value = _uiState.value.copy(showChannelList = false)
        }
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
}
