package com.archimedeprojects.arihna.feature.alarms.platform

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
    val ruleRevision: Long,
    val title: String,
    val soundProfile: AlarmSoundProfile,
    val isPrayer: Boolean,
    val occurrenceToken: String,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
)

internal fun createAlarmRingingPayload(
    rule: AlarmRule,
    occurrence: AlarmOccurrence,
): AlarmRingingPayload = AlarmRingingPayload(
    alarmId = rule.alarmId,
    ruleRevision = occurrence.ruleRevision,
    title = when (val definition = rule.definition) {
        is AlarmDefinition.PrayerLinked -> definition.prayer.displayName()
        is AlarmDefinition.Custom -> definition.label
    },
    soundProfile = rule.soundProfile,
    isPrayer = rule.definition is AlarmDefinition.PrayerLinked,
    occurrenceToken = occurrence.occurrenceToken,
    ringtoneUri = rule.ringtoneUri,
    ringtoneTitle = rule.ringtoneTitle,
)

internal object AlarmRingingIntentContract {
    const val EXTRA_ALARM_ID = "arihna.ringing.alarm_id"
    const val EXTRA_RULE_REVISION = "arihna.ringing.rule_revision"
    const val EXTRA_TITLE = "arihna.ringing.title"
    const val EXTRA_SOUND_PROFILE = "arihna.ringing.sound_profile"
    const val EXTRA_IS_PRAYER = "arihna.ringing.is_prayer"
    const val EXTRA_OCCURRENCE_TOKEN = "arihna.ringing.occurrence_token"
    const val EXTRA_RINGTONE_URI = "arihna.ringing.ringtone_uri"
    const val EXTRA_RINGTONE_TITLE = "arihna.ringing.ringtone_title"

    fun put(intent: Intent, payload: AlarmRingingPayload): Intent = intent.apply {
        putExtra(EXTRA_ALARM_ID, payload.alarmId)
        putExtra(EXTRA_RULE_REVISION, payload.ruleRevision)
        putExtra(EXTRA_TITLE, payload.title)
        putExtra(EXTRA_SOUND_PROFILE, payload.soundProfile.name)
        putExtra(EXTRA_IS_PRAYER, payload.isPrayer)
        putExtra(EXTRA_OCCURRENCE_TOKEN, payload.occurrenceToken)
        putExtra(EXTRA_RINGTONE_URI, payload.ringtoneUri)
        putExtra(EXTRA_RINGTONE_TITLE, payload.ringtoneTitle)
    }

    fun decode(intent: Intent?): AlarmRingingPayload? {
        intent ?: return null
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)?.takeIf { it.isNotBlank() } ?: return null
        val ruleRevision = intent.getLongExtra(EXTRA_RULE_REVISION, Long.MIN_VALUE)
        if (ruleRevision <= 0L) return null
        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: return null
        val sound = runCatching {
            AlarmSoundProfile.valueOf(intent.getStringExtra(EXTRA_SOUND_PROFILE).orEmpty())
        }.getOrNull() ?: return null
        val token = intent.getStringExtra(EXTRA_OCCURRENCE_TOKEN)?.takeIf { it.isNotBlank() } ?: return null
        return AlarmRingingPayload(
            alarmId = alarmId,
            ruleRevision = ruleRevision,
            title = title,
            soundProfile = sound,
            isPrayer = intent.getBooleanExtra(EXTRA_IS_PRAYER, false),
            occurrenceToken = token,
            ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI),
            ringtoneTitle = intent.getStringExtra(EXTRA_RINGTONE_TITLE),
        )
    }
}

