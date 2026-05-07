package com.vorsaciew.app.data.model

data class Convoy(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val destination: String = "",
    val memberIds: List<String> = emptyList(),
    val memberCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = 0L
)
