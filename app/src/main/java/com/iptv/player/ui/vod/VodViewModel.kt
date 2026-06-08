package com.iptv.player.ui.vod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.SessionManager
import com.iptv.player.domain.model.Category
import com.iptv.player.domain.model.Movie
import com.iptv.player.domain.repository.CATEGORY_ALL
import com.iptv.player.domain.repository.VodRepository
import com.iptv.player.ui.live.SyncState
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VodViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val accountId = MutableStateFlow<Long?>(null)
    private val _selectedCategoryId = MutableStateFlow(CATEGORY_ALL)
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val categories: StateFlow<List<Category>> = accountId.filterNotNull()
        .flatMapLatest { vodRepository.observeCategories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movies: StateFlow<List<Movie>> =
        combine(accountId.filterNotNull(), _selectedCategoryId) { id, cat -> id to cat }
            .flatMapLatest { (id, cat) -> vodRepository.observeMovies(id, cat) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val id = sessionManager.activeAccountId.filterNotNull().first()
            accountId.value = id
            if (vodRepository.observeMovies(id, CATEGORY_ALL).first().isEmpty()) {
                sync()
            }
        }
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
    }

    fun sync() {
        val id = accountId.value ?: return
        _syncState.value = SyncState.Loading
        viewModelScope.launch {
            _syncState.value = when (val result = vodRepository.syncVod(id)) {
                is NetworkResult.Success -> SyncState.Idle
                is NetworkResult.Error -> SyncState.Error(result.message)
                is NetworkResult.Exception ->
                    SyncState.Error(result.throwable.message ?: "Filme konnten nicht geladen werden.")
            }
        }
    }
}
