package com.vorsaciew.app.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.vorsaciew.app.data.model.BuildLog
import com.vorsaciew.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBuildLogViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {
    private val vehicleId = savedState.get<String>("vehicleId") ?: ""
    private val _state = MutableStateFlow<AddVehicleState>(AddVehicleState.Idle)
    val state: StateFlow<AddVehicleState> = _state.asStateFlow()

    fun addLog(log: BuildLog) {
        viewModelScope.launch {
            _state.value = AddVehicleState.Loading
            vehicleRepository.addBuildLog(vehicleId, log)
                .onSuccess { _state.value = AddVehicleState.Success }
                .onFailure { _state.value = AddVehicleState.Error(it.message ?: "Failed") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBuildLogScreen(navController: NavController, vm: AddBuildLogViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var title   by remember { mutableStateOf("") }
    var desc    by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var cost    by remember { mutableStateOf("") }

    LaunchedEffect(state) { if (state is AddVehicleState.Success) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Log Entry") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
            })
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Details") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6)
            OutlinedTextField(value = mileage, onValueChange = { mileage = it }, label = { Text("Mileage (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost USD (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (state is AddVehicleState.Error)
                Text((state as AddVehicleState.Error).message, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { vm.addLog(BuildLog(title = title, description = desc,
                    mileage = mileage.toIntOrNull(), costUsd = cost.toDoubleOrNull())) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = title.isNotBlank() && state !is AddVehicleState.Loading
            ) { Text("Save Entry") }
        }
    }
}
