package com.archimedeprojects.arihna.feature.prayers

import android.app.Activity
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel
import com.archimedeprojects.arihna.feature.alarms.domain.AdhanVariant
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

private val OrariIvory = Color(0xFFFFF8EA)
private val OrariCream = Color(0xFFFFFCF4)
private val OrariSage = Color(0xFFE6EAD7)
private val OrariSageStrong = Color(0xFFD7DFC2)
private val OrariForest = Color(0xFF173C30)
private val OrariGreen = Color(0xFF0F5132)
private val OrariGold = Color(0xFFC79B3B)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(OrariIvory, OrariCream, OrariSage.copy(alpha = 0.55f)),
                ),
            )
            .testTag("prayer-times-screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 18.dp,
                    top = contentPadding.calculateTopPadding() + 14.dp,
                    end = 18.dp,
                    bottom = contentPadding.calculateBottomPadding() + 10.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Orari",
                    color = OrariForest,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "Orari di preghiera e promemoria",
                    color = OrariForest.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            when (scheduleState) {
                PrayerScheduleUiState.Loading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = OrariGreen) }

                is PrayerScheduleUiState.NoLocation -> StatusCard(scheduleState.message)
                is PrayerScheduleUiState.CalculationUnavailable -> StatusCard(scheduleState.message)

                is PrayerScheduleUiState.Ready -> {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = OrariSage,
                    ) {
                        Text(
                            "Oggi • ${scheduleState.location.displayName}",
                            color = OrariForest,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    val zone = scheduleState.today.zoneId
                    val rows = listOf(
                        Triple(AlarmPrayer.FAJR, "Fajr", scheduleState.today.times.fajr),
                        Triple(AlarmPrayer.DHUHR, "Dhuhr", scheduleState.today.times.dhuhr),
                        Triple(AlarmPrayer.ASR, "Asr", scheduleState.today.times.asr),
                        Triple(AlarmPrayer.MAGHRIB, "Maghrib", scheduleState.today.times.maghrib),
                        Triple(AlarmPrayer.ISHA, "Isha", scheduleState.today.times.isha),
                    )

                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rows.forEach { (prayer, label, instant) ->
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
                }
            }
        }
    }
}

@Composable
private fun StatusCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OrariCream),
    ) {
        Text(message, modifier = Modifier.padding(20.dp), color = OrariForest)
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
    val enabled = rule?.enabled == true
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("prayer-reminder-${prayer.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) OrariSageStrong else OrariCream,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = OrariForest, fontWeight = FontWeight.ExtraBold)
                Text(time, color = OrariGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            if (rule != null) {
                SoundIconButton(rule = rule, onClick = { onOpenSound(rule) })
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabled,
                modifier = Modifier.testTag("prayer-reminder-${prayer.name.lowercase()}-switch"),
            )
        }
    }
}

@Composable
private fun SoundIconButton(
    rule: AlarmRule,
    onClick: () -> Unit,
) {
    val (icon, description) = when (rule.soundProfile) {
        AlarmSoundProfile.ADHAN -> Icons.Rounded.Mosque to "Adhan: ${AdhanVariant.fromStorage(rule.ringtoneUri).displayName}"
        AlarmSoundProfile.SYSTEM_DEFAULT -> Icons.Rounded.Notifications to "Suoneria: ${rule.ringtoneTitle ?: "predefinita"}"
        AlarmSoundProfile.SILENT -> Icons.Rounded.VolumeOff to "Silenzioso"
    }
    Surface(
        shape = CircleShape,
        color = if (rule.enabled) OrariGreen else OrariSageStrong,
        contentColor = if (rule.enabled) OrariCream else OrariForest,
        modifier = Modifier.size(44.dp).clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = description)
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
    var adhanVariant by remember(rule.alarmId, rule.revision) {
        mutableStateOf(AdhanVariant.fromStorage(rule.ringtoneUri))
    }
    var ringtoneUri by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.ringtoneUri) }
    var ringtoneTitle by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.ringtoneTitle) }
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AlarmRingtonePicker.pickedUri(result.data)?.let { uri ->
                ringtoneUri = uri.toString()
                ringtoneTitle = AlarmRingtonePicker.title(context, uri) ?: "Suoneria telefono"
                profile = AlarmSoundProfile.SYSTEM_DEFAULT
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suono promemoria", color = OrariForest) },
        containerColor = OrariCream,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Adhan", fontWeight = FontWeight.ExtraBold, color = OrariForest)
                AdhanVariant.entries.forEach { variant ->
                    SoundListRow(
                        title = variant.displayName,
                        icon = Icons.Rounded.Mosque,
                        selected = profile == AlarmSoundProfile.ADHAN && adhanVariant == variant,
                    ) {
                        profile = AlarmSoundProfile.ADHAN
                        adhanVariant = variant
                        ringtoneUri = variant.storageValue
                        ringtoneTitle = variant.displayName
                    }
                }
                SoundListRow(
                    title = "Suoneria telefono",
                    icon = Icons.Rounded.Notifications,
                    selected = profile == AlarmSoundProfile.SYSTEM_DEFAULT,
                ) {
                    profile = AlarmSoundProfile.SYSTEM_DEFAULT
                }
                if (profile == AlarmSoundProfile.SYSTEM_DEFAULT) {
                    TextButton(
                        onClick = { ringtoneLauncher.launch(AlarmRingtonePicker.createIntent(ringtoneUri)) },
                    ) { Text(ringtoneTitle ?: "Scegli suoneria") }
                }
                SoundListRow(
                    title = "Silenzioso",
                    icon = Icons.Rounded.VolumeOff,
                    selected = profile == AlarmSoundProfile.SILENT,
                ) {
                    profile = AlarmSoundProfile.SILENT
                    ringtoneUri = null
                    ringtoneTitle = null
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val storedUri = if (profile == AlarmSoundProfile.ADHAN) adhanVariant.storageValue else ringtoneUri
                    val storedTitle = if (profile == AlarmSoundProfile.ADHAN) adhanVariant.displayName else ringtoneTitle
                    onSave(profile, storedUri, storedTitle)
                },
            ) { Text("Conferma") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
    )
}

@Composable
private fun SoundListRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) OrariSageStrong else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = OrariForest)
            Text(title, modifier = Modifier.weight(1f), color = OrariForest, fontWeight = FontWeight.SemiBold)
            if (selected) {
                Surface(shape = CircleShape, color = OrariGold, modifier = Modifier.size(10.dp)) {}
            }
        }
    }
}

private fun formatTime(instant: Instant, zoneId: ZoneId): String =
    TIME_FORMATTER.withZone(zoneId).format(instant)

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
