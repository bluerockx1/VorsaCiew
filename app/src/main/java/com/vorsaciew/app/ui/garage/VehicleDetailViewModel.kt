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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val vehicleId: String = savedState["vehicleId"] ?: ""

    val vehicle: StateFlow<Vehicle?> = vehicleRepository.getVehicleFlow(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isOwner: StateFlow<Boolean> = vehicle.map { v ->
        v?.ownerId == auth.currentUser?.uid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
