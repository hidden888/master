package com.iptv.player.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.network.NetworkResult
import com.iptv.player.core.util.SessionManager
import com.iptv.player.domain.repository.AccountRepository
import com.iptv.player.domain.repository.LiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val liveRepository: LiveRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(name: String, url: String, username: String, password: String) {
        if (url.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Bitte Server-URL, Benutzer und Passwort ausfüllen.")
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            when (val result = accountRepository.authenticateAndSave(name, url, username, password)) {
                is NetworkResult.Success -> {
                    val accountId = result.data
                    sessionManager.setActiveAccount(accountId)
                    // Pre-fetch live content; failures are non-fatal (the Live screen can retry).
                    liveRepository.syncLive(accountId)
                    _uiState.value = LoginUiState.Success
                }
                is NetworkResult.Error -> _uiState.value = LoginUiState.Error(result.message)
                is NetworkResult.Exception -> _uiState.value = LoginUiState.Error(
                    result.throwable.message ?: "Verbindung fehlgeschlagen.",
                )
            }
        }
    }

    fun resetError() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}
