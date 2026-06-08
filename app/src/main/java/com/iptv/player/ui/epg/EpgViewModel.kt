package com.iptv.player.ui.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.network.NetworkResult
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

sealed interface EpgState {
    data object Loading : EpgState
    data object Ready : EpgState
    /** EPG loaded fine but the provider returned no programs (e.g. no XMLTV feed). */
    data object Empty : EpgState
    data class Error(val message: String) : EpgState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EpgViewModel @Inject constructor(
    private val liveRepository: LiveRepository,
    private val epgRepository: EpgRepository,
    private val sessionManager: SessionManager,
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

    private val _state = MutableStateFlow<EpgState>(EpgState.Loading)
    val state: StateFlow<EpgState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val id = sessionManager.activeAccountId.filterNotNull().first()
            accountId.value = id
            // Pull the guide from the network the first time it's missing, then read from cache.
            if (!epgRepository.hasEpg(id)) {
                loadFromNetwork(id)
            } else {
                loadFromCache(id)
            }
        }
    }

    /** Manual "refresh" action from the guide. */
    fun refresh() {
        val id = accountId.value ?: return
        viewModelScope.launch { loadFromNetwork(id) }
    }

    private suspend fun loadFromNetwork(id: Long) {
        _state.value = EpgState.Loading
        when (val result = epgRepository.refreshEpg(id)) {
            is NetworkResult.Success -> loadFromCache(id)
            is NetworkResult.Error -> _state.value = EpgState.Error(result.message)
            is NetworkResult.Exception ->
                _state.value = EpgState.Error(result.throwable.message ?: "EPG konnte nicht geladen werden.")
        }
    }

    private suspend fun loadFromCache(id: Long) {
        val data = epgRepository.getProgramsInWindow(id, windowStart, windowEnd)
        _programs.value = data
        _state.value = if (data.isEmpty()) EpgState.Empty else EpgState.Ready
    }

    private companion object {
        const val WINDOW_HOURS = 12L

        fun floorToHalfHour(millis: Long): Long {
            val halfHour = 30 * 60 * 1000L
            return millis - (millis % halfHour)
        }
    }
}
