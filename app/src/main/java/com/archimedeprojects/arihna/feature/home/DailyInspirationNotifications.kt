package com.archimedeprojects.arihna.feature.home

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.archimedeprojects.arihna.ArihnaApplication
import com.archimedeprojects.arihna.MainActivity
import com.archimedeprojects.arihna.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class DailyInspirationNotificationSettings(
    val enabled: Boolean = false,
    val hour: Int = DEFAULT_DAILY_INSPIRATION_HOUR,
    val minute: Int = DEFAULT_DAILY_INSPIRATION_MINUTE,
    val lastDeliveredDate: String? = null,
) {
    val deliveryTime: LocalTime
        get() = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

internal class DailyInspirationNotificationPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<DailyInspirationNotificationSettings> = dataStore.data
        .map(::decodeDailyInspirationNotificationSettings)
        .distinctUntilChanged()

    suspend fun current(): DailyInspirationNotificationSettings = settings.first()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLED_KEY] = enabled
        }
    }

    suspend fun setTime(time: LocalTime) {
        dataStore.edit { preferences ->
            preferences[HOUR_KEY] = time.hour
            preferences[MINUTE_KEY] = time.minute
        }
    }

    suspend fun markDelivered(date: LocalDate) {
        dataStore.edit { preferences ->
            preferences[LAST_DELIVERED_DATE_KEY] = date.toString()
        }
    }

    companion object {
        private val ENABLED_KEY = booleanPreferencesKey("daily.inspiration.notification.enabled.v1")
        private val HOUR_KEY = intPreferencesKey("daily.inspiration.notification.hour.v1")
        private val MINUTE_KEY = intPreferencesKey("daily.inspiration.notification.minute.v1")
        private val LAST_DELIVERED_DATE_KEY = stringPreferencesKey("daily.inspiration.notification.last_date.v1")

        internal fun enabledKey(): Preferences.Key<Boolean> = ENABLED_KEY
        internal fun hourKey(): Preferences.Key<Int> = HOUR_KEY
        internal fun minuteKey(): Preferences.Key<Int> = MINUTE_KEY
        internal fun lastDeliveredDateKey(): Preferences.Key<String> = LAST_DELIVERED_DATE_KEY
    }
}

internal fun decodeDailyInspirationNotificationSettings(
    preferences: Preferences,
): DailyInspirationNotificationSettings = DailyInspirationNotificationSettings(
    enabled = preferences[DailyInspirationNotificationPreferences.enabledKey()] ?: false,
    hour = (preferences[DailyInspirationNotificationPreferences.hourKey()]
        ?: DEFAULT_DAILY_INSPIRATION_HOUR).coerceIn(0, 23),
    minute = (preferences[DailyInspirationNotificationPreferences.minuteKey()]
        ?: DEFAULT_DAILY_INSPIRATION_MINUTE).coerceIn(0, 59),
    lastDeliveredDate = preferences[DailyInspirationNotificationPreferences.lastDeliveredDateKey()],
)

internal fun nextDailyInspirationTrigger(
    now: ZonedDateTime,
    deliveryTime: LocalTime,
): ZonedDateTime {
    val todayTarget = now.toLocalDate().atTime(deliveryTime).atZone(now.zone)
    return if (todayTarget.isAfter(now)) todayTarget else todayTarget.plusDays(1)
}

internal fun shouldDeliverDailyInspiration(
    settings: DailyInspirationNotificationSettings,
    localDate: LocalDate,
): Boolean = settings.enabled && settings.lastDeliveredDate != localDate.toString()

internal data class DailyInspirationNotificationPayload(
    val title: String,
    val arabic: String,
    val translationItalian: String,
    val reference: String,
)

internal fun dailyInspirationNotificationPayloadFor(
    localDate: LocalDate,
): DailyInspirationNotificationPayload =
    dailyInspirationNotificationPayload(dailyInspirationFor(localDate))

internal fun dailyInspirationNotificationPayload(
    inspiration: DailyInspiration,
): DailyInspirationNotificationPayload = DailyInspirationNotificationPayload(
    title = "Ispirazione del giorno",
    arabic = inspiration.text,
    translationItalian = inspiration.translationItalian.trim(),
    reference = inspiration.reference,
)

internal fun isDailyInspirationNotificationPermissionGranted(
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean = sdkInt < Build.VERSION_CODES.TIRAMISU || permissionGranted

class DailyInspirationNotificationController(
    context: Context,
    dataStore: DataStore<Preferences>,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = DailyInspirationNotificationPreferences(dataStore)

    val settings: Flow<DailyInspirationNotificationSettings> = preferences.settings

    suspend fun setEnabled(enabled: Boolean) {
        preferences.setEnabled(enabled)
        reconcile()
    }

    suspend fun setTime(time: LocalTime) {
        preferences.setTime(time)
        reconcile()
    }

    suspend fun reconcile(now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())) {
        cancelScheduled()
        val current = preferences.current()
        if (!current.enabled) return
        schedule(nextDailyInspirationTrigger(now, current.deliveryTime))
    }

    suspend fun handleScheduledDelivery(
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
    ) {
        val current = preferences.current()
        val localDate = now.toLocalDate()
        if (
            shouldDeliverDailyInspiration(current, localDate) &&
            isNotificationPermissionGranted()
        ) {
            val inspiration = dailyInspirationFor(localDate)
            if (DailyInspirationNotificationFactory.post(appContext, inspiration)) {
                preferences.markDelivered(localDate)
            }
        }
        reconcile(now.plusSeconds(1))
    }

    private fun schedule(trigger: ZonedDateTime) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.toInstant().toEpochMilli(),
            pendingIntent(),
        )
    }

    private fun cancelScheduled() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        DAILY_INSPIRATION_REQUEST_CODE,
        Intent(appContext, DailyInspirationNotificationReceiver::class.java).apply {
            action = ACTION_DAILY_INSPIRATION
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun isNotificationPermissionGranted(): Boolean =
        isDailyInspirationNotificationPermissionGranted(
            sdkInt = Build.VERSION.SDK_INT,
            permissionGranted =
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED,
        )
}

class DailyInspirationNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DAILY_INSPIRATION) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as? ArihnaApplication ?: return@launch
                application.appContainer.dailyInspirationNotificationController.handleScheduledDelivery()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

internal object DailyInspirationNotificationFactory {
    const val CHANNEL_ID = "arihna_daily_inspiration_v1"
    const val NOTIFICATION_ID = 2401

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Ispirazione del giorno",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Frase quotidiana di Arihna"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )
    }

    fun build(context: Context, inspiration: DailyInspiration): android.app.Notification {
        ensureChannel(context)
        val payload = dailyInspirationNotificationPayload(inspiration)
        val contentIntent = PendingIntent.getActivity(
            context,
            DAILY_INSPIRATION_REQUEST_CODE + 1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val bigText = buildString {
            append(payload.arabic)
            if (payload.translationItalian.isNotEmpty()) {
                append("\n\n")
                append(payload.translationItalian)
            }
            append("\n\n")
            append(payload.reference)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_arihna)
            .setContentTitle(payload.title)
            .setContentText(payload.arabic)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    fun post(context: Context, inspiration: DailyInspiration): Boolean {
        val notification = build(context, inspiration)
        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        }.getOrElse { false }
    }
}

internal const val DEFAULT_DAILY_INSPIRATION_HOUR = 8
internal const val DEFAULT_DAILY_INSPIRATION_MINUTE = 0
private const val ACTION_DAILY_INSPIRATION = "com.archimedeprojects.arihna.action.DAILY_INSPIRATION"
private const val DAILY_INSPIRATION_REQUEST_CODE = 8240
