package com.vorsaciew.app.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.vorsaciew.app.data.model.Post
import com.vorsaciew.app.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()
    fun create(caption: String) {
        viewModelScope.launch {
            postRepository.createPost(Post(caption = caption))
                .onSuccess { _done.value = true }
            // on failure: stay on screen so user can retry
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(navController: NavController, vm: CreatePostViewModel = hiltViewModel()) {
    val done    by vm.done.collectAsStateWithLifecycle()
    var caption by remember { mutableStateOf("") }

    LaunchedEffect(done) { if (done) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("New Post") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
            })
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = caption, onValueChange = { caption = it },
                label = { Text("What's happening on the road?") },
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
            Button(
                onClick = { vm.create(caption) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = caption.isNotBlank()
            ) { Text("Post") }
        }
    }
}
