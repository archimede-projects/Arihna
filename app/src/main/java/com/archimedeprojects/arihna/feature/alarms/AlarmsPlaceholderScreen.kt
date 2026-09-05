package com.archimedeprojects.arihna.feature.alarms

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory

@Composable
fun AlarmsRoute(
    contentPadding: PaddingValues,
    viewModel: AlarmsViewModel,
    exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory,
    fullScreenAccess: AlarmFullScreenAccess,
) {
    val state by viewModel.uiState.collectAsState()
    var fullScreenRefresh by remember { mutableIntStateOf(0) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshCapabilities() }
    val exactAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshCapabilities() }
    val fullScreenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        fullScreenRefresh += 1
        viewModel.refreshCapabilities()
    }
    val fullScreenReady = remember(fullScreenRefresh) { fullScreenAccess.isGranted() }

    LaunchedEffect(Unit) { viewModel.refreshCapabilities() }

    AlarmsScreen(
        contentPadding = contentPadding,
        state = state,
        fullScreenReady = fullScreenReady,
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.refreshCapabilities()
            }
        },
        onRequestExactAlarmAccess = {
            exactAlarmAccessIntentFactory.create()?.let(exactAccessLauncher::launch)
                ?: viewModel.refreshCapabilities()
        },
        onRequestFullScreenAccess = {
            fullScreenAccess.createSettingsIntent()?.let(fullScreenLauncher::launch)
                ?: run { fullScreenRefresh += 1 }
        },
        onPrayerEnabled = viewModel::setPrayerEnabled,
        onCreateCustom = viewModel::createCustom,
        onToggleRule = viewModel::toggle,
        onDeleteRule = viewModel::delete,
        onSoundSelected = viewModel::setSound,
    )
}

