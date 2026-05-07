package com.vorsaciew.app.ui.convoy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.core.navigation.Screen
import com.vorsaciew.app.data.model.Convoy
import com.vorsaciew.app.data.repository.ConvoyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModels ────────────────────────────────────────────────────────────────

@HiltViewModel
class ConvoysViewModel @Inject constructor(
    private val convoyRepository: ConvoyRepository
) : ViewModel() {
    val convoys: StateFlow<List<Convoy>> = convoyRepository.getActiveConvoys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

sealed class CreateConvoyState {
    object Idle    : CreateConvoyState()
    object Loading : CreateConvoyState()
    data class Success(val convoyId: String) : CreateConvoyState()
    data class Error(val message: String)    : CreateConvoyState()
}

@HiltViewModel
class CreateConvoyViewModel @Inject constructor(
    private val convoyRepository: ConvoyRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _state = MutableStateFlow<CreateConvoyState>(CreateConvoyState.Idle)
    val state: StateFlow<CreateConvoyState> = _state.asStateFlow()

    fun create(title: String, description: String, destination: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _state.value = CreateConvoyState.Loading
            convoyRepository.createConvoy(
                Convoy(title = title, description = description, destination = destination)
            )
                .onSuccess { id -> _state.value = CreateConvoyState.Success(id) }
                .onFailure { _state.value = CreateConvoyState.Error(it.message ?: "Failed to start convoy") }
        }
    }
}

// ── Screens ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvoysScreen(
    navController: NavController,
    vm: ConvoysViewModel = hiltViewModel()
) {
    val convoys by vm.convoys.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Convoys") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.CreateConvoy.route) },
                icon    = { Icon(Icons.Default.Add, null) },
                text    = { Text("Start Convoy") }
            )
        }
    ) { padding ->
        if (convoys.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DirectionsCar, null, Modifier.padding(bottom = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No active convoys", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Start one to drive together in a group!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(convoys, key = { it.id }) { convoy ->
                    ConvoyCard(convoy = convoy, onClick = {
                        navController.navigate(Screen.ConvoyDetail.createRoute(convoy.id))
                    })
                }
            }
        }
    }
}

@Composable
fun ConvoyCard(convoy: Convoy, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(convoy.title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Group, null, Modifier.padding(end = 2.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text("${convoy.memberCount}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            if (convoy.destination.isNotBlank()) {
                Text("→ ${convoy.destination}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (convoy.description.isNotBlank()) {
                Text(convoy.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Text("Led by ${convoy.creatorName.ifEmpty { "Unknown" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConvoyScreen(
    navController: NavController,
    vm: CreateConvoyViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is CreateConvoyState.Success) {
            navController.navigate(
                Screen.ConvoyDetail.createRoute((state as CreateConvoyState.Success).convoyId)
            ) { popUpTo(Screen.Convoys.route) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Start Convoy") }, navigationIcon = {
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
            Text("Gather your crew and roll out together.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Convoy Name *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = destination, onValueChange = { destination = it },
                label = { Text("Destination (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4
            )

            if (state is CreateConvoyState.Error) {
                Text((state as CreateConvoyState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick  = { vm.create(title, description, destination) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = title.isNotBlank() && state !is CreateConvoyState.Loading
            ) {
                if (state is CreateConvoyState.Loading) {
                    CircularProgressIndicator(Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                }
                Text("Roll Out")
            }
        }
    }
}
