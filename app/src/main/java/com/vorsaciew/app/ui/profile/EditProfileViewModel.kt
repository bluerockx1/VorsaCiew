package com.vorsaciew.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.User
import com.vorsaciew.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditProfileState {
    object Idle    : EditProfileState()
    object Saving  : EditProfileState()
    object Success : EditProfileState()
    data class Error(val message: String) : EditProfileState()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio.asStateFlow()

    init {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserFlow(uid).collect { user ->
                if (user != null && _displayName.value.isEmpty()) {
                    _displayName.value = user.displayName
                    _bio.value = user.bio
                }
            }
        }
    }

    fun setDisplayName(value: String) { _displayName.value = value }
    fun setBio(value: String) { _bio.value = value }

    fun save() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _state.value = EditProfileState.Saving
            // Fetch current user to preserve all other fields
            userRepository.getUserFlow(uid).collect { current ->
                val updated = (current ?: User(uid = uid)).copy(
                    displayName = _displayName.value.trim(),
                    bio         = _bio.value.trim()
                )
                userRepository.updateUser(updated)
                    .onSuccess { _state.value = EditProfileState.Success }
                    .onFailure { _state.value = EditProfileState.Error(it.message ?: "Save failed") }
                return@collect  // only handle first emission
            }
        }
    }
}
