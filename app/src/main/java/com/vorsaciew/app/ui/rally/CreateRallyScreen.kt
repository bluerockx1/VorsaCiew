package com.vorsaciew.app.ui.rally

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateRallyViewModel @Inject constructor(
    private val rallyRepository: RallyRepository
) : ViewModel() {
    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun createRally(title: String, description: String, checkpoints: List<Checkpoint>) {
        viewModelScope.launch {
            rallyRepository.createRally(Rally(
                title       = title,
                description = description,
                checkpoints = checkpoints
            )).onSuccess { _done.value = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRallyScreen(navController: NavController, vm: CreateRallyViewModel = hiltViewModel()) {
    val done  by vm.done.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var desc  by remember { mutableStateOf("") }
    val stops = remember { mutableStateListOf<Checkpoint>() }

    LaunchedEffect(done) { if (done) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Plan Rally") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
            })
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Rally Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)

            Text("Route Stops (${stops.size})", style = MaterialTheme.typography.titleSmall)
            stops.forEachIndexed { index, stop ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(stop.name.ifEmpty { "Unnamed stop" }, style = MaterialTheme.typography.bodyMedium)
                            Text(stop.type.label, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { stops.removeAt(index) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            // Add stop buttons for each type
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(CheckpointType.WAYPOINT, CheckpointType.REST_STOP, CheckpointType.FUEL).forEach { type ->
                    Button(onClick = {
                        stops.add(Checkpoint(
                            id    = UUID.randomUUID().toString(),
                            order = stops.size,
                            type  = type,
                            name  = type.label
                        ))
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, null, Modifier.padding(end = 4.dp))
                        Text(type.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Button(
                onClick = { vm.createRally(title, desc, stops) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = title.isNotBlank() && stops.isNotEmpty()
            ) { Text("Publish Rally") }
        }
    }
}
