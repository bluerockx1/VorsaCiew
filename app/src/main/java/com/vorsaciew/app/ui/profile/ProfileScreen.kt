package com.vorsaciew.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vorsaciew.app.core.navigation.Screen
import com.vorsaciew.app.ui.garage.VehicleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, vm: ProfileViewModel = hiltViewModel()) {
    val user        by vm.user.collectAsStateWithLifecycle()
    val posts       by vm.posts.collectAsStateWithLifecycle()
    val vehicles    by vm.vehicles.collectAsStateWithLifecycle()
    val isOwn       by vm.isOwnProfile.collectAsStateWithLifecycle()
    val isFollowing by vm.isFollowing.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.username ?: "") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isOwn) {
                        IconButton(onClick = { vm.startDm(navController) }) {
                            Icon(Icons.Default.Message, "Message")
                        }
                    }
                }
            )
        }
    ) { padding ->
        user?.let { u ->
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    // Cover + avatar
                    Box {
                        AsyncImage(
                            model = u.coverPhotoUrl.ifEmpty { null },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                        if (u.avatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = u.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(72.dp).clip(CircleShape)
                                    .align(Alignment.BottomStart).offset(x = 16.dp, y = 36.dp)
                            )
                        } else {
                            Surface(
                                Modifier.size(72.dp).align(Alignment.BottomStart).offset(x = 16.dp, y = 36.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(Icons.Default.Person, null, Modifier.padding(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(44.dp))

                    // Info row
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(u.displayName.ifEmpty { u.username },
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("@${u.username}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isOwn) {
                            OutlinedButton(onClick = {}) { Text("Edit Profile") }
                        } else {
                            if (isFollowing) {
                                OutlinedButton(onClick = { vm.unfollow() }) { Text("Unfollow") }
                            } else {
                                Button(onClick = { vm.follow() }) { Text("Follow") }
                            }
                        }
                    }

                    if (u.bio.isNotEmpty()) {
                        Text(u.bio, Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium)
                    }

                    // Stats row
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        StatItem("${u.postCount}", "Posts")
                        StatItem("${u.followerCount}", "Followers")
                        StatItem("${u.followingCount}", "Following")
                    }

                    // Garage icon link
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, Modifier.size(16.dp))
                        Text("${vehicles.size} vehicles", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f))
                    }

                    // Tabs
                    ScrollableTabRow(selectedTabIndex = selectedTab) {
                        listOf("Posts", "Garage").forEachIndexed { i, label ->
                            Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                                text = { Text(label) })
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // Post grid — embed in a separate fixed-height container
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxWidth().height(
                                    (((posts.size + 2) / 3) * 130).dp
                                ),
                                userScrollEnabled = false
                            ) {
                                items(posts, key = { it.id }) { post ->
                                    if (post.mediaUrls.isNotEmpty()) {
                                        AsyncImage(
                                            model = post.mediaUrls.first(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.aspectRatio(1f).padding(1.dp)
                                        )
                                    } else {
                                        Surface(Modifier.aspectRatio(1f).padding(1.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant) {
                                            Text(post.caption.take(40),
                                                Modifier.padding(4.dp),
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        items(vehicles, key = { it.id }) { vehicle ->
                            VehicleCard(vehicle = vehicle, onClick = {
                                navController.navigate(Screen.VehicleDetail.createRoute(vehicle.id))
                            }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
