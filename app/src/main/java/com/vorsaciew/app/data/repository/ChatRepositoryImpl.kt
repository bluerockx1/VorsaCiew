package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vorsaciew.app.data.model.ChatMessage
import com.vorsaciew.app.data.model.ChatRoom
import com.vorsaciew.app.data.model.ChatRoomType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ChatRepository {

    private val rooms = firestore.collection("chatRooms")

    override fun getChatRooms(): Flow<List<ChatRoom>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = rooms
            .whereArrayContains("participantIds", uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(ChatRoom::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = rooms.document(roomId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(ChatMessage::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getOrCreateDirectRoom(
        otherUid: String, otherName: String, otherAvatar: String
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        // Stable room ID: sorted UIDs joined with _
        val roomId = listOf(uid, otherUid).sorted().joinToString("_")
        val ref = rooms.document(roomId)
        if (!ref.get().await().exists()) {
            val room = ChatRoom(
                id             = roomId,
                type           = ChatRoomType.DIRECT,
                participantIds = listOf(uid, otherUid)
            )
            ref.set(room).await()
        }
        roomId
    }

    override suspend fun sendMessage(roomId: String, message: ChatMessage): Result<Unit> = runCatching {
        val ref = rooms.document(roomId).collection("messages").document()
        val withId = message.copy(
            id        = ref.id,
            roomId    = roomId,
            timestamp = System.currentTimeMillis()
        )
        ref.set(withId).await()
        rooms.document(roomId).update(
            mapOf(
                "lastMessage"          to message.text,
                "lastMessageTimestamp" to withId.timestamp
            )
        ).await()
    }
}
