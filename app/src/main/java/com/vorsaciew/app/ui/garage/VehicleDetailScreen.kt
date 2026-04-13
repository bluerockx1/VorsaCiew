package com.vorsaciew.app.ui.garage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vorsaciew.app.core.navigation.Screen
import com.vorsaciew.app.core.util.toFormattedDate
import com.vorsaciew.app.data.model.BuildLog
import com.vorsaciew.app.data.model.Modification

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VehicleDetailScreen(navController: NavController, vm: VehicleDetailViewModel = hiltViewModel()) {
    val vehicle  by vm.vehicle.collectAsStateWithLifecycle()
    val isOwner  by vm.isOwner.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vehicle?.nickname?.ifEmpty { vehicle?.label } ?: "Vehicle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isOwner) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(
                        onClick = { vehicle?.id?.let { navController.navigate(Screen.AddBuildLog.createRoute(it)) } },
                        modifier = Modifier.size(40.dp)
                    ) { Icon(Icons.Default.Build, null, Modifier.size(20.dp)) }
                    FloatingActionButton(
                        onClick = { vehicle?.id?.let { navController.navigate(Screen.AddModification.createRoute(it)) } }
                    ) { Icon(Icons.Default.Add, "Add modification") }
                }
            }
        }
    ) { padding ->
        vehicle?.let { v ->
            val pagerState = rememberPagerState { v.photoUrls.size.coerceAtLeast(1) }
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                // Photo pager
                if (v.photoUrls.isNotEmpty()) {
                    item {
                        HorizontalPager(state = pagerState) { page ->
                            AsyncImage(
                                model = v.photoUrls[page],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(260.dp)
                            )
                        }
                    }
                }

                // Stats
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        v.horsepower?.let { StatChip("$it hp") }
                        v.torque?.let { StatChip("$it lb-ft") }
                        StatChip(v.driveType.name)
                    }
                }

                // Description
                if (v.description.isNotEmpty()) {
                    item {
                        Text(v.description, Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Modifications
                item {
                    Text("Modifications (${v.modifications.size})",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (v.modifications.isEmpty()) {
                    item { Text("No mods listed yet", Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(v.modifications) { mod -> ModCard(mod) }
                }

                // Build log
                item {
                    Text("Build Log (${v.buildLogs.size})",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(v.buildLogs.sortedByDescending { it.timestamp }) { log -> BuildLogCard(log) }

                item { Spacer(Modifier.height(88.dp)) }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun StatChip(label: String) {
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(label,
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ModCard(mod: Modification) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(mod.name, style = MaterialTheme.typography.titleSmall)
                if (mod.brand.isNotEmpty())
                    Text(mod.brand, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (mod.category.isNotEmpty())
                    Text(mod.category, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun BuildLogCard(log: BuildLog) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(log.title, style = MaterialTheme.typography.titleSmall)
                Text(log.timestamp.toFormattedDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (log.description.isNotEmpty())
                Text(log.description, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                log.mileage?.let { Text("$it mi", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                log.costUsd?.let { Text("$${it.toInt()}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
