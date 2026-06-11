package com.iptv.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.util.AccentColor
import com.iptv.player.core.util.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes the chosen accent color so the root theme can react to settings changes. */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    settingsStore: SettingsStore,
) : ViewModel() {
    val accentArgb: StateFlow<Long> = settingsStore.settings
        .map { it.accentColor.argb }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentColor.BLUE.argb)
}
