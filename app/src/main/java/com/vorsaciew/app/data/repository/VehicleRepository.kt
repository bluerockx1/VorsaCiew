package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.BuildLog
import com.vorsaciew.app.data.model.Modification
import com.vorsaciew.app.data.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun getVehiclesForUser(uid: String): Flow<List<Vehicle>>
    fun getVehicleFlow(vehicleId: String): Flow<Vehicle?>
    suspend fun createVehicle(vehicle: Vehicle): Result<String>
    suspend fun updateVehicle(vehicle: Vehicle): Result<Unit>
    suspend fun deleteVehicle(vehicleId: String): Result<Unit>
    suspend fun addModification(vehicleId: String, mod: Modification): Result<Unit>
    suspend fun addBuildLog(vehicleId: String, log: BuildLog): Result<Unit>
}
