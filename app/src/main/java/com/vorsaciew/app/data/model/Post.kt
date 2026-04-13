package com.vorsaciew.app.data.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val vehicleId: String? = null,
    val clubId: String? = null,
    val caption: String = "",
    val mediaUrls: List<String> = emptyList(),
    val mediaType: MediaType = MediaType.PHOTO,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L
)

enum class MediaType { PHOTO, VIDEO, TEXT }

data class PostComment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
