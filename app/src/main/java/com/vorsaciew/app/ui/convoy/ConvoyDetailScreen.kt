package com.vorsaciew.app.ui.convoy

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.Convoy
import com.vorsaciew.app.data.model.LiveDriverPin
import com.vorsaciew.app.data.repository.ConvoyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ConvoyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val convoyRepository: ConvoyRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val auth: FirebaseAuth
) : ViewModel() {

    val convoyId: String = savedStateHandle["convoyId"] ?: ""

    val convoy: StateFlow<Convoy?> = convoyRepository.getConvoyFlow(convoyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val memberLocations: StateFlow<List<LiveDriverPin>> =
        convoyRepository.getConvoyMemberLocations(convoyId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _actionState = MutableStateFlow<String?>(null)
    val actionState: StateFlow<String?> = _actionState.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                viewModelScope.launch {
                    convoyRepository.updateMemberLocation(
                        convoyId, loc.latitude, loc.longitude,
                        loc.bearing, loc.speed * 3.6f
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .build()
        fusedLocationClient.requestLocationUpdates(req, locationCallback, null)
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun join() {
        viewModelScope.launch {
            convoyRepository.joinConvoy(convoyId)
                .onFailure { _actionState.value = it.message }
        }
    }

    fun leave(onDone: () -> Unit) {
        viewModelScope.launch {
            stopLocationUpdates()
            convoyRepository.leaveConvoy(convoyId)
                .onSuccess { onDone() }
                .onFailure { _actionState.value = it.message }
        }
    }

    fun end(onDone: () -> Unit) {
        viewModelScope.launch {
            stopLocationUpdates()
            convoyRepository.endConvoy(convoyId)
                .onSuccess { onDone() }
                .onFailure { _actionState.value = it.message }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvoyDetailScreen(
    navController: NavController,
    vm: ConvoyDetailViewModel = hiltViewModel()
) {
    val convoy    by vm.convoy.collectAsStateWithLifecycle()
    val members   by vm.memberLocations.collectAsStateWithLifecycle()
    val errorMsg  by vm.actionState.collectAsStateWithLifecycle()

    val isMember  = convoy?.memberIds?.contains(vm.currentUid) == true
    val isCreator = convoy?.creatorId == vm.currentUid

    var showEndDialog   by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    // Start publishing location as soon as we're a member
    LaunchedEffect(isMember) {
        if (isMember) vm.startLocationUpdates()
        else vm.stopLocationUpdates()
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopLocationUpdates() }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Convoy?") },
            text  = { Text("This will end the convoy for all members.") },
            confirmButton = {
                Button(
                    onClick = { showEndDialog = false; vm.end { navController.popBackStack() } },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("End") }
            },
            dismissButton = { TextButton(onClick = { showEndDialog = false }) { Text("Cancel") } }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Convoy?") },
            text  = { Text("You'll stop sharing your location with this convoy.") },
            confirmButton = {
                Button(onClick = { showLeaveDialog = false; vm.leave { navController.popBackStack() } }) {
                    Text("Leave")
                }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(convoy?.title ?: "Convoy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isCreator) {
                        IconButton(onClick = { showEndDialog = true }) {
                            Icon(Icons.Default.Stop, "End convoy",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (convoy == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // Live map
            item {
                ConvoyMap(
                    members = members,
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            }

            // Convoy info
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    convoy?.destination?.takeIf { it.isNotBlank() }?.let { dest ->
                        Text("Destination: $dest",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                    }
                    convoy?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(desc, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Group, null,
                            Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("${convoy?.memberCount ?: 0} members",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }

                    errorMsg?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(8.dp))
                    if (!isCreator) {
                        if (isMember) {
                            OutlinedButton(
                                onClick  = { showLeaveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Leave Convoy") }
                        } else {
                            Button(
                                onClick  = { vm.join() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Join Convoy") }
                        }
                    }
                }
            }

            // Members header
            item {
                HorizontalDivider()
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Members", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            // Member rows
            items(members, key = { it.uid }) { pin ->
                MemberRow(pin = pin, isYou = pin.uid == vm.currentUid)
            }

            if (members.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No live locations yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConvoyMap(members: List<LiveDriverPin>, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(
                    XYTileSource(
                        "CartoDB.Voyager", 0, 19, 256, ".png",
                        arrayOf(
                            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
                        )
                    )
                )
                setMultiTouchControls(true)
                controller.setZoom(13.0)
            }
        },
        update = { mapView ->
            // Clear existing pin overlays (keep only non-Marker ones like tile overlays)
            mapView.overlays.removeAll { it is Marker }
            members.forEach { pin ->
                if (pin.latitude != 0.0 || pin.longitude != 0.0) {
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(pin.latitude, pin.longitude)
                        title    = pin.displayName.ifEmpty { "Member" }
                        snippet  = pin.vehicleLabel.ifEmpty { null }
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(marker)
                }
            }
            // Center on first member with a real location
            members.firstOrNull { it.latitude != 0.0 || it.longitude != 0.0 }?.let { pin ->
                mapView.controller.animateTo(GeoPoint(pin.latitude, pin.longitude))
            }
            mapView.invalidate()
        },
        modifier = modifier
    )
}

@Composable
private fun MemberRow(pin: LiveDriverPin, isYou: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.DirectionsCar, null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(pin.displayName.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                if (isYou) {
                    Text("(you)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            if (pin.vehicleLabel.isNotBlank()) {
                Text(pin.vehicleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("${pin.speedKmh.toInt()} km/h",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
