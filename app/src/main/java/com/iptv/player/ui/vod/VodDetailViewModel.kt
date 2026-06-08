package com.iptv.player.ui.vod

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.SessionManager
import com.iptv.player.domain.model.MovieDetail
import com.iptv.player.domain.repository.VodRepository
import com.iptv.player.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VodDetailUiState(
    val loading: Boolean = true,
    val name: String = "",
    val cover: String? = null,
    val detail: MovieDetail? = null,
    val error: String? = null,
)

@HiltViewModel
class VodDetailViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val movieId: Int = savedStateHandle[Screen.VodDetail.ARG_ID] ?: 0

    private val _uiState = MutableStateFlow(VodDetailUiState())
    val uiState: StateFlow<VodDetailUiState> = _uiState.asStateFlow()

    /** Container extension to use for playback (resolved from detail, falling back to mp4). */
    val playbackExtension: String
        get() = _uiState.value.detail?.containerExtension?.ifBlank { null } ?: "mp4"

    init {
        viewModelScope.launch {
            val accountId = sessionManager.activeAccountId.filterNotNull().first()
            val movie = vodRepository.getMovie(accountId, movieId)
            _uiState.value = _uiState.value.copy(name = movie?.name ?: "", cover = movie?.cover)

            when (val result = vodRepository.getMovieDetail(accountId, movieId)) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    detail = result.data,
                    cover = result.data.cover ?: _uiState.value.cover,
                )
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(loading = false, error = result.message)
                is NetworkResult.Exception -> _uiState.value =
                    _uiState.value.copy(loading = false, error = result.throwable.message ?: "Fehler.")
            }
        }
    }
}
