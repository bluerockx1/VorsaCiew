package com.vorsaciew.app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vorsaciew.app.ui.auth.AuthState
import com.vorsaciew.app.ui.auth.AuthViewModel
import com.vorsaciew.app.ui.auth.LoginScreen
import com.vorsaciew.app.ui.auth.RegisterScreen
import com.vorsaciew.app.ui.clubs.ClubDetailScreen
import com.vorsaciew.app.ui.clubs.ClubsScreen
import com.vorsaciew.app.ui.clubs.CreateClubScreen
import com.vorsaciew.app.ui.events.CreateEventScreen
import com.vorsaciew.app.ui.events.EventDetailScreen
import com.vorsaciew.app.ui.events.EventsScreen
import com.vorsaciew.app.ui.garage.AddBuildLogScreen
import com.vorsaciew.app.ui.garage.AddModificationScreen
import com.vorsaciew.app.ui.garage.AddVehicleScreen
import com.vorsaciew.app.ui.garage.GarageScreen
import com.vorsaciew.app.ui.garage.VehicleDetailScreen
import com.vorsaciew.app.ui.map.MapScreen
import com.vorsaciew.app.ui.profile.EditProfileScreen
import com.vorsaciew.app.ui.profile.ProfileScreen
import com.vorsaciew.app.ui.rally.CreateRallyScreen
import com.vorsaciew.app.ui.rally.RallyDetailScreen
import com.vorsaciew.app.ui.social.ChatListScreen
import com.vorsaciew.app.ui.social.ChatScreen
import com.vorsaciew.app.ui.social.CreatePostScreen
import com.vorsaciew.app.ui.social.FeedScreen
import com.vorsaciew.app.ui.social.NotificationsScreen
import com.vorsaciew.app.ui.social.PostDetailScreen
import com.vorsaciew.app.ui.social.SearchScreen
import com.vorsaciew.app.ui.social.SettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun VorsaCiewNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUid = (authState as? AuthState.Authenticated)?.uid ?: ""

    // Redirect to Login whenever the user becomes unauthenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomTabs = listOf(
        BottomNavItem(Screen.Map,    Icons.Default.Map,         "Map"),
        BottomNavItem(Screen.Events, Icons.Default.Event,       "Events"),
        BottomNavItem(Screen.Feed,   Icons.Default.DynamicFeed, "Feed"),
        BottomNavItem(Screen.Clubs,  Icons.Default.Group,       "Clubs"),
        BottomNavItem(Screen.Profile, Icons.Default.DirectionsCar, "Me"),
    )

    val startDestination = when (authState) {
        is AuthState.Authenticated -> Screen.Map.route
        else -> Screen.Login.route
    }

    Scaffold(
        bottomBar = {
            if (authState is AuthState.Authenticated) {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                NavigationBar {
                    bottomTabs.forEach { item ->
                        val route = if (item.screen == Screen.Profile)
                            Screen.Profile.createRoute(currentUid)
                        else item.screen.route
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route ||
                                    (item.screen == Screen.Profile && currentRoute?.startsWith("profile/") == true),
                            onClick  = {
                                navController.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(Screen.Map.route) { saveState = true }
                                }
                            },
                            icon  = { Icon(item.icon, item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(padding)
        ) {
            // Auth
            composable(Screen.Login.route)    { LoginScreen(navController) }
            composable(Screen.Register.route) { RegisterScreen(navController) }

            // Main tabs
            composable(Screen.Map.route)    { MapScreen(navController) }
            composable(Screen.Events.route) { EventsScreen(navController) }
            composable(Screen.Feed.route)   { FeedScreen(navController) }
            composable(Screen.Clubs.route)  { ClubsScreen(navController) }

            composable(
                Screen.Profile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { ProfileScreen(navController) }

            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }

            // Events
            composable(
                Screen.EventDetail.route,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { EventDetailScreen(navController) }
            composable(Screen.CreateEvent.route) { CreateEventScreen(navController) }

            // Rally
            composable(
                Screen.RallyDetail.route,
                arguments = listOf(navArgument("rallyId") { type = NavType.StringType })
            ) { RallyDetailScreen(navController) }
            composable(Screen.CreateRally.route) { CreateRallyScreen(navController) }

            // Garage
            composable(
                Screen.Garage.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { GarageScreen(navController) }
            composable(
                Screen.VehicleDetail.route,
                arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
            ) { VehicleDetailScreen(navController) }
            composable(Screen.AddVehicle.route) { AddVehicleScreen(navController) }
            composable(
                Screen.AddBuildLog.route,
                arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
            ) { AddBuildLogScreen(navController) }
            composable(
                Screen.AddModification.route,
                arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
            ) { AddModificationScreen(navController) }

            // Clubs
            composable(
                Screen.ClubDetail.route,
                arguments = listOf(navArgument("clubId") { type = NavType.StringType })
            ) { ClubDetailScreen(navController) }
            composable(Screen.CreateClub.route) { CreateClubScreen(navController) }

            // Social
            composable(
                Screen.PostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { PostDetailScreen(navController) }
            composable(Screen.CreatePost.route) { CreatePostScreen(navController) }
            composable(
                Screen.Chat.route,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType })
            ) { ChatScreen(navController) }
            composable(Screen.ChatList.route)      { ChatListScreen(navController) }
            composable(Screen.Notifications.route) { NotificationsScreen(navController) }
            composable(Screen.Settings.route)      { SettingsScreen(navController, authViewModel) }
            composable(Screen.Search.route)        { SearchScreen(navController) }
        }
    }
}