internal object AlarmRingingNotificationFactory {
    const val CHANNEL_PRAYER = "arihna_prayer_alarm_v3"
    const val CHANNEL_CUSTOM = "arihna_custom_alarm_v3"

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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
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
        val snoozePendingIntent = PendingIntent.getService(
            context,
            requestCode(payload.alarmId, 3),
            AlarmRingingService.snoozeIntent(context, payload),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val subtitle = when (payload.soundProfile) {
            AlarmSoundProfile.ADHAN -> "Adhan in corso"
            AlarmSoundProfile.SYSTEM_DEFAULT -> payload.ringtoneTitle ?: "Sveglia in corso"
            AlarmSoundProfile.SILENT -> "Sveglia in corso"
        }
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_arihna)
            .setContentTitle(payload.title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setTicker("${payload.title} • Sveglia")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(activityPendingIntent)
            .addAction(0, "Interrompi", stopPendingIntent)
            .addAction(0, "Rinvia", snoozePendingIntent)
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

        if (intent?.action == ACTION_SNOOZE) {
            val payload = AlarmRingingIntentContract.decode(intent)
            if (payload != null && AlarmSnoozeScheduler(this).schedule(payload)) {
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
        startAudio(payload.soundProfile, payload.ringtoneUri)
        mainHandler.removeCallbacks(safetyStop)
        mainHandler.postDelayed(safetyStop, MAX_RINGING_MILLIS)
        return START_NOT_STICKY
    }

    private fun startAudio(profile: AlarmSoundProfile, selectedRingtoneUri: String?) {
        if (profile == AlarmSoundProfile.SILENT) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (profile == AlarmSoundProfile.ADHAN) {
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(attributes)
                resources.openRawResourceFd(R.raw.adhan_cc0).use { descriptor ->
                    player.setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length,
                    )
                }
                player.isLooping = false
                player.prepare()
                player.start()
                player.setOnCompletionListener { completed ->
                    if (mediaPlayer === completed) mediaPlayer = null
                    completed.release()
                }
                mediaPlayer = player
            } catch (throwable: Throwable) {
                player.release()
                mediaPlayer = null
            }
            return
        }

        val candidates = buildList<Uri> {
            selectedRingtoneUri?.let { raw ->
                runCatching { Uri.parse(raw) }.getOrNull()?.let(::add)
            }
            RingtoneManager.getActualDefaultRingtoneUri(
                this@AlarmRingingService,
                RingtoneManager.TYPE_ALARM,
            )?.let(::add)
            add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        }.distinct()
        for (uri in candidates) {
            val player = MediaPlayer()
            val started = runCatching {
                player.setAudioAttributes(attributes)
                player.setDataSource(this, uri)
                player.isLooping = true
                player.prepare()
                player.start()
            }.isSuccess
            if (started) {
                mediaPlayer = player
                return
            }
            player.release()
        }
        mediaPlayer = null
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
        private const val ACTION_SNOOZE = "com.archimedeprojects.arihna.action.SNOOZE_ALARM"
        private const val MAX_RINGING_MILLIS = 10 * 60 * 1000L

        fun ringIntent(context: Context, rule: AlarmRule, occurrence: AlarmOccurrence): Intent =
            ringPayloadIntent(context, createAlarmRingingPayload(rule, occurrence))

        internal fun ringPayloadIntent(context: Context, payload: AlarmRingingPayload): Intent =
            AlarmRingingIntentContract.put(
                Intent(context, AlarmRingingService::class.java).setAction(ACTION_RING),
                payload,
            )

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

        internal fun snoozeIntent(context: Context, payload: AlarmRingingPayload): Intent =
            AlarmRingingIntentContract.put(
                Intent(context, AlarmRingingService::class.java).apply {
                    action = ACTION_SNOOZE
                    data = Uri.Builder()
                        .scheme("arihna")
                        .authority("ringing-snooze-action")
                        .appendPath(payload.alarmId)
                        .appendPath(payload.occurrenceToken)
                        .build()
                },
                payload,
            )
    }
}

private fun AlarmPrayer.displayName(): String = when (this) {
    AlarmPrayer.FAJR -> "Fajr"
    AlarmPrayer.DHUHR -> "Dhuhr"
    AlarmPrayer.ASR -> "Asr"
    AlarmPrayer.MAGHRIB -> "Maghrib"
    AlarmPrayer.ISHA -> "Isha"
}
