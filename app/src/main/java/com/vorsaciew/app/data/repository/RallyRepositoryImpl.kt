package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vorsaciew.app.data.model.Rally
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RallyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RallyRepository {

    private val rallies = firestore.collection("rallies")

    override fun getPublicRallies(): Flow<List<Rally>> = callbackFlow {
        val listener = rallies
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(Rally::class.java).sortedBy { it.startTime })
            }
        awaitClose { listener.remove() }
    }

    override fun getRallyFlow(rallyId: String): Flow<Rally?> = callbackFlow {
        val listener = rallies.document(rallyId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Rally::class.java))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun createRally(rally: Rally): Result<String> = runCatching {
        val ref = rallies.document()
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val withId = rally.copy(
            id        = ref.id,
            hostId    = uid,
            createdAt = System.currentTimeMillis()
        )
        ref.set(withId).await()
        ref.id
    }

    override suspend fun joinRally(rallyId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        rallies.document(rallyId).update(
            "attendeeIds", com.google.firebase.firestore.FieldValue.arrayUnion(uid)
        ).await()
    }

    override suspend fun leaveRally(rallyId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        rallies.document(rallyId).update(
            "attendeeIds", com.google.firebase.firestore.FieldValue.arrayRemove(uid)
        ).await()
    }
}
