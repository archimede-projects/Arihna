from pathlib import Path


def write(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content)

# Domain: add a backward-compatible ADHAN enum value.
p = Path("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/domain/AlarmModels.kt")
text = p.read_text()
old = "enum class AlarmSoundProfile {\n    SYSTEM_DEFAULT,\n    SILENT,\n}"
new = "enum class AlarmSoundProfile {\n    ADHAN,\n    SYSTEM_DEFAULT,\n    SILENT,\n}"
if old not in text:
    raise SystemExit("AlarmSoundProfile block not found")
p.write_text(text.replace(old, new, 1))

# ViewModel: prayer defaults to Adhan and sound selection is explicit rather than binary toggle.
p = Path("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/AlarmsViewModel.kt")
text = p.read_text()
text = text.replace(
    "soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,\n                        definition = AlarmDefinition.PrayerLinked(prayer = prayer, offsetMinutes = 0),",
    "soundProfile = AlarmSoundProfile.ADHAN,\n                        definition = AlarmDefinition.PrayerLinked(prayer = prayer, offsetMinutes = 0),",
    1,
)
old = '''    fun toggleSound(rule: AlarmRule) {
        mutate {
            repository.save(
                AlarmRuleDraft(
                    alarmId = rule.alarmId,
                    enabled = rule.enabled,
                    soundProfile = if (rule.soundProfile == AlarmSoundProfile.SYSTEM_DEFAULT) {
                        AlarmSoundProfile.SILENT
                    } else {
                        AlarmSoundProfile.SYSTEM_DEFAULT
                    },
                    definition = rule.definition,
                ),
            )
        }
    }
'''
new = '''    fun setSound(rule: AlarmRule, soundProfile: AlarmSoundProfile) {
        if (rule.definition is AlarmDefinition.Custom && soundProfile == AlarmSoundProfile.ADHAN) {
            message.value = "Adhan è disponibile per le sveglie di preghiera"
            return
        }
        if (rule.soundProfile == soundProfile) return
        mutate {
            repository.save(
                AlarmRuleDraft(
                    alarmId = rule.alarmId,
                    enabled = rule.enabled,
                    soundProfile = soundProfile,
                    definition = rule.definition,
                ),
            )
            message.value = "Suono aggiornato"
        }
    }
'''
if old not in text:
    raise SystemExit("toggleSound block not found")
p.write_text(text.replace(old, new, 1))

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmFullScreenAccess.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

