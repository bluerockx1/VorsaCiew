package com.vorsaciew.app.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.vorsaciew.app.data.model.User
import com.vorsaciew.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio.asStateFlow()

    /** URI of a locally-picked image (not yet uploaded). */
    private val _pendingAvatarUri = MutableStateFlow<Uri?>(null)
    val pendingAvatarUri: StateFlow<Uri?> = _pendingAvatarUri.asStateFlow()

    /** The current saved avatar URL (from Firestore). */
    private val _savedAvatarUrl = MutableStateFlow("")
    val savedAvatarUrl: StateFlow<String> = _savedAvatarUrl.asStateFlow()

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                userRepository.getUserFlow(uid).collect { user ->
                    if (user != null && _displayName.value.isEmpty()) {
                        _displayName.value = user.displayName
                        _bio.value = user.bio
                        _savedAvatarUrl.value = user.avatarUrl
                    }
                }
            }
        }
    }

    fun setDisplayName(value: String) { _displayName.value = value }
    fun setBio(value: String) { _bio.value = value }

    /** Called when the user picks an image from the photo picker. */
    fun pickAvatar(uri: Uri) { _pendingAvatarUri.value = uri }

    fun save() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _state.value = EditProfileState.Saving
            try {
                // Upload new avatar if one was picked.
                // Use putStream via ContentResolver to handle content:// URIs reliably.
                val finalAvatarUrl = _pendingAvatarUri.value?.let { uri ->
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("Could not open image")
                    val ref = storage.reference.child("avatars/$uid.jpg")
                    stream.use { ref.putStream(it).await() }
                    ref.downloadUrl.await().toString()
                } ?: _savedAvatarUrl.value

                val current = userRepository.getUserFlow(uid).first()
                val updated = (current ?: User(uid = uid)).copy(
                    displayName = _displayName.value.trim(),
                    bio         = _bio.value.trim(),
                    avatarUrl   = finalAvatarUrl
                )
                userRepository.updateUser(updated)
                    .onSuccess { _state.value = EditProfileState.Success }
                    .onFailure { _state.value = EditProfileState.Error(it.message ?: "Save failed") }
            } catch (e: Exception) {
                _state.value = EditProfileState.Error(e.message ?: "Save failed")
            }
        }
    }
}
