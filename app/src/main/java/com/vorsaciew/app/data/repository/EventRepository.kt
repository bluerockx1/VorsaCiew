package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEventsNearby(lat: Double, lng: Double, radiusKm: Double): Flow<List<Event>>
    fun getEventFlow(eventId: String): Flow<Event?>
    fun getEventsForUser(uid: String): Flow<List<Event>>
    fun getEventsForClub(clubId: String): Flow<List<Event>>
    suspend fun createEvent(event: Event): Result<String>
    suspend fun joinEvent(eventId: String): Result<Unit>
    suspend fun leaveEvent(eventId: String): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