class AlarmFullScreenAccess(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            notificationManager.canUseFullScreenIntent()

    fun createSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            Uri.parse("package:${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
''')

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmNotifications.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule

fun interface AlarmNotificationPermissionReader {
    fun isGranted(): Boolean
}

class AndroidAlarmNotificationPermissionReader(
    context: Context,
) : AlarmNotificationPermissionReader {
    private val appContext = context.applicationContext

    override fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

enum class AlarmNotificationDeliveryResult {
    DELIVERED,
    NEEDS_NOTIFICATION_PERMISSION,
}

interface AlarmNotificationDelivery {
    fun ensureChannels()
    fun deliver(rule: AlarmRule, occurrence: AlarmOccurrence): AlarmNotificationDeliveryResult
}

fun interface AlarmRingingStarter {
    fun start(rule: AlarmRule, occurrence: AlarmOccurrence)
}

class AndroidAlarmRingingStarter(context: Context) : AlarmRingingStarter {
    private val appContext = context.applicationContext

    override fun start(rule: AlarmRule, occurrence: AlarmOccurrence) {
        ContextCompat.startForegroundService(
            appContext,
            AlarmRingingService.ringIntent(appContext, rule, occurrence),
        )
    }
}

class AndroidAlarmNotificationDelivery(
    context: Context,
    private val permissionReader: AlarmNotificationPermissionReader,
    private val ringingStarter: AlarmRingingStarter = AndroidAlarmRingingStarter(context),
) : AlarmNotificationDelivery {
    private val appContext = context.applicationContext

    override fun ensureChannels() {
        AlarmRingingNotificationFactory.ensureChannels(appContext)
    }

    override fun deliver(
        rule: AlarmRule,
        occurrence: AlarmOccurrence,
    ): AlarmNotificationDeliveryResult {
        if (!permissionReader.isGranted()) {
            return AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION
        }
        ensureChannels()
        ringingStarter.start(rule, occurrence)
        return AlarmNotificationDeliveryResult.DELIVERED
    }

    companion object {
        const val CHANNEL_PRAYER = AlarmRingingNotificationFactory.CHANNEL_PRAYER
        const val CHANNEL_CUSTOM = AlarmRingingNotificationFactory.CHANNEL_CUSTOM

        fun notificationId(alarmId: String): Int =
            AlarmRingingNotificationFactory.notificationId(alarmId)
    }
}
''')

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmRingingService.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.archimedeprojects.arihna.R
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile

internal data class AlarmRingingPayload(
    val alarmId: String,
    val title: String,
    val soundProfile: AlarmSoundProfile,
    val isPrayer: Boolean,
    val occurrenceToken: String,
)

internal object AlarmRingingIntentContract {
    const val EXTRA_ALARM_ID = "arihna.ringing.alarm_id"
    const val EXTRA_TITLE = "arihna.ringing.title"
    const val EXTRA_SOUND_PROFILE = "arihna.ringing.sound_profile"
    const val EXTRA_IS_PRAYER = "arihna.ringing.is_prayer"
    const val EXTRA_OCCURRENCE_TOKEN = "arihna.ringing.occurrence_token"

    fun put(intent: Intent, payload: AlarmRingingPayload): Intent = intent.apply {
        putExtra(EXTRA_ALARM_ID, payload.alarmId)
        putExtra(EXTRA_TITLE, payload.title)
        putExtra(EXTRA_SOUND_PROFILE, payload.soundProfile.name)
        putExtra(EXTRA_IS_PRAYER, payload.isPrayer)
        putExtra(EXTRA_OCCURRENCE_TOKEN, payload.occurrenceToken)
    }

    fun decode(intent: Intent?): AlarmRingingPayload? {
        intent ?: return null
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)?.takeIf { it.isNotBlank() } ?: return null
        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: return null
        val sound = runCatching {
            AlarmSoundProfile.valueOf(intent.getStringExtra(EXTRA_SOUND_PROFILE).orEmpty())
        }.getOrNull() ?: return null
        val token = intent.getStringExtra(EXTRA_OCCURRENCE_TOKEN)?.takeIf { it.isNotBlank() } ?: return null
        return AlarmRingingPayload(
            alarmId = alarmId,
            title = title,
            soundProfile = sound,
            isPrayer = intent.getBooleanExtra(EXTRA_IS_PRAYER, false),
            occurrenceToken = token,
        )
    }
}

