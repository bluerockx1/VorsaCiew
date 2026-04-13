package com.vorsaciew.app.data.model

data class Club(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val coverPhotoUrl: String = "",
    val tags: List<String> = emptyList(),   // jdm, muscle, euro, exotics, trucks, etc.
    val isPrivate: Boolean = false,
    val requiresApproval: Boolean = false,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val pendingMemberIds: List<String> = emptyList(),
    val memberCount: Int = 0,
    val chatRoomId: String = "",
    val createdAt: Long = 0L
)
