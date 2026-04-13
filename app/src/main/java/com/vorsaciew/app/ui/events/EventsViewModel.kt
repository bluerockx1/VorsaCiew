package com.vorsaciew.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.Event
import com.vorsaciew.app.data.repository.EventRepository
import com.vorsaciew.app.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _filter = MutableStateFlow(EventFilter.NEARBY)
    val filter: StateFlow<EventFilter> = _filter.asStateFlow()

    // Default to a broad radius for the events list
    private val _location = MutableStateFlow(Pair(37.7749, -122.4194))

    val events: StateFlow<List<Event>> = _filter.flatMapLatest { f ->
        val uid = auth.currentUser?.uid ?: ""
        when (f) {
            EventFilter.NEARBY   -> eventRepository.getEventsNearby(
                _location.value.first, _location.value.second, 50.0
            )
            EventFilter.UPCOMING -> eventRepository.getEventsNearby(
                _location.value.first, _location.value.second, 200.0
            )
            EventFilter.JOINED   -> eventRepository.getEventsForUser(uid)
            EventFilter.HOSTED   -> flow {
                emit(emptyList<Event>()) // future: filter by hostId
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(f: EventFilter) { _filter.value = f }
    fun updateLocation(lat: Double, lng: Double) { _location.value = lat to lng }
}
