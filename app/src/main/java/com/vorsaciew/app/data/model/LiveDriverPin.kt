package com.vorsaciew.app.data.model

/**
 * Stored in Firebase Realtime Database under /driverLocations/{uid} for
 * low-latency real-time updates.
 */
data class LiveDriverPin(
    val uid: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val vehicleId: String = "",
    val vehicleLabel: String = "",  // e.g. "2022 Subaru WRX"
    val heading: Float = 0f,
    val speedKmh: Float = 0f,
    val isVisible: Boolean = true,
    val lastSeen: Long = 0L
)
