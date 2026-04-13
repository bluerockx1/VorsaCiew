package com.vorsaciew.app.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.core.util.toRelativeTime
import com.vorsaciew.app.data.model.ChatMessage
import com.vorsaciew.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    val roomId: String = savedState.get<String>("roomId") ?: ""
    val currentUserId: String get() = auth.currentUser?.uid ?: ""
    val messages: StateFlow<List<ChatMessage>> = chatRepository.getMessages(roomId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            chatRepository.sendMessage(roomId, ChatMessage(
                senderId       = uid,
                senderName     = auth.currentUser?.displayName ?: "Driver",
                text           = text
            ))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, vm: ChatViewModel = hiltViewModel()) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chat") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
            })
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(8.dp).imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = text, onValueChange = { text = it },
                    placeholder = { Text("Message…") },
                    modifier = Modifier.weight(1f), maxLines = 4)
                IconButton(
                    onClick  = { if (text.isNotBlank()) { vm.sendMessage(text); text = "" } },
                    enabled  = text.isNotBlank()
                ) { Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.primary) }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isOwn = msg.senderId == vm.currentUserId
                Box(Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .align(if (isOwn) Alignment.CenterEnd else Alignment.CenterStart),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOwn) MaterialTheme.colorScheme.primaryContainer
                                             else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            if (!isOwn) {
                                Text(msg.senderName, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                            Text(msg.timestamp.toRelativeTime(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }
    }
}
