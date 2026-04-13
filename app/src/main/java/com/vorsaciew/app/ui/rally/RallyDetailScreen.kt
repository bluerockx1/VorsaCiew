package com.vorsaciew.app.ui.rally

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.CheckpointType
import com.vorsaciew.app.data.model.Rally
import com.vorsaciew.app.data.repository.RallyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RallyDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val rallyRepository: RallyRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val rallyId = savedState.get<String>("rallyId") ?: ""
    val rally: StateFlow<Rally?> = rallyRepository.getRallyFlow(rallyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val isAttending: StateFlow<Boolean> = rally.map { r ->
        r?.attendeeIds?.contains(auth.currentUser?.uid) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    fun join()  { viewModelScope.launch { rallyRepository.joinRally(rallyId) } }
    fun leave() { viewModelScope.launch { rallyRepository.leaveRally(rallyId) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RallyDetailScreen(navController: NavController, vm: RallyDetailViewModel = hiltViewModel()) {
    val rally      by vm.rally.collectAsStateWithLifecycle()
    val isAttending by vm.isAttending.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(rally?.title ?: "Rally") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
            })
        }
    ) { padding ->
        rally?.let { r ->
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(r.title, style = MaterialTheme.typography.headlineSmall)
                        Text("${r.attendeeIds.size} attending · ${r.checkpoints.size} stops",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (r.description.isNotEmpty())
                            Text(r.description, style = MaterialTheme.typography.bodyMedium)
                        if (isAttending) {
                            OutlinedButton(onClick = { vm.leave() }, Modifier.fillMaxWidth()) { Text("Leave Rally") }
                        } else {
                            Button(onClick = { vm.join() }, Modifier.fillMaxWidth()) { Text("Join Rally") }
                        }
                        HorizontalDivider()
                        Text("Route", style = MaterialTheme.typography.titleMedium)
                    }
                }
                itemsIndexed(r.checkpoints.sortedBy { it.order }) { index, stop ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val icon = when (stop.type) {
                            CheckpointType.START    -> Icons.Default.FlagCircle
                            CheckpointType.FINISH   -> Icons.Default.SportsScore
                            CheckpointType.REST_STOP -> Icons.Default.Restaurant
                            CheckpointType.FUEL     -> Icons.Default.LocalGasStation
                            else                    -> Icons.Default.Place
                        }
                        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(stop.name.ifEmpty { "Stop ${index + 1}" }, style = MaterialTheme.typography.bodyMedium)
                            Text(stop.type.label, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (stop.address.isNotEmpty())
                                Text(stop.address, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 52.dp))
                }
                item { Box(Modifier.height(80.dp)) }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
