package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.LiveDriverPin
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getNearbyDrivers(centerLat: Double, centerLng: Double, radiusKm: Double): Flow<List<LiveDriverPin>>
    suspend fun updateOwnLocation(uid: String, lat: Double, lng: Double, heading: Float, speedKmh: Float)
    suspend fun setVisible(uid: String, visible: Boolean)
}
