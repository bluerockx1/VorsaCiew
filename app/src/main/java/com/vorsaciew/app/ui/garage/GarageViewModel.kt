package com.vorsaciew.app.ui.garage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.Vehicle
import com.vorsaciew.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GarageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val userId: String = savedState["userId"] ?: auth.currentUser?.uid ?: ""

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getVehiclesForUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isOwner: StateFlow<Boolean> = kotlinx.coroutines.flow.flow {
        emit(auth.currentUser?.uid == userId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
