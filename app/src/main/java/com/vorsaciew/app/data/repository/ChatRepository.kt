package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.ChatMessage
import com.vorsaciew.app.data.model.ChatRoom
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatRooms(): Flow<List<ChatRoom>>
    fun getMessages(roomId: String): Flow<List<ChatMessage>>
    suspend fun getOrCreateDirectRoom(otherUid: String, otherName: String, otherAvatar: String): Result<String>
    suspend fun sendMessage(roomId: String, message: ChatMessage): Result<Unit>
}
