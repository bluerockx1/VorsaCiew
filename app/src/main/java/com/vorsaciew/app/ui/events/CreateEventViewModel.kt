package com.vorsaciew.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vorsaciew.app.data.model.Event
import com.vorsaciew.app.data.model.EventType
import com.vorsaciew.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CreateEventState {
    object Idle    : CreateEventState()
    object Loading : CreateEventState()
    data class Success(val eventId: String) : CreateEventState()
    data class Error(val message: String)   : CreateEventState()
}

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _formState = MutableStateFlow<CreateEventState>(CreateEventState.Idle)
    val formState: StateFlow<CreateEventState> = _formState.asStateFlow()

    fun createEvent(
        title: String,
        description: String,
        type: EventType,
        locationName: String,
        address: String,
        lat: Double = 0.0,
        lng: Double = 0.0
    ) {
        viewModelScope.launch {
            _formState.value = CreateEventState.Loading
            val event = Event(
                title        = title,
                description  = description,
                type         = type,
                locationName = locationName,
                address      = address,
                latitude     = lat,
                longitude    = lng,
                startTime    = System.currentTimeMillis() + 3_600_000L // default: 1 hour from now
            )
            eventRepository.createEvent(event)
                .onSuccess { id -> _formState.value = CreateEventState.Success(id) }
                .onFailure { _formState.value = CreateEventState.Error(it.message ?: "Failed to create event") }
        }
    }
}
