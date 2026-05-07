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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.vorsaciew.app.core.navigation.Screen
import com.vorsaciew.app.data.model.LiveDriverPin
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, vm: MapViewModel = hiltViewModel()) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationPerms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    val nearbyDrivers by vm.nearbyDrivers.collectAsStateWithLifecycle()
    val nearbyEvents  by vm.nearbyEvents.collectAsStateWithLifecycle()
    val filterRadius  by vm.filterRadiusMi.collectAsStateWithLifecycle()
    val currentLatLng by vm.currentLatLng.collectAsStateWithLifecycle()

    var showFilterSheet  by remember { mutableStateOf(false) }
    var selectedDriver   by remember { mutableStateOf<LiveDriverPin?>(null) }
    var hasCenteredOnLocation by remember { mutableStateOf(false) }

    // Hold MapView and overlay references so we can drive them imperatively
    val mapViewRef         = remember { mutableStateOf<MapView?>(null) }
    val myLocationOverlay  = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    // Request permissions on first launch
    LaunchedEffect(locationPerms.allPermissionsGranted) {
        if (locationPerms.allPermissionsGranted) vm.startLocationUpdates()
        else locationPerms.launchMultiplePermissionRequest()
    }

    // Center map on first real GPS fix (replaces the SF hardcoded default)
    LaunchedEffect(currentLatLng) {
        if (!hasCenteredOnLocation && currentLatLng != null) {
            val (lat, lng) = currentLatLng!!
            mapViewRef.value?.controller?.animateTo(GeoPoint(lat, lng))
            hasCenteredOnLocation = true
        }
    }

    // Center map when FAB is tapped
    LaunchedEffect(Unit) {
        vm.centerOnMeFlow.collect { (lat, lng) ->
            mapViewRef.value?.controller?.animateTo(GeoPoint(lat, lng))
        }
    }

    // Refresh driver + event markers whenever data or mapView changes
    LaunchedEffect(nearbyDrivers, nearbyEvents, mapViewRef.value) {
        val mv = mapViewRef.value ?: return@LaunchedEffect
        // Keep only the MyLocationNewOverlay; remove all Marker overlays
        mv.overlays.removeAll { it is Marker }

        nearbyDrivers.forEach { driver ->
            val marker = Marker(mv).apply {
                position = GeoPoint(driver.latitude, driver.longitude)
                title    = driver.displayName
                snippet  = driver.vehicleLabel
                setOnMarkerClickListener { _, _ ->
                    selectedDriver = driver
                    true
                }
            }
            mv.overlays.add(marker)
        }

        nearbyEvents.forEach { event ->
            val marker = Marker(mv).apply {
                position = GeoPoint(event.latitude, event.longitude)
                title    = event.title
                snippet  = event.type.label
                setOnMarkerClickListener { _, _ ->
                    navController.navigate(Screen.EventDetail.createRoute(event.id))
                    true
                }
            }
            mv.overlays.add(marker)
        }

        mv.invalidate()
    }

    // Lifecycle: onResume / onPause for OSMDroid
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapViewRef.value?.onResume()
                    if (locationPerms.allPermissionsGranted) {
                        myLocationOverlay.value?.enableMyLocation()
                    }
                }
                Lifecycle.Event.ON_PAUSE  -> {
                    mapViewRef.value?.onPause()
                    myLocationOverlay.value?.disableMyLocation()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.stopLocationUpdates()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // OSMDroid map view
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(XYTileSource(
                        "CartoDB.Voyager", 0, 19, 256, ".png",
                        arrayOf(
                            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
                        )
                    ))
                    setMultiTouchControls(true)
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint(37.7749, -122.4194))

                    // My-location blue dot
                    val overlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    overlay.enableMyLocation()
                    overlays.add(overlay)
                    myLocationOverlay.value = overlay
                    mapViewRef.value = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top overlay: radius chip + driver count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            FilterChip(
                selected    = true,
                onClick     = { showFilterSheet = true },
                label       = { Text("${filterRadius.toInt()} mi") },
                leadingIcon = { Icon(Icons.Default.FilterList, null, Modifier.size(16.dp)) }
            )
            Surface(
                shape          = CircleShape,
                color          = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                Text(
                    "${nearbyDrivers.size} nearby",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Center-on-me FAB
        FloatingActionButton(
            onClick          = { vm.centerOnMyLocation() },
            modifier         = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding(),
            containerColor   = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.MyLocation, "Center map", tint = MaterialTheme.colorScheme.onPrimary)
        }

        // Driver quick-view bottom sheet
        selectedDriver?.let { driver ->
            DriverQuickViewSheet(
                driver        = driver,
                onViewProfile = { navController.navigate(Screen.Profile.createRoute(driver.uid)) },
                onDismiss     = { selectedDriver = null }
            )
        }

        // Radius filter sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState       = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier  = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Filter Radius",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${filterRadius.toInt()} mi",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value         = filterRadius.toFloat(),
                        onValueChange = { vm.setFilterRadius(it.toDouble()) },
                        valueRange    = 1f..50f,
                        steps         = 48
                    )
                    Button(onClick = { showFilterSheet = false }, Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }
            }
        }

        // No-permission fallback
        if (!locationPerms.allPermissionsGranted) {
            Surface(
                modifier       = Modifier.align(Alignment.Center).padding(32.dp),
                shape          = MaterialTheme.shapes.medium,
                color          = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier             = Modifier.padding(24.dp),
                    horizontalAlignment  = Alignment.CenterHorizontally,
                    verticalArrangement  = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Location Required", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "VorsaCiew needs your location to show nearby drivers and events.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { locationPerms.launchMultiplePermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
