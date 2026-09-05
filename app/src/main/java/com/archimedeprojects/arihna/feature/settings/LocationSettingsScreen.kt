package com.archimedeprojects.arihna.feature.settings

import android.Manifest
import android.app.Activity
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver
import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticKind
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticScheduleResult
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticTestScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmOverlayAccess
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeChangeResult
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeController
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeState
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory
import kotlin.math.roundToInt

@Composable
fun LocationSettingsRoute(
    contentPadding: PaddingValues,
    activity: Activity,
    viewModel: LocationSettingsViewModel,
    environment: AndroidLocationEnvironment,
    permissionResolver: AndroidLocationPermissionStateResolver,
    alarmsViewModel: AlarmsViewModel,
    exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory,
    alarmFullScreenAccess: AlarmFullScreenAccess,
    alarmDiagnosticTestScheduler: AlarmDiagnosticTestScheduler,
) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        val permissionState = permissionResolver.resolve(
            activity = activity,
            hasRequestedBefore = viewModel.hasRequestedPermissionBefore(),
        )
        viewModel.selectDevice(
            permissionState = permissionState,
            locationServicesEnabled = environment.isLocationServicesEnabled(),
        )
    }

    val alarmsState by alarmsViewModel.uiState.collectAsState()
    var capabilityRefresh by remember { mutableIntStateOf(0) }
    var diagnosticMessage by remember { mutableStateOf<String?>(null) }
    val fullScreenReady = alarmFullScreenAccess.isGranted()
    val alarmOverlayAccess = remember(activity) { AlarmOverlayAccess(activity) }
    val alarmVolumeController = remember(activity) { AlarmVolumeController(activity) }
    val overlayReady = alarmOverlayAccess.isGranted()
    var alarmVolumeState by remember { mutableStateOf(alarmVolumeController.read()) }
    var alarmVolumeMessage by remember { mutableStateOf<String?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        capabilityRefresh += 1
        alarmsViewModel.refreshCapabilities()
    }
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        capabilityRefresh += 1
        alarmsViewModel.refreshCapabilities()
    }
    val fullScreenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        capabilityRefresh += 1
        alarmsViewModel.refreshCapabilities()
    }
    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        capabilityRefresh += 1
        alarmsViewModel.refreshCapabilities()
    }

    fun diagnosticResultMessage(kind: AlarmDiagnosticKind, result: AlarmDiagnosticScheduleResult): String =
        when (result) {
            AlarmDiagnosticScheduleResult.SCHEDULED ->
                if (kind == AlarmDiagnosticKind.ADHAN) "Test Adhan programmato tra 20 secondi" else "Test sveglia programmato tra 20 secondi"
            AlarmDiagnosticScheduleResult.NEEDS_NOTIFICATION_PERMISSION -> "Consenti prima le notifiche"
            AlarmDiagnosticScheduleResult.NEEDS_EXACT_ALARM_ACCESS -> "Consenti prima gli allarmi esatti"
            AlarmDiagnosticScheduleResult.NEEDS_FULL_SCREEN_ACCESS -> "Consenti prima lo schermo intero"
        }

    LocationSettingsScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onUseDevice = viewModel::onUseDeviceClick,
        onDismissRationale = viewModel::dismissRationale,
        onConfirmRationale = {
            if (environment.isCoarsePermissionGranted()) {
                viewModel.dismissRationale()
                viewModel.selectDevice(
                    permissionState = LocationPermissionState.Granted,
                    locationServicesEnabled = environment.isLocationServicesEnabled(),
                )
            } else {
                viewModel.markPermissionRequestStarted()
                permissionLauncher.launch(permissionResolver.permission)
            }
        },
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSelectCity = viewModel::selectManual,
        onOpenAppSettings = {
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
        },
        onOpenLocationSettings = {
            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
        alarmSettings = AlarmSettingsPresentation(
            notificationReady = alarmsState.notificationReady,
            exactReady = alarmsState.exactAlarmReady,
            fullScreenReady = fullScreenReady,
            overlayReady = overlayReady,
            alarmVolumeState = alarmVolumeState,
            alarmVolumeMessage = alarmVolumeMessage,
            diagnosticMessage = diagnosticMessage,
        ),
        onManageNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alarmsState.notificationReady) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                activity.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName),
                )
            }
        },
        onManageExactAlarms = {
            exactAlarmAccessIntentFactory.create()?.let(exactAlarmLauncher::launch)
                ?: alarmsViewModel.refreshCapabilities()
        },
        onManageFullScreen = {
            alarmFullScreenAccess.createSettingsIntent()?.let(fullScreenLauncher::launch)
                ?: alarmsViewModel.refreshCapabilities()
        },
        onManageOverlay = {
            overlayLauncher.launch(alarmOverlayAccess.createSettingsIntent())
        },
        onAlarmVolumeChange = { requested ->
            when (val result = alarmVolumeController.setVolume(requested)) {
                is AlarmVolumeChangeResult.Success -> {
                    alarmVolumeState = result.state
                    alarmVolumeMessage = null
                }
                is AlarmVolumeChangeResult.Failure -> {
                    alarmVolumeState = result.state
                    alarmVolumeMessage = result.message
                }
            }
        },
        onTestAlarm = {
            diagnosticMessage = diagnosticResultMessage(
                AlarmDiagnosticKind.SYSTEM_ALARM,
                alarmDiagnosticTestScheduler.scheduleOneMinute(AlarmDiagnosticKind.SYSTEM_ALARM),
            )
        },
        onTestAdhan = {
            diagnosticMessage = diagnosticResultMessage(
                AlarmDiagnosticKind.ADHAN,
                alarmDiagnosticTestScheduler.scheduleOneMinute(AlarmDiagnosticKind.ADHAN),
            )
        },
        onCancelDiagnostic = {
            alarmDiagnosticTestScheduler.cancel()
            diagnosticMessage = "Test in attesa annullato"
        },
    )
}

