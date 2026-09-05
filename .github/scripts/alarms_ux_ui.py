from pathlib import Path


def write(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"missing patch marker in {path}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/AlarmsPlaceholderScreen.kt", r'''package com.archimedeprojects.arihna.feature.alarms

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmRingtonePicker
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AlarmsRoute(
    contentPadding: PaddingValues,
    viewModel: AlarmsViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AlarmRule?>(null) }

    AlarmsScreen(
        contentPadding = contentPadding,
        state = state,
        onNew = { creating = true },
        onEdit = { editingRule = it },
        onToggle = viewModel::toggle,
        onDelete = viewModel::delete,
    )

    if (creating || editingRule != null) {
        CustomAlarmEditorDialog(
            initialRule = editingRule,
            onDismiss = {
                creating = false
                editingRule = null
            },
            onSave = { rule, label, time, weekdays, sound, ringtoneUri, ringtoneTitle ->
                viewModel.saveCustom(
                    existing = rule,
                    label = label,
                    localTime = time,
                    weekdays = weekdays,
                    soundProfile = sound,
                    ringtoneUri = ringtoneUri,
                    ringtoneTitle = ringtoneTitle,
                )
                creating = false
                editingRule = null
            },
        )
    }
}

@Composable
fun AlarmsScreen(
    contentPadding: PaddingValues,
    state: AlarmsUiState,
    onNew: () -> Unit,
    onEdit: (AlarmRule) -> Unit,
    onToggle: (AlarmRule) -> Unit,
    onDelete: (AlarmRule) -> Unit,
) {
    val customRules = state.rules.filter { it.definition is AlarmDefinition.Custom }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("alarms-screen"),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = contentPadding.calculateTopPadding() + 14.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PersonalAlarmHero() }
        item {
            Button(
                onClick = onNew,
                modifier = Modifier.fillMaxWidth().height(58.dp).testTag("alarm-new"),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("＋  Nuova sveglia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Le tue sveglie", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (customRules.isEmpty()) "Nessuna sveglia personale" else "${customRules.size} ${if (customRules.size == 1) "sveglia" else "sveglie"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(customRules, key = { it.alarmId }) { rule ->
            CustomAlarmCard(
                rule = rule,
                onEdit = { onEdit(rule) },
                onToggle = { onToggle(rule) },
                onDelete = { onDelete(rule) },
            )
        }
        state.message?.let { message ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ArihnaGold.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().testTag("alarms-message"),
                ) {
                    Text(message, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}

@Composable
private fun PersonalAlarmHero() {
    Surface(
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(ArihnaGreen, Color(0xFF0B5A41), Color(0xFF7A6323)),
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("Sveglie", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Le tue sveglie personali, facili da creare e modificare.",
                color = Color.White.copy(alpha = 0.84f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CustomAlarmCard(
    rule: AlarmRule,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val definition = rule.definition as AlarmDefinition.Custom
    Card(
        modifier = Modifier.fillMaxWidth().testTag("alarm-custom-row-${rule.alarmId}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) ArihnaGreen.copy(alpha = 0.23f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        definition.localTime.format(TIME_FORMATTER),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(definition.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("alarm-custom-switch-${rule.alarmId}"),
                )
            }
            Text(
                recurrenceLabel(definition.weekdays),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                soundLabel(rule),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f).testTag("alarm-edit-${rule.alarmId}"),
                ) { Text("Modifica") }
                TextButton(onClick = onDelete, modifier = Modifier.testTag("alarm-delete-${rule.alarmId}")) {
                    Text("Elimina")
                }
            }
        }
    }
}

@Composable
private fun CustomAlarmEditorDialog(
    initialRule: AlarmRule?,
    onDismiss: () -> Unit,
    onSave: (
        AlarmRule?, String, LocalTime, Set<DayOfWeek>, AlarmSoundProfile, String?, String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    val initialDefinition = initialRule?.definition as? AlarmDefinition.Custom
    var label by remember(initialRule?.alarmId) { mutableStateOf(initialDefinition?.label.orEmpty()) }
    var time by remember(initialRule?.alarmId) {
        mutableStateOf(initialDefinition?.localTime ?: LocalTime.now().plusMinutes(1).withSecond(0).withNano(0))
    }
    var weekdays by remember(initialRule?.alarmId) { mutableStateOf(initialDefinition?.weekdays ?: emptySet()) }
    var soundProfile by remember(initialRule?.alarmId) {
        mutableStateOf(initialRule?.soundProfile ?: AlarmSoundProfile.SYSTEM_DEFAULT)
    }
    var ringtoneUri by remember(initialRule?.alarmId) { mutableStateOf(initialRule?.ringtoneUri) }
    var ringtoneTitle by remember(initialRule?.alarmId) { mutableStateOf(initialRule?.ringtoneTitle) }

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            AlarmRingtonePicker.pickedUri(result.data)?.let { uri ->
                ringtoneUri = uri.toString()
                ringtoneTitle = AlarmRingtonePicker.title(context, uri) ?: "Suoneria telefono"
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) "Nuova sveglia" else "Modifica sveglia") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> time = LocalTime.of(hour, minute) },
                            time.hour,
                            time.minute,
                            true,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp).testTag("alarm-editor-time"),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(time.format(TIME_FORMATTER), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Tocca l'orario per scegliere ore e minuti dall'orologio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("alarm-editor-label"),
                )
                Text("Ripeti", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in weekdays,
                            onClick = {
                                weekdays = if (day in weekdays) weekdays - day else weekdays + day
                            },
                            label = { Text(dayShort(day)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text(
                    if (weekdays.isEmpty()) "Singola" else recurrenceLabel(weekdays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Suono", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SoundChoiceRow(
                    title = "Adhan",
                    subtitle = "Adhan offline incluso in Arihna",
                    selected = soundProfile == AlarmSoundProfile.ADHAN,
                    tag = "alarm-sound-adhan",
                    onClick = {
                        soundProfile = AlarmSoundProfile.ADHAN
                        ringtoneUri = null
                        ringtoneTitle = null
                    },
                )
                SoundChoiceRow(
                    title = "Suoneria telefono",
                    subtitle = ringtoneTitle ?: "Predefinita del telefono",
                    selected = soundProfile == AlarmSoundProfile.SYSTEM_DEFAULT,
                    tag = "alarm-sound-system",
                    onClick = { soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT },
                )
                if (soundProfile == AlarmSoundProfile.SYSTEM_DEFAULT) {
                    OutlinedButton(
                        onClick = { ringtoneLauncher.launch(AlarmRingtonePicker.createIntent(ringtoneUri)) },
                        modifier = Modifier.fillMaxWidth().testTag("alarm-ringtone-change"),
                    ) { Text("Cambia suoneria") }
                }
                SoundChoiceRow(
                    title = "Silenzioso",
                    subtitle = "Notifica e schermata senza audio",
                    selected = soundProfile == AlarmSoundProfile.SILENT,
                    tag = "alarm-sound-silent",
                    onClick = {
                        soundProfile = AlarmSoundProfile.SILENT
                        ringtoneUri = null
                        ringtoneTitle = null
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initialRule, label, time, weekdays, soundProfile, ringtoneUri, ringtoneTitle)
                },
                enabled = label.isNotBlank(),
                modifier = Modifier.testTag("alarm-editor-save"),
            ) { Text("Salva sveglia") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun SoundChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ArihnaGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun soundLabel(rule: AlarmRule): String = when (rule.soundProfile) {
    AlarmSoundProfile.ADHAN -> "Adhan"
    AlarmSoundProfile.SYSTEM_DEFAULT -> rule.ringtoneTitle ?: "Suoneria telefono"
    AlarmSoundProfile.SILENT -> "Silenzioso"
}

private fun recurrenceLabel(days: Set<DayOfWeek>): String =
    if (days.isEmpty()) "Singola" else days.sortedBy { it.value }.joinToString(" • ")(::dayShort)

private fun dayShort(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Lun"
    DayOfWeek.TUESDAY -> "Mar"
    DayOfWeek.WEDNESDAY -> "Mer"
    DayOfWeek.THURSDAY -> "Gio"
    DayOfWeek.FRIDAY -> "Ven"
    DayOfWeek.SATURDAY -> "Sab"
    DayOfWeek.SUNDAY -> "Dom"
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
''')

