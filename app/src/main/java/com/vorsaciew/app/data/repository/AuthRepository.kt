package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.vorsaciew.app.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    val authStateFlow: Flow<FirebaseUser?>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun registerWithEmail(email: String, password: String, username: String): Result<User>
    suspend fun signOut()
    suspend fun updateFcmToken(token: String)
}
