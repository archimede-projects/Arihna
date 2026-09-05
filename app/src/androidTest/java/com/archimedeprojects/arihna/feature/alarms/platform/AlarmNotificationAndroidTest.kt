package com.archimedeprojects.arihna.feature.alarms.platform

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
