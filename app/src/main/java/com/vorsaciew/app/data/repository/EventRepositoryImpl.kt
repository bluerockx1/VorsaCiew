package com.vorsaciew.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vorsaciew.app.core.util.distanceKm
import com.vorsaciew.app.data.model.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : EventRepository {

    private val events = firestore.collection("events")

    override fun getEventsNearby(lat: Double, lng: Double, radiusKm: Double): Flow<List<Event>> =
        callbackFlow {
            val listener = events
                .whereEqualTo("isPublic", true)
                .addSnapshotListener { snap, error ->
                    if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                    val nearby = snap.toObjects(Event::class.java).filter { e ->
                        distanceKm(lat, lng, e.latitude, e.longitude) <= radiusKm
                    }.sortedBy { it.startTime }
                    trySend(nearby)
                }
            awaitClose { listener.remove() }
        }

    override fun getEventFlow(eventId: String): Flow<Event?> = callbackFlow {
        val listener = events.document(eventId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Event::class.java))
        }
        awaitClose { listener.remove() }
    }

    override fun getEventsForUser(uid: String): Flow<List<Event>> = callbackFlow {
        val listener = events
            .whereArrayContains("attendeeIds", uid)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(Event::class.java).sortedBy { it.startTime })
            }
        awaitClose { listener.remove() }
    }

    override fun getEventsForClub(clubId: String): Flow<List<Event>> = callbackFlow {
        val listener = events
            .whereEqualTo("clubId", clubId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap.toObjects(Event::class.java).sortedBy { it.startTime })
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createEvent(event: Event): Result<String> = runCatching {
        val ref = events.document()
        val withId = event.copy(id = ref.id, hostId = auth.currentUser?.uid ?: "")
        ref.set(withId).await()
        ref.id
    }

    override suspend fun joinEvent(eventId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        events.document(eventId).update(
            "attendeeIds", com.google.firebase.firestore.FieldValue.arrayUnion(uid)
        ).await()
    }

    override suspend fun leaveEvent(eventId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        events.document(eventId).update(
            "attendeeIds", com.google.firebase.firestore.FieldValue.arrayRemove(uid)
        ).await()
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        events.document(eventId).delete().await()
    }
}
