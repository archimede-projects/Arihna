package com.archimedeprojects.arihna.app

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver
import com.archimedeprojects.arihna.feature.alarms.AlarmsRoute
import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticTestScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory
import com.archimedeprojects.arihna.feature.home.HomePrayerScheduleRoute
import com.archimedeprojects.arihna.feature.prayers.PrayerTimesRoute
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import com.archimedeprojects.arihna.feature.qibla.QiblaRoute
import com.archimedeprojects.arihna.feature.qibla.domain.QiblaRepository
import com.archimedeprojects.arihna.feature.quran.QuranPlaceholderScreen
import com.archimedeprojects.arihna.feature.settings.LocationSettingsRoute
import com.archimedeprojects.arihna.feature.settings.LocationSettingsViewModel

private val AlbaNavBar = Color(0xFFFFFAEF)
private val AlbaNavIcon = Color(0xFF52705F)
private val AlbaNavSelectedIcon = Color(0xFF173C30)
private val AlbaNavIndicator = Color(0xFFDDE6C9)

private enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Rounded.Home),
    Prayers("prayers", "Orari", Icons.Rounded.Schedule),
    Qibla("qibla", "Qibla", Icons.Rounded.Explore),
    Quran("quran", "Corano", Icons.Rounded.MenuBook),
    Alarms("alarms", "Sveglie", Icons.Rounded.Alarm),
    Settings("settings", "Impostazioni", Icons.Rounded.Settings),
}

@Composable
fun ArihnaNavHost(
    activity: Activity,
    locationSettingsViewModel: LocationSettingsViewModel,
    prayerScheduleViewModel: PrayerScheduleViewModel,
    alarmsViewModel: AlarmsViewModel,
    exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory,
    alarmFullScreenAccess: AlarmFullScreenAccess,
    alarmDiagnosticTestScheduler: AlarmDiagnosticTestScheduler,
    qiblaRepository: QiblaRepository,
    locationEnvironment: AndroidLocationEnvironment,
    locationPermissionStateResolver: AndroidLocationPermissionStateResolver,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = AlbaNavBar, contentColor = AlbaNavIcon) {
                Destination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigateTopLevel(
                                route = destination.route,
                                homeRoute = Destination.Home.route,
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = null,
    alwaysShowLabel = false,
    colors = NavigationBarItemDefaults.colors(
        selectedIconColor = AlbaNavSelectedIcon,
        unselectedIconColor = AlbaNavIcon,
        indicatorColor = AlbaNavIndicator,
    ),
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
                        navController.navigateTopLevel(Destination.Settings.route, Destination.Home.route)
                    },
                    onOpenQibla = {
                        navController.navigateTopLevel(Destination.Qibla.route, Destination.Home.route)
                    },
                    onOpenAlarms = {
                        navController.navigateTopLevel(Destination.Alarms.route, Destination.Home.route)
                    },
                    onRefreshLocation = {
                        val permissionState = locationPermissionStateResolver.resolve(
                            activity = activity,
                            hasRequestedBefore = locationSettingsViewModel.hasRequestedPermissionBefore(),
                        )
                        if (
                            permissionState == LocationPermissionState.Granted &&
                            locationEnvironment.isLocationServicesEnabled()
                        ) {
                            locationSettingsViewModel.selectDevice(
                                permissionState = permissionState,
                                locationServicesEnabled = true,
                            )
                        } else {
                            navController.navigateTopLevel(Destination.Settings.route, Destination.Home.route)
                        }
                    },
                )
            }
            composable(Destination.Prayers.route) {
                PrayerTimesRoute(
                    contentPadding = innerPadding,
                    prayerScheduleViewModel = prayerScheduleViewModel,
                    alarmsViewModel = alarmsViewModel,
                )
            }
            composable(Destination.Qibla.route) { qiblaBackStackEntry ->
                val lifecycleBoundQiblaStates = remember(qiblaRepository, qiblaBackStackEntry) {
                    qiblaRepository.observeQibla().flowWithLifecycle(
                        lifecycle = qiblaBackStackEntry.lifecycle,
                        minActiveState = Lifecycle.State.STARTED,
                    )
                }
                QiblaRoute(
                    contentPadding = innerPadding,
                    states = lifecycleBoundQiblaStates,
                    onOpenLocationSettings = {
                        navController.navigateTopLevel(Destination.Settings.route, Destination.Home.route)
                    },
                )
            }
            composable(Destination.Quran.route) { QuranPlaceholderScreen(innerPadding) }
            composable(Destination.Alarms.route) {
                AlarmsRoute(contentPadding = innerPadding, viewModel = alarmsViewModel)
            }
            composable(Destination.Settings.route) {
                LocationSettingsRoute(
                    contentPadding = innerPadding,
                    activity = activity,
                    viewModel = locationSettingsViewModel,
                    environment = locationEnvironment,
                    permissionResolver = locationPermissionStateResolver,
                    alarmsViewModel = alarmsViewModel,
                    exactAlarmAccessIntentFactory = exactAlarmAccessIntentFactory,
                    alarmFullScreenAccess = alarmFullScreenAccess,
                    alarmDiagnosticTestScheduler = alarmDiagnosticTestScheduler,
                )
            }
        }
    }
}
