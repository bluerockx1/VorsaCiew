package com.vorsaciew.app.ui.clubs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.Club
import com.vorsaciew.app.data.model.Event
import com.vorsaciew.app.data.model.Post
import com.vorsaciew.app.data.repository.ClubRepository
import com.vorsaciew.app.data.repository.EventRepository
import com.vorsaciew.app.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClubDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val clubRepository: ClubRepository,
    private val postRepository: PostRepository,
    private val eventRepository: EventRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val clubId: String = savedState["clubId"] ?: ""

    val club: StateFlow<Club?> = clubRepository.getClubFlow(clubId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val posts: StateFlow<List<Post>> = postRepository.getPostsForClub(clubId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val events: StateFlow<List<Event>> = eventRepository.getEventsForClub(clubId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isMember: StateFlow<Boolean> = club.map { c ->
        val uid = auth.currentUser?.uid ?: return@map false
        c?.memberIds?.contains(uid) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun join()  { viewModelScope.launch { clubRepository.joinClub(clubId) } }
    fun leave() { viewModelScope.launch { clubRepository.leaveClub(clubId) } }
}
