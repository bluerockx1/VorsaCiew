package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vorsaciew.app.data.model.BuildLog
import com.vorsaciew.app.data.model.Modification
import com.vorsaciew.app.data.model.Vehicle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : VehicleRepository {

    private val vehicles = firestore.collection("vehicles")

    override fun getVehiclesForUser(uid: String): Flow<List<Vehicle>> = callbackFlow {
        val listener = vehicles
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                val sorted = snap.toObjects(Vehicle::class.java).sortedByDescending { it.createdAt }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    override fun getVehicleFlow(vehicleId: String): Flow<Vehicle?> = callbackFlow {
        val listener = vehicles.document(vehicleId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Vehicle::class.java))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun createVehicle(vehicle: Vehicle): Result<String> = runCatching {
        val ref = vehicles.document()
        val withId = vehicle.copy(
            id       = ref.id,
            ownerId  = auth.currentUser?.uid ?: "",
            createdAt = System.currentTimeMillis()
        )
        ref.set(withId).await()
        // Also add to user's vehicleIds
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .update("vehicleIds", com.google.firebase.firestore.FieldValue.arrayUnion(ref.id))
                .await()
        }
        ref.id
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Unit> = runCatching {
        vehicles.document(vehicle.id).set(vehicle).await()
    }

    override suspend fun deleteVehicle(vehicleId: String): Result<Unit> = runCatching {
        vehicles.document(vehicleId).delete().await()
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .update("vehicleIds", com.google.firebase.firestore.FieldValue.arrayRemove(vehicleId))
                .await()
        }
    }

    override suspend fun addModification(vehicleId: String, mod: Modification): Result<Unit> = runCatching {
        val withId = mod.copy(id = UUID.randomUUID().toString())
        vehicles.document(vehicleId)
            .update("modifications", com.google.firebase.firestore.FieldValue.arrayUnion(withId))
            .await()
    }

    override suspend fun addBuildLog(vehicleId: String, log: BuildLog): Result<Unit> = runCatching {
        val withId = log.copy(
            id        = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis()
        )
        vehicles.document(vehicleId)
            .update("buildLogs", com.google.firebase.firestore.FieldValue.arrayUnion(withId))
            .await()
    }
}
