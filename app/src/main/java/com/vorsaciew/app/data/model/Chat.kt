package com.vorsaciew.app.data.model

data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

data class ChatRoom(
    val id: String = "",
    val type: ChatRoomType = ChatRoomType.DIRECT,
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),   // uid -> displayName
    val participantAvatars: Map<String, String> = emptyMap(), // uid -> avatarUrl
    val clubId: String? = null,
    val name: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCounts: Map<String, Int> = emptyMap()           // uid -> count
)

enum class ChatRoomType { DIRECT, CLUB, EVENT }
