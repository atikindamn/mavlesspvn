package com.myvpn.client.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddProfile : Screen("add_profile")
    data object EditProfile : Screen("edit_profile/{profileId}") {
        fun createRoute(profileId: Long) = "edit_profile/$profileId"
    }
    data object Logs : Screen("logs")
    data object IpInfo : Screen("ip_info")
    data object Subscriptions : Screen("subscriptions")
    data object Proxy : Screen("proxy")
    data object Tools : Screen("tools")
    data object WifiAnalyzer : Screen("wifi_analyzer")
    data object WifiFinder : Screen("wifi_finder")
    data object SpeedTest : Screen("speed_test")
    data object RouterGuide : Screen("router_guide")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("VPN", Screen.Home.route, Icons.Filled.VpnLock, Icons.Outlined.VpnLock),
    BottomNavItem("Инструменты", Screen.Tools.route, Icons.Filled.Build, Icons.Outlined.Build),
    BottomNavItem("Настройки", Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)
