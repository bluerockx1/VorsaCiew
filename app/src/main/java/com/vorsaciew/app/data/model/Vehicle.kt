package com.vorsaciew.app.data.model

data class Vehicle(
    val id: String = "",
    val ownerId: String = "",
    val year: Int = 0,
    val make: String = "",
    val model: String = "",
    val trim: String = "",
    val color: String = "",
    val nickname: String = "",
    val description: String = "",
    val photoUrls: List<String> = emptyList(),
    val coverPhotoUrl: String = "",
    val modifications: List<Modification> = emptyList(),
    val horsepower: Int? = null,
    val torque: Int? = null,
    val weightLbs: Int? = null,
    val driveType: DriveType = DriveType.UNKNOWN,
    val buildLogs: List<BuildLog> = emptyList(),
    val isPublic: Boolean = true,
    val createdAt: Long = 0L
) {
    /** Human-readable label, e.g. "2022 Subaru WRX" */
    val label: String get() = "$year $make $model".trim()
}

data class Modification(
    val id: String = "",
    val category: String = "",   // Engine, Suspension, Wheels, Aero, Interior, Exterior, Other
    val name: String = "",
    val brand: String = "",
    val notes: String = "",
    val photoUrl: String = ""
)

data class BuildLog(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val photoUrls: List<String> = emptyList(),
    val mileage: Int? = null,
    val costUsd: Double? = null,
    val timestamp: Long = 0L
)

enum class DriveType { FWD, RWD, AWD, FOUR_WD, UNKNOWN }
