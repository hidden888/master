package com.iptv.player.ui.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.SessionManager
import com.iptv.player.core.util.StreamType
import com.iptv.player.domain.model.SeriesDetail
import com.iptv.player.domain.repository.FavoriteRepository
import com.iptv.player.domain.repository.SeriesRepository
import com.iptv.player.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesDetailUiState(
    val loading: Boolean = true,
    val name: String = "",
    val cover: String? = null,
    val detail: SeriesDetail? = null,
    val selectedSeason: Int? = null,
    val isFavorite: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val seriesRepository: SeriesRepository,
    private val favoriteRepository: FavoriteRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val seriesId: Int = savedStateHandle[Screen.SeriesDetail.ARG_ID] ?: 0

    private var accountId: Long = 0
    private var profileId: Long = SessionManager.DEFAULT_PROFILE_ID

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountId = sessionManager.activeAccountId.filterNotNull().first()
            profileId = sessionManager.activeProfileIdOrDefault.first()
            val series = seriesRepository.getSeries(accountId, seriesId)
            _uiState.value = _uiState.value.copy(name = series?.name ?: "", cover = series?.cover)

            launch {
                favoriteRepository.observeFavoriteIds(accountId, profileId, StreamType.SERIES).collect { ids ->
                    _uiState.value = _uiState.value.copy(isFavorite = seriesId in ids)
                }
            }

            when (val result = seriesRepository.getSeriesDetail(accountId, seriesId)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    detail = result.data,
                    cover = result.data.cover ?: _uiState.value.cover,
                    selectedSeason = result.data.seasons.firstOrNull(),
                )
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                is NetworkResult.Exception -> _uiState.value =
                    _uiState.value.copy(loading = false, error = result.throwable.message ?: "Fehler.")
            }
        }
    }

    fun selectSeason(season: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            favoriteRepository.setFavorite(
                accountId, profileId, StreamType.SERIES, seriesId,
                favorite = !_uiState.value.isFavorite,
            )
        }
    }
}
