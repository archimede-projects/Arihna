package com.archimedeprojects.arihna.feature.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
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

private val SettingsBackground = Color(0xFF091411)
private val SettingsSurface = Color(0xFF14211D)
private val SettingsSurfaceRaised = Color(0xFF1A2A25)
private val SettingsText = Color(0xFFF6F2E8)
private val SettingsMuted = Color(0xFFAAB8B1)
private val SettingsAccent = Color(0xFFE0C56D)
private val SettingsSuccess = Color(0xFF7ED0A3)
private val SettingsDanger = Color(0xFFFF9188)
private val SettingsOutline = Color(0xFF31443D)

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
    val ready = uiState.resolutionState is LocationResolutionState.Ready

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = contentPadding.calculateTopPadding() + 18.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Impostazioni",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = SettingsText,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }

        item { SettingsSectionTitle("Posizione", "settings-section-location") }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings-location-summary"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SettingsSurfaceRaised),
                border = BorderStroke(1.dp, SettingsOutline),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        text = presentation.locationName ?: presentation.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SettingsText,
                    )
                    if (ready) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsPill(uiState.activeMode.label(), SettingsAccent)
                            presentation.freshness?.let {
                                SettingsPill(
                                    label = it,
                                    accent = if (it == "FRESH") SettingsSuccess else SettingsAccent,
                                )
                            }
                        }
                        presentation.zoneId?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SettingsMuted,
                            )
                        }
                    } else {
                        SourceBadge(uiState.activeMode.label())
                        Text(
                            text = presentation.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SettingsMuted,
                        )
                    }

                    if (uiState.resolutionState is LocationResolutionState.Resolving) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = SettingsAccent)
                        }
                    }

                    if (presentation.showAppSettingsAction) {
                        CompactOutlinedAction("Apri impostazioni app", onOpenAppSettings)
                    }
                    if (presentation.showLocationSettingsAction) {
                        CompactOutlinedAction("Apri impostazioni Posizione", onOpenLocationSettings)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings-location-controls"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SettingsSurface),
                border = BorderStroke(1.dp, SettingsOutline),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onUseDevice,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SettingsAccent,
                            contentColor = SettingsBackground,
                        ),
                    ) {
                        Text("Usa posizione attuale", fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        label = { Text("Cerca città") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SettingsText,
                            unfocusedTextColor = SettingsText,
                            focusedBorderColor = SettingsAccent,
                            unfocusedBorderColor = SettingsOutline,
                            focusedLabelColor = SettingsAccent,
                            unfocusedLabelColor = SettingsMuted,
                            cursorColor = SettingsAccent,
                        ),
                    )
                    if (uiState.searchInProgress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(color = SettingsAccent)
                            Text("Ricerca locale…", color = SettingsMuted)
                        }
                    }
                    uiState.searchMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = SettingsMuted,
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

        item { SettingsSectionTitle("Sveglie e notifiche", "settings-section-alarms") }
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

        item { SettingsSectionTitle("Test rapidi", "settings-section-tests") }
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
private fun SettingsSectionTitle(text: String, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = SettingsAccent,
        modifier = Modifier
            .padding(top = 5.dp, start = 2.dp)
            .testTag(tag),
    )
}

@Composable
private fun SettingsPill(label: String, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        contentColor = accent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SourceBadge(label: String) {
    SettingsPill(label = label, accent = SettingsAccent)
}

@Composable
private fun CompactOutlinedAction(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, SettingsOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsText),
    ) {
        Text(label)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurface),
        border = BorderStroke(1.dp, SettingsOutline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = city.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SettingsText,
            )
            Text(
                text = city.timeZoneId,
                style = MaterialTheme.typography.bodySmall,
                color = SettingsMuted,
            )
            if (!city.timeZoneSupported) {
                Text(
                    text = "Fuso non supportato su questa versione Android: selezionando la città Arihna mostrerà un errore controllato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsDanger,
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
    Card(
        modifier = Modifier.fillMaxWidth().testTag("settings-alarm-capabilities"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurface),
        border = BorderStroke(1.dp, SettingsOutline),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AlarmCapabilitySettingRow("Notifiche", state.notificationReady, "Gestisci", onManageNotifications)
            HorizontalDivider(color = SettingsOutline)
            AlarmCapabilitySettingRow("Allarmi esatti", state.exactReady, "Gestisci", onManageExactAlarms)
            HorizontalDivider(color = SettingsOutline)
            AlarmCapabilitySettingRow("Schermo intero", state.fullScreenReady, "Apri", onManageFullScreen)
            HorizontalDivider(color = SettingsOutline)
            AlarmCapabilitySettingRow(
                label = "Popup sveglia",
                ready = state.overlayReady,
                actionLabel = "Gestisci",
                onClick = onManageOverlay,
                modifier = Modifier.testTag("settings-overlay-access"),
            )
            HorizontalDivider(color = SettingsOutline)
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SettingsText,
            modifier = Modifier.weight(1f),
        )
        SettingsPill(
            label = if (ready) "Pronto" else "Da autorizzare",
            accent = if (ready) SettingsSuccess else SettingsDanger,
        )
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(contentColor = SettingsAccent),
        ) {
            Text(actionLabel)
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
            .testTag("settings-alarm-volume"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Volume sveglia",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SettingsText,
            )
            Text(
                "${volume.percent}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SettingsAccent,
            )
        }
        Slider(
            value = volume.current.coerceIn(volume.min, volume.max).toFloat(),
            onValueChange = { onAlarmVolumeChange(it.roundToInt()) },
            valueRange = volume.min.toFloat()..sliderMax.toFloat(),
            steps = (volume.max - volume.min - 1).coerceAtLeast(0),
            enabled = volume.max > volume.min,
            modifier = Modifier.fillMaxWidth().testTag("settings-alarm-volume-slider"),
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccent,
                activeTrackColor = SettingsAccent,
                inactiveTrackColor = SettingsOutline,
            ),
        )
        Text(
            "Volume globale delle sveglie del telefono",
            style = MaterialTheme.typography.bodySmall,
            color = SettingsMuted,
        )
        state.alarmVolumeMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = SettingsDanger)
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
    Card(
        modifier = Modifier.fillMaxWidth().testTag("settings-alarm-tests"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurfaceRaised),
        border = BorderStroke(1.dp, SettingsOutline),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onTestAlarm,
                modifier = Modifier.fillMaxWidth().testTag("settings-test-alarm-one-minute"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsAccent,
                    contentColor = SettingsBackground,
                ),
            ) { Text("Test sveglia (20 secondi)", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onTestAdhan,
                modifier = Modifier.fillMaxWidth().testTag("settings-test-adhan-one-minute"),
                border = BorderStroke(1.dp, SettingsAccent.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsText),
            ) { Text("Test Adhan (20 secondi)") }
            TextButton(
                onClick = onCancelDiagnostic,
                modifier = Modifier.fillMaxWidth().testTag("settings-test-cancel"),
                colors = ButtonDefaults.textButtonColors(contentColor = SettingsMuted),
            ) { Text("Annulla test in corso") }
            state.diagnosticMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = SettingsAccent)
            }
        }
    }
}