@Composable
fun AlarmsScreen(
    contentPadding: PaddingValues,
    state: AlarmsUiState,
    fullScreenReady: Boolean = true,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestFullScreenAccess: () -> Unit = {},
    onPrayerEnabled: (AlarmPrayer, Boolean) -> Unit,
    onCreateCustom: (String, String) -> Unit,
    onToggleRule: (AlarmRule) -> Unit,
    onDeleteRule: (AlarmRule) -> Unit,
    onSoundSelected: (AlarmRule, AlarmSoundProfile) -> Unit,
) {
    var customLabel by remember { mutableStateOf("") }
    var customTime by remember { mutableStateOf("") }
    var soundRule by remember { mutableStateOf<AlarmRule?>(null) }
    val prayerRules = state.rules.mapNotNull { rule ->
        val definition = rule.definition as? AlarmDefinition.PrayerLinked ?: return@mapNotNull null
        definition.prayer to rule
    }.toMap()
    val customRules = state.rules.filter { it.definition is AlarmDefinition.Custom }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("alarms-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AlarmHero()
        CapabilityCard(
            notificationReady = state.notificationReady,
            exactReady = state.exactAlarmReady,
            fullScreenReady = fullScreenReady,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
            onRequestFullScreenAccess = onRequestFullScreenAccess,
        )

        SectionTitle("Preghiere", "Adhan o suono sveglia, con un tocco")
        AlarmPrayer.entries.forEach { prayer ->
            val rule = prayerRules[prayer]
            PrayerRow(
                prayer = prayer,
                rule = rule,
                onEnabled = { onPrayerEnabled(prayer, it) },
                onOpenSound = { if (rule != null) soundRule = rule },
            )
        }

        SectionTitle("Sveglia personalizzata", "Una sveglia semplice, precisa e indipendente dagli orari di preghiera")
        Card(
            modifier = Modifier.fillMaxWidth().testTag("alarm-custom-form"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("alarm-custom-label"),
                )
                OutlinedTextField(
                    value = customTime,
                    onValueChange = { customTime = it },
                    label = { Text("Ora HH:mm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("alarm-custom-time"),
                )
                Button(
                    onClick = {
                        onCreateCustom(customLabel, customTime)
                        customLabel = ""
                        customTime = ""
                    },
                    modifier = Modifier.fillMaxWidth().testTag("alarm-custom-add"),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Aggiungi sveglia") }
                Text(
                    "Senza giorni selezionati è una sveglia singola e si disattiva dopo la consegna validata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (customRules.isNotEmpty()) {
            SectionTitle("Le tue sveglie", "Suono e stato sempre visibili")
            customRules.forEach { rule ->
                CustomRuleRow(
                    rule = rule,
                    onToggle = { onToggleRule(rule) },
                    onDelete = { onDeleteRule(rule) },
                    onOpenSound = { soundRule = rule },
                )
            }
        }
        state.message?.let {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ArihnaGold.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth().testTag("alarms-message"),
            ) {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    soundRule?.let { rule ->
        SoundPickerDialog(
            rule = rule,
            onDismiss = { soundRule = null },
            onSelect = { profile ->
                onSoundSelected(rule, profile)
                soundRule = null
            },
        )
    }
}

@Composable
private fun AlarmHero() {
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(ArihnaGreen, Color(0xFF0B5A41), Color(0xFF9B7A22)),
                ),
                shape,
            )
            .testTag("alarm-wow-hero"),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val tone = ArihnaGold.copy(alpha = 0.20f)
            drawCircle(tone, size.height * 0.55f, Offset(size.width * 0.87f, size.height * 0.20f))
            drawCircle(Color(0xFF0B5A41), size.height * 0.48f, Offset(size.width * 0.91f, size.height * 0.15f))
            drawCircle(tone, size.height * 0.08f, Offset(size.width * 0.77f, size.height * 0.35f))
        }
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Sveglie", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Precise. Udibili. Pronte anche a schermo spento.", color = Color.White.copy(alpha = 0.84f))
            Text("Adhan offline • sveglia di sistema • silenzioso", color = ArihnaGold, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CapabilityCard(
    notificationReady: Boolean,
    exactReady: Boolean,
    fullScreenReady: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestFullScreenAccess: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("alarm-capabilities"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = ArihnaGold.copy(alpha = 0.18f)) {
                    Text("✓", color = ArihnaGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
                Text("  Prontezza sveglie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            CapabilityLine("Notifiche", notificationReady)
            if (!notificationReady) {
                Button(onClick = onRequestNotificationPermission, modifier = Modifier.testTag("alarm-notification-permission-action")) {
                    Text("Abilita notifiche")
                }
            }
            CapabilityLine("Allarmi esatti", exactReady)
            if (!exactReady) {
                Button(onClick = onRequestExactAlarmAccess, modifier = Modifier.testTag("alarm-exact-access-action")) {
                    Text("Consenti allarmi esatti")
                }
            }
            CapabilityLine("Schermo intero", fullScreenReady)
            if (!fullScreenReady) {
                Text(
                    "Consenti ad Arihna di mostrare la schermata della sveglia quando il telefono è bloccato.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onRequestFullScreenAccess, modifier = Modifier.testTag("alarm-fullscreen-access-action")) {
                    Text("Consenti schermo intero")
                }
            }
            if (!notificationReady || !exactReady) {
                Text(
                    "Le regole restano salvate, ma non vengono programmate finché manca un accesso necessario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CapabilityLine(label: String, ready: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            if (ready) "Pronto" else "Accesso richiesto",
            color = if (ready) ArihnaGold else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PrayerRow(
    prayer: AlarmPrayer,
    rule: AlarmRule?,
    onEnabled: (Boolean) -> Unit,
    onOpenSound: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("alarm-prayer-${prayer.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule?.enabled == true) ArihnaGold.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = ArihnaGreen.copy(alpha = 0.14f)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (rule?.soundProfile == AlarmSoundProfile.ADHAN || rule == null) "☾" else "♪", color = ArihnaGold, fontSize = 22.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(prayer.displayName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    rule?.soundProfile?.displayName() ?: "Adhan predefinito",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rule != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ArihnaGold.copy(alpha = 0.15f),
                    modifier = Modifier
                        .clickable(onClick = onOpenSound)
                        .testTag("alarm-prayer-${prayer.name.lowercase()}-sound"),
                ) {
                    Text("Suono", color = ArihnaGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp))
                }
            }
            Switch(
                checked = rule?.enabled == true,
                onCheckedChange = onEnabled,
                modifier = Modifier.testTag("alarm-prayer-${prayer.name.lowercase()}-switch"),
            )
        }
    }
}

@Composable
private fun CustomRuleRow(
    rule: AlarmRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onOpenSound: () -> Unit,
) {
    val definition = rule.definition as AlarmDefinition.Custom
    Card(
        modifier = Modifier.fillMaxWidth().testTag("alarm-custom-row-${rule.alarmId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(definition.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${definition.localTime} • ${rule.soundProfile.displayName()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("alarm-custom-switch-${rule.alarmId}"),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenSound, modifier = Modifier.testTag("alarm-custom-sound-${rule.alarmId}")) { Text("Suono") }
                TextButton(onClick = onDelete) { Text("Elimina") }
            }
        }
    }
}

@Composable
private fun SoundPickerDialog(
    rule: AlarmRule,
    onDismiss: () -> Unit,
    onSelect: (AlarmSoundProfile) -> Unit,
) {
    val prayer = rule.definition is AlarmDefinition.PrayerLinked
    val options = if (prayer) {
        listOf(AlarmSoundProfile.ADHAN, AlarmSoundProfile.SYSTEM_DEFAULT, AlarmSoundProfile.SILENT)
    } else {
        listOf(AlarmSoundProfile.SYSTEM_DEFAULT, AlarmSoundProfile.SILENT)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Scegli il suono", fontWeight = FontWeight.ExtraBold)
                Text(
                    if (prayer) "Preghiera • riproduzione offline" else "Sveglia personalizzata",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { profile ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (rule.soundProfile == profile) ArihnaGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(profile) }
                            .testTag("alarm-sound-option-${profile.name.lowercase()}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = rule.soundProfile == profile, onClick = { onSelect(profile) })
                            Column {
                                Text(profile.displayName(), fontWeight = FontWeight.Bold)
                                Text(profile.description(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        modifier = Modifier.testTag("alarm-sound-picker"),
    )
}

private fun AlarmSoundProfile.displayName(): String = when (this) {
    AlarmSoundProfile.ADHAN -> "Adhan"
    AlarmSoundProfile.SYSTEM_DEFAULT -> "Suono sveglia di sistema"
    AlarmSoundProfile.SILENT -> "Silenzioso"
}

private fun AlarmSoundProfile.description(): String = when (this) {
    AlarmSoundProfile.ADHAN -> "Richiamo alla preghiera incluso in Arihna"
    AlarmSoundProfile.SYSTEM_DEFAULT -> "Usa il suono sveglia configurato sul telefono"
    AlarmSoundProfile.SILENT -> "Schermata e notifica senza audio"
}

private fun AlarmPrayer.displayName(): String = when (this) {
    AlarmPrayer.FAJR -> "Fajr"
    AlarmPrayer.DHUHR -> "Dhuhr"
    AlarmPrayer.ASR -> "Asr"
    AlarmPrayer.MAGHRIB -> "Maghrib"
    AlarmPrayer.ISHA -> "Isha"
}
