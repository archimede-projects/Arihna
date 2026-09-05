package com.archimedeprojects.arihna.feature.alarms

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold
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
            onDelete = { rule ->
                viewModel.delete(rule)
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
) {
    val customRules = state.rules.filter { it.definition is AlarmDefinition.Custom }
    val dark = isSystemInDarkTheme()
    val listBackground = if (dark) Color(0xFF111914) else Color(0xFFF2F0E9)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        if (dark) Color(0xFF0B2117) else Color(0xFFF5F1E5),
                    ),
                ),
            )
            .testTag("alarms-screen"),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = contentPadding.calculateTopPadding() + 18.dp,
            end = 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            CompactAlarmHeader(onNew = onNew)
        }

        if (customRules.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = listBackground,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            "Nessuna sveglia",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Crea la prima con Nuova sveglia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            itemsIndexed(customRules, key = { _, rule -> rule.alarmId }) { index, rule ->
                val shape = when {
                    customRules.size == 1 -> RoundedCornerShape(28.dp)
                    index == 0 -> RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp,
                    )
                    index == customRules.lastIndex -> RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp,
                    )
                    else -> RoundedCornerShape(8.dp)
                }
                CompactAlarmRow(
                    rule = rule,
                    shape = shape,
                    containerColor = listBackground,
                    onEdit = { onEdit(rule) },
                    onToggle = { onToggle(rule) },
                )
            }
        }

        state.message?.takeUnless { it == "Suono aggiornato" }?.let { message ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ArihnaGold.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("alarms-message"),
                ) {
                    Text(message, modifier = Modifier.padding(14.dp))
                }
            }
        }
    }
}

@Composable
private fun CompactAlarmHeader(onNew: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Sveglie",
            modifier = Modifier.weight(1f),
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ArihnaGold,
            contentColor = Color(0xFF102C20),
            modifier = Modifier
                .height(46.dp)
                .testTag("alarm-new")
                .clickable(onClick = onNew),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("＋", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Nuova", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun CompactAlarmRow(
    rule: AlarmRule,
    shape: RoundedCornerShape,
    containerColor: Color,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
) {
    val definition = rule.definition as AlarmDefinition.Custom
    val primaryColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (rule.enabled) 1f else 0.46f)
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (rule.enabled) 0.88f else 0.52f)
    val detailText = buildList {
        definition.label.trim().takeIf { it.isNotEmpty() }?.let(::add)
        add(recurrenceLabel(definition.weekdays))
    }.joinToString("  •  ")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm-custom-row-${rule.alarmId}")
            .clickable(onClick = onEdit),
        shape = shape,
        color = containerColor,
        tonalElevation = if (rule.enabled) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 94.dp)
                .padding(start = 18.dp, top = 12.dp, end = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    definition.localTime.format(TIME_FORMATTER),
                    color = primaryColor,
                    fontSize = 42.sp,
                    lineHeight = 45.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    detailText,
                    color = secondaryColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("alarm-custom-switch-${rule.alarmId}"),
            )
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
    onDelete: (AlarmRule) -> Unit = {},
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val editorBackground = if (dark) Color(0xFF08150F) else Color(0xFFF4F1E8)
    val panelColor = if (dark) Color(0xFF15251E) else Color(0xFFFFFFFF)
    val secondaryPanelColor = if (dark) Color(0xFF102019) else Color(0xFFEBE8DF)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(editorBackground)
                .testTag("alarm-editor"),
            color = editorBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (initialRule == null) "Nuova sveglia" else "Sveglia",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        if (initialRule != null && label.isNotBlank()) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color.Transparent,
                        shadowElevation = 3.dp,
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
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF0B5E3B),
                                            Color(0xFF173E2D),
                                            Color(0xFF69551C),
                                        ),
                                    ),
                                )
                                .padding(horizontal = 24.dp, vertical = 30.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "ORA",
                                    color = ArihnaGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                )
                                Text(
                                    time.format(TIME_FORMATTER),
                                    color = Color.White,
                                    fontSize = 58.sp,
                                    lineHeight = 62.sp,
                                    fontWeight = FontWeight.Light,
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = panelColor,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Ripeti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                DayOfWeek.entries.forEach { day ->
                                    val selected = day in weekdays
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) ArihnaGold else secondaryPanelColor,
                                        contentColor = if (selected) Color(0xFF123222) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                weekdays = if (selected) weekdays - day else weekdays + day
                                            }
                                            .testTag("alarm-day-${day.name.lowercase()}"),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                dayInitial(day),
                                                fontWeight = FontWeight.ExtraBold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = panelColor,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            OutlinedTextField(
                                value = label,
                                onValueChange = { label = it },
                                label = { Text("Nome sveglia") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("alarm-editor-label"),
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        ringtoneLauncher.launch(
                                            AlarmRingtonePicker.createIntent(ringtoneUri),
                                        )
                                    }
                                    .padding(horizontal = 18.dp, vertical = 16.dp)
                                    .testTag("alarm-ringtone-row"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        "Suono",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        if (soundEnabled) {
                                            ringtoneTitle ?: "Predefinito"
                                        } else {
                                            "Disattivato"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = { soundEnabled = it },
                                    modifier = Modifier.testTag("alarm-sound-switch"),
                                )
                            }
                        }
                    }

                    if (initialRule != null) {
                        TextButton(
                            onClick = { onDelete(initialRule) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("alarm-editor-delete"),
                        ) {
                            Text(
                                "Elimina sveglia",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Box(modifier = Modifier.height(4.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = panelColor,
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier.height(58.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .testTag("alarm-editor-cancel"),
                        ) {
                            Text(
                                "Annulla",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)),
                        )
                        TextButton(
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
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .testTag("alarm-editor-save"),
                        ) {
                            Text(
                                "Salva",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (label.isNotBlank()) ArihnaGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun recurrenceLabel(days: Set<DayOfWeek>): String =
    if (days.isEmpty()) {
        "Singola"
    } else {
        days.sortedBy { it.value }.joinToString(separator = " ", transform = ::dayInitial)
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