write("app/src/main/java/com/archimedeprojects/arihna/feature/prayers/PrayerTimesPlaceholderScreen.kt", r'''package com.archimedeprojects.arihna.feature.prayers

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
''')

# AppContainer: expose only the approved isolated diagnostic scheduler.
replace_once(
    "app/src/main/java/com/archimedeprojects/arihna/app/AppContainer.kt",
    '''import com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess\n''',
    '''import com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticTestScheduler\nimport com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess\n''',
)
replace_once(
    "app/src/main/java/com/archimedeprojects/arihna/app/AppContainer.kt",
    '''    val alarmNotificationPermissionReader: AlarmNotificationPermissionReader by lazy {\n        AndroidAlarmNotificationPermissionReader(appContext)\n    }\n''',
    '''    val alarmNotificationPermissionReader: AlarmNotificationPermissionReader by lazy {\n        AndroidAlarmNotificationPermissionReader(appContext)\n    }\n    val alarmDiagnosticTestScheduler: AlarmDiagnosticTestScheduler by lazy {\n        AlarmDiagnosticTestScheduler(\n            context = appContext,\n            notificationPermissionReader = alarmNotificationPermissionReader,\n            fullScreenAccess = alarmFullScreenAccess,\n        )\n    }\n''',
)

# Navigation ownership: Orari owns prayer reminders; Sveglie owns personal alarms; Settings owns capabilities/tests.
nav = Path("app/src/main/java/com/archimedeprojects/arihna/app/ArihnaNavHost.kt")
text = nav.read_text(encoding="utf-8")
text = text.replace('import com.archimedeprojects.arihna.feature.prayers.PrayerTimesPlaceholderScreen', 'import com.archimedeprojects.arihna.feature.prayers.PrayerTimesRoute')
text = text.replace('import com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess', 'import com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticTestScheduler\nimport com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess')
text = text.replace(
'''    alarmFullScreenAccess: AlarmFullScreenAccess,\n    qiblaRepository: QiblaRepository,\n''',
'''    alarmFullScreenAccess: AlarmFullScreenAccess,\n    alarmDiagnosticTestScheduler: AlarmDiagnosticTestScheduler,\n    qiblaRepository: QiblaRepository,\n''',
1,
)
text = text.replace(
'''            composable(Destination.Prayers.route) { PrayerTimesPlaceholderScreen(innerPadding) }\n''',
'''            composable(Destination.Prayers.route) {\n                PrayerTimesRoute(\n                    contentPadding = innerPadding,\n                    prayerScheduleViewModel = prayerScheduleViewModel,\n                    alarmsViewModel = alarmsViewModel,\n                )\n            }\n''',
1,
)
text = text.replace(
'''                AlarmsRoute(\n                    contentPadding = innerPadding,\n                    viewModel = alarmsViewModel,\n                    exactAlarmAccessIntentFactory = exactAlarmAccessIntentFactory,\n                    fullScreenAccess = alarmFullScreenAccess,\n                )\n''',
'''                AlarmsRoute(\n                    contentPadding = innerPadding,\n                    viewModel = alarmsViewModel,\n                )\n''',
1,
)
text = text.replace(
'''                    environment = locationEnvironment,\n                    permissionResolver = locationPermissionStateResolver,\n                )\n''',
'''                    environment = locationEnvironment,\n                    permissionResolver = locationPermissionStateResolver,\n                    alarmsViewModel = alarmsViewModel,\n                    exactAlarmAccessIntentFactory = exactAlarmAccessIntentFactory,\n                    alarmFullScreenAccess = alarmFullScreenAccess,\n                    alarmDiagnosticTestScheduler = alarmDiagnosticTestScheduler,\n                )\n''',
1,
)
nav.write_text(text, encoding="utf-8")

