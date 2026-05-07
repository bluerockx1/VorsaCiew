package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vorsaciew.app.data.model.Club
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClubRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ClubRepository {

    private val clubs = firestore.collection("clubs")

    override fun getPublicClubs(): Flow<List<Club>> = callbackFlow {
        val listener = clubs
            .whereEqualTo("isPrivate", false)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                val sorted = snap.toObjects(Club::class.java).sortedByDescending { it.memberCount }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    override fun getClubFlow(clubId: String): Flow<Club?> = callbackFlow {
        val listener = clubs.document(clubId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Club::class.java))
        }
        awaitClose { listener.remove() }
    }

    override fun getClubsForUser(uid: String): Flow<List<Club>> = callbackFlow {
        val listener = clubs.whereArrayContains("memberIds", uid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(Club::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createClub(club: Club): Result<String> = runCatching {
        val ref = clubs.document()
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val withId = club.copy(
            id        = ref.id,
            ownerId   = uid,
            adminIds  = listOf(uid),
            memberIds = listOf(uid),
            memberCount = 1,
            createdAt = System.currentTimeMillis()
        )
        ref.set(withId).await()
        // Fire-and-forget: don't let user-doc update failure roll back club creation
        try {
            firestore.collection("users").document(uid)
                .update("clubIds", com.google.firebase.firestore.FieldValue.arrayUnion(ref.id))
                .await()
        } catch (_: Exception) { }
        ref.id
    }

    override suspend fun joinClub(clubId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val batch = firestore.batch()
        batch.update(clubs.document(clubId), "memberIds",
            com.google.firebase.firestore.FieldValue.arrayUnion(uid))
        batch.update(clubs.document(clubId), "memberCount",
            com.google.firebase.firestore.FieldValue.increment(1))
        batch.update(firestore.collection("users").document(uid), "clubIds",
            com.google.firebase.firestore.FieldValue.arrayUnion(clubId))
        batch.commit().await()
    }

    override suspend fun leaveClub(clubId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val batch = firestore.batch()
        batch.update(clubs.document(clubId), "memberIds",
            com.google.firebase.firestore.FieldValue.arrayRemove(uid))
        batch.update(clubs.document(clubId), "memberCount",
            com.google.firebase.firestore.FieldValue.increment(-1))
        batch.update(firestore.collection("users").document(uid), "clubIds",
            com.google.firebase.firestore.FieldValue.arrayRemove(clubId))
        batch.commit().await()
    }

    override suspend fun searchClubs(query: String): List<Club> {
        if (query.isBlank()) return emptyList()
        return clubs
            .whereEqualTo("isPrivate", false)
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(20)
            .get().await()
            .toObjects(Club::class.java)
    }
}
