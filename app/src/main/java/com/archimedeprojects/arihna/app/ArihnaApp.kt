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
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.settings.LocationSettingsViewModel

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
            locationEnvironment = appContainer.locationEnvironment,
            locationPermissionStateResolver = appContainer.locationPermissionStateResolver,
        )
    }
}
