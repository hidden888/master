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
import com.iptv.player.core.util.UrlBuilder
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.repository.AccountRepository
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.LiveRepository
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
    val channelName: String = "",
    val channelNumber: Int = 0,
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
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var currentStreamId: Int = savedStateHandle[Screen.Player.ARG_STREAM_ID] ?: 0

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    /** Live format fallback: prefer HLS, fall back to raw TS on the first failure. */
    private val formats = listOf("m3u8", "ts")
    private var formatIndex = 0

    private var currentBase = ""
    private var currentUser = ""
    private var currentPass = ""

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _uiState.value = _uiState.value.copy(isBuffering = state == Player.STATE_BUFFERING)
        }

        override fun onPlayerError(error: PlaybackException) {
            if (formatIndex < formats.lastIndex) {
                formatIndex++
                playCurrentFormat()
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
            _channels.value = liveRepository.observeChannels(accountId, CATEGORY_ALL).first()
            updateChannelInfo()
            sessionManager.setLastChannel(currentStreamId.toLong())
            playCurrentFormat()
        }
    }

    private fun updateChannelInfo() {
        val channel = _channels.value.firstOrNull { it.streamId == currentStreamId }
        _uiState.value = _uiState.value.copy(
            channelName = channel?.name ?: _uiState.value.channelName,
            channelNumber = channel?.num ?: _uiState.value.channelNumber,
        )
    }

    private fun playCurrentFormat() {
        val url = UrlBuilder.liveUrl(currentBase, currentUser, currentPass, currentStreamId, formats[formatIndex])
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    fun tuneTo(streamId: Int) {
        if (streamId == currentStreamId && _uiState.value.error == null) {
            _uiState.value = _uiState.value.copy(showChannelList = false)
            return
        }
        currentStreamId = streamId
        formatIndex = 0
        _uiState.value = _uiState.value.copy(error = null, isBuffering = true, showChannelList = false)
        updateChannelInfo()
        viewModelScope.launch { sessionManager.setLastChannel(streamId.toLong()) }
        playCurrentFormat()
    }

    fun channelUp() = stepChannel(+1)

    fun channelDown() = stepChannel(-1)

    private fun stepChannel(delta: Int) {
        val list = _channels.value
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.streamId == currentStreamId }
        if (index < 0) return
        val next = ((index + delta) % list.size + list.size) % list.size
        tuneTo(list[next].streamId)
    }

    fun toggleChannelList() {
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
        playCurrentFormat()
    }

    override fun onCleared() {
        player.removeListener(playerListener)
        player.release()
    }
}
