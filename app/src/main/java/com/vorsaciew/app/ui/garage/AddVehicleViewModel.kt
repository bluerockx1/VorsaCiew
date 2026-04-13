package com.vorsaciew.app.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vorsaciew.app.data.model.Vehicle
import com.vorsaciew.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddVehicleState {
    object Idle    : AddVehicleState()
    object Loading : AddVehicleState()
    object Success : AddVehicleState()
    data class Error(val message: String) : AddVehicleState()
}

@HiltViewModel
class AddVehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _formState = MutableStateFlow<AddVehicleState>(AddVehicleState.Idle)
    val formState: StateFlow<AddVehicleState> = _formState.asStateFlow()

    fun addVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            _formState.value = AddVehicleState.Loading
            vehicleRepository.createVehicle(vehicle)
                .onSuccess { _formState.value = AddVehicleState.Success }
                .onFailure { _formState.value = AddVehicleState.Error(it.message ?: "Failed to add vehicle") }
        }
    }
}