@Composable
fun LocationSettingsScreen(
    contentPadding: PaddingValues,
    uiState: LocationSettingsUiState,
    onUseDevice: () -> Unit,
    onDismissRationale: () -> Unit,
    onConfirmRationale: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectCity: (Long) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    alarmSettings: AlarmSettingsPresentation = AlarmSettingsPresentation(),
    onManageNotifications: () -> Unit = {},
    onManageExactAlarms: () -> Unit = {},
    onManageFullScreen: () -> Unit = {},
    onManageOverlay: () -> Unit = {},
    onAlarmVolumeChange: (Int) -> Unit = {},
    onTestAlarm: () -> Unit = {},
    onTestAdhan: () -> Unit = {},
    onCancelDiagnostic: () -> Unit = {},
) {
    val presentation = uiState.resolutionState.toPresentation()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 20.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Impostazioni",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Controlli di sistema e test rapidi per sveglie e Adhan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Posizione",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Pannello funzionale STEP 6 — la Home definitiva verrà costruita più avanti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Stato",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        SourceBadge(uiState.activeMode.label())
                    }

                    Text(
                        text = presentation.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = presentation.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    presentation.locationName?.let {
                        HorizontalDivider()
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    presentation.zoneId?.let {
                        Text(
                            text = "Fuso orario: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    presentation.freshness?.let {
                        Text(
                            text = "Dati: $it",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (it == "FRESH") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                        )
                    }

                    if (uiState.resolutionState is LocationResolutionState.Resolving) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    if (presentation.showAppSettingsAction) {
                        OutlinedButton(onClick = onOpenAppSettings) {
                            Text("Apri impostazioni app")
                        }
                    }
                    if (presentation.showLocationSettingsAction) {
                        OutlinedButton(onClick = onOpenLocationSettings) {
                            Text("Apri impostazioni Posizione")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Posizione del dispositivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Arihna richiede solo la posizione approssimativa e la usa localmente per calcolare gli orari di preghiera.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onUseDevice,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Usa posizione attuale")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Città manuale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "La ricerca usa l’archivio GeoNames offline incluso in Arihna. Non serve concedere la posizione.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        label = { Text("Cerca città") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.searchInProgress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Ricerca nell’archivio locale…")
                        }
                    }
                    uiState.searchMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        items(
            items = uiState.searchResults,
            key = { city -> city.id },
        ) { city ->
            CityResultCard(city = city, onSelectCity = onSelectCity)
        }

        item {
            AlarmSystemSettingsCard(
                state = alarmSettings,
                onManageNotifications = onManageNotifications,
                onManageExactAlarms = onManageExactAlarms,
                onManageFullScreen = onManageFullScreen,
                onManageOverlay = onManageOverlay,
                onAlarmVolumeChange = onAlarmVolumeChange,
            )
        }
        item {
            AlarmDiagnosticCard(
                state = alarmSettings,
                onTestAlarm = onTestAlarm,
                onTestAdhan = onTestAdhan,
                onCancelDiagnostic = onCancelDiagnostic,
            )
        }
    }

    if (uiState.rationaleVisible) {
        AlertDialog(
            onDismissRequest = onDismissRationale,
            title = { Text("Perché Arihna chiede la posizione") },
            text = {
                Text(
                    "La posizione approssimativa è sufficiente per calcolare gli orari di preghiera. " +
                        "Resta sul dispositivo e non viene inviata a servizi esterni. " +
                        "Se preferisci non concederla, puoi sempre scegliere una città manualmente.",
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmRationale) {
                    Text("Continua")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRationale) {
                    Text("Annulla")
                }
            },
        )
    }
}

@Composable
private fun SourceBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = "Origine: $label",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun CityResultCard(
    city: CitySearchResult,
    onSelectCity: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectCity(city.id) },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = city.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Fuso: ${city.timeZoneId}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!city.timeZoneSupported) {
                Text(
                    text = "Fuso non supportato su questa versione Android: selezionando la città Arihna mostrerà un errore controllato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

data class AlarmSettingsPresentation(
    val notificationReady: Boolean = false,
    val exactReady: Boolean = false,
    val fullScreenReady: Boolean = false,
    val overlayReady: Boolean = false,
    val alarmVolumeState: AlarmVolumeState = AlarmVolumeState(current = 0, min = 0, max = 1),
    val alarmVolumeMessage: String? = null,
    val diagnosticMessage: String? = null,
)

@Composable
private fun AlarmSystemSettingsCard(
    state: AlarmSettingsPresentation,
    onManageNotifications: () -> Unit,
    onManageExactAlarms: () -> Unit,
    onManageFullScreen: () -> Unit,
    onManageOverlay: () -> Unit,
    onAlarmVolumeChange: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().testTag("settings-alarm-capabilities")) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Sveglie e notifiche", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "I permessi di sistema sono gestiti qui, separati dalle sveglie personali.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AlarmCapabilitySettingRow("Notifiche", state.notificationReady, "Gestisci", onManageNotifications)
            HorizontalDivider()
            AlarmCapabilitySettingRow("Allarmi esatti", state.exactReady, "Gestisci", onManageExactAlarms)
            HorizontalDivider()
            AlarmCapabilitySettingRow("Schermo intero", state.fullScreenReady, "Apri impostazioni", onManageFullScreen)
            HorizontalDivider()
            AlarmCapabilitySettingRow(
                label = "Popup sveglia",
                ready = state.overlayReady,
                actionLabel = "Gestisci",
                onClick = onManageOverlay,
                modifier = Modifier.testTag("settings-overlay-access"),
            )
            HorizontalDivider()
            AlarmVolumeSetting(
                state = state,
                onAlarmVolumeChange = onAlarmVolumeChange,
            )
        }
    }
}

