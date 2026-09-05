package com.archimedeprojects.arihna.feature.alarms

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
    if (days.isEmpty()) "Singola" else days.sortedBy { it.value }.joinToString(separator = " • ", transform = ::dayShort)

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
