package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.Convoy
import com.vorsaciew.app.data.model.LiveDriverPin
import kotlinx.coroutines.flow.Flow

interface ConvoyRepository {
    fun getActiveConvoys(): Flow<List<Convoy>>
    fun getConvoyFlow(id: String): Flow<Convoy?>
    fun getConvoyMemberLocations(convoyId: String): Flow<List<LiveDriverPin>>
    suspend fun createConvoy(convoy: Convoy): Result<String>
    suspend fun joinConvoy(convoyId: String): Result<Unit>
    suspend fun leaveConvoy(convoyId: String): Result<Unit>
    suspend fun endConvoy(convoyId: String): Result<Unit>
    suspend fun updateMemberLocation(
        convoyId: String, lat: Double, lng: Double, heading: Float, speedKmh: Float
    ): Result<Unit>
}
