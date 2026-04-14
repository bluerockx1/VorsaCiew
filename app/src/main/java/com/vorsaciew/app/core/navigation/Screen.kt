package com.vorsaciew.app.core.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login    : Screen("login")
    object Register : Screen("register")

    // Bottom nav tabs
    object Map     : Screen("map")
    object Events  : Screen("events")
    object Feed    : Screen("feed")
    object Clubs   : Screen("clubs")

    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }

    object EditProfile : Screen("profile/edit")

    // Events
    object EventDetail  : Screen("event/{eventId}") {
        fun createRoute(id: String) = "event/$id"
    }
    object CreateEvent  : Screen("event/create")

    // Rally
    object RallyDetail  : Screen("rally/{rallyId}") {
        fun createRoute(id: String) = "rally/$id"
    }
    object CreateRally  : Screen("rally/create")

    // Garage
    object Garage       : Screen("garage/{userId}") {
        fun createRoute(userId: String) = "garage/$userId"
    }
    object VehicleDetail: Screen("vehicle/{vehicleId}") {
        fun createRoute(id: String) = "vehicle/$id"
    }
    object AddVehicle   : Screen("vehicle/add")
    object AddBuildLog  : Screen("vehicle/{vehicleId}/buildlog/add") {
        fun createRoute(id: String) = "vehicle/$id/buildlog/add"
    }
    object AddModification : Screen("vehicle/{vehicleId}/mod/add") {
        fun createRoute(id: String) = "vehicle/$id/mod/add"
    }

    // Clubs
    object ClubDetail   : Screen("club/{clubId}") {
        fun createRoute(id: String) = "club/$id"
    }
    object CreateClub   : Screen("club/create")

    // Social
    object PostDetail   : Screen("post/{postId}") {
        fun createRoute(id: String) = "post/$id"
    }
    object CreatePost   : Screen("post/create")
    object Chat         : Screen("chat/{roomId}") {
        fun createRoute(id: String) = "chat/$id"
    }
    object ChatList     : Screen("chats")
    object Notifications: Screen("notifications")
    object Settings     : Screen("settings")
    object Search       : Screen("search")
}
