package com.vorsaciew.app.ui.clubs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vorsaciew.app.core.navigation.Screen
import com.vorsaciew.app.ui.events.EventCard
import com.vorsaciew.app.ui.social.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailScreen(navController: NavController, vm: ClubDetailViewModel = hiltViewModel()) {
    val club     by vm.club.collectAsStateWithLifecycle()
    val posts    by vm.posts.collectAsStateWithLifecycle()
    val events   by vm.events.collectAsStateWithLifecycle()
    val isMember by vm.isMember.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Feed", "Events", "Members")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(club?.name ?: "Club") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isMember && !club?.chatRoomId.isNullOrEmpty()) {
                        IconButton(onClick = {
                            navController.navigate(Screen.Chat.createRoute(club!!.chatRoomId))
                        }) { Icon(Icons.Default.Message, "Chat") }
                    }
                }
            )
        }
    ) { padding ->
        club?.let { c ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Cover
                AsyncImage(model = c.coverPhotoUrl.ifEmpty { null }, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp))
                // Header info
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(c.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${c.memberCount} members", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isMember) {
                            OutlinedButton(onClick = { vm.leave() }) { Text("Leave") }
                        } else {
                            Button(onClick = { vm.join() }) { Text("Join") }
                        }
                    }
                }
                // Tabs
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { i, label ->
                        Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                            text = { Text(label) })
                    }
                }
                when (selectedTab) {
                    0 -> LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(posts, key = { it.id }) { post ->
                            PostCard(post = post, onLike = {}, onComment = {
                                navController.navigate(Screen.PostDetail.createRoute(post.id))
                            }, onAuthorClick = {
                                navController.navigate(Screen.Profile.createRoute(post.authorId))
                            })
                        }
                    }
                    1 -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events, key = { it.id }) { event ->
                            EventCard(event = event, onClick = {
                                navController.navigate(Screen.EventDetail.createRoute(event.id))
                            })
                        }
                    }
                    2 -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(c.memberIds) { uid ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.size(36.dp)) {
                                    Text(uid.take(2).uppercase(),
                                        Modifier.align(Alignment.Center),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                Text(uid, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
