package com.vorsaciew.app.data.model

data class Rally(
    val id: String = "",
    val hostId: String = "",
    val title: String = "",
    val description: String = "",
    val coverPhotoUrl: String = "",
    val checkpoints: List<Checkpoint> = emptyList(),
    val startTime: Long = 0L,
    val estimatedDurationMinutes: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val attendeeIds: List<String> = emptyList(),
    val isPublic: Boolean = true,
    val status: RallyStatus = RallyStatus.UPCOMING,
    val createdAt: Long = 0L
)

data class Checkpoint(
    val id: String = "",
    val order: Int = 0,
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val type: CheckpointType = CheckpointType.WAYPOINT,
    val estimatedArrivalMinutes: Int = 0
)

enum class CheckpointType(val label: String) {
    START("Start"),
    WAYPOINT("Waypoint"),
    REST_STOP("Rest Stop"),
    FUEL("Fuel Stop"),
    FINISH("Finish")
}

enum class RallyStatus { UPCOMING, ACTIVE, COMPLETED, CANCELLED }
