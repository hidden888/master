package com.iptv.player.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.ChannelSort
import com.iptv.player.core.util.SessionManager
import com.iptv.player.core.util.SettingsStore
import com.iptv.player.core.util.StreamType
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.EpgProgram
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.EpgRepository
import com.iptv.player.domain.repository.FavoriteRepository
import com.iptv.player.domain.repository.LiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SyncState {
    data object Idle : SyncState
    data object Loading : SyncState
    data class Error(val message: String) : SyncState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveViewModel @Inject constructor(
    private val liveRepository: LiveRepository,
    private val epgRepository: EpgRepository,
    private val favoriteRepository: FavoriteRepository,
    private val settingsStore: SettingsStore,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val accountId = MutableStateFlow<Long?>(null)
    private var profileId: Long = SessionManager.DEFAULT_PROFILE_ID
    private val _selectedCategoryId = MutableStateFlow(CATEGORY_ALL)
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val categories: StateFlow<List<Category>> = accountId.filterNotNull()
        .flatMapLatest { liveRepository.observeCategories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val channels: StateFlow<List<Channel>> =
        combine(
            combine(accountId.filterNotNull(), _selectedCategoryId) { id, cat -> id to cat }
                .flatMapLatest { (id, cat) -> liveRepository.observeChannels(id, cat) },
            settingsStore.settings,
        ) { list, settings -> list.sortedFor(settings.channelSort) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Currently airing program per EPG channel id, for the now/next labels. */
    val currentPrograms: StateFlow<Map<String, EpgProgram>> = accountId.filterNotNull()
        .flatMapLatest { epgRepository.observeCurrentByChannel(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** streamIds the user has favorited, to render the star toggles. */
    val favoriteIds: StateFlow<Set<Int>> = accountId.filterNotNull()
        .flatMapLatest { favoriteRepository.observeFavoriteIds(it, profileId, StreamType.LIVE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            profileId = sessionManager.activeProfileIdOrDefault.first()
            val id = sessionManager.activeAccountId.filterNotNull().first()
            accountId.value = id
            // Sync if the cache looks empty on first entry.
            if (liveRepository.observeChannels(id, CATEGORY_ALL).first().isEmpty()) {
                sync()
            }
            // Load the EPG once if we don't have it yet (non-fatal on failure).
            if (!epgRepository.hasEpg(id)) {
                epgRepository.refreshEpg(id)
            }
        }
    }

    private fun List<Channel>.sortedFor(sort: ChannelSort): List<Channel> = when (sort) {
        ChannelSort.NAME -> sortedBy { it.name.lowercase() }
        ChannelSort.NUMBER -> sortedBy { it.num }
        ChannelSort.DEFAULT -> this
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun toggleFavorite(streamId: Int) {
        val id = accountId.value ?: return
        val isFav = streamId in favoriteIds.value
        viewModelScope.launch {
            favoriteRepository.setFavorite(id, profileId, StreamType.LIVE, streamId, favorite = !isFav)
        }
    }

    fun sync() {
        val id = accountId.value ?: return
        _syncState.value = SyncState.Loading
        viewModelScope.launch {
            _syncState.value = when (val result = liveRepository.syncLive(id)) {
                is NetworkResult.Success -> SyncState.Idle
                is NetworkResult.Error -> SyncState.Error(result.message)
                is NetworkResult.Exception ->
                    SyncState.Error(result.throwable.message ?: "Synchronisierung fehlgeschlagen.")
            }
        }
    }
}
