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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmNotificationAndroidTest {
    @Test
    fun api28CreatesSystemAndSilentChannelsAndCanPostValidatedDelivery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val delivery = AndroidAlarmNotificationDelivery(
            context = context,
            permissionReader = AlarmNotificationPermissionReader { true },
        )
        val rule = AlarmRule(
            alarmId = "instrumented-notification",
            revision = 1,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom(
                label = "Test Arihna",
                localTime = LocalTime.of(12, 0),
            ),
        )
        val occurrence = AlarmOccurrence(
            alarmId = rule.alarmId,
            ruleRevision = rule.revision,
            triggerAt = Instant.parse("2026-09-04T12:00:00Z"),
            occurrenceToken = "instrumented",
        )

        delivery.ensureChannels()
        val systemChannel = manager.getNotificationChannel(AndroidAlarmNotificationDelivery.CHANNEL_SYSTEM)
        val silentChannel = manager.getNotificationChannel(AndroidAlarmNotificationDelivery.CHANNEL_SILENT)
        assertNotNull(systemChannel)
        assertNotNull(silentChannel)
        assertNull(silentChannel.sound)

        val result = delivery.deliver(rule, occurrence)
        val notificationId = AndroidAlarmNotificationDelivery.notificationId(rule.alarmId)
        assertEquals(AlarmNotificationDeliveryResult.DELIVERED, result)
        assertTrue(manager.activeNotifications.any { it.id == notificationId })

        manager.cancel(notificationId)
    }
}
