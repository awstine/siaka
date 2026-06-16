package com.siaka.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Map : Screen("map", "Map", Icons.Default.Map)
    object Routes : Screen("routes", "Routes", Icons.Default.Route)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Map, Routes, Profile, Settings)
    }
}
