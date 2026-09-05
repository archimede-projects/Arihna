package com.archimedeprojects.arihna.feature.alarms.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
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

fun interface AlarmNotificationPoster {
    fun post(rule: AlarmRule, occurrence: AlarmOccurrence)
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

class AndroidAlarmNotificationPoster(context: Context) : AlarmNotificationPoster {
    private val appContext = context.applicationContext

    override fun post(rule: AlarmRule, occurrence: AlarmOccurrence) {
        val payload = createAlarmRingingPayload(rule, occurrence)
        val notification = AlarmRingingNotificationFactory.build(appContext, payload)
        NotificationManagerCompat.from(appContext).notify(
            AlarmRingingNotificationFactory.notificationId(rule.alarmId),
            notification,
        )
    }
}

class AndroidAlarmNotificationDelivery(
    context: Context,
    private val permissionReader: AlarmNotificationPermissionReader,
    private val ringingStarter: AlarmRingingStarter = AndroidAlarmRingingStarter(context),
    private val notificationPoster: AlarmNotificationPoster = AndroidAlarmNotificationPoster(context),
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
        notificationPoster.post(rule, occurrence)
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
