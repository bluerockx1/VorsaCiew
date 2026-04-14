package com.vorsaciew.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, vm: EditProfileViewModel = hiltViewModel()) {
    val state           by vm.state.collectAsStateWithLifecycle()
    val displayName     by vm.displayName.collectAsStateWithLifecycle()
    val bio             by vm.bio.collectAsStateWithLifecycle()
    val pendingUri      by vm.pendingAvatarUri.collectAsStateWithLifecycle()
    val savedAvatarUrl  by vm.savedAvatarUrl.collectAsStateWithLifecycle()
    val snackbar        = remember { SnackbarHostState() }

    // Photo picker — opened when user taps the avatar circle
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.pickAvatar(it) } }

    LaunchedEffect(state) {
        when (val s = state) {
            is EditProfileState.Success -> navController.popBackStack()
            is EditProfileState.Error   -> snackbar.showSnackbar(s.message)
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar picker
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val imageModel = pendingUri ?: savedAvatarUrl.ifEmpty { null }
                if (imageModel != null) {
                    AsyncImage(
                        model            = imageModel,
                        contentDescription = "Profile picture",
                        contentScale     = ContentScale.Crop,
                        modifier         = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(Modifier.fillMaxSize(), shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.Person, null, Modifier.padding(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                // Camera icon overlay
                Surface(
                    Modifier.align(Alignment.BottomEnd).size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.AddAPhoto, "Change photo",
                        Modifier.padding(4.dp),
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Text(
                "Tap to change photo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value         = displayName,
                onValueChange = vm::setDisplayName,
                label         = { Text("Display Name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value         = bio,
                onValueChange = vm::setBio,
                label         = { Text("Bio") },
                minLines      = 3,
                maxLines      = 5,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick  = vm::save,
                enabled  = state !is EditProfileState.Saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state is EditProfileState.Saving) {
                    CircularProgressIndicator(
                        Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text("Save")
            }
        }
    }
}
