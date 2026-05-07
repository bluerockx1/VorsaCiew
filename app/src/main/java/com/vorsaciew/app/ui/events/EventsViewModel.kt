package com.vorsaciew.app.ui.events

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.Event
import com.vorsaciew.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class EventFilter(val label: String) {
    NEARBY("Nearby"),
    UPCOMING("Upcoming"),
    JOINED("Joined"),
    HOSTED("Hosted")
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val auth: FirebaseAuth,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _filter = MutableStateFlow(EventFilter.NEARBY)
    val filter: StateFlow<EventFilter> = _filter.asStateFlow()

    private val _location = MutableStateFlow(Pair(0.0, 0.0))

    // Re-query whenever EITHER filter OR location changes
    val events: StateFlow<List<Event>> = combine(_filter, _location) { f, loc -> f to loc }
        .flatMapLatest { (f, loc) ->
            val uid = auth.currentUser?.uid ?: ""
            val (lat, lng) = loc
            when (f) {
                EventFilter.NEARBY   -> eventRepository.getEventsNearby(lat, lng, 80.0)   // ~50 mi
                EventFilter.UPCOMING -> eventRepository.getEventsNearby(lat, lng, 320.0)  // ~200 mi
                EventFilter.JOINED   -> eventRepository.getEventsForUser(uid)
                EventFilter.HOSTED   -> eventRepository.getEventsForHost(uid)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { fetchLocation() }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        viewModelScope.launch {
            try {
                fusedLocationClient.lastLocation.await()?.let { loc ->
                    _location.value = loc.latitude to loc.longitude
                }
            } catch (_: Exception) { /* location permission denied or unavailable */ }
        }
    }

    fun setFilter(f: EventFilter) { _filter.value = f }
    fun updateLocation(lat: Double, lng: Double) { _location.value = lat to lng }
}