internal object AlarmRingingNotificationFactory {
    const val CHANNEL_PRAYER = "arihna_prayer_alarm_v2"
    const val CHANNEL_CUSTOM = "arihna_custom_alarm_v2"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prayer = NotificationChannel(
            CHANNEL_PRAYER,
            "Adhan e sveglie di preghiera",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Sveglie collegate agli orari di preghiera"
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val custom = NotificationChannel(
            CHANNEL_CUSTOM,
            "Sveglie personalizzate",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Sveglie personali create in Arihna"
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(prayer)
        manager.createNotificationChannel(custom)
    }

    fun build(
        context: Context,
        payload: AlarmRingingPayload,
        fullScreenAccess: AlarmFullScreenAccess = AlarmFullScreenAccess(context),
    ): Notification {
        ensureChannels(context)
        val channel = if (payload.isPrayer) CHANNEL_PRAYER else CHANNEL_CUSTOM
        val activityIntent = AlarmRingingIntentContract.put(
            Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                data = Uri.Builder()
                    .scheme("arihna")
                    .authority("ringing")
                    .appendPath(payload.alarmId)
                    .appendPath(payload.occurrenceToken)
                    .build()
            },
            payload,
        )
        val activityPendingIntent = PendingIntent.getActivity(
            context,
            requestCode(payload.alarmId, 1),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPendingIntent = PendingIntent.getService(
            context,
            requestCode(payload.alarmId, 2),
            AlarmRingingService.stopIntent(context, payload.alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val subtitle = when (payload.soundProfile) {
            AlarmSoundProfile.ADHAN -> "Adhan • tocca Stop per interrompere"
            AlarmSoundProfile.SYSTEM_DEFAULT -> "Sveglia in corso • tocca Stop per interrompere"
            AlarmSoundProfile.SILENT -> "Sveglia silenziosa • tocca Stop per chiudere"
        }
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_arihna)
            .setContentTitle(payload.title)
            .setContentText(subtitle)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(activityPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
        if (fullScreenAccess.isGranted()) {
            builder.setFullScreenIntent(activityPendingIntent, true)
        }
        return builder.build()
    }

    fun notificationId(alarmId: String): Int = alarmId.hashCode() and Int.MAX_VALUE

    private fun requestCode(alarmId: String, salt: Int): Int =
        (31 * alarmId.hashCode() + salt) and Int.MAX_VALUE
}

class AlarmRingingService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var activeNotificationId: Int? = null
    private var activeAlarmId: String? = null
    private val safetyStop = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val requestedAlarmId = intent.getStringExtra(AlarmRingingIntentContract.EXTRA_ALARM_ID)
            if (requestedAlarmId == null || requestedAlarmId == activeAlarmId) {
                stopRinging()
            }
            return START_NOT_STICKY
        }

        val payload = AlarmRingingIntentContract.decode(intent) ?: run {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        stopAudioOnly()
        activeAlarmId = payload.alarmId
        activeNotificationId = AlarmRingingNotificationFactory.notificationId(payload.alarmId)
        val notification = AlarmRingingNotificationFactory.build(this, payload)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                activeNotificationId ?: 1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(activeNotificationId ?: 1, notification)
        }
        startAudio(payload.soundProfile)
        mainHandler.removeCallbacks(safetyStop)
        mainHandler.postDelayed(safetyStop, MAX_RINGING_MILLIS)
        return START_NOT_STICKY
    }

    private fun startAudio(profile: AlarmSoundProfile) {
        if (profile == AlarmSoundProfile.SILENT) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(attributes)
            when (profile) {
                AlarmSoundProfile.SYSTEM_DEFAULT -> {
                    val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(
                        this,
                        RingtoneManager.TYPE_ALARM,
                    ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    player.setDataSource(this, alarmUri)
                    player.isLooping = true
                }
                AlarmSoundProfile.ADHAN -> {
                    resources.openRawResourceFd(R.raw.adhan_cc0).use { descriptor ->
                        player.setDataSource(
                            descriptor.fileDescriptor,
                            descriptor.startOffset,
                            descriptor.length,
                        )
                    }
                    player.isLooping = false
                }
                AlarmSoundProfile.SILENT -> Unit
            }
            player.prepare()
            player.start()
            if (profile == AlarmSoundProfile.ADHAN) {
                player.setOnCompletionListener { completed ->
                    if (mediaPlayer === completed) {
                        mediaPlayer = null
                    }
                    completed.release()
                }
            }
            mediaPlayer = player
        } catch (throwable: Throwable) {
            player.release()
            mediaPlayer = null
        }
    }

