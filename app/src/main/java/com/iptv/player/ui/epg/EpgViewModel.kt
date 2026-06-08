package com.iptv.player.ui.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.util.SessionManager
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.EpgProgram
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.EpgRepository
import com.iptv.player.domain.repository.LiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EpgViewModel @Inject constructor(
    private val liveRepository: LiveRepository,
    private val epgRepository: EpgRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    /** Guide window: start of the current half hour, spanning [WINDOW_HOURS] hours. */
    val windowStart: Long = floorToHalfHour(System.currentTimeMillis())
    val windowEnd: Long = windowStart + WINDOW_HOURS * 3600_000L

    private val accountId = MutableStateFlow<Long?>(null)

    val channels: StateFlow<List<Channel>> = accountId.filterNotNull()
        .flatMapLatest { liveRepository.observeChannels(it, CATEGORY_ALL) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _programs = MutableStateFlow<Map<String, List<EpgProgram>>>(emptyMap())
    val programs: StateFlow<Map<String, List<EpgProgram>>> = _programs.asStateFlow()

    init {
        viewModelScope.launch {
            val id = sessionManager.activeAccountId.filterNotNull().first()
            accountId.value = id
            _programs.value = epgRepository.getProgramsInWindow(id, windowStart, windowEnd)
        }
    }

    private companion object {
        const val WINDOW_HOURS = 12L

        fun floorToHalfHour(millis: Long): Long {
            val halfHour = 30 * 60 * 1000L
            return millis - (millis % halfHour)
        }
    }
}
