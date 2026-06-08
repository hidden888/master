package com.iptv.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.util.SessionManager
import com.iptv.player.domain.model.Account
import com.iptv.player.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeAccountId: StateFlow<Long?> = sessionManager.activeAccountId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Flips once an account switch has been persisted so the screen can re-enter the app. */
    private val _accountSwitched = MutableStateFlow(false)
    val accountSwitched: StateFlow<Boolean> = _accountSwitched.asStateFlow()

    fun switchAccount(id: Long) {
        viewModelScope.launch {
            sessionManager.setActiveAccount(id)
            _accountSwitched.value = true
        }
    }
}
