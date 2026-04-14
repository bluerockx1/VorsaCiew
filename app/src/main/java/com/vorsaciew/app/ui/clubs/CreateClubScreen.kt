package com.vorsaciew.app.ui.clubs

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.vorsaciew.app.data.model.Club
import com.vorsaciew.app.data.repository.ClubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CreateClubState {
    object Idle    : CreateClubState()
    object Loading : CreateClubState()
    data class Success(val clubId: String) : CreateClubState()
    data class Error(val message: String)  : CreateClubState()
}

@HiltViewModel
class CreateClubViewModel @Inject constructor(
    private val clubRepository: ClubRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CreateClubState>(CreateClubState.Idle)
    val state: StateFlow<CreateClubState> = _state.asStateFlow()

    fun createClub(name: String, description: String, tags: List<String>, isPrivate: Boolean) {
        viewModelScope.launch {
            _state.value = CreateClubState.Loading
            clubRepository.createClub(
                Club(name = name, description = description, tags = tags, isPrivate = isPrivate)
            )
                .onSuccess { id -> _state.value = CreateClubState.Success(id) }
                .onFailure { _state.value = CreateClubState.Error(it.message ?: "Failed to create club") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClubScreen(navController: NavController, vm: CreateClubViewModel = hiltViewModel()) {
    val state   by vm.state.collectAsStateWithLifecycle()
    var name    by remember { mutableStateOf("") }
    var desc    by remember { mutableStateOf("") }
    var tagsStr by remember { mutableStateOf("") }
    var private by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        when (val s = state) {
            is CreateClubState.Success -> navController.popBackStack()
            is CreateClubState.Error   -> snackbar.showSnackbar(s.message)
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Create Club") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) }
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
                value = name, onValueChange = { name = it },
                label = { Text("Club Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = desc, onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4
            )
            OutlinedTextField(
                value = tagsStr, onValueChange = { tagsStr = it },
                label = { Text("Tags (comma-separated, e.g. jdm, euro)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Private Club", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = private, onCheckedChange = { private = it })
            }
            Button(
                onClick = {
                    val tags = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    vm.createClub(name, desc, tags, private)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = name.isNotBlank() && state !is CreateClubState.Loading
            ) {
                if (state is CreateClubState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text("Create Club")
            }
        }
    }
}
