package com.archimedeprojects.arihna.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.archimedeprojects.arihna.core.location.diagnostics.LocationDiagnosticTrace
import com.archimedeprojects.arihna.core.prayer.calculation.AdhanPrayerTimeCalculator
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.debug.LocationDiagnosticOverlay
import com.archimedeprojects.arihna.feature.prayerschedule.domain.DefaultPrayerScheduleRepository
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.OneSecondPrayerScheduleTicker
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import com.archimedeprojects.arihna.feature.settings.LocationSettingsViewModel
import java.time.Clock
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val prayerScheduleViewModel: PrayerScheduleViewModel = viewModel(
        factory = prayerScheduleViewModelFactory,
    )

    LaunchedEffect(locationViewModel) {
        locationViewModel.uiState
            .map { state ->
                "resolution=${state.resolutionState.javaClass.simpleName} mode=${state.activeMode} rationale=${state.rationaleVisible} permissionRequested=${state.hasRequestedPermissionBefore}"
            }
            .distinctUntilChanged()
            .collect { snapshot ->
                LocationDiagnosticTrace.record("LOCATION_VM_STATE", snapshot)
            }
    }

    DisposableEffect(activity, locationViewModel) {
        fun refreshForegroundLocation(origin: String) {
            val permissionState = appContainer.locationPermissionStateResolver.resolve(
                activity = activity,
                hasRequestedBefore = locationViewModel.hasRequestedPermissionBefore(),
            )
            val servicesEnabled = appContainer.locationEnvironment.isLocationServicesEnabled()
            LocationDiagnosticTrace.record(
                "APP_ON_FOREGROUND",
                "origin=$origin permission=$permissionState servicesEnabled=$servicesEnabled wallMs=${System.currentTimeMillis()}",
            )
            locationViewModel.onForeground(
                permissionState = permissionState,
                locationServicesEnabled = servicesEnabled,
            )
        }

        val observer = LifecycleEventObserver { _, event ->
            LocationDiagnosticTrace.record("ACTIVITY_LIFECYCLE", event.name)
            when (event) {
                Lifecycle.Event.ON_START -> refreshForegroundLocation("ON_START")
                Lifecycle.Event.ON_STOP -> {
                    LocationDiagnosticTrace.record("APP_ON_BACKGROUND", "origin=ON_STOP")
                    locationViewModel.onBackground()
                }
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        LocationDiagnosticTrace.record(
            "LIFECYCLE_OBSERVER_ATTACHED",
            "state=${activity.lifecycle.currentState}",
        )
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            refreshForegroundLocation("INITIAL_STARTED")
        }

        onDispose {
            LocationDiagnosticTrace.record("LIFECYCLE_EFFECT_DISPOSED")
            activity.lifecycle.removeObserver(observer)
            locationViewModel.onBackground()
        }
    }

    ArihnaTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ArihnaNavHost(
                activity = activity,
                locationSettingsViewModel = locationViewModel,
                prayerScheduleViewModel = prayerScheduleViewModel,
                locationEnvironment = appContainer.locationEnvironment,
                locationPermissionStateResolver = appContainer.locationPermissionStateResolver,
            )
            LocationDiagnosticOverlay(
                providerProbe = appContainer.providerCurrentLocationProbe,
            )
        }
    }
}