    private fun stopAudioOnly() {
        mediaPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            player.release()
        }
        mediaPlayer = null
    }

    private fun stopRinging() {
        mainHandler.removeCallbacks(safetyStop)
        stopAudioOnly()
        activeNotificationId?.let { id ->
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
        }
        activeNotificationId = null
        activeAlarmId = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(safetyStop)
        stopAudioOnly()
        activeNotificationId?.let { id ->
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
        }
        activeNotificationId = null
        activeAlarmId = null
        super.onDestroy()
    }

    companion object {
        private const val ACTION_RING = "com.archimedeprojects.arihna.action.RING_ALARM"
        private const val ACTION_STOP = "com.archimedeprojects.arihna.action.STOP_ALARM"
        private const val MAX_RINGING_MILLIS = 10 * 60 * 1000L

        fun ringIntent(context: Context, rule: AlarmRule, occurrence: AlarmOccurrence): Intent {
            val payload = AlarmRingingPayload(
                alarmId = rule.alarmId,
                title = titleFor(rule),
                soundProfile = rule.soundProfile,
                isPrayer = rule.definition is AlarmDefinition.PrayerLinked,
                occurrenceToken = occurrence.occurrenceToken,
            )
            return AlarmRingingIntentContract.put(
                Intent(context, AlarmRingingService::class.java).setAction(ACTION_RING),
                payload,
            )
        }

        fun stopIntent(context: Context, alarmId: String): Intent =
            Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_STOP
                putExtra(AlarmRingingIntentContract.EXTRA_ALARM_ID, alarmId)
                data = Uri.Builder()
                    .scheme("arihna")
                    .authority("ringing-stop")
                    .appendPath(alarmId)
                    .build()
            }

        private fun titleFor(rule: AlarmRule): String = when (val definition = rule.definition) {
            is AlarmDefinition.PrayerLinked -> "${definition.prayer.displayName()} • Adhan"
            is AlarmDefinition.Custom -> definition.label
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
''')

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmRingingActivity.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

class AlarmRingingActivity : ComponentActivity() {
    private var payload: AlarmRingingPayload? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAlarmWindow()
        payload = AlarmRingingIntentContract.decode(intent)
        val current = payload ?: run {
            finish()
            return
        }
        setContent {
            ArihnaTheme(darkTheme = true) {
                AlarmRingingScreen(current, onStop = ::stopAndFinish)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        payload = AlarmRingingIntentContract.decode(intent)
        payload?.let { current ->
            setContent {
                ArihnaTheme(darkTheme = true) {
                    AlarmRingingScreen(current, onStop = ::stopAndFinish)
                }
            }
        }
    }

    private fun configureAlarmWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun stopAndFinish() {
        payload?.alarmId?.let { alarmId ->
            startService(AlarmRingingService.stopIntent(this, alarmId))
        } ?: stopService(android.content.Intent(this, AlarmRingingService::class.java))
        finishAndRemoveTask()
    }

    @Composable
    private fun AlarmRingingScreen(payload: AlarmRingingPayload, onStop: () -> Unit) {
        BackHandler(onBack = onStop)
        var now by remember { mutableStateOf(LocalTime.now()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = LocalTime.now()
                delay(1_000L)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF062E22), Color(0xFF0A4734), Color(0xFF041F18)),
                    ),
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val glow = ArihnaGold.copy(alpha = 0.10f)
                drawCircle(glow, size.minDimension * 0.42f, Offset(size.width * 0.82f, size.height * 0.15f))
                drawCircle(glow, size.minDimension * 0.25f, Offset(size.width * 0.15f, size.height * 0.82f))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = ArihnaGold.copy(alpha = 0.16f),
                        modifier = Modifier.padding(top = 24.dp),
                    ) {
                        Text(
                            text = if (payload.soundProfile == AlarmSoundProfile.ADHAN) "☾" else "♪",
                            color = ArihnaGold,
                            fontSize = 54.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                    Spacer(Modifier.height(34.dp))
                    Text(
                        now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        payload.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ArihnaGold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (payload.soundProfile) {
                            AlarmSoundProfile.ADHAN -> "È il momento della preghiera"
                            AlarmSoundProfile.SYSTEM_DEFAULT -> "Sveglia Arihna"
                            AlarmSoundProfile.SILENT -> "Sveglia silenziosa"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Scorri dai bordi per mostrare temporaneamente i controlli di sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArihnaGold,
                            contentColor = ArihnaGreen,
                        ),
                    ) {
                        Text("Stop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
''')

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/AlarmsPlaceholderScreen.kt", r'''package com.archimedeprojects.arihna.feature.alarms

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
''')

# AppContainer wiring.
p = Path("app/src/main/java/com/archimedeprojects/arihna/app/AppContainer.kt")
text = p.read_text()
text = text.replace(
    "import com.archimedeprojects.arihna.feature.alarms.platform.AlarmNotificationDelivery\n",
    "import com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess\nimport com.archimedeprojects.arihna.feature.alarms.platform.AlarmNotificationDelivery\n",
    1,
)
text = text.replace(
    "    val exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory by lazy {\n        ExactAlarmAccessIntentFactory(appContext)\n    }\n",
    "    val exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory by lazy {\n        ExactAlarmAccessIntentFactory(appContext)\n    }\n    val alarmFullScreenAccess: AlarmFullScreenAccess by lazy { AlarmFullScreenAccess(appContext) }\n",
    1,
)
p.write_text(text)

# ArihnaApp -> NavHost wiring.
p = Path("app/src/main/java/com/archimedeprojects/arihna/app/ArihnaApp.kt")
text = p.read_text()
text = text.replace(
    "            exactAlarmAccessIntentFactory = appContainer.exactAlarmAccessIntentFactory,\n",
    "            exactAlarmAccessIntentFactory = appContainer.exactAlarmAccessIntentFactory,\n            alarmFullScreenAccess = appContainer.alarmFullScreenAccess,\n",
    1,
)
p.write_text(text)

# NavHost -> AlarmsRoute wiring.
p = Path("app/src/main/java/com/archimedeprojects/arihna/app/ArihnaNavHost.kt")
text = p.read_text()
text = text.replace(
    "import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory\n",
    "import com.archimedeprojects.arihna.feature.alarms.platform.AlarmFullScreenAccess\nimport com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory\n",
    1,
)
text = text.replace(
    "    exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory,\n",
    "    exactAlarmAccessIntentFactory: ExactAlarmAccessIntentFactory,\n    alarmFullScreenAccess: AlarmFullScreenAccess,\n",
    1,
)
text = text.replace(
    "                    exactAlarmAccessIntentFactory = exactAlarmAccessIntentFactory,\n",
    "                    exactAlarmAccessIntentFactory = exactAlarmAccessIntentFactory,\n                    fullScreenAccess = alarmFullScreenAccess,\n",
    1,
)
p.write_text(text)

# Manifest additions for full-screen alarm and target-37 compliant mediaPlayback FGS.
p = Path("app/src/main/AndroidManifest.xml")
text = p.read_text()
text = text.replace(
    "    <uses-permission android:name=\"android.permission.RECEIVE_BOOT_COMPLETED\" />\n",
    "    <uses-permission android:name=\"android.permission.RECEIVE_BOOT_COMPLETED\" />\n    <uses-permission android:name=\"android.permission.USE_FULL_SCREEN_INTENT\" />\n    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\" />\n    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK\" />\n",
    1,
)
needle = '''        <activity
            android:name=".MainActivity"
            android:exported="true">
