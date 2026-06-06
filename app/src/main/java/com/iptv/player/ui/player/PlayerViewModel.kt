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
import com.iptv.player.domain.repository.AccountRepository
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

    private val streamId: Int = savedStateHandle[Screen.Player.ARG_STREAM_ID] ?: 0

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** Live format fallback: prefer HLS, fall back to raw TS on the first failure. */
    private val formats = listOf("m3u8", "ts")
    private var formatIndex = 0

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _uiState.value = _uiState.value.copy(isBuffering = state == Player.STATE_BUFFERING)
        }

        override fun onPlayerError(error: PlaybackException) {
            if (formatIndex < formats.lastIndex) {
                formatIndex++
                playCurrentFormat(currentBase, currentUser, currentPass)
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
            val channel = liveRepository.getChannel(accountId, streamId)
            if (account == null) {
                _uiState.value = _uiState.value.copy(error = "Konto nicht gefunden.", isBuffering = false)
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                channelName = channel?.name ?: "",
                channelNumber = channel?.num ?: 0,
            )
            sessionManager.setLastChannel(streamId.toLong())
            playCurrentFormat(account.baseUrl, account.username, account.password)
        }
    }

    private var currentBase = ""
    private var currentUser = ""
    private var currentPass = ""

    private fun playCurrentFormat(base: String, user: String, pass: String) {
        currentBase = base; currentUser = user; currentPass = pass
        val url = UrlBuilder.liveUrl(base, user, pass, streamId, formats[formatIndex])
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    fun retry() {
        formatIndex = 0
        _uiState.value = _uiState.value.copy(error = null, isBuffering = true)
        playCurrentFormat(currentBase, currentUser, currentPass)
    }

    override fun onCleared() {
        player.removeListener(playerListener)
        player.release()
    }
}
