package com.vorsaciew.app.ui.events

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.vorsaciew.app.data.model.Event
import com.vorsaciew.app.data.model.EventType
import com.vorsaciew.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class CreateEventState {
    object Idle    : CreateEventState()
    object Loading : CreateEventState()
    data class Success(val eventId: String) : CreateEventState()
    data class Error(val message: String)   : CreateEventState()
}

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _formState = MutableStateFlow<CreateEventState>(CreateEventState.Idle)
    val formState: StateFlow<CreateEventState> = _formState.asStateFlow()

    /** Lat/lng resolved lazily from FusedLocation when the user submits. */
    private var cachedLat = 0.0
    private var cachedLng = 0.0

    init { fetchLocation() }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        viewModelScope.launch {
            fusedLocationClient.lastLocation.await()?.let { loc ->
                cachedLat = loc.latitude
                cachedLng = loc.longitude
            }
        }
    }

    fun createEvent(
        title: String,
        description: String,
        type: EventType,
        locationName: String,
        address: String
    ) {
        viewModelScope.launch {
            _formState.value = CreateEventState.Loading

            // Re-fetch location right before submission in case it wasn't ready on init
            if (cachedLat == 0.0 && cachedLng == 0.0) {
                @SuppressLint("MissingPermission")
                val loc = fusedLocationClient.lastLocation.await()
                cachedLat = loc?.latitude ?: 0.0
                cachedLng = loc?.longitude ?: 0.0
            }

            val event = Event(
                title        = title,
                description  = description,
                type         = type,
                locationName = locationName,
                address      = address,
                latitude     = cachedLat,
                longitude    = cachedLng,
                startTime    = System.currentTimeMillis() + 3_600_000L
            )
            eventRepository.createEvent(event)
                .onSuccess { id -> _formState.value = CreateEventState.Success(id) }
                .onFailure { _formState.value = CreateEventState.Error(it.message ?: "Failed to create event") }
        }
    }
}
