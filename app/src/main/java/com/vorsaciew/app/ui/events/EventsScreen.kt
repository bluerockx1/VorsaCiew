package com.vorsaciew.app.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.vorsaciew.app.core.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(navController: NavController, vm: EventsViewModel = hiltViewModel()) {
    val events         by vm.events.collectAsStateWithLifecycle()
    val selectedFilter by vm.filter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Events") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.CreateEvent.route) },
                icon    = { Icon(Icons.Default.Add, null) },
                text    = { Text("Host Event") }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.foundation.layout.Column {
                // Quick-access row for Rallies and Convoys
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick   = { navController.navigate(Screen.Rallies.route) },
                        modifier  = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FlagCircle, null, Modifier.padding(end = 4.dp))
                        Text("Rallies")
                    }
                    FilledTonalButton(
                        onClick   = { navController.navigate(Screen.Convoys.route) },
                        modifier  = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, Modifier.padding(end = 4.dp))
                        Text("Convoys")
                    }
                }

                ScrollableTabRow(selectedTabIndex = selectedFilter.ordinal) {
                    EventFilter.values().forEach { filter ->
                        Tab(
                            selected = selectedFilter == filter,
                            onClick  = { vm.setFilter(filter) },
                            text     = { Text(filter.label) }
                        )
                    }
                }
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(event = event, onClick = {
                            navController.navigate(Screen.EventDetail.createRoute(event.id))
                        })
                    }
                    if (events.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxSize().padding(top = 48.dp),
                                contentAlignment = Alignment.Center) {
                                Text("No events found")
                            }
                        }
                    }
                }
            }
        }
    }
}
