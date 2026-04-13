package com.vorsaciew.app.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.vorsaciew.app.data.model.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(navController: NavController, vm: AddVehicleViewModel = hiltViewModel()) {
    val formState by vm.formState.collectAsStateWithLifecycle()
    var year      by remember { mutableStateOf("") }
    var make      by remember { mutableStateOf("") }
    var model     by remember { mutableStateOf("") }
    var trim      by remember { mutableStateOf("") }
    var color     by remember { mutableStateOf("") }
    var nickname  by remember { mutableStateOf("") }
    var hp        by remember { mutableStateOf("") }
    var desc      by remember { mutableStateOf("") }

    LaunchedEffect(formState) {
        if (formState is AddVehicleState.Success) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Vehicle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = year, onValueChange = { year = it },
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = make, onValueChange = { make = it },
                    label = { Text("Make") },
                    modifier = Modifier.weight(2f), singleLine = true
                )
            }
            OutlinedTextField(value = model, onValueChange = { model = it },
                label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = trim, onValueChange = { trim = it },
                label = { Text("Trim (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = color, onValueChange = { color = it },
                label = { Text("Color") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = nickname, onValueChange = { nickname = it },
                label = { Text("Nickname (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                value = hp, onValueChange = { hp = it },
                label = { Text("Horsepower (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(value = desc, onValueChange = { desc = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                minLines = 2, maxLines = 4)

            if (formState is AddVehicleState.Error) {
                Text((formState as AddVehicleState.Error).message,
                    color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.addVehicle(
                        Vehicle(
                            year = year.toIntOrNull() ?: 0,
                            make = make, model = model, trim = trim,
                            color = color, nickname = nickname,
                            horsepower = hp.toIntOrNull(),
                            description = desc
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = make.isNotBlank() && model.isNotBlank() && formState !is AddVehicleState.Loading
            ) { Text("Add to Garage") }
        }
    }
}
