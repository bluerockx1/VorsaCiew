package com.vorsaciew.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.vorsaciew.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Loading        : AuthState()
    object Unauthenticated: AuthState()
    data class Authenticated(val uid: String) : AuthState()
}

sealed class FormState {
    object Idle    : FormState()
    object Loading : FormState()
    object Success : FormState()
    data class Error(val message: String) : FormState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authStateFlow
        .map { user: FirebaseUser? ->
            if (user != null) AuthState.Authenticated(user.uid)
            else AuthState.Unauthenticated
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val _formState = MutableStateFlow<FormState>(FormState.Idle)
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _formState.value = FormState.Loading
            authRepository.signInWithEmail(email, password)
                .onSuccess  { _formState.value = FormState.Success }
                .onFailure  { _formState.value = FormState.Error(it.message ?: "Login failed") }
        }
    }

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _formState.value = FormState.Loading
            authRepository.registerWithEmail(email, password, username)
                .onSuccess  { _formState.value = FormState.Success }
                .onFailure  { _formState.value = FormState.Error(it.message ?: "Registration failed") }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun clearFormState() { _formState.value = FormState.Idle }
}
