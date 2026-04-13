package com.vorsaciew.app.ui.map

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vorsaciew.app.core.navigation.Screen
import com.vorsaciew.app.data.model.LiveDriverPin

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, vm: MapViewModel = hiltViewModel()) {
    val locationPerms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val nearbyDrivers by vm.nearbyDrivers.collectAsStateWithLifecycle()
    val nearbyEvents  by vm.nearbyEvents.collectAsStateWithLifecycle()
    val filterRadius  by vm.filterRadiusKm.collectAsStateWithLifecycle()
    val cameraState   = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 11f)
    }
    var showFilterSheet  by remember { mutableStateOf(false) }
    var selectedDriver   by remember { mutableStateOf<LiveDriverPin?>(null) }

    LaunchedEffect(locationPerms.allPermissionsGranted) {
        if (locationPerms.allPermissionsGranted) vm.startLocationUpdates()
        else locationPerms.launchMultiplePermissionRequest()
    }
    DisposableEffect(Unit) { onDispose { vm.stopLocationUpdates() } }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = locationPerms.allPermissionsGranted),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
        ) {
            // Driver pins
            nearbyDrivers.forEach { driver ->
                Marker(
                    state   = MarkerState(LatLng(driver.latitude, driver.longitude)),
                    title   = driver.displayName,
                    snippet = driver.vehicleLabel,
                    icon    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                    onClick = { selectedDriver = driver; true }
                )
            }
            // Event pins
            nearbyEvents.forEach { event ->
                Marker(
                    state   = MarkerState(LatLng(event.latitude, event.longitude)),
                    title   = event.title,
                    snippet = event.type.label,
                    icon    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE),
                    onClick = { navController.navigate(Screen.EventDetail.createRoute(event.id)); true }
                )
            }
        }

        // Top overlay row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = true,
                onClick  = { showFilterSheet = true },
                label    = { Text("${filterRadius.toInt()} km") },
                leadingIcon = { Icon(Icons.Default.FilterList, null, Modifier.size(16.dp)) }
            )
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp) {
                Text(
                    "${nearbyDrivers.size} nearby",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Center FAB
        FloatingActionButton(
            onClick = { vm.centerOnMyLocation(cameraState) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Icon(Icons.Default.MyLocation, "Center map", tint = MaterialTheme.colorScheme.onPrimary) }

        // Driver sheet
        selectedDriver?.let { driver ->
            DriverQuickViewSheet(
                driver    = driver,
                onViewProfile = { navController.navigate(Screen.Profile.createRoute(driver.uid)) },
                onDismiss = { selectedDriver = null }
            )
        }

        // Radius filter sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Filter Radius", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("${filterRadius.toInt()} km",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Slider(
                        value     = filterRadius.toFloat(),
                        onValueChange = { vm.setFilterRadius(it.toDouble()) },
                        valueRange = 1f..100f,
                        steps     = 98
                    )
                    Button(onClick = { showFilterSheet = false }, Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }
            }
        }

        // No permission fallback
        if (!locationPerms.allPermissionsGranted) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Location Required", style = MaterialTheme.typography.titleMedium)
                    Text("VorsaCiew needs your location to show nearby drivers and events.",
                        style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { locationPerms.launchMultiplePermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
