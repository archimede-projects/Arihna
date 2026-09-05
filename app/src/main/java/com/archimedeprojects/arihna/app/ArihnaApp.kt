package com.archimedeprojects.arihna.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.archimedeprojects.arihna.core.prayer.calculation.AdhanPrayerTimeCalculator
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel
import com.archimedeprojects.arihna.feature.prayerschedule.domain.DefaultPrayerScheduleRepository
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.OneSecondPrayerScheduleTicker
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import com.archimedeprojects.arihna.feature.qibla.domain.DefaultQiblaRepository
import com.archimedeprojects.arihna.feature.qibla.domain.QiblaRepository
import com.archimedeprojects.arihna.feature.settings.LocationSettingsViewModel
import java.time.Clock
import kotlinx.coroutines.flow.map

@Composable
fun ArihnaApp(
    appContainer: AppContainer,
    activity: ComponentActivity,
) {
    val locationViewModelFactory = remember(appContainer) {
        viewModelFactory {
            initializer {
                LocationSettingsViewModel(
                    coordinator = appContainer.locationCoordinator,
                    cityRepository = appContainer.cityRepository,
                    deviceLocationDataSource = appContainer.deviceLocationDataSource,
                    preferencesRepository = appContainer.locationPreferencesRepository,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
    val locationViewModel: LocationSettingsViewModel = viewModel(factory = locationViewModelFactory)

    val prayerClock = remember { Clock.systemUTC() }
    val prayerScheduleRepository = remember(appContainer, locationViewModel, prayerClock) {
        DefaultPrayerScheduleRepository(
            locationStates = locationViewModel.uiState.map { it.resolutionState },
            prayerSettingsRepository = appContainer.prayerSettingsRepository,
            prayerTimeCalculator = AdhanPrayerTimeCalculator(),
            clock = prayerClock,
        )
    }
    val prayerScheduleViewModelFactory = remember(prayerScheduleRepository, prayerClock) {
        viewModelFactory {
            initializer {
                PrayerScheduleViewModel(
                    repository = prayerScheduleRepository,
                    clock = prayerClock,
                    ticker = OneSecondPrayerScheduleTicker(),
                )
            }
        }
    }
    val prayerScheduleViewModel: PrayerScheduleViewModel = viewModel(factory = prayerScheduleViewModelFactory)

    val alarmsViewModelFactory = remember(appContainer) {
        viewModelFactory {
            initializer {
                AlarmsViewModel(
                    repository = appContainer.alarmRuleRepository,
                    reconciler = appContainer.alarmReconciler,
                    scheduler = appContainer.alarmPlatformScheduler,
                    notificationPermissionReader = appContainer.alarmNotificationPermissionReader,
                )
            }
        }
    }
    val alarmsViewModel: AlarmsViewModel = viewModel(factory = alarmsViewModelFactory)

    val qiblaRepository: QiblaRepository = remember(appContainer, locationViewModel) {
        DefaultQiblaRepository(
            locationStates = locationViewModel.uiState.map { it.resolutionState },
            bearingCalculator = appContainer.qiblaBearingCalculator,
            headingDataSource = appContainer.qiblaHeadingDataSource,
        )
    }

    DisposableEffect(activity, locationViewModel) {
        fun refreshForegroundLocation() {
            val permissionState = appContainer.locationPermissionStateResolver.resolve(
                activity = activity,
                hasRequestedBefore = locationViewModel.hasRequestedPermissionBefore(),
            )
            locationViewModel.onForeground(
                permissionState = permissionState,
                locationServicesEnabled = appContainer.locationEnvironment.isLocationServicesEnabled(),
            )
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> refreshForegroundLocation()
                Lifecycle.Event.ON_STOP -> locationViewModel.onBackground()
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            refreshForegroundLocation()
        }

        onDispose {
            activity.lifecycle.removeObserver(observer)
            locationViewModel.onBackground()
        }
    }

    ArihnaTheme {
        ArihnaNavHost(
            activity = activity,
            locationSettingsViewModel = locationViewModel,
            prayerScheduleViewModel = prayerScheduleViewModel,
            alarmsViewModel = alarmsViewModel,
            exactAlarmAccessIntentFactory = appContainer.exactAlarmAccessIntentFactory,
            alarmFullScreenAccess = appContainer.alarmFullScreenAccess,
            qiblaRepository = qiblaRepository,
            locationEnvironment = appContainer.locationEnvironment,
            locationPermissionStateResolver = appContainer.locationPermissionStateResolver,
        )
    }
}
