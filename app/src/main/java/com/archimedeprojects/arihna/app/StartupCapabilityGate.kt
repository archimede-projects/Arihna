package com.archimedeprojects.arihna.app

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmNotificationPermissionReader
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmOverlayAccess
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmCapability
import com.archimedeprojects.arihna.feature.settings.LocationSettingsViewModel
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver

enum class StartupCapability {
    LOCATION,
    NOTIFICATIONS,
    EXACT_ALARM,
    FULL_SCREEN,
    OVERLAY,
}

data class StartupCapabilitySnapshot(
    val locationReady: Boolean,
    val notificationsReady: Boolean,
    val exactAlarmReady: Boolean,
    val fullScreenReady: Boolean,
    val overlayReady: Boolean,
)

internal fun nextStartupCapability(
    snapshot: StartupCapabilitySnapshot,
    dismissed: Set<StartupCapability>,
): StartupCapability? = listOf(
    StartupCapability.LOCATION to snapshot.locationReady,
    StartupCapability.NOTIFICATIONS to snapshot.notificationsReady,
    StartupCapability.EXACT_ALARM to snapshot.exactAlarmReady,
    StartupCapability.FULL_SCREEN to snapshot.fullScreenReady,
    StartupCapability.OVERLAY to snapshot.overlayReady,
).firstOrNull { (capability, ready) -> !ready && capability !in dismissed }?.first

@Composable
fun StartupCapabilityGate(
    activity: ComponentActivity,
    locationViewModel: LocationSettingsViewModel,
    locationEnvironment: AndroidLocationEnvironment,
    locationPermissionStateResolver: AndroidLocationPermissionStateResolver,
    alarmsViewModel: AlarmsViewModel,
    alarmPlatformScheduler: AlarmPlatformScheduler,
    alarmNotificationPermissionReader: AlarmNotificationPermissionReader,
    exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory,
    alarmFullScreenAccess: AlarmFullScreenAccess,
) {
    var dismissed by remember { mutableStateOf(emptySet<StartupCapability>()) }
    var capabilityRefresh by remember { mutableIntStateOf(0) }
    var resumed by remember {
        mutableStateOf(activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val overlayAccess = remember(activity) { AlarmOverlayAccess(activity) }

    fun refreshCapabilities() {
        capabilityRefresh += 1
        alarmsViewModel.refreshCapabilities()
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        dismissed = dismissed + StartupCapability.LOCATION
        val permissionState = locationPermissionStateResolver.resolve(
            activity = activity,
            hasRequestedBefore = locationViewModel.hasRequestedPermissionBefore(),
        )
        locationViewModel.onForeground(
            permissionState = permissionState,
            locationServicesEnabled = locationEnvironment.isLocationServicesEnabled(),
        )
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        dismissed = dismissed + StartupCapability.NOTIFICATIONS
        refreshCapabilities()
    }
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshCapabilities()
    }
    val fullScreenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshCapabilities()
    }
    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshCapabilities()
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    resumed = true
                    refreshCapabilities()
                }
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    capabilityRefresh
    val snapshot = StartupCapabilitySnapshot(
        locationReady = locationEnvironment.isCoarsePermissionGranted(),
        notificationsReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            alarmNotificationPermissionReader.isGranted(),
        exactAlarmReady = alarmPlatformScheduler.capability() == ExactAlarmCapability.READY,
        fullScreenReady = alarmFullScreenAccess.isGranted(),
        overlayReady = overlayAccess.isGranted(),
    )
    val next = nextStartupCapability(snapshot, dismissed)

    LaunchedEffect(next, resumed) {
        if (!resumed) return@LaunchedEffect
        when (next) {
            StartupCapability.LOCATION -> {
                locationViewModel.markPermissionRequestStarted()
                locationLauncher.launch(locationPermissionStateResolver.permission)
            }
            StartupCapability.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    dismissed = dismissed + StartupCapability.NOTIFICATIONS
                }
            }
            else -> Unit
        }
    }

    when (next) {
        StartupCapability.EXACT_ALARM -> StartupSpecialAccessDialog(
            title = "Allarmi esatti",
            message = "Per far partire le sveglie all'ora corretta, consenti ad Arihna di programmare allarmi esatti nelle impostazioni Android.",
            onConfigure = {
                dismissed = dismissed + StartupCapability.EXACT_ALARM
                exactAlarmAccessIntentFactory.create()?.let(exactAlarmLauncher::launch)
                    ?: refreshCapabilities()
            },
            onDismiss = { dismissed = dismissed + StartupCapability.EXACT_ALARM },
        )
        StartupCapability.FULL_SCREEN -> StartupSpecialAccessDialog(
            title = "Sveglia a schermo bloccato",
            message = "Per mostrare la sveglia anche quando il telefono è bloccato, abilita l'accesso a schermo intero nelle impostazioni Android.",
            onConfigure = {
                dismissed = dismissed + StartupCapability.FULL_SCREEN
                alarmFullScreenAccess.createSettingsIntent()?.let(fullScreenLauncher::launch)
                    ?: refreshCapabilities()
            },
            onDismiss = { dismissed = dismissed + StartupCapability.FULL_SCREEN },
        )
        StartupCapability.OVERLAY -> StartupSpecialAccessDialog(
            title = "Popup sveglia",
            message = "Per mostrare il popup grande della sveglia quando il telefono è sbloccato, consenti ad Arihna di apparire sopra le altre app.",
            onConfigure = {
                dismissed = dismissed + StartupCapability.OVERLAY
                overlayLauncher.launch(overlayAccess.createSettingsIntent())
            },
            onDismiss = { dismissed = dismissed + StartupCapability.OVERLAY },
        )
        else -> Unit
    }
}

@Composable
private fun StartupSpecialAccessDialog(
    title: String,
    message: String,
    onConfigure: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfigure) {
                Text("Configura")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Non ora")
            }
        },
    )
}
