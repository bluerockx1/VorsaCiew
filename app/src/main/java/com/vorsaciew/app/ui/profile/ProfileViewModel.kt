package com.vorsaciew.app.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vorsaciew.app.data.model.Post
import com.vorsaciew.app.data.model.User
import com.vorsaciew.app.data.model.Vehicle
import com.vorsaciew.app.data.repository.ChatRepository
import com.vorsaciew.app.data.repository.PostRepository
import com.vorsaciew.app.data.repository.UserRepository
import com.vorsaciew.app.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.vorsaciew.app.core.navigation.Screen
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val vehicleRepository: VehicleRepository,
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val userId: String = savedState["userId"] ?: auth.currentUser?.uid ?: ""

    val user: StateFlow<User?> = userRepository.getUserFlow(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val posts: StateFlow<List<Post>> = postRepository.getPostsForUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val vehicles: StateFlow<List<Vehicle>> = vehicleRepository.getVehiclesForUser(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isOwnProfile: StateFlow<Boolean> = flow {
        emit(auth.currentUser?.uid == userId)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isFollowing: StateFlow<Boolean> = flow {
        emit(userRepository.isFollowing(userId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun follow()   { viewModelScope.launch { userRepository.followUser(userId) } }
    fun unfollow() { viewModelScope.launch { userRepository.unfollowUser(userId) } }

    fun startDm(navController: NavController) {
        viewModelScope.launch {
            val u = user.value ?: return@launch
            chatRepository.getOrCreateDirectRoom(userId, u.displayName, u.avatarUrl)
                .onSuccess { roomId -> navController.navigate(Screen.Chat.createRoute(roomId)) }
        }
    }
}
