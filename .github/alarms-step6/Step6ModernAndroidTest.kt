package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Step6ModernAndroidTest {
    @Test
    fun api36ControlledPermissionMatrixMatchesRealPlatformBehavior() {
        assertEquals(36, Build.VERSION.SDK_INT)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val args = InstrumentationRegistry.getArguments()
        val expectedExact = args.getString("expectedExact") == "true"
        val expectedNotification = args.getString("expectedNotification") == "true"

        val backend = AndroidExactAlarmBackend(context)
        val scheduler = DefaultAlarmPlatformScheduler(backend)
        assertEquals(expectedExact, backend.hasExactAlarmAccess())
        assertEquals(
            if (expectedExact) ExactAlarmCapability.READY else ExactAlarmCapability.NEEDS_EXACT_ALARM_ACCESS,
            scheduler.capability(),
        )

        val occurrence = AlarmOccurrence(
            alarmId = "step6-modern-exact",
            ruleRevision = 7,
            triggerAt = Instant.now().plusSeconds(3_600),
            occurrenceToken = "step6-modern-token",
        )
        val scheduleResult = scheduler.scheduleExact(occurrence)
        if (expectedExact) {
            assertEquals(AlarmScheduleResult.Scheduled(occurrence), scheduleResult)
            scheduler.cancel(occurrence.alarmId)
        } else {
            assertEquals(AlarmScheduleResult.NeedsExactAlarmAccess, scheduleResult)
        }

        val accessIntent = ExactAlarmAccessIntentFactory(context).create()
        assertNotNull(accessIntent)
        assertEquals(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, accessIntent!!.action)
        assertEquals("package:${context.packageName}", accessIntent.data.toString())

        val permissionReader = AndroidAlarmNotificationPermissionReader(context)
        assertEquals(expectedNotification, permissionReader.isGranted())
        val delivery = AndroidAlarmNotificationDelivery(context, permissionReader)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        delivery.ensureChannels()
        val systemChannel = manager.getNotificationChannel(AndroidAlarmNotificationDelivery.CHANNEL_SYSTEM)
        val silentChannel = manager.getNotificationChannel(AndroidAlarmNotificationDelivery.CHANNEL_SILENT)
        assertNotNull(systemChannel)
        assertNotNull(silentChannel)
        assertNotNull(systemChannel.sound)
        assertNull(silentChannel.sound)
        assertFalse(silentChannel.shouldVibrate())

        val systemRule = rule("step6-modern-system", AlarmSoundProfile.SYSTEM_DEFAULT)
        val silentRule = rule("step6-modern-silent", AlarmSoundProfile.SILENT)
        val systemOccurrence = occurrenceFor(systemRule)
        val silentOccurrence = occurrenceFor(silentRule)
        val systemId = AndroidAlarmNotificationDelivery.notificationId(systemRule.alarmId)
        val silentId = AndroidAlarmNotificationDelivery.notificationId(silentRule.alarmId)
        manager.cancel(systemId)
        manager.cancel(silentId)

        val systemResult = delivery.deliver(systemRule, systemOccurrence)
        val silentResult = delivery.deliver(silentRule, silentOccurrence)
        if (expectedNotification) {
            assertEquals(AlarmNotificationDeliveryResult.DELIVERED, systemResult)
            assertEquals(AlarmNotificationDeliveryResult.DELIVERED, silentResult)
            val deadline = SystemClock.uptimeMillis() + 3_000L
            var active = manager.activeNotifications.associateBy { it.id }
            while ((!active.containsKey(systemId) || !active.containsKey(silentId)) && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(50L)
                active = manager.activeNotifications.associateBy { it.id }
            }
            assertTrue(active.containsKey(systemId))
            assertTrue(active.containsKey(silentId))
            assertEquals(AndroidAlarmNotificationDelivery.CHANNEL_SYSTEM, active.getValue(systemId).notification.channelId)
            assertEquals(AndroidAlarmNotificationDelivery.CHANNEL_SILENT, active.getValue(silentId).notification.channelId)
        } else {
            assertEquals(AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION, systemResult)
            assertEquals(AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION, silentResult)
            val activeIds = manager.activeNotifications.map { it.id }.toSet()
            assertFalse(systemId in activeIds)
            assertFalse(silentId in activeIds)
        }

        manager.cancel(systemId)
        manager.cancel(silentId)
    }

    private fun rule(id: String, profile: AlarmSoundProfile) = AlarmRule(
        alarmId = id,
        revision = 1,
        enabled = true,
        soundProfile = profile,
        definition = AlarmDefinition.Custom(
            label = "STEP 6 modern",
            localTime = LocalTime.of(12, 0),
        ),
    )

    private fun occurrenceFor(rule: AlarmRule) = AlarmOccurrence(
        alarmId = rule.alarmId,
        ruleRevision = rule.revision,
        triggerAt = Instant.now().plusSeconds(3_600),
        occurrenceToken = "token-${rule.alarmId}",
    )
}
