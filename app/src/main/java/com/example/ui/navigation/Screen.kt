package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Editor : Screen("editor?noteId={noteId}") {
        fun createRoute(noteId: Int) = "editor?noteId=$noteId"
    }
    object Archive : Screen("archive")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object BackupSync : Screen("backup_sync")
}
