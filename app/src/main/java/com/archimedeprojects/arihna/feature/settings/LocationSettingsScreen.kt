package com.archimedeprojects.arihna.feature.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
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
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeChangeResult
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeController
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeState
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory
import kotlin.math.roundToInt

private val SettingsBackgroundTop = Color(0xFF06110D)
private val SettingsBackgroundBottom = Color(0xFF020805)
private val SettingsSurface = Color(0xFF0D1B16)
private val SettingsSurfaceRaised = Color(0xFF12251D)
private val SettingsText = Color(0xFFF8F4E9)
private val SettingsMuted = Color(0xFFA8B7AF)
private val SettingsAccent = Color(0xFFD7B95A)
private val SettingsDanger = Color(0xFFFF9188)
private val SettingsOutline = Color(0xFF274338)

@Suppress("UNUSED_PARAMETER")
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
        contract = ActivityResultContracts.RequestMultiplePermissions(),
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

    var diagnosticMessage by remember { mutableStateOf<String?>(null) }
    val alarmVolumeController = remember(activity) { AlarmVolumeController(activity) }
    var alarmVolumeState by remember { mutableStateOf(alarmVolumeController.read()) }
    var alarmVolumeMessage by remember { mutableStateOf<String?>(null) }

    fun diagnosticResultMessage(kind: AlarmDiagnosticKind, result: AlarmDiagnosticScheduleResult): String =
        when (result) {
            AlarmDiagnosticScheduleResult.SCHEDULED ->
                if (kind == AlarmDiagnosticKind.ADHAN) {
                    "Test Adhan programmato tra 10 secondi"
                } else {
                    "Test sveglia programmato tra 10 secondi"
                }
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
                permissionLauncher.launch(permissionResolver.permissions)
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
            alarmVolumeState = alarmVolumeState,
            alarmVolumeMessage = alarmVolumeMessage,
            diagnosticMessage = diagnosticMessage,
        ),
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
    onAlarmVolumeChange: (Int) -> Unit = {},
    onTestAlarm: () -> Unit = {},
    onTestAdhan: () -> Unit = {},
    onCancelDiagnostic: () -> Unit = {},
) {
    val presentation = uiState.resolutionState.toPresentation()
    val ready = uiState.resolutionState is LocationResolutionState.Ready

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SettingsBackgroundTop, SettingsBackgroundBottom)))
            .padding(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 6.dp,
            )
            .testTag("settings-root"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsHeader()
        LocationControlCard(
            uiState = uiState,
            presentation = presentation,
            ready = ready,
            onUseDevice = onUseDevice,
            onSearchQueryChanged = onSearchQueryChanged,
            onSelectCity = onSelectCity,
            onOpenAppSettings = onOpenAppSettings,
            onOpenLocationSettings = onOpenLocationSettings,
        )
        AlarmVolumeCard(
            state = alarmSettings,
            onAlarmVolumeChange = onAlarmVolumeChange,
        )
        AlarmDiagnosticCard(
            state = alarmSettings,
            onTestAlarm = onTestAlarm,
            onTestAdhan = onTestAdhan,
            onCancelDiagnostic = onCancelDiagnostic,
        )
    }

    if (uiState.rationaleVisible) {
        AlertDialog(
            onDismissRequest = onDismissRationale,
            title = { Text("Perché Arihna chiede la posizione") },
            text = {
                Text(
                    "Arihna usa la posizione del telefono per calcolare gli orari di preghiera. " +
                        "Android può concedere una posizione precisa o approssimativa; in alternativa puoi scegliere una città manualmente.",
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmRationale) { Text("Continua") }
            },
            dismissButton = {
                TextButton(onClick = onDismissRationale) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun SettingsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(shape = CircleShape, color = SettingsAccent.copy(alpha = 0.13f)) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = null,
                tint = SettingsAccent,
                modifier = Modifier.padding(8.dp).size(20.dp),
            )
        }
        Column {
            Text(
                text = "Impostazioni",
                color = SettingsText,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Preferenze essenziali",
                color = SettingsMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun LocationControlCard(
    uiState: LocationSettingsUiState,
    presentation: LocationStatusPresentation,
    ready: Boolean,
    onUseDevice: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectCity: (Long) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    var searchMenuVisible by remember(uiState.searchQuery, uiState.searchResults) {
        mutableStateOf(uiState.searchResults.isNotEmpty())
    }

    PremiumCard(
        modifier = Modifier.testTag("settings-location-summary"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = SettingsAccent.copy(alpha = 0.13f)) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = SettingsAccent,
                    modifier = Modifier.padding(7.dp).size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 9.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text("Posizione", color = SettingsAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = presentation.locationName ?: presentation.title,
                    color = SettingsText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                presentation.zoneId?.let {
                    Text(it, color = SettingsMuted, fontSize = 10.sp, maxLines = 1)
                }
            }
            if (uiState.resolutionState is LocationResolutionState.Resolving) {
                CircularProgressIndicator(
                    color = SettingsAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (!ready) {
            Text(
                text = presentation.message,
                style = MaterialTheme.typography.bodySmall,
                color = SettingsMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (presentation.showAppSettingsAction) {
                CompactOutlinedAction("Apri impostazioni app", onOpenAppSettings)
            }
            if (presentation.showLocationSettingsAction) {
                CompactOutlinedAction("Apri impostazioni Posizione", onOpenLocationSettings)
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = {
                    searchMenuVisible = true
                    onSearchQueryChanged(it)
                },
                label = { Text("Cerca città") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = SettingsMuted)
                },
                trailingIcon = {
                    IconButton(
                        onClick = onUseDevice,
                        modifier = Modifier.testTag("settings-use-current-location"),
                    ) {
                        Icon(
                            Icons.Filled.MyLocation,
                            contentDescription = "Usa posizione attuale",
                            tint = SettingsAccent,
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-location-search"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SettingsText,
                    unfocusedTextColor = SettingsText,
                    focusedBorderColor = SettingsAccent.copy(alpha = 0.82f),
                    unfocusedBorderColor = SettingsOutline,
                    focusedLabelColor = SettingsAccent,
                    unfocusedLabelColor = SettingsMuted,
                    cursorColor = SettingsAccent,
                    focusedContainerColor = SettingsBackgroundTop.copy(alpha = 0.35f),
                    unfocusedContainerColor = SettingsBackgroundTop.copy(alpha = 0.25f),
                ),
                shape = RoundedCornerShape(16.dp),
            )

            DropdownMenu(
                expanded = searchMenuVisible && uiState.searchResults.isNotEmpty(),
                onDismissRequest = { searchMenuVisible = false },
                modifier = Modifier
                    .width(maxWidth)
                    .heightIn(max = 300.dp)
                    .testTag("settings-location-suggestions"),
                containerColor = SettingsSurfaceRaised,
                properties = PopupProperties(focusable = false),
            ) {
                uiState.searchResults.forEach { city ->
                    DropdownMenuItem(
                        text = { CityResultContent(city) },
                        onClick = {
                            searchMenuVisible = false
                            onSelectCity(city.id)
                        },
                    )
                }
            }
        }

        if (uiState.searchInProgress) {
            Text("Ricerca locale…", color = SettingsMuted, fontSize = 10.sp)
        }
        uiState.searchMessage?.let {
            Text(it, color = SettingsMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurfaceRaised),
        border = BorderStroke(1.dp, SettingsOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            content = content,
        )
    }
}

@Composable
private fun CompactOutlinedAction(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, SettingsOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsText),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CityResultContent(city: CitySearchResult) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = city.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = SettingsText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            city.timeZoneId,
            style = MaterialTheme.typography.bodySmall,
            color = SettingsMuted,
            maxLines = 1,
        )
        if (!city.timeZoneSupported) {
            Text(
                text = "Fuso non supportato su questa versione Android.",
                style = MaterialTheme.typography.bodySmall,
                color = SettingsDanger,
                maxLines = 1,
            )
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
private fun AlarmVolumeCard(
    state: AlarmSettingsPresentation,
    onAlarmVolumeChange: (Int) -> Unit,
) {
    PremiumCard(modifier = Modifier.testTag("settings-alarm-volume-card")) {
        val volume = state.alarmVolumeState
        val sliderMax = if (volume.max > volume.min) volume.max else volume.min + 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = SettingsAccent.copy(alpha = 0.13f)) {
                Icon(
                    Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = SettingsAccent,
                    modifier = Modifier.padding(7.dp).size(18.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 9.dp),
            ) {
                Text("Sveglia", color = SettingsAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Volume sveglia",
                    color = SettingsText,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("settings-alarm-volume"),
                )
            }
            Text(
                "${volume.percent}%",
                color = SettingsAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Slider(
            value = volume.current.coerceIn(volume.min, volume.max).toFloat(),
            onValueChange = { onAlarmVolumeChange(it.roundToInt()) },
            valueRange = volume.min.toFloat()..sliderMax.toFloat(),
            steps = (volume.max - volume.min - 1).coerceAtLeast(0),
            enabled = volume.max > volume.min,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-alarm-volume-slider"),
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccent,
                activeTrackColor = SettingsAccent,
                inactiveTrackColor = SettingsOutline,
            ),
        )
        Text("Volume globale delle sveglie del telefono", color = SettingsMuted, fontSize = 10.sp)
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
    PremiumCard(modifier = Modifier.testTag("settings-alarm-tests")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = SettingsAccent.copy(alpha = 0.13f)) {
                Icon(
                    Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = SettingsAccent,
                    modifier = Modifier.padding(7.dp).size(18.dp),
                )
            }
            Column {
                Text("Test rapidi", color = SettingsAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Verifica suono e overlay", color = SettingsMuted, fontSize = 10.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Button(
                onClick = onTestAlarm,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings-test-alarm-one-minute"),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 7.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsAccent,
                    contentColor = SettingsBackgroundBottom,
                ),
            ) {
                Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    "  Test sveglia (10 secondi)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            OutlinedButton(
                onClick = onTestAdhan,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings-test-adhan-one-minute"),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 7.dp),
                border = BorderStroke(1.dp, SettingsAccent.copy(alpha = 0.58f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsText),
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = SettingsAccent, modifier = Modifier.size(16.dp))
                Text("  Test Adhan (10 secondi)", style = MaterialTheme.typography.labelSmall)
            }
        }
        state.diagnosticMessage?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = SettingsAccent,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = onCancelDiagnostic,
                    modifier = Modifier.testTag("settings-test-cancel"),
                    colors = ButtonDefaults.textButtonColors(contentColor = SettingsMuted),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("Annulla", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
