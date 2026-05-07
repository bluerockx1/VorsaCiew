package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vorsaciew.app.data.model.Convoy
import com.vorsaciew.app.data.model.LiveDriverPin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConvoyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : ConvoyRepository {

    private val convoys = firestore.collection("convoys")

    override fun getActiveConvoys(): Flow<List<Convoy>> = callbackFlow {
        val listener = convoys
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(Convoy::class.java).sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    override fun getConvoyFlow(id: String): Flow<Convoy?> = callbackFlow {
        val listener = convoys.document(id).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Convoy::class.java))
        }
        awaitClose { listener.remove() }
    }

    override fun getConvoyMemberLocations(convoyId: String): Flow<List<LiveDriverPin>> = callbackFlow {
        val ref = database.getReference("convoyLocations").child(convoyId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pins = snapshot.children.mapNotNull { child ->
                    val uid      = child.child("uid").getValue(String::class.java) ?: return@mapNotNull null
                    val lat      = child.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng      = child.child("longitude").getValue(Double::class.java) ?: 0.0
                    val name     = child.child("displayName").getValue(String::class.java) ?: ""
                    val avatar   = child.child("avatarUrl").getValue(String::class.java) ?: ""
                    val vehicle  = child.child("vehicleLabel").getValue(String::class.java) ?: ""
                    val heading  = child.child("heading").getValue(Float::class.java) ?: 0f
                    val speed    = child.child("speedKmh").getValue(Float::class.java) ?: 0f
                    val lastSeen = child.child("lastSeen").getValue(Long::class.java) ?: 0L
                    LiveDriverPin(uid, name, avatar, lat, lng, "", vehicle, heading, speed, true, lastSeen)
                }
                trySend(pins)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun createConvoy(convoy: Convoy): Result<String> = runCatching {
        val ref = convoys.document()
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val name = auth.currentUser?.displayName ?: ""
        val withId = convoy.copy(
            id          = ref.id,
            creatorId   = uid,
            creatorName = name,
            memberIds   = listOf(uid),
            memberCount = 1,
            createdAt   = System.currentTimeMillis()
        )
        ref.set(withId).await()
        ref.id
    }

    override suspend fun joinConvoy(convoyId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        convoys.document(convoyId).update(
            mapOf(
                "memberIds"   to FieldValue.arrayUnion(uid),
                "memberCount" to FieldValue.increment(1)
            )
        ).await()
    }

    override suspend fun leaveConvoy(convoyId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        convoys.document(convoyId).update(
            mapOf(
                "memberIds"   to FieldValue.arrayRemove(uid),
                "memberCount" to FieldValue.increment(-1)
            )
        ).await()
        // Remove from RTDB live locations
        database.getReference("convoyLocations").child(convoyId).child(uid).removeValue().await()
    }

    override suspend fun endConvoy(convoyId: String): Result<Unit> = runCatching {
        convoys.document(convoyId).update("isActive", false).await()
        // Clean up live locations for this convoy
        database.getReference("convoyLocations").child(convoyId).removeValue().await()
    }

    override suspend fun updateMemberLocation(
        convoyId: String, lat: Double, lng: Double, heading: Float, speedKmh: Float
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val update = mapOf(
            "uid"          to uid,
            "displayName"  to (auth.currentUser?.displayName ?: ""),
            "latitude"     to lat,
            "longitude"    to lng,
            "heading"      to heading,
            "speedKmh"     to speedKmh,
            "lastSeen"     to System.currentTimeMillis()
        )
        database.getReference("convoyLocations").child(convoyId).child(uid)
            .updateChildren(update).await()
    }
}