'''
if needle not in text:
    raise SystemExit("MainActivity manifest marker not found")
insert = '''        <activity
            android:name=".feature.alarms.platform.AlarmRingingActivity"
            android:excludeFromRecents="true"
            android:exported="false"
            android:launchMode="singleTop"
            android:theme="@style/Theme.Arihna" />

        <service
            android:name=".feature.alarms.platform.AlarmRingingService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback" />

'''
text = text.replace(needle, insert + needle, 1)
p.write_text(text)

# JVM persistence regression for new ADHAN value and old payload compatibility.
p = Path("app/src/test/java/com/archimedeprojects/arihna/feature/alarms/data/preferences/AlarmRulePreferencesCodecTest.kt")
text = p.read_text()
marker = "    @Test\n    fun collectionEncodingIsDeterministicRegardlessOfInputOrder()"
addition = '''    @Test
    fun adhanProfileRoundTripsWithoutChangingVersionAndOldProfilesStillDecode() {
        val adhan = AlarmRule(
            alarmId = "prayer-adhan",
            revision = 4,
            enabled = true,
            soundProfile = AlarmSoundProfile.ADHAN,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.MAGHRIB),
        )
        val encoded = AlarmRulePreferencesCodec.encode(listOf(adhan))
        assertTrue(encoded.startsWith("ARIHNA_ALARMS_V1\\n"))
        assertEquals(listOf(adhan), AlarmRulePreferencesCodec.decode(encoded))

        val oldSystem = "ARIHNA_ALARMS_V1\\nP|cHJheWVyLWZhanI|1|1|SYSTEM_DEFAULT|FAJR|0"
        val oldSilent = "ARIHNA_ALARMS_V1\\nP|cHJheWVyLWlzaGE|2|0|SILENT|ISHA|0"
        assertEquals(AlarmSoundProfile.SYSTEM_DEFAULT, AlarmRulePreferencesCodec.decode(oldSystem).single().soundProfile)
        assertEquals(AlarmSoundProfile.SILENT, AlarmRulePreferencesCodec.decode(oldSilent).single().soundProfile)
    }

