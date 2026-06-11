package com.iptv.player.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.util.AppSettings
import com.iptv.player.core.util.SessionManager
import com.iptv.player.core.util.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Channel to auto-open once on startup, or null. */
    private val _startupChannel = MutableStateFlow<Int?>(null)
    val startupChannel: StateFlow<Int?> = _startupChannel.asStateFlow()

    init {
        viewModelScope.launch {
            if (settingsStore.settings.first().openLastChannelOnStart) {
                val last = sessionManager.lastChannelId.first()
                if (last != null && last > 0) _startupChannel.value = last.toInt()
            }
        }
    }

    fun consumeStartupChannel() {
        _startupChannel.value = null
    }
}
