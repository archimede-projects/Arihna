package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence

class AndroidExactAlarmBackend(
    private val context: Context,
) : ExactAlarmBackend {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun hasExactAlarmAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun scheduleRtcWakeupExact(occurrence: AlarmOccurrence) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            occurrence.triggerAt.toEpochMilli(),
            AlarmPendingIntentFactory.occurrence(context, occurrence),
        )
    }

    override fun cancel(alarmId: String) {
        val operation = AlarmPendingIntentFactory.forAlarmId(context, alarmId)
        alarmManager.cancel(operation)
        operation.cancel()
    }
}

object AlarmPendingIntentFactory {
    private const val REQUEST_CODE = 0

    fun occurrence(context: Context, occurrence: AlarmOccurrence): PendingIntent {
        val intent = baseIntent(context, occurrence.alarmId).apply {
            putExtra(AlarmIntentContract.EXTRA_ALARM_ID, occurrence.alarmId)
            putExtra(AlarmIntentContract.EXTRA_RULE_REVISION, occurrence.ruleRevision)
            putExtra(AlarmIntentContract.EXTRA_TRIGGER_AT_EPOCH_MILLIS, occurrence.triggerAt.toEpochMilli())
            putExtra(AlarmIntentContract.EXTRA_OCCURRENCE_TOKEN, occurrence.occurrenceToken)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun forAlarmId(context: Context, alarmId: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        baseIntent(context, alarmId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun baseIntent(context: Context, alarmId: String): Intent =
        Intent(context, AlarmOccurrenceReceiver::class.java).apply {
            data = Uri.Builder()
                .scheme("arihna")
                .authority("alarm")
                .appendPath(alarmId)
                .build()
        }
}

class ExactAlarmAccessIntentFactory(
    private val context: Context,
) {
    fun create(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
