package com.iptv.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.AppSettings
import com.iptv.player.core.util.AspectMode
import com.iptv.player.core.util.BufferProfile
import com.iptv.player.core.util.ChannelSort
import com.iptv.player.core.util.DecoderMode
import com.iptv.player.core.util.SessionManager
import com.iptv.player.core.util.SettingsStore
import com.iptv.player.core.util.StreamFormat
import com.iptv.player.domain.model.Account
import com.iptv.player.domain.repository.AccountRepository
import com.iptv.player.domain.repository.EpgRepository
import com.iptv.player.domain.repository.LiveRepository
import com.iptv.player.domain.repository.SeriesRepository
import com.iptv.player.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val liveRepository: LiveRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository,
    private val epgRepository: EpgRepository,
    private val settingsStore: SettingsStore,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeAccountId: StateFlow<Long?> = sessionManager.activeAccountId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Flips once an account switch has been persisted so the screen can re-enter the app. */
    private val _accountSwitched = MutableStateFlow(false)
    val accountSwitched: StateFlow<Boolean> = _accountSwitched.asStateFlow()

    /** Transient status line for the maintenance actions. */
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun switchAccount(id: Long) {
        viewModelScope.launch {
            sessionManager.setActiveAccount(id)
            _accountSwitched.value = true
        }
    }

    fun setStreamFormat(value: StreamFormat) {
        viewModelScope.launch { settingsStore.setStreamFormat(value) }
    }

    fun setChannelSort(value: ChannelSort) {
        viewModelScope.launch { settingsStore.setChannelSort(value) }
    }

    fun setAspectMode(value: AspectMode) {
        viewModelScope.launch { settingsStore.setAspectMode(value) }
    }

    fun setBufferProfile(value: BufferProfile) {
        viewModelScope.launch { settingsStore.setBufferProfile(value) }
    }

    fun setDecoderMode(value: DecoderMode) {
        viewModelScope.launch { settingsStore.setDecoderMode(value) }
    }

    fun setShowChannelLogos(value: Boolean) {
        viewModelScope.launch { settingsStore.setShowChannelLogos(value) }
    }

    fun setShowChannelNumbers(value: Boolean) {
        viewModelScope.launch { settingsStore.setShowChannelNumbers(value) }
    }

    fun setShowHiddenChannels(value: Boolean) {
        viewModelScope.launch { settingsStore.setShowHiddenChannels(value) }
    }

    fun setEpgOffsetHours(value: Int) {
        viewModelScope.launch {
            settingsStore.setEpgOffsetHours(value)
            refreshEpg()
        }
    }

    fun setEpgUrl(value: String) {
        viewModelScope.launch {
            settingsStore.setEpgUrl(value)
            refreshEpg()
        }
    }

    fun refreshEpg() = run("Programmführer wird aktualisiert…") { id ->
        when (val r = epgRepository.refreshEpg(id)) {
            is NetworkResult.Success -> "Programmführer aktualisiert."
            is NetworkResult.Error -> "EPG: ${r.message}"
            is NetworkResult.Exception -> "EPG-Fehler: ${r.throwable.message ?: "unbekannt"}"
        }
    }

    fun reloadCatalogs() = run("Inhalte werden neu geladen…") { id ->
        liveRepository.syncLive(id)
        vodRepository.syncVod(id)
        seriesRepository.syncSeries(id)
        "Sender, Filme und Serien neu geladen."
    }

    private fun run(pending: String, block: suspend (Long) -> String) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _status.value = pending
            _status.value = try {
                val id = sessionManager.activeAccountId.filterNotNull().first()
                block(id)
            } catch (t: Throwable) {
                "Fehler: ${t.message ?: "unbekannt"}"
            }
            _busy.value = false
        }
    }
}
