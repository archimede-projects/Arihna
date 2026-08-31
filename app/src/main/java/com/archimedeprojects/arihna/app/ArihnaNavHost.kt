package com.archimedeprojects.arihna.app

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver
import com.archimedeprojects.arihna.feature.alarms.AlarmsPlaceholderScreen
import com.archimedeprojects.arihna.feature.home.HomePrayerScheduleRoute
import com.archimedeprojects.arihna.feature.prayers.PrayerTimesPlaceholderScreen
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import com.archimedeprojects.arihna.feature.qibla.QiblaPlaceholderScreen
import com.archimedeprojects.arihna.feature.quran.QuranPlaceholderScreen
import com.archimedeprojects.arihna.feature.settings.LocationSettingsRoute
import com.archimedeprojects.arihna.feature.settings.LocationSettingsViewModel

private enum class Destination(
    val route: String,
    val label: String,
    val shortLabel: String,
) {
    Home("home", "Home", "H"),
    Prayers("prayers", "Orari", "O"),
    Qibla("qibla", "Qibla", "Q"),
    Quran("quran", "Corano", "C"),
    Alarms("alarms", "Sveglie", "S"),
    Settings("settings", "Impostazioni", "I"),
}

@Composable
fun ArihnaNavHost(
    activity: Activity,
    locationSettingsViewModel: LocationSettingsViewModel,
    prayerScheduleViewModel: PrayerScheduleViewModel,
    locationEnvironment: AndroidLocationEnvironment,
    locationPermissionStateResolver: AndroidLocationPermissionStateResolver,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(Destination.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(destination.shortLabel) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Destination.Home.route) {
                HomePrayerScheduleRoute(
                    contentPadding = innerPadding,
                    viewModel = prayerScheduleViewModel,
                    onOpenLocationSettings = {
                        navController.navigate(Destination.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Destination.Prayers.route) { PrayerTimesPlaceholderScreen(innerPadding) }
            composable(Destination.Qibla.route) { QiblaPlaceholderScreen(innerPadding) }
            composable(Destination.Quran.route) { QuranPlaceholderScreen(innerPadding) }
            composable(Destination.Alarms.route) { AlarmsPlaceholderScreen(innerPadding) }
            composable(Destination.Settings.route) {
                LocationSettingsRoute(
                    contentPadding = innerPadding,
                    activity = activity,
                    viewModel = locationSettingsViewModel,
                    environment = locationEnvironment,
                    permissionResolver = locationPermissionStateResolver,
                )
            }
        }
    }
}
