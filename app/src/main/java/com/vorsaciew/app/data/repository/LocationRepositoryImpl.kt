package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.vorsaciew.app.data.model.LiveDriverPin
import com.vorsaciew.app.core.util.distanceKm
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : LocationRepository {

    private val driversRef = database.getReference("driverLocations")

    override fun getNearbyDrivers(
        centerLat: Double, centerLng: Double, radiusKm: Double
    ): Flow<List<LiveDriverPin>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUid = auth.currentUser?.uid
                val pins = snapshot.children.mapNotNull { child ->
                    // Manual field extraction avoids kotlin-reflect issues with val data classes.
                    // FirebaseDatabase Java reflection cannot inject into final (val) Kotlin fields,
                    // so getValue(Class) would return all-default values — we bypass it entirely.
                    val uid = child.child("uid").getValue(String::class.java)
                        ?: return@mapNotNull null
                    val lat      = child.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng      = child.child("longitude").getValue(Double::class.java) ?: 0.0
                    val visible  = child.child("isVisible").getValue(Boolean::class.java) ?: true
                    val name     = child.child("displayName").getValue(String::class.java) ?: ""
                    val avatar   = child.child("avatarUrl").getValue(String::class.java) ?: ""
                    val vehicle  = child.child("vehicleLabel").getValue(String::class.java) ?: ""
                    val heading  = child.child("heading").getValue(Float::class.java) ?: 0f
                    val speed    = child.child("speedKmh").getValue(Float::class.java) ?: 0f
                    val lastSeen = child.child("lastSeen").getValue(Long::class.java) ?: 0L
                    LiveDriverPin(uid, name, avatar, lat, lng, "", vehicle, heading, speed, visible, lastSeen)
                }.filter { pin ->
                    pin.isVisible &&
                    pin.uid != currentUid &&
                    distanceKm(centerLat, centerLng, pin.latitude, pin.longitude) <= radiusKm
                }
                trySend(pins)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        driversRef.addValueEventListener(listener)
        awaitClose { driversRef.removeEventListener(listener) }
    }

    override suspend fun updateOwnLocation(
        uid: String, lat: Double, lng: Double, heading: Float, speedKmh: Float
    ) {
        val update = mapOf(
            "uid"         to uid,
            "displayName" to (auth.currentUser?.displayName ?: ""),
            "latitude"    to lat,
            "longitude"   to lng,
            "heading"     to heading,
            "speedKmh"    to speedKmh,
            "isVisible"   to true,
            "lastSeen"    to System.currentTimeMillis()
        )
        driversRef.child(uid).updateChildren(update).await()
    }

    override suspend fun setVisible(uid: String, visible: Boolean) {
        driversRef.child(uid).child("isVisible").setValue(visible).await()
    }
}
