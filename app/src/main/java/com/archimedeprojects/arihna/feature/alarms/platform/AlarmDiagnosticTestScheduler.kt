package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

enum class AlarmDiagnosticKind {
    SYSTEM_ALARM,
    ADHAN,
}

enum class AlarmDiagnosticScheduleResult {
    SCHEDULED,
    NEEDS_NOTIFICATION_PERMISSION,
    NEEDS_EXACT_ALARM_ACCESS,
    NEEDS_FULL_SCREEN_ACCESS,
}

class AlarmDiagnosticTestScheduler(
    context: Context,
    private val notificationPermissionReader: AlarmNotificationPermissionReader,
    private val fullScreenAccess: AlarmFullScreenAccess,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleOneMinute(kind: AlarmDiagnosticKind): AlarmDiagnosticScheduleResult {
        if (!notificationPermissionReader.isGranted()) {
            return AlarmDiagnosticScheduleResult.NEEDS_NOTIFICATION_PERMISSION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return AlarmDiagnosticScheduleResult.NEEDS_EXACT_ALARM_ACCESS
        }
        if (!fullScreenAccess.isGranted()) {
            return AlarmDiagnosticScheduleResult.NEEDS_FULL_SCREEN_ACCESS
        }
        cancel()
        val triggerAt = System.currentTimeMillis() + TEST_DELAY_MILLIS
        val token = "diagnostic-${UUID.randomUUID()}"
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(kind, triggerAt, token),
        )
        return AlarmDiagnosticScheduleResult.SCHEDULED
    }

    fun cancel() {
        AlarmDiagnosticKind.entries.forEach { kind ->
            val operation = pendingIntent(kind, 0L, "cancel")
            alarmManager.cancel(operation)
            operation.cancel()
        }
    }

    private fun pendingIntent(
        kind: AlarmDiagnosticKind,
        triggerAt: Long,
        token: String,
    ): PendingIntent {
        val intent = Intent(appContext, AlarmDiagnosticReceiver::class.java).apply {
            data = Uri.Builder()
                .scheme("arihna")
                .authority("alarm-diagnostic")
                .appendPath(kind.name.lowercase())
                .build()
            putExtra(EXTRA_KIND, kind.name)
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
            putExtra(EXTRA_TOKEN, token)
        }
        return PendingIntent.getBroadcast(
            appContext,
            kind.ordinal + 7000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val TEST_DELAY_MILLIS = 20_000L
        internal const val EXTRA_KIND = "arihna.diagnostic.kind"
        internal const val EXTRA_TRIGGER_AT = "arihna.diagnostic.trigger_at"
        internal const val EXTRA_TOKEN = "arihna.diagnostic.token"
    }
}

class AlarmDiagnosticReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return
        val kind = runCatching {
            AlarmDiagnosticKind.valueOf(intent.getStringExtra(AlarmDiagnosticTestScheduler.EXTRA_KIND).orEmpty())
        }.getOrNull() ?: return
        val triggerAt = intent.getLongExtra(AlarmDiagnosticTestScheduler.EXTRA_TRIGGER_AT, 0L)
        val token = intent.getStringExtra(AlarmDiagnosticTestScheduler.EXTRA_TOKEN)?.takeIf { it.isNotBlank() } ?: return
        if (triggerAt <= 0L) return

        val profile = when (kind) {
            AlarmDiagnosticKind.SYSTEM_ALARM -> AlarmSoundProfile.SYSTEM_DEFAULT
            AlarmDiagnosticKind.ADHAN -> AlarmSoundProfile.ADHAN
        }
        val id = when (kind) {
            AlarmDiagnosticKind.SYSTEM_ALARM -> "diagnostic-system-alarm"
            AlarmDiagnosticKind.ADHAN -> "diagnostic-adhan"
        }
        val title = when (kind) {
            AlarmDiagnosticKind.SYSTEM_ALARM -> "Test sveglia"
            AlarmDiagnosticKind.ADHAN -> "Test Adhan"
        }
        val rule = AlarmRule(
            alarmId = id,
            revision = 1L,
            enabled = true,
            soundProfile = profile,
            definition = AlarmDefinition.Custom(
                label = title,
                localTime = LocalTime.now().withSecond(0).withNano(0),
            ),
        )
        val occurrence = AlarmOccurrence(
            alarmId = id,
            ruleRevision = 1L,
            triggerAt = Instant.ofEpochMilli(triggerAt),
            occurrenceToken = token,
        )
        AndroidAlarmNotificationDelivery(
            context = context.applicationContext,
            permissionReader = AndroidAlarmNotificationPermissionReader(context.applicationContext),
        ).deliver(rule, occurrence)
    }
}
