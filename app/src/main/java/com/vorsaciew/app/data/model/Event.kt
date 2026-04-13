package com.vorsaciew.app.data.model

data class Event(
    val id: String = "",
    val hostId: String = "",
    val title: String = "",
    val description: String = "",
    val type: EventType = EventType.MEET,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val address: String = "",
    val coverPhotoUrl: String = "",
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val maxAttendees: Int? = null,
    val attendeeIds: List<String> = emptyList(),
    val isPublic: Boolean = true,
    val clubId: String? = null,
    val rallyId: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L
)

enum class EventType(val label: String) {
    MEET("Meet"),
    CRUISE("Cruise"),
    TRACK_DAY("Track Day"),
    SHOW("Car Show"),
    RALLY_STOP("Rally Stop"),
    OTHER("Other")
}
