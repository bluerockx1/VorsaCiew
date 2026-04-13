package com.vorsaciew.app.ui.clubs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vorsaciew.app.data.model.Club
import com.vorsaciew.app.data.repository.ClubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ClubsViewModel @Inject constructor(
    clubRepository: ClubRepository
) : ViewModel() {

    val clubs: StateFlow<List<Club>> = clubRepository.getPublicClubs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
