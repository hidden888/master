package com.iptv.player.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.util.SessionManager
import com.iptv.player.domain.model.Channel
import com.iptv.player.domain.model.Movie
import com.iptv.player.domain.model.Series
import com.iptv.player.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val scope = MutableStateFlow<Pair<Long, Long>?>(null)

    val channels: StateFlow<List<Channel>> = scope.filterNotNull()
        .flatMapLatest { (account, profile) -> favoriteRepository.observeFavoriteChannels(account, profile) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movies: StateFlow<List<Movie>> = scope.filterNotNull()
        .flatMapLatest { (account, profile) -> favoriteRepository.observeFavoriteMovies(account, profile) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val series: StateFlow<List<Series>> = scope.filterNotNull()
        .flatMapLatest { (account, profile) -> favoriteRepository.observeFavoriteSeries(account, profile) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val account = sessionManager.activeAccountId.filterNotNull().first()
            val profile = sessionManager.activeProfileIdOrDefault.first()
            scope.value = account to profile
        }
    }
}
