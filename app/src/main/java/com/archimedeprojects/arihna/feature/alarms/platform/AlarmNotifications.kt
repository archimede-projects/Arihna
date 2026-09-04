package com.archimedeprojects.arihna.feature.alarms.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.archimedeprojects.arihna.MainActivity
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile

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

class AndroidAlarmNotificationDelivery(
    context: Context,
    private val permissionReader: AlarmNotificationPermissionReader,
) : AlarmNotificationDelivery {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val alarmUsage = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        val system = NotificationChannel(
            CHANNEL_SYSTEM,
            "Sveglie Arihna",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Sveglie di preghiera e personalizzate"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmUsage)
        }
        val silent = NotificationChannel(
            CHANNEL_SILENT,
            "Sveglie Arihna silenziose",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Sveglie Arihna senza suono"
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(system)
        notificationManager.createNotificationChannel(silent)
    }

    override fun deliver(
        rule: AlarmRule,
        occurrence: AlarmOccurrence,
    ): AlarmNotificationDeliveryResult {
        if (!permissionReader.isGranted()) {
            return AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION
        }
        ensureChannels()
        val channel = when (rule.soundProfile) {
            AlarmSoundProfile.SYSTEM_DEFAULT -> CHANNEL_SYSTEM
            AlarmSoundProfile.SILENT -> CHANNEL_SILENT
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titleFor(rule))
            .setContentText("Arihna • sveglia programmata")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(notificationId(rule.alarmId), notification)
        return AlarmNotificationDeliveryResult.DELIVERED
    }

    private fun titleFor(rule: AlarmRule): String = when (val definition = rule.definition) {
        is AlarmDefinition.PrayerLinked -> "Preghiera ${definition.prayer.displayName()}"
        is AlarmDefinition.Custom -> definition.label
    }

    companion object {
        const val CHANNEL_SYSTEM = "arihna_alarm_system"
        const val CHANNEL_SILENT = "arihna_alarm_silent"

        fun notificationId(alarmId: String): Int = alarmId.hashCode() and Int.MAX_VALUE
    }
}

private fun com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer.displayName(): String = when (this) {
    com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer.FAJR -> "Fajr"
    com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer.DHUHR -> "Dhuhr"
    com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer.ASR -> "Asr"
    com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer.MAGHRIB -> "Maghrib"
    com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer.ISHA -> "Isha"
}
