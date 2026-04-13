package com.vorsaciew.app.ui.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.Event
import com.vorsaciew.app.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val eventId: String = savedState["eventId"] ?: ""

    val event: StateFlow<Event?> = eventRepository.getEventFlow(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isAttending: StateFlow<Boolean> = event.map { e ->
        val uid = auth.currentUser?.uid ?: return@map false
        e?.attendeeIds?.contains(uid) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun join()  { viewModelScope.launch { eventRepository.joinEvent(eventId) } }
    fun leave() { viewModelScope.launch { eventRepository.leaveEvent(eventId) } }
}
