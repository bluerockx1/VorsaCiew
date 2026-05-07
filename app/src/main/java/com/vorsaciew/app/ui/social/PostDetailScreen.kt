package com.vorsaciew.app.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.vorsaciew.app.core.util.toRelativeTime
import com.vorsaciew.app.data.model.Post
import com.vorsaciew.app.data.model.PostComment
import com.vorsaciew.app.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val postRepository: PostRepository
) : ViewModel() {
    private val postId = savedState.get<String>("postId") ?: ""
    val post: StateFlow<Post?> = postRepository.getPostFlow(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val comments: StateFlow<List<PostComment>> = postRepository.getComments(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun addComment(text: String) {
        viewModelScope.launch {
            postRepository.addComment(postId, PostComment(text = text))
        }
    }

    fun toggleLike() {
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(navController: NavController, vm: PostDetailViewModel = hiltViewModel()) {
    val post     by vm.post.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    var text     by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Post") }, navigationIcon = {
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
                    placeholder = { Text("Add a comment…") },
                    modifier = Modifier.weight(1f), maxLines = 3)
                IconButton(onClick = { if (text.isNotBlank()) { vm.addComment(text); text = "" } },
                    enabled = text.isNotBlank()) {
                    Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            post?.let { p ->
                item {
                    PostCard(post = p, onLike = { vm.toggleLike() }, onComment = {}, onAuthorClick = {})
                    HorizontalDivider()
                    Text("Comments", Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleSmall)
                }
            }
            items(comments, key = { it.id }) { c ->
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(c.authorName, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(c.text, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(c.timestamp.toRelativeTime(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        }
    }
}
