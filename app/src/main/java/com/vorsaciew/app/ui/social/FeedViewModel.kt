package com.vorsaciew.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.Post
import com.vorsaciew.app.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    val posts: StateFlow<List<Post>> = postRepository.getFeedPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    fun toggleLike(postId: String) {
        // Optimistic toggle — UI updates immediately
        _likedIds.value = if (postId in _likedIds.value) _likedIds.value - postId else _likedIds.value + postId
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch { postRepository.deletePost(postId) }
    }
}
