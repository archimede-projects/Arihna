package com.archimedeprojects.arihna.feature.planning

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.i18n.appText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmRingtonePicker
import com.archimedeprojects.arihna.feature.alarms.platform.ARIHNA_CLASSIC_ALARM_TITLE
import com.archimedeprojects.arihna.feature.alarms.platform.ARIHNA_CLASSIC_ALARM_URI
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ITALIAN)

@Composable
fun PlanningRoute(
    contentPadding: PaddingValues,
    repository: PlanningRepository,
    scheduler: PlanningScheduler,
    onBack: () -> Unit,
) {
    val items by repository.entries.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editorType by remember { mutableStateOf<PlanningType?>(null) }
    var editing by remember { mutableStateOf<PlanningItem?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val upcoming = items.filter { it.pending }.sortedWith(
        compareBy<PlanningItem> { it.date ?: LocalDate.MAX }.thenBy { it.time ?: LocalTime.MAX },
    )
    val completed = items.filterNot { it.pending }.sortedByDescending { it.date }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
            .padding(16.dp).testTag("planning-root"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(appText("Planning", "التخطيط"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text(appText("Indietro", "رجوع")) }
        }
        Text(appText("Cose da fare, promemoria e sveglie con data", "المهام والتذكيرات والمنبهات بتاريخ محدد"))
        if (!scheduler.notificationsGranted() || !scheduler.exactAlarmsGranted()) {
            Text(
                "Attenzione: abilita notifiche e sveglie esatte nelle impostazioni Android per ricevere gli avvisi programmati.",
                modifier = Modifier.testTag("planning-permission-warning"),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { editing = null; editorType = PlanningType.TASK },
                modifier = Modifier.weight(1f).testTag("planning-new-task"),
                contentPadding = PaddingValues(4.dp),
            ) { Text("Da fare", maxLines = 1) }
            Button(
                onClick = { editing = null; editorType = PlanningType.REMINDER },
                modifier = Modifier.weight(1f).testTag("planning-new-reminder"),
                contentPadding = PaddingValues(4.dp),
            ) { Text("Promemoria", maxLines = 1) }
            Button(
                onClick = { editing = null; editorType = PlanningType.ALARM },
                modifier = Modifier.weight(1f).testTag("planning-new-alarm"),
                contentPadding = PaddingValues(4.dp),
            ) { Text("Sveglia", maxLines = 1) }
        }
        feedback?.let { Text(it, color = ArihnaGreen, modifier = Modifier.testTag("planning-feedback")) }
        Text("Oggi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        upcoming.filter { it.date == today }.forEach { item ->
            PlanningRow(item, onEdit = { editing = item; editorType = item.type },
                onComplete = { scope.launch { repository.setCompleted(item.id, !item.complete); scheduler.reconcile() } },
                onDelete = { scope.launch { repository.delete(item.id); scheduler.reconcile() } })
        }
        Text("Prossimi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        upcoming.filter { it.date != today }.forEach { item ->
            PlanningRow(item, onEdit = { editing = item; editorType = item.type },
                onComplete = { scope.launch { repository.setCompleted(item.id, !item.complete); scheduler.reconcile() } },
                onDelete = { scope.launch { repository.delete(item.id); scheduler.reconcile() } })
        }
        if (upcoming.isEmpty()) Text("Nessun impegno da programmare")
        HorizontalDivider()
        Text("Completati", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        completed.forEach { item ->
            PlanningRow(item, onEdit = { editing = item; editorType = item.type },
                onComplete = { scope.launch { repository.setCompleted(item.id, !item.complete); scheduler.reconcile() } },
                onDelete = { scope.launch { repository.delete(item.id); scheduler.reconcile() } })
        }
    }

    editorType?.let { type ->
        PlanningEditor(
            type = type, old = editing, onDismiss = { editorType = null },
            onSave = { title, note, date, time, sound, uri, ringtoneTitle, volume ->
                scope.launch {
                    val attempted = runCatching {
                        require(title.isNotBlank()) { "Inserisci un titolo" }
                        if (type != PlanningType.TASK) {
                            require(date != null && time != null && PlanningTime.validFuture(
                                date, time, java.time.Instant.now(), ZoneId.systemDefault(),
                            )) { "Scegli una data e ora future" }
                        }
                        repository.save(type, title, note, date, time, sound, uri, ringtoneTitle, volume, editing?.id)
                    }
                    attempted.onSuccess { item ->
                        scheduler.reconcile()
                        feedback = "Salvato: ${item.title}" +
                            (item.date?.let { " · ${it.format(dateFormat)}" } ?: "") +
                            (item.time?.let { " · $it" } ?: "")
                        editorType = null
                    }.onFailure { feedback = it.message ?: "Impossibile salvare" }
                }
            },
        )
    }
}

@Composable
private fun PlanningRow(
    item: PlanningItem,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ArihnaGold.copy(alpha = .3f)),
        modifier = Modifier.fillMaxWidth().testTag("planning-item-${item.id}"),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.title, fontWeight = FontWeight.Bold, color = ArihnaForest)
                if (item.type == PlanningType.TASK) Checkbox(checked = item.complete, onCheckedChange = { onComplete() })
            }
            val kind = when (item.type) {
                PlanningType.TASK -> "Da fare"
                PlanningType.REMINDER -> "Promemoria"
                PlanningType.ALARM -> "Sveglia"
            }
            Text(
                "$kind" + (item.date?.let { " · ${it.format(dateFormat)}" } ?: "") +
                    (item.time?.let { " · ${it.toString()}" } ?: "") +
                    (if (item.delivered) " · Consegnato" else ""),
                color = ArihnaGreen,
                style = MaterialTheme.typography.bodySmall,
            )
            if (item.note.isNotBlank()) Text(item.note, color = ArihnaForest)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onEdit) { Text("Modifica") }
                TextButton(onClick = onDelete) { Text("Elimina") }
            }
        }
    }
}

