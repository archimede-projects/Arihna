package com.archimedeprojects.arihna.feature.alarms

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("alarm-new"),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    "＋  Nuova sveglia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Le tue sveglie",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    if (customRules.isEmpty()) {
                        "Nessuna sveglia personale"
                    } else {
                        "${customRules.size} ${if (customRules.size == 1) "sveglia" else "sveglie"}"
                    },
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
        state.message?.takeUnless { it == "Suono aggiornato" }?.let { message ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ArihnaGold.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarms-message"),
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
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF075B3D),
                            ArihnaGreen,
                            Color(0xFF8A6D1F),
                        ),
                    ),
                )
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = ArihnaGold.copy(alpha = 0.20f),
                ) {
                    Text(
                        "♪",
                        color = ArihnaGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                Text(
                    "ARIHNA • SVEGLIE PERSONALI",
                    color = ArihnaGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text(
                "Sveglie",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Le tue sveglie personali, facili da creare e modificare.",
                color = Color.White.copy(alpha = 0.86f),
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
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm-custom-row-${rule.alarmId}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) {
                ArihnaGreen.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        definition.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("alarm-custom-switch-${rule.alarmId}"),
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
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
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("alarm-edit-${rule.alarmId}"),
                ) {
                    Text("Modifica")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("alarm-delete-${rule.alarmId}"),
                ) {
                    Text("Elimina")
                }
            }
        }
    }
}

@Composable
internal fun CustomAlarmEditorDialog(
    initialRule: AlarmRule?,
    onDismiss: () -> Unit,
    onSave: (
        AlarmRule?, String, LocalTime, Set<DayOfWeek>, AlarmSoundProfile, String?, String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    val initialDefinition = initialRule?.definition as? AlarmDefinition.Custom
    var label by remember(initialRule?.alarmId) {
        mutableStateOf(initialDefinition?.label.orEmpty())
    }
    var time by remember(initialRule?.alarmId) {
        mutableStateOf(
            initialDefinition?.localTime
                ?: LocalTime.now().plusMinutes(1).withSecond(0).withNano(0),
        )
    }
    var weekdays by remember(initialRule?.alarmId) {
        mutableStateOf(initialDefinition?.weekdays ?: emptySet())
    }
    var soundEnabled by remember(initialRule?.alarmId) {
        mutableStateOf(initialRule?.soundProfile != AlarmSoundProfile.SILENT)
    }
    var ringtoneUri by remember(initialRule?.alarmId) {
        mutableStateOf(initialRule?.ringtoneUri)
    }
    var ringtoneTitle by remember(initialRule?.alarmId) {
        mutableStateOf(initialRule?.ringtoneTitle)
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            AlarmRingtonePicker.pickedUri(result.data)?.let { uri ->
                ringtoneUri = uri.toString()
                ringtoneTitle = AlarmRingtonePicker.title(context, uri) ?: "Suoneria telefono"
                soundEnabled = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialRule == null) "Nuova sveglia" else "Modifica sveglia",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ArihnaGold.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> time = LocalTime.of(hour, minute) },
                                time.hour,
                                time.minute,
                                true,
                            ).show()
                        }
                        .testTag("alarm-editor-time"),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            time.format(TIME_FORMATTER),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ArihnaGold,
                        )
                        Text(
                            "Tocca per cambiare ora",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm-editor-label"),
                )
                Text(
                    "Ripeti",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in weekdays
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                ArihnaGold.copy(alpha = 0.24f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable {
                                    weekdays = if (selected) weekdays - day else weekdays + day
                                }
                                .testTag("alarm-day-${day.name.lowercase()}"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    dayInitial(day),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selected) ArihnaGold else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                Text(
                    if (weekdays.isEmpty()) "Singola" else recurrenceLabel(weekdays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Suono",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (soundEnabled) {
                        ArihnaGold.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    ringtoneLauncher.launch(
                                        AlarmRingtonePicker.createIntent(ringtoneUri),
                                    )
                                }
                                .testTag("alarm-ringtone-row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ArihnaGold.copy(alpha = 0.18f),
                            ) {
                                Text(
                                    "♪",
                                    color = ArihnaGold,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                            Column {
                                Text("Suoneria telefono", fontWeight = FontWeight.Bold)
                                Text(
                                    ringtoneTitle ?: "Predefinita del telefono",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it },
                            modifier = Modifier.testTag("alarm-sound-switch"),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialRule,
                        label,
                        time,
                        weekdays,
                        if (soundEnabled) {
                            AlarmSoundProfile.SYSTEM_DEFAULT
                        } else {
                            AlarmSoundProfile.SILENT
                        },
                        ringtoneUri,
                        ringtoneTitle,
                    )
                },
                enabled = label.isNotBlank(),
                modifier = Modifier.testTag("alarm-editor-save"),
            ) {
                Text("Salva sveglia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
    )
}

private fun soundLabel(rule: AlarmRule): String = when (rule.soundProfile) {
    AlarmSoundProfile.ADHAN -> "Adhan"
    AlarmSoundProfile.SYSTEM_DEFAULT -> rule.ringtoneTitle ?: "Suoneria telefono"
    AlarmSoundProfile.SILENT -> "Suono disattivato"
}

private fun recurrenceLabel(days: Set<DayOfWeek>): String =
    if (days.isEmpty()) {
        "Singola"
    } else {
        days.sortedBy { it.value }.joinToString(separator = " • ", transform = ::dayInitial)
    }

private fun dayInitial(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "M"
    DayOfWeek.THURSDAY -> "G"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
