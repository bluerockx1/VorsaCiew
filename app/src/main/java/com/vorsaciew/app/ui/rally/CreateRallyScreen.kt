package com.vorsaciew.app.ui.rally

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.vorsaciew.app.data.model.Checkpoint
import com.vorsaciew.app.data.model.CheckpointType
import com.vorsaciew.app.data.model.Rally
import com.vorsaciew.app.data.repository.RallyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateRallyViewModel @Inject constructor(
    private val rallyRepository: RallyRepository
) : ViewModel() {

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    private val _geocoding = MutableStateFlow(false)
    val geocoding: StateFlow<Boolean> = _geocoding.asStateFlow()

    private val httpClient = OkHttpClient()

    fun createRally(title: String, description: String, checkpoints: List<Checkpoint>) {
        viewModelScope.launch {
            rallyRepository.createRally(Rally(
                title       = title,
                description = description,
                checkpoints = checkpoints
            )).onSuccess { _done.value = true }
        }
    }

    /** Returns (lat, lng) from Nominatim, or null if address not found / offline. */
    suspend fun geocodeAddress(query: String): Pair<Double, Double>? {
        if (query.isBlank()) return null
        _geocoding.value = true
        return withContext(Dispatchers.IO) {
            try {
                val encoded = Uri.encode(query)
                val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "VorsaCiew/1.0")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string() ?: return@withContext null
                    val arr = JSONArray(body)
                    if (arr.length() == 0) null
                    else arr.getJSONObject(0).run { getDouble("lat") to getDouble("lon") }
                }
            } catch (_: Exception) {
                null
            }
        }.also { _geocoding.value = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRallyScreen(navController: NavController, vm: CreateRallyViewModel = hiltViewModel()) {
    val done      by vm.done.collectAsStateWithLifecycle()
    val geocoding by vm.geocoding.collectAsStateWithLifecycle()
    val scope     = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var desc  by remember { mutableStateOf("") }
    val stops = remember { mutableStateListOf<Checkpoint>() }

    // Add-stop inline form state
    var showAddForm   by remember { mutableStateOf(false) }
    var stopName      by remember { mutableStateOf("") }
    var stopAddress   by remember { mutableStateOf("") }
    var stopType      by remember { mutableStateOf(CheckpointType.WAYPOINT) }
    var typeMenuOpen  by remember { mutableStateOf(false) }

    LaunchedEffect(done) { if (done) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Plan Rally") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
            })
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Rally Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = desc, onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4
            )

            // Route stops list
            Text("Route Stops (${stops.size})", style = MaterialTheme.typography.titleSmall)
            stops.forEachIndexed { index, stop ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.weight(1f)) {
                            Text(stop.name.ifEmpty { "Stop ${index + 1}" }, style = MaterialTheme.typography.bodyMedium)
                            Text(stop.type.label, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (stop.address.isNotEmpty()) {
                                Text(stop.address, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (stop.latitude != 0.0 || stop.longitude != 0.0) {
                                Text(
                                    "%.4f, %.4f".format(stop.latitude, stop.longitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { stops.removeAt(index) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Add stop toggle button
            OutlinedButton(
                onClick = { showAddForm = !showAddForm },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, Modifier.padding(end = 4.dp))
                Text(if (showAddForm) "Cancel" else "+ Add Stop")
            }

            // Inline add-stop form
            AnimatedVisibility(visible = showAddForm) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("New Stop", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = stopName, onValueChange = { stopName = it },
                            label = { Text("Stop Name") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            value = stopAddress, onValueChange = { stopAddress = it },
                            label = { Text("Address (optional — used for map)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        // Type dropdown
                        ExposedDropdownMenuBox(
                            expanded = typeMenuOpen,
                            onExpandedChange = { typeMenuOpen = it }
                        ) {
                            OutlinedTextField(
                                value = stopType.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Stop Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuOpen) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = typeMenuOpen,
                                onDismissRequest = { typeMenuOpen = false }
                            ) {
                                CheckpointType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        onClick = { stopType = type; typeMenuOpen = false }
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        Button(
                            onClick = {
                                val name = stopName
                                val addr = stopAddress
                                val type = stopType
                                val order = stops.size
                                scope.launch {
                                    val coords = if (addr.isNotBlank()) vm.geocodeAddress(addr) else null
                                    stops.add(Checkpoint(
                                        id        = UUID.randomUUID().toString(),
                                        order     = order,
                                        name      = name.ifEmpty { type.label },
                                        address   = addr,
                                        type      = type,
                                        latitude  = coords?.first ?: 0.0,
                                        longitude = coords?.second ?: 0.0
                                    ))
                                    stopName    = ""
                                    stopAddress = ""
                                    stopType    = CheckpointType.WAYPOINT
                                    showAddForm = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled  = !geocoding
                        ) {
                            if (geocoding) {
                                CircularProgressIndicator(
                                    Modifier.padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(if (stopAddress.isNotBlank()) "Geocode & Add Stop" else "Add Stop")
                        }
                    }
                }
            }

            Button(
                onClick = { vm.createRally(title, desc, stops.toList()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = title.isNotBlank() && stops.isNotEmpty() && !geocoding
            ) { Text("Publish Rally") }
        }
    }
}
