package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.vorsaciew.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: FirebaseUser? get() = auth.currentUser

    override val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("No UID after sign-in")
        firestore.collection("users").document(uid).get().await().toObject(User::class.java)
            ?: User(uid = uid)
    }

    override suspend fun registerWithEmail(
        email: String, password: String, username: String
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: error("No UID after registration")
        val user = User(
            uid         = uid,
            username    = username,
            displayName = username,
            createdAt   = System.currentTimeMillis()
        )
        firestore.collection("users").document(uid).set(user).await()
        user
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun updateFcmToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("fcmToken", token).await()
    }
}
