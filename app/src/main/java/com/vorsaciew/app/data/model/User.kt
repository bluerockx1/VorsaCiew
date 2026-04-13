package com.vorsaciew.app.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val coverPhotoUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isOnline: Boolean = false,
    val isLocationVisible: Boolean = true,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,
    val vehicleIds: List<String> = emptyList(),
    val clubIds: List<String> = emptyList(),
    val fcmToken: String = "",
    val createdAt: Long = 0L
)
