package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.archimedeprojects.arihna.ArihnaApplication
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object AlarmSnoozePolicy {
    const val DELAY_MILLIS = 5 * 60 * 1000L

    fun triggerAt(now: Instant): Instant = now.plusMillis(DELAY_MILLIS)

    fun canReplay(payload: AlarmRingingPayload, rule: AlarmRule?): Boolean {
        if (payload.alarmId.startsWith(DIAGNOSTIC_PREFIX)) return true
        rule ?: return false
        if (rule.alarmId != payload.alarmId) return false
        if (rule.enabled && rule.revision == payload.ruleRevision) return true

        return rule.isOneShotCustom &&
            !rule.enabled &&
            rule.revision == payload.ruleRevision + 1L
    }

    private const val DIAGNOSTIC_PREFIX = "diagnostic-"
}

internal class AlarmSnoozeScheduler(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(payload: AlarmRingingPayload): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }
        val triggerAt = AlarmSnoozePolicy.triggerAt(clock.instant())
        val intent = AlarmRingingIntentContract.put(
            Intent(appContext, AlarmSnoozeReceiver::class.java).apply {
                action = ACTION_SNOOZE_FIRE
                data = Uri.Builder()
                    .scheme("arihna")
                    .authority("ringing-snooze")
                    .appendPath(payload.alarmId)
                    .appendPath(payload.occurrenceToken)
                    .build()
            },
            payload,
        )
        val operation = PendingIntent.getBroadcast(
            appContext,
            requestCode(payload.alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt.toEpochMilli(),
                operation,
            )
        }.isSuccess
    }

    private fun requestCode(alarmId: String): Int =
        (37 * alarmId.hashCode() + 5) and Int.MAX_VALUE

    companion object {
        private const val ACTION_SNOOZE_FIRE = "com.archimedeprojects.arihna.action.SNOOZE_ALARM_FIRE"
    }
}

class AlarmSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val payload = AlarmRingingIntentContract.decode(intent) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                if (!AndroidAlarmNotificationPermissionReader(appContext).isGranted()) return@launch
                val application = appContext as? ArihnaApplication
                val rule = application?.appContainer?.alarmRuleRepository?.get(payload.alarmId)
                if (!AlarmSnoozePolicy.canReplay(payload, rule)) return@launch

                ContextCompat.startForegroundService(
                    appContext,
                    AlarmRingingService.ringPayloadIntent(appContext, payload),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
