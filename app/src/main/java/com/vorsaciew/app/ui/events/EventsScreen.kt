package com.vorsaciew.app.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
