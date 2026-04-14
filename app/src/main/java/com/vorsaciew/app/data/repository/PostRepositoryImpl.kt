package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vorsaciew.app.data.model.Post
import com.vorsaciew.app.data.model.PostComment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PostRepository {

    private val posts = firestore.collection("posts")

    override fun getFeedPosts(): Flow<List<Post>> = callbackFlow {
        val listener = posts
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(Post::class.java))
            }
        awaitClose { listener.remove() }
    }

    override fun getPostsForUser(uid: String): Flow<List<Post>> = callbackFlow {
        val listener = posts
            .whereEqualTo("authorId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(Post::class.java).sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    override fun getPostsForClub(clubId: String): Flow<List<Post>> = callbackFlow {
        val listener = posts
            .whereEqualTo("clubId", clubId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(Post::class.java).sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    override fun getPostFlow(postId: String): Flow<Post?> = callbackFlow {
        val listener = posts.document(postId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Post::class.java))
        }
        awaitClose { listener.remove() }
    }

    override fun getComments(postId: String): Flow<List<PostComment>> = callbackFlow {
        val listener = posts.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(PostComment::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createPost(post: Post): Result<String> = runCatching {
        val ref = posts.document()
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val userDoc = firestore.collection("users").document(uid).get().await()
        val displayName = userDoc.getString("displayName")?.ifEmpty { null }
            ?: userDoc.getString("username") ?: ""
        val avatarUrl = userDoc.getString("avatarUrl") ?: ""
        val withId = post.copy(
            id              = ref.id,
            authorId        = uid,
            authorName      = displayName,
            authorAvatarUrl = avatarUrl,
            createdAt       = System.currentTimeMillis()
        )
        ref.set(withId).await()
        firestore.collection("users").document(uid)
            .update("postCount", com.google.firebase.firestore.FieldValue.increment(1))
            .await()
        ref.id
    }

    override suspend fun toggleLike(postId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val likeRef = posts.document(postId).collection("likes").document(uid)
        val exists = likeRef.get().await().exists()
        if (exists) {
            likeRef.delete().await()
            posts.document(postId).update(
                "likeCount", com.google.firebase.firestore.FieldValue.increment(-1)
            ).await()
        } else {
            likeRef.set(mapOf("uid" to uid, "timestamp" to System.currentTimeMillis())).await()
            posts.document(postId).update(
                "likeCount", com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
        }
    }

    override suspend fun addComment(postId: String, comment: PostComment): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val userDoc = firestore.collection("users").document(uid).get().await()
        val displayName = userDoc.getString("displayName")?.ifEmpty { null }
            ?: userDoc.getString("username") ?: ""
        val avatarUrl = userDoc.getString("avatarUrl") ?: ""
        val ref = posts.document(postId).collection("comments").document()
        val withId = comment.copy(
            id              = ref.id,
            postId          = postId,
            authorId        = uid,
            authorName      = displayName,
            authorAvatarUrl = avatarUrl,
            timestamp       = System.currentTimeMillis()
        )
        ref.set(withId).await()
        posts.document(postId).update(
            "commentCount", com.google.firebase.firestore.FieldValue.increment(1)
        ).await()
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        posts.document(postId).delete().await()
        val uid = auth.currentUser?.uid ?: return@runCatching
        firestore.collection("users").document(uid)
            .update("postCount", com.google.firebase.firestore.FieldValue.increment(-1))
            .await()
    }
}
