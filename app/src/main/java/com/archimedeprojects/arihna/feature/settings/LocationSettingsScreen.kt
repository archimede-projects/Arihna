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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
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
import com.archimedeprojects.arihna.core.i18n.AppLanguage
import com.archimedeprojects.arihna.core.i18n.LocalAppLanguageController
import com.archimedeprojects.arihna.core.i18n.appText
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationEnvironment
import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaMutedText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline
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

private val SettingsBackgroundTop = ArihnaDawnTop
private val SettingsBackgroundBottom = ArihnaDawnBottom
private val SettingsSurface = ArihnaCream
private val SettingsSurfaceRaised = ArihnaSage.copy(alpha = 0.62f)
private val SettingsText = ArihnaForest
private val SettingsMuted = ArihnaMutedText
private val SettingsAccent = ArihnaDawnGold
private val SettingsDanger = Color(0xFF9A3C34)
private val SettingsOutline = ArihnaWarmOutline

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
            if (environment.isLocationPermissionGranted()) {
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
    val languageController = LocalAppLanguageController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SettingsBackgroundTop, SettingsBackgroundBottom)))
            .padding(
                start = 18.dp,
                top = contentPadding.calculateTopPadding() + 10.dp,
                end = 18.dp,
                bottom = contentPadding.calculateBottomPadding() + 6.dp,
            )
            .testTag("settings-root"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "ARIHNA",
                    color = SettingsAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 2.4.sp,
                )
                Text(
                    text = appText("Impostazioni", "الإعدادات"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SettingsText,
                )
            }
        }

        SettingsSectionTitle(appText("Lingua", "اللغة"), "settings-section-language")
        LanguageSettingsCard(
            selected = languageController.language,
            onSelect = languageController::updateLanguage,
        )

        SettingsSectionTitle(appText("Posizione", "الموقع"), "settings-section-location")
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

        SettingsSectionTitle(appText("Sveglia", "المنبه"), "settings-section-alarms")
        AlarmVolumeCard(state = alarmSettings, onAlarmVolumeChange = onAlarmVolumeChange)

        SettingsSectionTitle(appText("Test rapidi", "اختبارات سريعة"), "settings-section-tests")
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
                    "Arihna usa la posizione fornita da Android e Google Play Services per calcolare gli orari di preghiera. " +
                        "Puoi consentire la posizione precisa o approssimativa. Se preferisci, puoi sempre scegliere una città manualmente.",
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-location-summary"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurfaceRaised),
        border = BorderStroke(1.dp, SettingsAccent.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(SettingsAccent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = SettingsAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = presentation.locationName ?: presentation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SettingsText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    presentation.zoneId?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = SettingsMuted)
                    }
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
            if (uiState.resolutionState is LocationResolutionState.Resolving) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(color = SettingsAccent, modifier = Modifier.size(18.dp))
                    Text("Ricerca posizione…", style = MaterialTheme.typography.bodySmall, color = SettingsMuted)
                }
            }
            if (presentation.showAppSettingsAction) {
                CompactOutlinedAction("Apri impostazioni app", onOpenAppSettings)
            }
            if (presentation.showLocationSettingsAction) {
                CompactOutlinedAction("Apri impostazioni Posizione", onOpenLocationSettings)
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = {
                        searchMenuVisible = true
                        onSearchQueryChanged(it)
                    },
                    placeholder = { Text("Cerca città", color = SettingsMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings-location-search"),
                    trailingIcon = {
                        IconButton(
                            onClick = onUseDevice,
                            modifier = Modifier.testTag("settings-use-current-location"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = "Usa posizione attuale",
                                tint = SettingsAccent,
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SettingsText,
                        unfocusedTextColor = SettingsText,
                        focusedContainerColor = SettingsSurface,
                        unfocusedContainerColor = SettingsSurface,
                        focusedBorderColor = SettingsAccent.copy(alpha = 0.72f),
                        unfocusedBorderColor = SettingsOutline,
                        cursorColor = SettingsAccent,
                    ),
                )
                DropdownMenu(
                    expanded = searchMenuVisible && uiState.searchResults.isNotEmpty(),
                    onDismissRequest = { searchMenuVisible = false },
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .testTag("settings-city-suggestions"),
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
                Text("Ricerca locale…", style = MaterialTheme.typography.bodySmall, color = SettingsMuted)
            }
            uiState.searchMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = SettingsMuted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String, tag: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = SettingsAccent,
        modifier = Modifier
            .padding(top = 2.dp, start = 2.dp)
            .testTag(tag),
    )
}

@Composable
private fun CompactOutlinedAction(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, SettingsOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsText),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
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
        Text(city.timeZoneId, style = MaterialTheme.typography.bodySmall, color = SettingsMuted)
        if (!city.timeZoneSupported) {
            Text(
                text = "Fuso non supportato su questa versione Android: selezionando la città Arihna mostrerà un errore controllato.",
                style = MaterialTheme.typography.bodySmall,
                color = SettingsDanger,
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
private fun LanguageSettingsCard(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-language-card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurface),
        border = BorderStroke(1.dp, SettingsOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(
                onClick = { onSelect(AppLanguage.ITALIAN) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings-language-italian"),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected == AppLanguage.ITALIAN) SettingsAccent.copy(alpha = 0.18f) else Color.Transparent,
                    contentColor = SettingsText,
                ),
            ) { Text("Italiano", fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = { onSelect(AppLanguage.ARABIC) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings-language-arabic"),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected == AppLanguage.ARABIC) SettingsAccent.copy(alpha = 0.18f) else Color.Transparent,
                    contentColor = SettingsText,
                ),
            ) { Text("العربية", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AlarmVolumeCard(
    state: AlarmSettingsPresentation,
    onAlarmVolumeChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-alarm-volume-card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurface),
        border = BorderStroke(1.dp, SettingsOutline),
    ) {
        AlarmVolumeSetting(state, onAlarmVolumeChange)
    }
}

@Composable
private fun AlarmVolumeSetting(
    state: AlarmSettingsPresentation,
    onAlarmVolumeChange: (Int) -> Unit,
) {
    val volume = state.alarmVolumeState
    val current = volume.current.coerceIn(volume.min, volume.max)
    val span = (volume.max - volume.min).coerceAtLeast(1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("settings-alarm-volume"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Alarm,
                contentDescription = null,
                tint = SettingsAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                appText("Volume sveglia", "مستوى صوت المنبه"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SettingsText,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${volume.percent}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SettingsAccent,
                modifier = Modifier.testTag("settings-alarm-volume-value"),
            )
        }
        Slider(
            value = current.toFloat(),
            onValueChange = { requested ->
                onAlarmVolumeChange(requested.roundToInt().coerceIn(volume.min, volume.max))
            },
            valueRange = volume.min.toFloat()..volume.max.toFloat(),
            steps = (span - 1).coerceAtLeast(0),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-alarm-volume-slider"),
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccent,
                activeTrackColor = SettingsAccent,
                inactiveTrackColor = SettingsSurfaceRaised,
                activeTickColor = SettingsText.copy(alpha = 0.45f),
                inactiveTickColor = SettingsMuted.copy(alpha = 0.24f),
            ),
        )
        Text(
            appText(
                "Volume globale delle sveglie del telefono",
                "هذا يغيّر مستوى صوت المنبهات في الهاتف بالكامل",
            ),
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-alarm-tests"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurfaceRaised),
        border = BorderStroke(1.dp, SettingsOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Button(
                    onClick = onTestAlarm,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings-test-alarm-one-minute"),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsAccent,
                        contentColor = SettingsBackgroundTop,
                    ),
                ) {
                    Text(
                        "Test sveglia (10 secondi)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedButton(
                    onClick = onTestAdhan,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings-test-adhan-one-minute"),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
                    border = BorderStroke(1.dp, SettingsAccent.copy(alpha = 0.58f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsText),
                ) {
                    Text("Test Adhan (10 secondi)", style = MaterialTheme.typography.labelMedium)
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
}
