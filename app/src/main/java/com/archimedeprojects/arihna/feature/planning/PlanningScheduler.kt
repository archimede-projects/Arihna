package com.archimedeprojects.arihna.feature.planning

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.archimedeprojects.arihna.ArihnaApplication
import com.archimedeprojects.arihna.R
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmRingingService
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmRingingPayload
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlanningScheduler(
    private val context: Context,
    private val repository: PlanningRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val manager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager get() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val lock = Mutex()

    fun notificationsGranted(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun exactAlarmsGranted(): Boolean =
        Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()

    private fun pendingIntent(item: PlanningItem): PendingIntent {
        val intent = Intent(context, PlanningReceiver::class.java).apply {
            data = Uri.parse("arihna://planning/${item.id}")
            putExtra("id", item.id)
            putExtra("rev", item.revision)
            putExtra("instant", item.date?.let { date ->
                item.time?.let { time -> PlanningTime.instant(date, time, zone()).toEpochMilli() }
            } ?: -1L)
        }
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancel(item: PlanningItem) {
        val p = pendingIntent(item)
        manager.cancel(p)
        p.cancel()
    }

    suspend fun reconcile() = lock.withLock {
        val now = clock.instant()
        val entries = repository.all()
        entries.forEach { item ->
            // Stable PendingIntent URI, same ID across all revisions.
            cancel(item)
            if (item.type == PlanningType.TASK || !item.pending || !notificationsGranted() ||
                !exactAlarmsGranted()
            ) return@forEach
            val date = item.date ?: return@forEach
            val time = item.time ?: return@forEach
            val at = PlanningTime.instant(date, time, zone())
            if (!at.isAfter(now)) return@forEach
            runCatching {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pendingIntent(item),
                )
            }
        }
    }

    suspend fun onFire(id: String, revision: Long, epochMillis: Long) = lock.withLock {
        val item = repository.find(id) ?: return@withLock
        if (item.revision != revision || !item.pending || item.type == PlanningType.TASK ||
            !notificationsGranted()
        ) return@withLock
        val date = item.date ?: return@withLock
        val time = item.time ?: return@withLock
        val expected = PlanningTime.instant(date, time, zone()).toEpochMilli()
        if (expected != epochMillis || clock.millis() + 30_000L < expected) return@withLock
        if (item.type == PlanningType.REMINDER) {
            if (!postQuietReminder(item)) return@withLock
        } else {
            val payload = AlarmRingingPayload(
                alarmId = item.id, ruleRevision = item.revision, title = item.title,
                soundProfile = item.sound, isPrayer = false,
                occurrenceToken = "planning-${item.id}-${item.revision}",
                ringtoneUri = item.ringtoneUri, ringtoneTitle = item.ringtoneTitle,
                playbackVolumePercent = item.volume,
            )
            val success = runCatching {
                ContextCompat.startForegroundService(
                    context, AlarmRingingService.ringPayloadIntent(context, payload),
                )
            }.isSuccess
            if (!success) return@withLock
        }
        repository.markDelivered(item.id, item.revision)
    }

    private fun postQuietReminder(item: PlanningItem): Boolean {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL, "Promemoria Planning", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                description = "Notifiche discrete dei promemoria personali"
            }
            notificationManager.createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(
            context, 10, context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_arihna)
            .setContentTitle(item.title)
            .setContentText(item.note.ifBlank { "Promemoria Arihna" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.note.ifBlank { item.title }))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching {
            notificationManager.notify(item.id.hashCode() and Int.MAX_VALUE, notification)
        }.isSuccess
    }

    companion object { private const val CHANNEL = "arihna_planning_reminders_v1" }
}

class PlanningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id")?.takeIf { it.startsWith("planning-") } ?: return
        val revision = intent.getLongExtra("rev", -1L)
        val millis = intent.getLongExtra("instant", -1L)
        if (revision < 1L || millis < 0L) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                (context.applicationContext as? ArihnaApplication)?.appContainer
                    ?.planningScheduler?.onFire(id, revision, millis)
            } finally {
                pending.finish()
            }
        }
    }
}