@Composable
private fun PlanningEditor(
    type: PlanningType,
    old: PlanningItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate?, LocalTime?, AlarmSoundProfile, String?, String?, Int) -> Unit,
) {
    val context = LocalContext.current
    var title by remember(old?.id, type) { mutableStateOf(old?.title.orEmpty()) }
    var note by remember(old?.id, type) { mutableStateOf(old?.note.orEmpty()) }
    var date by remember(old?.id, type) { mutableStateOf(old?.date ?: LocalDate.now().plusDays(1)) }
    var hasTaskDate by remember(old?.id, type) { mutableStateOf(old?.date != null) }
    var time by remember(old?.id, type) { mutableStateOf(old?.time ?: LocalTime.of(9, 0)) }
    var sound by remember(old?.id, type) { mutableStateOf(old?.sound ?: AlarmSoundProfile.SYSTEM_DEFAULT) }
    var ringtoneUri by remember(old?.id, type) { mutableStateOf(old?.ringtoneUri) }
    var ringtoneTitle by remember(old?.id, type) { mutableStateOf(old?.ringtoneTitle) }
    var volume by remember(old?.id, type) { mutableStateOf(old?.volume ?: 100) }
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AlarmRingtonePicker.pickedUri(result.data)?.let {
                ringtoneUri = it.toString()
                ringtoneTitle = AlarmRingtonePicker.title(context, it) ?: "Suoneria telefono"
                sound = AlarmSoundProfile.SYSTEM_DEFAULT
            }
        }
    }
    val heading = when (type) {
        PlanningType.TASK -> "Da fare"
        PlanningType.REMINDER -> "Promemoria"
        PlanningType.ALARM -> "Sveglia con data"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (old == null) "Nuovo $heading" else "Modifica $heading") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .testTag("planning-editor"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    title, { title = it }, label = { Text("Titolo") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("planning-editor-title"),
                )
                OutlinedTextField(
                    note, { note = it }, label = { Text("Note / messaggio") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == PlanningType.TASK) {
                    Row {
                        Checkbox(checked = hasTaskDate, onCheckedChange = { hasTaskDate = it })
                        Text("Aggiungi data")
                    }
                }
                if (type != PlanningType.TASK || hasTaskDate) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                                date.year, date.monthValue - 1, date.dayOfMonth,
                            ).show()
                        },
                        modifier = Modifier.testTag("planning-editor-date"),
                    ) { Text("Data: ${date.format(dateFormat)}") }
                }
                if (type != PlanningType.TASK) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context, { _, hour, minute -> time = LocalTime.of(hour, minute) },
                                time.hour, time.minute, true,
                            ).show()
                        },
                        modifier = Modifier.testTag("planning-editor-time"),
                    ) { Text("Ora: $time") }
                }
                if (type == PlanningType.ALARM) {
                    Text("Suoneria", fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = {
                        sound = AlarmSoundProfile.SYSTEM_DEFAULT
                        ringtoneUri = ARIHNA_CLASSIC_ALARM_URI
                        ringtoneTitle = ARIHNA_CLASSIC_ALARM_TITLE
                    }) { Text("Sveglia classica Arihna") }
                    OutlinedButton(onClick = {
                        sound = AlarmSoundProfile.ADHAN
                        ringtoneUri = null; ringtoneTitle = null
                    }) { Text("Adhan") }
                    OutlinedButton(onClick = {
                        ringtoneLauncher.launch(AlarmRingtonePicker.createIntent(ringtoneUri))
                    }) { Text(ringtoneTitle ?: "Scegli suoneria Android") }
                    OutlinedButton(
                        onClick = { sound = AlarmSoundProfile.VIBRATION },
                        modifier = Modifier.testTag("planning-alarm-vibration"),
                    ) { Text("Solo vibrazione · senza suoneria") }
                    OutlinedButton(onClick = {
                        sound = AlarmSoundProfile.SILENT
                        ringtoneUri = null; ringtoneTitle = null
                    }) { Text("Silenziosa") }
                    Text("Volume: $volume%")
                    Slider(
                        value = volume.toFloat(), onValueChange = { volume = it.toInt().coerceIn(0, 100) },
                        valueRange = 0f..100f,
                        enabled = sound != AlarmSoundProfile.VIBRATION &&
                            sound != AlarmSoundProfile.SILENT,
                        modifier = Modifier.testTag("planning-editor-volume"),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title, note, if (type == PlanningType.TASK && !hasTaskDate) null else date,
                        if (type == PlanningType.TASK) null else time,
                        sound, ringtoneUri, ringtoneTitle, volume,
                    )
                },
                modifier = Modifier.testTag("planning-editor-save"),
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