'''
if marker not in text:
    raise SystemExit("codec test insertion marker not found")
p.write_text(text.replace(marker, addition + marker, 1))

# Compose instrumentation updated for full-screen capability and explicit sound picker.
write("app/src/androidTest/java/com/archimedeprojects/arihna/feature/alarms/AlarmsScreenAndroidTest.kt", r'''package com.archimedeprojects.arihna.feature.alarms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlarmsScreenAndroidTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun missingCapabilitiesAreExplicitAndNeverFabricatedAsReady() {
        setScreen(AlarmsUiState(exactAlarmReady = false, notificationReady = false), fullScreenReady = false)

        composeRule.onNodeWithTag("alarm-capabilities").assertIsDisplayed()
        composeRule.onNodeWithText("Notifiche").assertIsDisplayed()
        composeRule.onNodeWithText("Allarmi esatti").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-notification-permission-action").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-exact-access-action").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-fullscreen-access-action").performScrollTo().assertIsDisplayed()
        assertTextAbsent("Schermo intero: pronto")
    }

    @Test
    fun allFivePrayerControlsArePresent() {
        setScreen(AlarmsUiState(exactAlarmReady = true, notificationReady = true))

        AlarmPrayer.entries.forEach { prayer ->
            composeRule.onNodeWithTag("alarm-prayer-${prayer.name.lowercase()}").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("alarm-prayer-${prayer.name.lowercase()}-switch").assertIsDisplayed()
        }
    }

    @Test
    fun prayerSoundButtonOpensExplicitPickerAndSelectsAdhan() {
        val rule = AlarmRule(
            alarmId = "prayer-dhuhr",
            revision = 2,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.DHUHR),
        )
        var selected: AlarmSoundProfile? = null
        setScreen(
            state = AlarmsUiState(rules = listOf(rule), exactAlarmReady = true, notificationReady = true),
            onSoundSelected = { _, profile -> selected = profile },
        )

        composeRule.onNodeWithTag("alarm-prayer-dhuhr-sound").performScrollTo().performClick()
        composeRule.onNodeWithTag("alarm-sound-picker").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-sound-option-adhan").performClick()
        composeRule.runOnIdle { assertEquals(AlarmSoundProfile.ADHAN, selected) }
    }

    @Test
    fun customCreatorSendsLabelAndLocalTimeTextWithoutInventingSuccess() {
        var captured: Pair<String, String>? = null
        setScreen(
            state = AlarmsUiState(exactAlarmReady = true, notificationReady = true),
            onCreateCustom = { label, time -> captured = label to time },
        )

        composeRule.onNodeWithTag("alarm-custom-label").performScrollTo().assertIsDisplayed().performTextInput("Farmaco")
        composeRule.onNodeWithTag("alarm-custom-time").performScrollTo().assertIsDisplayed().performTextInput("07:30")
        composeRule.onNodeWithTag("alarm-custom-add").performScrollTo().assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals("Farmaco" to "07:30", captured) }
        assertTextAbsent("Sveglia salvata")
    }

    @Test
    fun persistedCustomRuleIsRenderedWithItsState() {
        val rule = AlarmRule(
            alarmId = "custom-visible",
            revision = 3,
            enabled = true,
            soundProfile = AlarmSoundProfile.SILENT,
            definition = AlarmDefinition.Custom(
                label = "Farmaco",
                localTime = LocalTime.of(7, 30),
            ),
        )
        setScreen(
            AlarmsUiState(
                rules = listOf(rule),
                exactAlarmReady = true,
                notificationReady = true,
            ),
        )

        composeRule.onNodeWithTag("alarm-custom-row-custom-visible").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Farmaco").assertIsDisplayed()
        composeRule.onNodeWithText("07:30 • Silenzioso").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-custom-switch-custom-visible").assertIsDisplayed()
    }

    private fun setScreen(
        state: AlarmsUiState,
        fullScreenReady: Boolean = true,
        onCreateCustom: (String, String) -> Unit = { _, _ -> },
        onSoundSelected: (AlarmRule, AlarmSoundProfile) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            ArihnaTheme {
                AlarmsScreen(
                    contentPadding = PaddingValues(0.dp),
                    state = state,
                    fullScreenReady = fullScreenReady,
                    onRequestNotificationPermission = {},
                    onRequestExactAlarmAccess = {},
                    onRequestFullScreenAccess = {},
                    onPrayerEnabled = { _, _ -> },
                    onCreateCustom = onCreateCustom,
                    onToggleRule = {},
                    onDeleteRule = {},
                    onSoundSelected = onSoundSelected,
                )
            }
        }
    }

    private fun assertTextAbsent(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }
}
''')

# API28 instrumentation: channels, full-screen PendingIntent wiring, and delivery handoff without actually ringing CI audio.
write("app/src/androidTest/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmNotificationAndroidTest.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmNotificationAndroidTest {
    @Test
    fun api28CreatesSemanticSilentChannelsAndBuildsFullScreenAlarmNotification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AlarmRingingNotificationFactory.ensureChannels(context)

        val prayerChannel = manager.getNotificationChannel(AlarmRingingNotificationFactory.CHANNEL_PRAYER)
        val customChannel = manager.getNotificationChannel(AlarmRingingNotificationFactory.CHANNEL_CUSTOM)
        assertNotNull(prayerChannel)
        assertNotNull(customChannel)
        assertNull(prayerChannel.sound)
        assertNull(customChannel.sound)

        val payload = AlarmRingingPayload(
            alarmId = "instrumented-notification",
            title = "Fajr • Adhan",
            soundProfile = AlarmSoundProfile.ADHAN,
            isPrayer = true,
            occurrenceToken = "instrumented",
        )
        val notification = AlarmRingingNotificationFactory.build(context, payload)
        assertNotNull(notification.fullScreenIntent)
        assertNotNull(notification.contentIntent)
        assertEquals(1, notification.actions.size)
    }

    @Test
    fun validatedDeliveryHandsOffToRingingStarterExactlyOnce() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var starts = 0
        val delivery = AndroidAlarmNotificationDelivery(
            context = context,
            permissionReader = AlarmNotificationPermissionReader { true },
            ringingStarter = AlarmRingingStarter { _, _ -> starts += 1 },
        )
        val rule = AlarmRule(
            alarmId = "instrumented-handoff",
            revision = 1,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom("Test Arihna", LocalTime.NOON),
        )
        val occurrence = AlarmOccurrence(
            alarmId = rule.alarmId,
            ruleRevision = rule.revision,
            triggerAt = Instant.parse("2026-09-05T10:00:00Z"),
            occurrenceToken = "token",
        )
        assertEquals(AlarmNotificationDeliveryResult.DELIVERED, delivery.deliver(rule, occurrence))
        assertEquals(1, starts)
    }
}
''')
