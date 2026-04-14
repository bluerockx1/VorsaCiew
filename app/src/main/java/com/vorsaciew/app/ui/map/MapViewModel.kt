package com.vorsaciew.app.ui.map

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.vorsaciew.app.data.model.Event
import com.vorsaciew.app.data.model.LiveDriverPin
import com.vorsaciew.app.data.repository.EventRepository
import com.vorsaciew.app.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 1 mile = 1.60934 km */
private const val MI_TO_KM = 1.60934

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val eventRepository: EventRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    /** Radius slider is in miles; repositories expect km — convert on the way in. */
    private val _filterRadiusMi = MutableStateFlow(10.0)
    val filterRadiusMi: StateFlow<Double> = _filterRadiusMi.asStateFlow()

    private val _currentLatLng = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLatLng: StateFlow<Pair<Double, Double>?> = _currentLatLng.asStateFlow()

    /** Emits the user's current location whenever the FAB is tapped. */
    private val _centerOnMeFlow = MutableSharedFlow<Pair<Double, Double>>(replay = 0)
    val centerOnMeFlow: SharedFlow<Pair<Double, Double>> = _centerOnMeFlow.asSharedFlow()

    val nearbyDrivers: StateFlow<List<LiveDriverPin>> = _currentLatLng
        .filterNotNull()
        .flatMapLatest { (lat, lng) ->
            locationRepository.getNearbyDrivers(lat, lng, _filterRadiusMi.value * MI_TO_KM)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nearbyEvents: StateFlow<List<Event>> = _currentLatLng
        .filterNotNull()
        .flatMapLatest { (lat, lng) ->
            eventRepository.getEventsNearby(lat, lng, _filterRadiusMi.value * MI_TO_KM)
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
        fusedLocationClient.requestLocationUpdates(
            request, locationCallback, android.os.Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun setFilterRadius(miles: Double) { _filterRadiusMi.value = miles }

    fun centerOnMyLocation() {
        _currentLatLng.value?.let { latLng ->
            viewModelScope.launch { _centerOnMeFlow.emit(latLng) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
