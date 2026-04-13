package com.vorsaciew.app.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.vorsaciew.app.data.model.Modification
import com.vorsaciew.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val MOD_CATEGORIES = listOf("Engine", "Exhaust", "Suspension", "Wheels/Tires", "Aero", "Interior", "Exterior", "Brakes", "Transmission", "Other")

@HiltViewModel
class AddModificationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {
    private val vehicleId = savedState.get<String>("vehicleId") ?: ""
    private val _state = MutableStateFlow<AddVehicleState>(AddVehicleState.Idle)
    val state: StateFlow<AddVehicleState> = _state.asStateFlow()

    fun addMod(mod: Modification) {
        viewModelScope.launch {
            _state.value = AddVehicleState.Loading
            vehicleRepository.addModification(vehicleId, mod)
                .onSuccess { _state.value = AddVehicleState.Success }
                .onFailure { _state.value = AddVehicleState.Error(it.message ?: "Failed") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddModificationScreen(navController: NavController, vm: AddModificationViewModel = hiltViewModel()) {
    val state    by vm.state.collectAsStateWithLifecycle()
    var name     by remember { mutableStateOf("") }
    var brand    by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MOD_CATEGORIES.first()) }
    var notes    by remember { mutableStateOf("") }
    var catMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state) { if (state is AddVehicleState.Success) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add Modification") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
            })
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(expanded = catMenuOpen, onExpandedChange = { catMenuOpen = it }) {
                OutlinedTextField(value = category, onValueChange = {}, readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = catMenuOpen, onDismissRequest = { catMenuOpen = false }) {
                    MOD_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; catMenuOpen = false })
                    }
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Modification Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
            if (state is AddVehicleState.Error)
                Text((state as AddVehicleState.Error).message, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = { vm.addMod(Modification(name = name, brand = brand, category = category, notes = notes)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = name.isNotBlank() && state !is AddVehicleState.Loading
            ) { Text("Add Mod") }
        }
    }
}