@Composable
private fun AlarmCapabilitySettingRow(
    label: String,
    ready: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (ready) "Consentito" else "Da autorizzare",
                color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OutlinedButton(onClick = onClick) { Text(actionLabel) }
    }
}

@Composable
private fun AlarmVolumeSetting(
    state: AlarmSettingsPresentation,
    onAlarmVolumeChange: (Int) -> Unit,
) {
    val volume = state.alarmVolumeState
    val sliderMax = if (volume.max > volume.min) volume.max else volume.min + 1
    Column(
        modifier = Modifier.fillMaxWidth().testTag("settings-alarm-volume"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Volume sveglia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${volume.percent}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = volume.current.coerceIn(volume.min, volume.max).toFloat(),
            onValueChange = { onAlarmVolumeChange(it.roundToInt()) },
            valueRange = volume.min.toFloat()..sliderMax.toFloat(),
            steps = (volume.max - volume.min - 1).coerceAtLeast(0),
            enabled = volume.max > volume.min,
            modifier = Modifier.fillMaxWidth().testTag("settings-alarm-volume-slider"),
        )
        Text(
            "Modifica il volume globale delle sveglie del telefono.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.alarmVolumeMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AlarmDiagnosticCard(
    state: AlarmSettingsPresentation,
    onTestAlarm: () -> Unit,
    onTestAdhan: () -> Unit,
    onCancelDiagnostic: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().testTag("settings-alarm-tests")) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Test hardware", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Esegue un test reale tra 20 secondi usando lo stesso percorso di sveglie, notifica e schermo intero.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onTestAlarm,
                modifier = Modifier.fillMaxWidth().testTag("settings-test-alarm-one-minute"),
            ) { Text("Test sveglia (20 secondi)") }
            Button(
                onClick = onTestAdhan,
                modifier = Modifier.fillMaxWidth().testTag("settings-test-adhan-one-minute"),
            ) { Text("Test Adhan (20 secondi)") }
            TextButton(
                onClick = onCancelDiagnostic,
                modifier = Modifier.fillMaxWidth().testTag("settings-test-cancel"),
            ) { Text("Annulla test in corso") }
            state.diagnosticMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
