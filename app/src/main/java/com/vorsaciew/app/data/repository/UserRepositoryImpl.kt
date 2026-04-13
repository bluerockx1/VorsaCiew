package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vorsaciew.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    private val users = firestore.collection("users")

    override fun getUserFlow(uid: String): Flow<User?> = callbackFlow {
        val listener = users.document(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(User::class.java))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun updateUser(user: User): Result<Unit> = runCatching {
        users.document(user.uid).set(user).await()
    }

    override suspend fun followUser(targetUid: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val batch = firestore.batch()
        batch.set(users.document(targetUid).collection("followers").document(uid),
            mapOf("uid" to uid, "timestamp" to System.currentTimeMillis()))
        batch.set(users.document(uid).collection("following").document(targetUid),
            mapOf("uid" to targetUid, "timestamp" to System.currentTimeMillis()))
        // Increment counts
        batch.update(users.document(targetUid), "followerCount",
            com.google.firebase.firestore.FieldValue.increment(1))
        batch.update(users.document(uid), "followingCount",
            com.google.firebase.firestore.FieldValue.increment(1))
        batch.commit().await()
    }

    override suspend fun unfollowUser(targetUid: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val batch = firestore.batch()
        batch.delete(users.document(targetUid).collection("followers").document(uid))
        batch.delete(users.document(uid).collection("following").document(targetUid))
        batch.update(users.document(targetUid), "followerCount",
            com.google.firebase.firestore.FieldValue.increment(-1))
        batch.update(users.document(uid), "followingCount",
            com.google.firebase.firestore.FieldValue.increment(-1))
        batch.commit().await()
    }

    override fun getFollowers(uid: String): Flow<List<User>> = flow {
        val followerIds = users.document(uid).collection("followers")
            .get().await().documents.map { it.id }
        if (followerIds.isEmpty()) { emit(emptyList()); return@flow }
        val followerUsers = users.whereIn(com.google.firebase.firestore.FieldPath.documentId(), followerIds)
            .get().await().toObjects(User::class.java)
        emit(followerUsers)
    }

    override fun getFollowing(uid: String): Flow<List<User>> = flow {
        val followingIds = users.document(uid).collection("following")
            .get().await().documents.map { it.id }
        if (followingIds.isEmpty()) { emit(emptyList()); return@flow }
        val followingUsers = users.whereIn(com.google.firebase.firestore.FieldPath.documentId(), followingIds)
            .get().await().toObjects(User::class.java)
        emit(followingUsers)
    }

    override suspend fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()
        return users
            .orderBy("username")
            .startAt(query.lowercase())
            .endAt(query.lowercase() + "\uf8ff")
            .limit(20)
            .get().await()
            .toObjects(User::class.java)
    }

    override suspend fun isFollowing(targetUid: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val doc = users.document(uid).collection("following").document(targetUid).get().await()
        return doc.exists()
    }
}
