package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmNotificationAndroidTest {
    @Test
    fun api28CreatesHighImportanceSilentChannelsAndBuildsActionableFullScreenAlarmNotification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AlarmRingingNotificationFactory.ensureChannels(context)

        val prayerChannel = manager.getNotificationChannel(AlarmRingingNotificationFactory.CHANNEL_PRAYER)
        val customChannel = manager.getNotificationChannel(AlarmRingingNotificationFactory.CHANNEL_CUSTOM)
        assertNotNull(prayerChannel)
        assertNotNull(customChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, prayerChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, customChannel.importance)
        assertNull(prayerChannel.sound)
        assertNull(customChannel.sound)

        val payload = AlarmRingingPayload(
            alarmId = "instrumented-notification",
            ruleRevision = 1L,
            title = "Sveglia test",
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            isPrayer = false,
            occurrenceToken = "instrumented",
        )
        val notification = AlarmRingingNotificationFactory.build(context, payload)
        assertNotNull(notification.fullScreenIntent)
        assertNotNull(notification.contentIntent)
        assertNotNull(notification.headsUpContentView)
        assertNotNull(notification.bigContentView)
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(2, notification.actions.size)
        assertEquals("Interrompi", notification.actions[0].title.toString())
        assertEquals("Rinvia", notification.actions[1].title.toString())
        assertNotNull(notification.actions[0].actionIntent)
        assertNotNull(notification.actions[1].actionIntent)
    }

    @Test
    fun validatedDeliveryPostsUrgentNotificationBeforeStartingRingingExactlyOnce() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val events = mutableListOf<String>()
        val delivery = AndroidAlarmNotificationDelivery(
            context = context,
            permissionReader = AlarmNotificationPermissionReader { true },
            ringingStarter = AlarmRingingStarter { _, _ -> events += "start" },
            notificationPoster = AlarmNotificationPoster { _, _ -> events += "post" },
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
        assertEquals(listOf("post", "start"), events)
    }

    @Test
    fun ringtonePickerIsRestrictedToAlarmTonesAndNeverUsesSilentAsPlatformChoice() {
        val intent = AlarmRingtonePicker.createIntent("content://alarm/current")
        assertEquals(RingtoneManager.ACTION_RINGTONE_PICKER, intent.action)
        assertEquals(
            RingtoneManager.TYPE_ALARM,
            intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1),
        )
        assertTrue(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false))
        assertEquals(false, intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true))
    }
}
