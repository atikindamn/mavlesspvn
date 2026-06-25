package com.myvpn.client.ui

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myvpn.client.ui.navigation.Screen
import com.myvpn.client.ui.navigation.bottomNavItems
import com.myvpn.client.ui.screens.home.HomeScreen
import com.myvpn.client.ui.screens.ipinfo.IpInfoScreen
import com.myvpn.client.ui.screens.logs.LogsScreen
import com.myvpn.client.ui.screens.profile.ProfileScreen
import com.myvpn.client.ui.screens.proxy.ProxyScreen
import com.myvpn.client.ui.screens.router.RouterGuideScreen
import com.myvpn.client.ui.screens.settings.SettingsScreen
import com.myvpn.client.ui.screens.settings.ThemeManager
import com.myvpn.client.ui.screens.speedtest.SpeedTestScreen
import com.myvpn.client.ui.screens.subscriptions.SubscriptionsScreen
import com.myvpn.client.ui.screens.tools.ToolsScreen
import com.myvpn.client.ui.screens.wifi.WifiAnalyzerScreen
import com.myvpn.client.ui.screens.wififinder.WifiFinderScreen
import com.myvpn.client.ui.theme.MyVPNClientTheme

class MainActivity : ComponentActivity() {
    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        return try { super.dispatchGenericMotionEvent(ev) }
        catch (e: IllegalStateException) { if (e.message?.contains("ACTION_HOVER_EXIT") == true) true else throw e }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            MyVPNClientTheme(themeMode = themeMode) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { VpnNavigation() }
            }
        }
        val mainHandler = android.os.Handler(mainLooper)
        mainHandler.post { while (true) { try { android.os.Looper.loop() } catch (e: IllegalStateException) { if (e.message?.contains("ACTION_HOVER_EXIT") != true) throw e } } }
    }
}

@Composable
fun VpnNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(selected = selected,
                            onClick = { navController.navigate(item.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Home.route, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigateToAddProfile = { navController.navigate(Screen.AddProfile.route) },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.createRoute(it)) },
                    onNavigateToLogs = { navController.navigate(Screen.Logs.route) })
            }
            composable(Screen.Tools.route) {
                ToolsScreen(onNavigateToWifi = { navController.navigate(Screen.WifiAnalyzer.route) },
                    onNavigateToSpeedTest = { navController.navigate(Screen.SpeedTest.route) },
                    onNavigateToRouter = { navController.navigate(Screen.RouterGuide.route) },
                    onNavigateToIpInfo = { navController.navigate(Screen.IpInfo.route) },
                    onNavigateToSubscriptions = { navController.navigate(Screen.Subscriptions.route) },
                    onNavigateToProxy = { navController.navigate(Screen.Proxy.route) },
                    onNavigateToWifiFinder = { navController.navigate(Screen.WifiFinder.route) })
            }
            composable(Screen.Settings.route) { SettingsScreen() }

            composable(Screen.Proxy.route) { ProxyScreen() }
            composable(Screen.WifiAnalyzer.route) { WifiAnalyzerScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.WifiFinder.route) { WifiFinderScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.SpeedTest.route) { SpeedTestScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.RouterGuide.route) { RouterGuideScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.IpInfo.route) { IpInfoScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.Subscriptions.route) { SubscriptionsScreen() }

            composable(Screen.Logs.route) { LogsScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.AddProfile.route) { ProfileScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.EditProfile.route, arguments = listOf(navArgument("profileId") { type = NavType.LongType })) {
                ProfileScreen(profileId = it.arguments?.getLong("profileId"), onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