replace_once(
    "app/src/main/java/com/archimedeprojects/arihna/app/ArihnaApp.kt",
    '''            alarmFullScreenAccess = appContainer.alarmFullScreenAccess,\n            qiblaRepository = qiblaRepository,\n''',
    '''            alarmFullScreenAccess = appContainer.alarmFullScreenAccess,\n            alarmDiagnosticTestScheduler = appContainer.alarmDiagnosticTestScheduler,\n            qiblaRepository = qiblaRepository,\n''',
)

# Settings route and screen gain the approved Alarm capability panel and the two one-minute real-path tests.
settings = Path("app/src/main/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreen.kt")
text = settings.read_text(encoding="utf-8")
text = text.replace('import android.app.Activity\n', 'import android.Manifest\nimport android.app.Activity\nimport android.os.Build\n')
text = text.replace('import androidx.compose.runtime.getValue\n', 'import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.mutableIntStateOf\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\n')
text = text.replace('import androidx.compose.ui.Modifier\n', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.testTag\n')
insert_imports = '''import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel\nimport com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticKind\nimport com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticScheduleResult\nimport com.archimedeprojects.arihna.feature.alarms.platform.AlarmDiagnosticTestScheduler\nimport com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess\nimport com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory\n'''
text = text.replace('import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver\n', 'import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver\n' + insert_imports)
text = text.replace(
'''    environment: AndroidLocationEnvironment,\n    permissionResolver: AndroidLocationPermissionStateResolver,\n) {\n''',
'''    environment: AndroidLocationEnvironment,\n    permissionResolver: AndroidLocationPermissionStateResolver,\n    alarmsViewModel: AlarmsViewModel,\n    exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory,\n    alarmFullScreenAccess: AlarmFullScreenAccess,\n    alarmDiagnosticTestScheduler: AlarmDiagnosticTestScheduler,\n) {\n''',
1,
)
marker = '''    val permissionLauncher = rememberLauncherForActivityResult(\n        contract = ActivityResultContracts.RequestPermission(),\n    ) {\n        val permissionState = permissionResolver.resolve(\n            activity = activity,\n            hasRequestedBefore = viewModel.hasRequestedPermissionBefore(),\n        )\n        viewModel.selectDevice(\n            permissionState = permissionState,\n            locationServicesEnabled = environment.isLocationServicesEnabled(),\n        )\n    }\n\n'''
addition = marker + '''    val alarmsState by alarmsViewModel.uiState.collectAsState()\n    var capabilityRefresh by remember { mutableIntStateOf(0) }\n    var diagnosticMessage by remember { mutableStateOf<String?>(null) }\n    val fullScreenReady = alarmFullScreenAccess.isGranted()\n\n    val notificationLauncher = rememberLauncherForActivityResult(\n        contract = ActivityResultContracts.RequestPermission(),\n    ) {\n        capabilityRefresh += 1\n        alarmsViewModel.refreshCapabilities()\n    }\n    val exactAlarmLauncher = rememberLauncherForActivityResult(\n        contract = ActivityResultContracts.StartActivityForResult(),\n    ) {\n        capabilityRefresh += 1\n        alarmsViewModel.refreshCapabilities()\n    }\n    val fullScreenLauncher = rememberLauncherForActivityResult(\n        contract = ActivityResultContracts.StartActivityForResult(),\n    ) {\n        capabilityRefresh += 1\n        alarmsViewModel.refreshCapabilities()\n    }\n\n    fun diagnosticResultMessage(kind: AlarmDiagnosticKind, result: AlarmDiagnosticScheduleResult): String =\n        when (result) {\n            AlarmDiagnosticScheduleResult.SCHEDULED ->\n                if (kind == AlarmDiagnosticKind.ADHAN) "Test Adhan programmato tra 1 minuto" else "Test sveglia programmato tra 1 minuto"\n            AlarmDiagnosticScheduleResult.NEEDS_NOTIFICATION_PERMISSION -> "Consenti prima le notifiche"\n            AlarmDiagnosticScheduleResult.NEEDS_EXACT_ALARM_ACCESS -> "Consenti prima gli allarmi esatti"\n            AlarmDiagnosticScheduleResult.NEEDS_FULL_SCREEN_ACCESS -> "Consenti prima lo schermo intero"\n        }\n\n'''
if marker not in text:
    raise SystemExit("settings permission launcher marker missing")
text = text.replace(marker, addition, 1)
old_call_end = '''        onOpenLocationSettings = {\n            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))\n        },\n    )\n}\n'''
new_call_end = '''        onOpenLocationSettings = {\n            activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))\n        },\n        alarmSettings = AlarmSettingsPresentation(\n            notificationReady = alarmsState.notificationReady,\n            exactReady = alarmsState.exactAlarmReady,\n            fullScreenReady = fullScreenReady,\n            diagnosticMessage = diagnosticMessage,\n        ),\n        onManageNotifications = {\n            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alarmsState.notificationReady) {\n                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)\n            } else {\n                activity.startActivity(\n                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)\n                        .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName),\n                )\n            }\n        },\n        onManageExactAlarms = {\n            exactAlarmAccessIntentFactory.create()?.let(exactAlarmLauncher::launch)\n                ?: alarmsViewModel.refreshCapabilities()\n        },\n        onManageFullScreen = {\n            alarmFullScreenAccess.createSettingsIntent()?.let(fullScreenLauncher::launch)\n                ?: alarmsViewModel.refreshCapabilities()\n        },\n        onTestAlarm = {\n            diagnosticMessage = diagnosticResultMessage(\n                AlarmDiagnosticKind.SYSTEM_ALARM,\n                alarmDiagnosticTestScheduler.scheduleOneMinute(AlarmDiagnosticKind.SYSTEM_ALARM),\n            )\n        },\n        onTestAdhan = {\n            diagnosticMessage = diagnosticResultMessage(\n                AlarmDiagnosticKind.ADHAN,\n                alarmDiagnosticTestScheduler.scheduleOneMinute(AlarmDiagnosticKind.ADHAN),\n            )\n        },\n        onCancelDiagnostic = {\n            alarmDiagnosticTestScheduler.cancel()\n            diagnosticMessage = "Test in attesa annullato"\n        },\n    )\n}\n'''
if old_call_end not in text:
    raise SystemExit("settings route call end marker missing")
text = text.replace(old_call_end, new_call_end, 1)
old_signature = '''    onSelectCity: (Long) -> Unit,\n    onOpenAppSettings: () -> Unit,\n    onOpenLocationSettings: () -> Unit,\n) {\n'''
new_signature = '''    onSelectCity: (Long) -> Unit,\n    onOpenAppSettings: () -> Unit,\n    onOpenLocationSettings: () -> Unit,\n    alarmSettings: AlarmSettingsPresentation = AlarmSettingsPresentation(),\n    onManageNotifications: () -> Unit = {},\n    onManageExactAlarms: () -> Unit = {},\n    onManageFullScreen: () -> Unit = {},\n    onTestAlarm: () -> Unit = {},\n    onTestAdhan: () -> Unit = {},\n    onCancelDiagnostic: () -> Unit = {},\n) {\n'''
if old_signature not in text:
    raise SystemExit("settings screen signature marker missing")
text = text.replace(old_signature, new_signature, 1)
lazy_marker = '''    ) {\n        item {\n            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {\n                Text(\n                    text = "Posizione",\n'''
lazy_new = '''    ) {\n        item {\n            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {\n                Text(\n                    text = "Impostazioni",\n                    style = MaterialTheme.typography.headlineLarge,\n                    color = MaterialTheme.colorScheme.primary,\n                )\n                Text(\n                    text = "Controlli di sistema e test rapidi per sveglie e Adhan.",\n                    style = MaterialTheme.typography.bodyMedium,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant,\n                )\n            }\n        }\n        item {\n            AlarmSystemSettingsCard(\n                state = alarmSettings,\n                onManageNotifications = onManageNotifications,\n                onManageExactAlarms = onManageExactAlarms,\n                onManageFullScreen = onManageFullScreen,\n            )\n        }\n        item {\n            AlarmDiagnosticCard(\n                state = alarmSettings,\n                onTestAlarm = onTestAlarm,\n                onTestAdhan = onTestAdhan,\n                onCancelDiagnostic = onCancelDiagnostic,\n            )\n        }\n        item {\n            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {\n                Text(\n                    text = "Posizione",\n'''
if lazy_marker not in text:
    raise SystemExit("settings LazyColumn marker missing")
text = text.replace(lazy_marker, lazy_new, 1)
append = r'''

data class AlarmSettingsPresentation(
    val notificationReady: Boolean = false,
    val exactReady: Boolean = false,
    val fullScreenReady: Boolean = false,
    val diagnosticMessage: String? = null,
)

@Composable
private fun AlarmSystemSettingsCard(
    state: AlarmSettingsPresentation,
    onManageNotifications: () -> Unit,
    onManageExactAlarms: () -> Unit,
    onManageFullScreen: () -> Unit,
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
        }
    }
}

@Composable
private fun AlarmCapabilitySettingRow(
    label: String,
    ready: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                "Esegue un test reale tra 1 minuto usando lo stesso percorso di sveglie, notifica e schermo intero.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onTestAlarm,
                modifier = Modifier.fillMaxWidth().testTag("settings-test-alarm-one-minute"),
            ) { Text("Test sveglia (1 minuto)") }
            Button(
                onClick = onTestAdhan,
                modifier = Modifier.fillMaxWidth().testTag("settings-test-adhan-one-minute"),
            ) { Text("Test Adhan (1 minuto)") }
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
'''
text = text.rstrip() + append + "\n"
settings.write_text(text, encoding="utf-8")
