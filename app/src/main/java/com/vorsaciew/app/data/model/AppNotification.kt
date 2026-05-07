package com.vorsaciew.app.data.model

data class AppNotification(
    val id: String = "",
    val recipientUid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val type: NotificationType = NotificationType.COMMENT,
    val postId: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)

enum class NotificationType { COMMENT, LIKE, FOLLOW }
