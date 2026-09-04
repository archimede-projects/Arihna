package com.archimedeprojects.arihna.feature.alarms

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory

@Composable
fun AlarmsRoute(
    contentPadding: PaddingValues,
    viewModel: AlarmsViewModel,
    exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory,
) {
    val state by viewModel.uiState.collectAsState()
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshCapabilities() }
    val exactAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshCapabilities() }

    LaunchedEffect(Unit) { viewModel.refreshCapabilities() }

    AlarmsScreen(
        contentPadding = contentPadding,
        state = state,
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
        onPrayerEnabled = viewModel::setPrayerEnabled,
        onCreateCustom = viewModel::createCustom,
        onToggleRule = viewModel::toggle,
        onDeleteRule = viewModel::delete,
        onToggleSound = viewModel::toggleSound,
    )
}

@Composable
fun AlarmsScreen(
    contentPadding: PaddingValues,
    state: AlarmsUiState,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onPrayerEnabled: (AlarmPrayer, Boolean) -> Unit,
    onCreateCustom: (String, String) -> Unit,
    onToggleRule: (AlarmRule) -> Unit,
    onDeleteRule: (AlarmRule) -> Unit,
    onToggleSound: (AlarmRule) -> Unit,
) {
    var customLabel by remember { mutableStateOf("") }
    var customTime by remember { mutableStateOf("") }
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Sveglie", style = MaterialTheme.typography.headlineSmall)
        CapabilityCard(
            notificationReady = state.notificationReady,
            exactReady = state.exactAlarmReady,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
        )

        Text("Preghiere", style = MaterialTheme.typography.titleMedium)
        AlarmPrayer.entries.forEach { prayer ->
            val rule = prayerRules[prayer]
            PrayerRow(
                prayer = prayer,
                rule = rule,
                onEnabled = { onPrayerEnabled(prayer, it) },
                onToggleSound = { rule?.let(onToggleSound) },
            )
        }

        Text("Sveglia personalizzata", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth().testTag("alarm-custom-form")) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    modifier = Modifier.testTag("alarm-custom-add"),
                ) { Text("Aggiungi") }
                Text(
                    "Senza giorni selezionati è una sveglia singola.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (customRules.isNotEmpty()) {
            Text("Le tue sveglie", style = MaterialTheme.typography.titleMedium)
            customRules.forEach { rule ->
                CustomRuleRow(
                    rule = rule,
                    onToggle = { onToggleRule(rule) },
                    onDelete = { onDeleteRule(rule) },
                    onToggleSound = { onToggleSound(rule) },
                )
            }
        }
        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("alarms-message"))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CapabilityCard(
    notificationReady: Boolean,
    exactReady: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().testTag("alarm-capabilities")) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Prontezza sveglie", style = MaterialTheme.typography.titleSmall)
            Text(if (notificationReady) "Notifiche: pronte" else "Notifiche: autorizzazione richiesta")
            if (!notificationReady) {
                Text(
                    "Le notifiche servono per mostrare la sveglia quando arriva l'orario scelto.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier.testTag("alarm-notification-permission-action"),
                ) { Text("Abilita notifiche") }
            }
            Text(if (exactReady) "Allarmi esatti: pronti" else "Allarmi esatti: accesso richiesto")
            if (!exactReady) {
                Button(
                    onClick = onRequestExactAlarmAccess,
                    modifier = Modifier.testTag("alarm-exact-access-action"),
                ) { Text("Consenti allarmi esatti") }
            }
            if (!notificationReady || !exactReady) {
                Text(
                    "Le regole restano salvate, ma non vengono programmate finché manca un accesso richiesto.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(
    prayer: AlarmPrayer,
    rule: AlarmRule?,
    onEnabled: (Boolean) -> Unit,
    onToggleSound: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("alarm-prayer-${prayer.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(prayer.displayName(), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (rule?.soundProfile == AlarmSoundProfile.SILENT) "Silenzioso" else "Suono di sistema",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (rule != null) {
            TextButton(
                onClick = onToggleSound,
                modifier = Modifier.testTag("alarm-prayer-${prayer.name.lowercase()}-sound"),
            ) { Text("Suono") }
        }
        Switch(
            checked = rule?.enabled == true,
            onCheckedChange = onEnabled,
            modifier = Modifier.testTag("alarm-prayer-${prayer.name.lowercase()}-switch"),
        )
    }
}

@Composable
private fun CustomRuleRow(
    rule: AlarmRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onToggleSound: () -> Unit,
) {
    val definition = rule.definition as AlarmDefinition.Custom
    Card(modifier = Modifier.fillMaxWidth().testTag("alarm-custom-row-${rule.alarmId}")) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(definition.label)
                Text(
                    "${definition.localTime} • ${if (rule.soundProfile == AlarmSoundProfile.SILENT) "Silenzioso" else "Suono di sistema"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onToggleSound) { Text("Suono") }
            TextButton(onClick = onDelete) { Text("Elimina") }
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("alarm-custom-switch-${rule.alarmId}"),
            )
        }
    }
}

private fun AlarmPrayer.displayName(): String = when (this) {
    AlarmPrayer.FAJR -> "Fajr"
    AlarmPrayer.DHUHR -> "Dhuhr"
    AlarmPrayer.ASR -> "Asr"
    AlarmPrayer.MAGHRIB -> "Maghrib"
    AlarmPrayer.ISHA -> "Isha"
}
