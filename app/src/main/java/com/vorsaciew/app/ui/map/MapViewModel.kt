package com.vorsaciew.app.ui.map

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.vorsaciew.app.data.model.Event
import com.vorsaciew.app.data.model.LiveDriverPin
import com.vorsaciew.app.data.repository.EventRepository
import com.vorsaciew.app.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val eventRepository: EventRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _filterRadiusKm = MutableStateFlow(10.0)
    val filterRadiusKm: StateFlow<Double> = _filterRadiusKm.asStateFlow()

    private val _currentLatLng = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLatLng: StateFlow<Pair<Double, Double>?> = _currentLatLng.asStateFlow()

    val nearbyDrivers: StateFlow<List<LiveDriverPin>> = _currentLatLng
        .filterNotNull()
        .flatMapLatest { (lat, lng) ->
            locationRepository.getNearbyDrivers(lat, lng, _filterRadiusKm.value)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nearbyEvents: StateFlow<List<Event>> = _currentLatLng
        .filterNotNull()
        .flatMapLatest { (lat, lng) ->
            eventRepository.getEventsNearby(lat, lng, _filterRadiusKm.value)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _currentLatLng.value = loc.latitude to loc.longitude
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, android.os.Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun setFilterRadius(km: Double) { _filterRadiusKm.value = km }

    fun centerOnMyLocation(cameraState: CameraPositionState) {
        _currentLatLng.value?.let { (lat, lng) ->
            viewModelScope.launch {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 13f))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
