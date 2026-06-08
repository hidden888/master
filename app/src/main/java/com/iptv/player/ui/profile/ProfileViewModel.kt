package com.iptv.player.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.player.core.util.SessionManager
import com.iptv.player.domain.model.Profile
import com.iptv.player.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Avatar colors offered when creating/editing a profile. */
val PROFILE_COLORS = listOf(
    0xFF3B82F6, 0xFF22C55E, 0xFFF59E0B, 0xFFEF4444, 0xFFA855F7, 0xFF14B8A6,
)

data class ProfileScreenState(
    val managing: Boolean = false,
    /** When set, the parental PIN sheet is shown for this profile. */
    val pinPromptFor: Profile? = null,
    val pinError: Boolean = false,
    /** When set, the create/edit sheet is shown. null id = create. */
    val editing: ProfileEdit? = null,
)

data class ProfileEdit(
    val id: Long?,
    val name: String,
    val avatarColor: Long,
    val isKids: Boolean,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _screenState = MutableStateFlow(ProfileScreenState())
    val screenState: StateFlow<ProfileScreenState> = _screenState.asStateFlow()

    /** Emits true once a profile has been activated and the caller should navigate onward. */
    private val _activated = MutableStateFlow(false)
    val activated: StateFlow<Boolean> = _activated.asStateFlow()

    init {
        viewModelScope.launch { profileRepository.ensureDefaultProfile() }
    }

    fun toggleManaging() {
        _screenState.value = _screenState.value.copy(managing = !_screenState.value.managing)
    }

    fun onProfileClicked(profile: Profile) {
        if (_screenState.value.managing) {
            _screenState.value = _screenState.value.copy(
                editing = ProfileEdit(profile.id, profile.name, profile.avatarColor, profile.isKids),
            )
            return
        }
        if (profile.hasPin) {
            _screenState.value = _screenState.value.copy(pinPromptFor = profile, pinError = false)
        } else {
            activate(profile.id)
        }
    }

    fun submitPin(pin: String) {
        val profile = _screenState.value.pinPromptFor ?: return
        viewModelScope.launch {
            if (profileRepository.verifyPin(profile.id, pin)) {
                _screenState.value = _screenState.value.copy(pinPromptFor = null, pinError = false)
                activate(profile.id)
            } else {
                _screenState.value = _screenState.value.copy(pinError = true)
            }
        }
    }

    fun dismissPin() {
        _screenState.value = _screenState.value.copy(pinPromptFor = null, pinError = false)
    }

    fun startCreate() {
        _screenState.value = _screenState.value.copy(
            editing = ProfileEdit(id = null, name = "", avatarColor = PROFILE_COLORS.first(), isKids = false),
        )
    }

    fun dismissEdit() {
        _screenState.value = _screenState.value.copy(editing = null)
    }

    fun saveEdit(name: String, avatarColor: Long, isKids: Boolean, pin: String) {
        val edit = _screenState.value.editing ?: return
        viewModelScope.launch {
            if (edit.id == null) {
                profileRepository.createProfile(name, avatarColor, isKids, pin)
            } else {
                profileRepository.updateProfile(edit.id, name, avatarColor, isKids, pin)
            }
            _screenState.value = _screenState.value.copy(editing = null)
        }
    }

    fun deleteEditing() {
        val id = _screenState.value.editing?.id ?: return
        viewModelScope.launch {
            profileRepository.deleteProfile(id)
            _screenState.value = _screenState.value.copy(editing = null)
        }
    }

    private fun activate(id: Long) {
        viewModelScope.launch {
            sessionManager.setActiveProfile(id)
            _activated.value = true
        }
    }
}
