package com.archimedeprojects.arihna.feature.prayers

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.archimedeprojects.arihna.R
import com.archimedeprojects.arihna.core.i18n.appText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSageStrong
import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel
import com.archimedeprojects.arihna.feature.alarms.domain.AdhanVariant
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmRingtonePicker
import com.archimedeprojects.arihna.feature.alarms.platform.adhanRawResource
import com.archimedeprojects.arihna.feature.alarms.platform.adhanRepeatCount
import com.archimedeprojects.arihna.feature.alarms.platform.playbackGain
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleUiState
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val OrariIvory = ArihnaDawnTop
private val OrariCream = ArihnaCream
private val OrariSage = ArihnaSage
private val OrariSageStrong = ArihnaSageStrong
private val OrariForest = ArihnaForest
private val OrariGreen = ArihnaGreen
private val OrariGold = ArihnaDawnGold

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
        val prayer = (rule.definition as? AlarmDefinition.PrayerLinked)?.prayer
        val initialVolume = prayer?.let { alarmsState.prayerVolumes[it] } ?: 100
        PrayerSoundDialog(
            rule = rule,
            initialVolumePercent = initialVolume,
            onDismiss = { soundRule = null },
            onSave = { profile, uri, title, volumePercent ->
                alarmsViewModel.setPrayerSound(rule, profile, uri, title, volumePercent)
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
                    appText("Orari", "مواقيت الصلاة"),
                    color = OrariForest,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    appText("Orari di preghiera e promemoria", "مواقيت الصلاة والتذكيرات"),
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
                            "${appText("Oggi", "اليوم")} • ${scheduleState.location.displayName}",
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
                                modifier = Modifier.weight(1f),
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
    modifier: Modifier,
    prayer: AlarmPrayer,
    label: String,
    time: String,
    rule: AlarmRule?,
    onEnabled: (Boolean) -> Unit,
    onOpenSound: (AlarmRule) -> Unit,
) {
    val enabled = rule?.enabled == true
    Card(
        modifier = modifier
            .fillMaxWidth()
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

private class AdhanPreviewPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var currentGain: Float = 1f

    fun play(variant: AdhanVariant, volumePercent: Int) {
        stop()
        currentGain = playbackGain(volumePercent)
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            context.resources.openRawResourceFd(adhanRawResource(variant)).use { descriptor ->
                next.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            next.isLooping = false
            next.setVolume(currentGain, currentGain)
            next.prepare()
            var remainingPlays = adhanRepeatCount(variant)
            next.setOnCompletionListener { completed ->
                remainingPlays -= 1
                if (remainingPlays > 0 && player === completed) {
                    runCatching {
                        completed.seekTo(0)
                        completed.start()
                    }.onFailure {
                        if (player === completed) player = null
                        completed.release()
                    }
                } else {
                    if (player === completed) player = null
                    completed.release()
                }
            }
            player = next
            next.start()
        } catch (_: Throwable) {
            next.release()
            player = null
        }
    }

    fun setVolume(percent: Int) {
        currentGain = playbackGain(percent)
        player?.setVolume(currentGain, currentGain)
    }

    fun stop() {
        player?.let { active ->
            runCatching { if (active.isPlaying) active.stop() }
            active.release()
        }
        player = null
    }
}

@Composable
@Composable
private fun adhanVariantLabel(variant: AdhanVariant): String = when (variant) {
    AdhanVariant.TAKBIR_X2 -> appText(
        "Takbīr breve · Allahu Akbar ×2",
        "تكبير قصير · الله أكبر ×٢",
    )
    else -> variant.displayName
}

private fun PrayerSoundDialog(
    rule: AlarmRule,
    initialVolumePercent: Int,
    onDismiss: () -> Unit,
    onSave: (AlarmSoundProfile, String?, String?, Int) -> Unit,
) {
    val context = LocalContext.current
    var profile by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.soundProfile) }
    var adhanVariant by remember(rule.alarmId, rule.revision) {
        mutableStateOf(AdhanVariant.fromStorage(rule.ringtoneUri))
    }
    var ringtoneUri by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.ringtoneUri) }
    var ringtoneTitle by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.ringtoneTitle) }
    var adhanListOpen by remember(rule.alarmId, rule.revision) { mutableStateOf(false) }
    var previewing by remember(rule.alarmId, rule.revision) { mutableStateOf<AdhanVariant?>(null) }
    var volumePercent by remember(rule.alarmId, rule.revision) {
        mutableStateOf(initialVolumePercent.coerceIn(0, 100))
    }
    val previewPlayer = remember(context, rule.alarmId) { AdhanPreviewPlayer(context.applicationContext) }

    DisposableEffect(previewPlayer) {
        onDispose { previewPlayer.stop() }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AlarmRingtonePicker.pickedUri(result.data)?.let { uri ->
                previewPlayer.stop()
                previewing = null
                ringtoneUri = uri.toString()
                ringtoneTitle = AlarmRingtonePicker.title(context, uri) ?: "Suoneria telefono"
                profile = AlarmSoundProfile.SYSTEM_DEFAULT
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            previewPlayer.stop()
            previewing = null
            onDismiss()
        },
        title = {
            Text(
                if (adhanListOpen) appText("Scegli Adhan", "اختر الأذان") else appText("Suono promemoria", "صوت التذكير"),
                color = OrariForest,
            )
        },
        containerColor = OrariCream,
        text = {
            if (adhanListOpen) {
                Column(
                    modifier = Modifier.testTag("prayer-sound-adhan-list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        appText("Tocca un Adhan per ascoltarlo prima di scegliere.", "اضغط على الأذان للاستماع إليه قبل الاختيار."),
                        color = OrariForest.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    AdhanVariant.entries.forEach { variant ->
                        SoundListRow(
                            title = if (previewing == variant) {
                                "${adhanVariantLabel(variant)} · ${appText("in ascolto", "يعمل الآن")}"
                            } else {
                                adhanVariantLabel(variant)
                            },
                            icon = Icons.Rounded.Mosque,
                            selected = profile == AlarmSoundProfile.ADHAN && adhanVariant == variant,
                        ) {
                            profile = AlarmSoundProfile.ADHAN
                            adhanVariant = variant
                            ringtoneUri = variant.storageValue
                            ringtoneTitle = variant.displayName
                            previewing = variant
                            previewPlayer.play(variant, volumePercent)
                        }
                    }
                    TextButton(
                        onClick = {
                            previewPlayer.stop()
                            previewing = null
                            adhanListOpen = false
                        },
                    ) { Text(appText("Indietro", "رجوع")) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(appText("Adhan", "الأذان"), fontWeight = FontWeight.ExtraBold, color = OrariForest)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (profile == AlarmSoundProfile.ADHAN) OrariSageStrong else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.Mosque, contentDescription = null, tint = OrariForest)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (profile == AlarmSoundProfile.ADHAN) adhanVariantLabel(adhanVariant) else appText("Nessun Adhan selezionato", "لم يتم اختيار أذان"),
                                    color = OrariForest,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    appText("Puoi ascoltarli prima di confermare", "يمكنك الاستماع قبل التأكيد"),
                                    color = OrariForest.copy(alpha = 0.66f),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            previewPlayer.stop()
                            previewing = null
                            adhanListOpen = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("prayer-sound-choose-adhan"),
                    ) {
                        Icon(Icons.Rounded.Mosque, contentDescription = null)
                        Text(appText("  Scegli Adhan", "  اختر الأذان"), fontWeight = FontWeight.Bold)
                    }

                    SoundListRow(
                        title = appText("Suoneria telefono", "نغمة الهاتف"),
                        icon = Icons.Rounded.Notifications,
                        selected = profile == AlarmSoundProfile.SYSTEM_DEFAULT,
                    ) {
                        previewPlayer.stop()
                        previewing = null
                        profile = AlarmSoundProfile.SYSTEM_DEFAULT
                    }
                    if (profile == AlarmSoundProfile.SYSTEM_DEFAULT) {
                        TextButton(
                            onClick = {
                                previewPlayer.stop()
                                ringtoneLauncher.launch(AlarmRingtonePicker.createIntent(ringtoneUri))
                            },
                        ) { Text(ringtoneTitle ?: appText("Scegli suoneria", "اختر نغمة")) }
                    }
                    SoundListRow(
                        title = appText("Silenzioso", "صامت"),
                        icon = Icons.Rounded.VolumeOff,
                        selected = profile == AlarmSoundProfile.SILENT,
                    ) {
                        previewPlayer.stop()
                        previewing = null
                        profile = AlarmSoundProfile.SILENT
                        ringtoneUri = null
                        ringtoneTitle = null
                    }

                    val prayer = (rule.definition as? AlarmDefinition.PrayerLinked)?.prayer
                    if (prayer != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                appText("Volume di questa preghiera", "مستوى صوت هذه الصلاة"),
                                color = OrariForest,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "$volumePercent%",
                                color = OrariGreen,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.testTag(
                                    "prayer-volume-value-${prayer.name.lowercase()}",
                                ),
                            )
                        }
                        Slider(
                            value = volumePercent.toFloat(),
                            onValueChange = { requested ->
                                volumePercent = requested.roundToInt().coerceIn(0, 100)
                                previewPlayer.setVolume(volumePercent)
                            },
                            valueRange = 0f..100f,
                            steps = 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prayer-volume-slider-${prayer.name.lowercase()}"),
                            colors = SliderDefaults.colors(
                                thumbColor = OrariForest,
                                activeTrackColor = OrariGreen,
                                inactiveTrackColor = OrariSage,
                            ),
                        )
                        Text(
                            appText(
                                "Percentuale locale: non cambia il volume globale delle sveglie del telefono.",
                                "نسبة خاصة بهذه الصلاة ولا تغيّر مستوى صوت منبّه الهاتف العام.",
                            ),
                            color = OrariForest.copy(alpha = 0.66f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!adhanListOpen) {
                Button(
                    onClick = {
                        previewPlayer.stop()
                        previewing = null
                        val storedUri = if (profile == AlarmSoundProfile.ADHAN) adhanVariant.storageValue else ringtoneUri
                        val storedTitle = if (profile == AlarmSoundProfile.ADHAN) adhanVariant.displayName else ringtoneTitle
                        onSave(profile, storedUri, storedTitle, volumePercent)
                    },
                ) { Text(appText("Conferma", "تأكيد")) }
            }
        },
        dismissButton = {
            if (!adhanListOpen) {
                TextButton(
                    onClick = {
                        previewPlayer.stop()
                        previewing = null
                        onDismiss()
                    },
                ) { Text(appText("Chiudi", "إغلاق")) }
            }
        },
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
