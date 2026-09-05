package com.archimedeprojects.arihna.feature.prayers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmRingtonePicker
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleUiState
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PrayerTimesRoute(
    contentPadding: PaddingValues,
    prayerScheduleViewModel: PrayerScheduleViewModel,
    alarmsViewModel: AlarmsViewModel,
) {
    val scheduleState by prayerScheduleViewModel.uiState.collectAsState()
    val alarmsState by alarmsViewModel.uiState.collectAsState()
    var soundRule by remember { mutableStateOf<AlarmRule?>(null) }

    PrayerTimesScreen(
        contentPadding = contentPadding,
        scheduleState = scheduleState,
        rules = alarmsState.rules,
        onEnabled = alarmsViewModel::setPrayerEnabled,
        onOpenSound = { soundRule = it },
    )

    soundRule?.let { rule ->
        PrayerSoundDialog(
            rule = rule,
            onDismiss = { soundRule = null },
            onSave = { profile, uri, title ->
                alarmsViewModel.setSound(rule, profile, uri, title)
                soundRule = null
            },
        )
    }
}

@Composable
private fun PrayerTimesScreen(
    contentPadding: PaddingValues,
    scheduleState: PrayerScheduleUiState,
    rules: List<AlarmRule>,
    onEnabled: (AlarmPrayer, Boolean) -> Unit,
    onOpenSound: (AlarmRule) -> Unit,
) {
    val prayerRules = rules.mapNotNull { rule ->
        val definition = rule.definition as? AlarmDefinition.PrayerLinked ?: return@mapNotNull null
        definition.prayer to rule
    }.toMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("prayer-times-screen"),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = contentPadding.calculateTopPadding() + 18.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Orari", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Orari di preghiera e promemoria dedicati.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        when (scheduleState) {
            PrayerScheduleUiState.Loading -> item { CircularProgressIndicator() }
            is PrayerScheduleUiState.NoLocation -> item { Text(scheduleState.message) }
            is PrayerScheduleUiState.CalculationUnavailable -> item { Text(scheduleState.message) }
            is PrayerScheduleUiState.Ready -> {
                item {
                    Surface(
                        color = ArihnaGold.copy(alpha = 0.13f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            "Oggi • ${scheduleState.location.displayName}",
                            color = ArihnaGold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                item { Text("Preghiere", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
                val zone = scheduleState.today.zoneId
                val rows = listOf(
                    Triple(AlarmPrayer.FAJR, "Fajr", scheduleState.today.times.fajr),
                    Triple(AlarmPrayer.DHUHR, "Dhuhr", scheduleState.today.times.dhuhr),
                    Triple(AlarmPrayer.ASR, "Asr", scheduleState.today.times.asr),
                    Triple(AlarmPrayer.MAGHRIB, "Maghrib", scheduleState.today.times.maghrib),
                    Triple(AlarmPrayer.ISHA, "Isha", scheduleState.today.times.isha),
                )
                rows.forEach { (prayer, label, instant) ->
                    item(key = prayer.name) {
                        PrayerReminderRow(
                            prayer = prayer,
                            label = label,
                            time = formatTime(instant, zone),
                            rule = prayerRules[prayer],
                            onEnabled = { onEnabled(prayer, it) },
                            onOpenSound = onOpenSound,
                        )
                    }
                }
                item {
                    Text(
                        "I promemoria di preghiera si gestiscono qui, separati dalle sveglie personali.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerReminderRow(
    prayer: AlarmPrayer,
    label: String,
    time: String,
    rule: AlarmRule?,
    onEnabled: (Boolean) -> Unit,
    onOpenSound: (AlarmRule) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("prayer-reminder-${prayer.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule?.enabled == true) ArihnaGreen.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(time, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (rule != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ArihnaGold.copy(alpha = 0.14f),
                    modifier = Modifier.clickable { onOpenSound(rule) }
                        .testTag("prayer-reminder-${prayer.name.lowercase()}-sound"),
                ) {
                    Text(
                        prayerSoundLabel(rule),
                        color = ArihnaGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Text("Disattivato", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = rule?.enabled == true,
                onCheckedChange = onEnabled,
                modifier = Modifier.testTag("prayer-reminder-${prayer.name.lowercase()}-switch"),
            )
        }
    }
}

@Composable
private fun PrayerSoundDialog(
    rule: AlarmRule,
    onDismiss: () -> Unit,
    onSave: (AlarmSoundProfile, String?, String?) -> Unit,
) {
    val context = LocalContext.current
    var profile by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.soundProfile) }
    var ringtoneUri by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.ringtoneUri) }
    var ringtoneTitle by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.ringtoneTitle) }
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            AlarmRingtonePicker.pickedUri(result.data)?.let { uri ->
                ringtoneUri = uri.toString()
                ringtoneTitle = AlarmRingtonePicker.title(context, uri) ?: "Suoneria telefono"
                profile = AlarmSoundProfile.SYSTEM_DEFAULT
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suono promemoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrayerSoundOption("Adhan", "Adhan offline", profile == AlarmSoundProfile.ADHAN) {
                    profile = AlarmSoundProfile.ADHAN
                    ringtoneUri = null
                    ringtoneTitle = null
                }
                PrayerSoundOption(
                    "Suoneria telefono",
                    ringtoneTitle ?: "Predefinita del telefono",
                    profile == AlarmSoundProfile.SYSTEM_DEFAULT,
                ) { profile = AlarmSoundProfile.SYSTEM_DEFAULT }
                if (profile == AlarmSoundProfile.SYSTEM_DEFAULT) {
                    Button(
                        onClick = { ringtoneLauncher.launch(AlarmRingtonePicker.createIntent(ringtoneUri)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cambia suoneria") }
                }
                PrayerSoundOption("Silenzioso", "Nessun audio", profile == AlarmSoundProfile.SILENT) {
                    profile = AlarmSoundProfile.SILENT
                    ringtoneUri = null
                    ringtoneTitle = null
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(profile, ringtoneUri, ringtoneTitle) }) { Text("Conferma") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}

@Composable
private fun PrayerSoundOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ArihnaGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun prayerSoundLabel(rule: AlarmRule): String = when (rule.soundProfile) {
    AlarmSoundProfile.ADHAN -> "Adhan"
    AlarmSoundProfile.SYSTEM_DEFAULT -> rule.ringtoneTitle ?: "Suoneria"
    AlarmSoundProfile.SILENT -> "Silenzioso"
}

private fun formatTime(instant: Instant, zoneId: ZoneId): String =
    TIME_FORMATTER.withZone(zoneId).format(instant)

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
