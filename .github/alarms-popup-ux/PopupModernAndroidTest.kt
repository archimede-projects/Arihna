package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
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
class PopupModernAndroidTest {
    @Test
    fun api36CapabilityAndActionableAlarmContractMatchesPlatform() {
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
        val exactOccurrence = occurrence("popup-modern-exact", 7)
        val scheduleResult = scheduler.scheduleExact(exactOccurrence)
        if (expectedExact) {
            assertEquals(AlarmScheduleResult.Scheduled(exactOccurrence), scheduleResult)
            scheduler.cancel(exactOccurrence.alarmId)
        } else {
            assertEquals(AlarmScheduleResult.NeedsExactAlarmAccess, scheduleResult)
        }

        val permissionReader = AndroidAlarmNotificationPermissionReader(context)
        assertEquals(expectedNotification, permissionReader.isGranted())
        var starts = 0
        val delivery = AndroidAlarmNotificationDelivery(
            context = context,
            permissionReader = permissionReader,
            ringingStarter = AlarmRingingStarter { _, _ -> starts += 1 },
        )
        val customRule = AlarmRule(
            alarmId = "popup-modern-custom",
            revision = 3,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom("Popup UX", LocalTime.NOON),
        )
        val deliveryResult = delivery.deliver(customRule, occurrence(customRule.alarmId, customRule.revision))
        if (expectedNotification) {
            assertEquals(AlarmNotificationDeliveryResult.DELIVERED, deliveryResult)
            assertEquals(1, starts)
        } else {
            assertEquals(AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION, deliveryResult)
            assertEquals(0, starts)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        delivery.ensureChannels()
        val customChannel = manager.getNotificationChannel(AlarmRingingNotificationFactory.CHANNEL_CUSTOM)
        assertNotNull(customChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, customChannel.importance)
        assertNull(customChannel.sound)

        val fullScreenAccess = AlarmFullScreenAccess(context)
        assertEquals(manager.canUseFullScreenIntent(), fullScreenAccess.isGranted())
        val payload = AlarmRingingPayload(
            alarmId = "popup-modern-ring",
            ruleRevision = 4L,
            title = "Lavoro",
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            isPrayer = false,
            occurrenceToken = "modern-token",
            ringtoneTitle = "Morning Flower",
        )
        val urgent = AlarmRingingNotificationFactory.build(context, payload, fullScreenAccess)
        assertEquals(Notification.CATEGORY_ALARM, urgent.category)
        assertNotNull(urgent.contentIntent)
        assertTrue(urgent.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(2, urgent.actions.size)
        assertEquals("Interrompi", urgent.actions[0].title.toString())
        assertEquals("Rinvia", urgent.actions[1].title.toString())
        if (fullScreenAccess.isGranted()) assertNotNull(urgent.fullScreenIntent) else assertNull(urgent.fullScreenIntent)

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS,
        )
        val ringingService = packageInfo.services?.firstOrNull { it.name == AlarmRingingService::class.java.name }
        assertNotNull(ringingService)
        assertFalse(ringingService!!.exported)
        assertTrue((ringingService.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) != 0)
        val snoozeReceiver = packageInfo.receivers?.firstOrNull { it.name == AlarmSnoozeReceiver::class.java.name }
        assertNotNull(snoozeReceiver)
        assertFalse(snoozeReceiver!!.exported)
    }

    private fun occurrence(id: String, revision: Long) = AlarmOccurrence(
        alarmId = id,
        ruleRevision = revision,
        triggerAt = Instant.now().plusSeconds(3_600),
        occurrenceToken = "token-$id-$revision",
    )
}
