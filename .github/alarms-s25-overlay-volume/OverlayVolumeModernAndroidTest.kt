package com.archimedeprojects.arihna.feature.alarms.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
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
class OverlayVolumeModernAndroidTest {
    @Test
    fun api36HonorsAlarmCapabilitiesOverlayAccessAndOemSafeFallback() {
        assertEquals(36, Build.VERSION.SDK_INT)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val args = InstrumentationRegistry.getArguments()
        val expectedExact = args.getString("expectedExact") == "true"
        val expectedNotification = args.getString("expectedNotification") == "true"
        val expectedFullScreen = args.getString("expectedFullScreen") == "true"
        val expectedOverlay = args.getString("expectedOverlay") == "true"

        val backend = AndroidExactAlarmBackend(context)
        val scheduler = DefaultAlarmPlatformScheduler(backend)
        assertEquals(expectedExact, backend.hasExactAlarmAccess())
        assertEquals(
            if (expectedExact) ExactAlarmCapability.READY else ExactAlarmCapability.NEEDS_EXACT_ALARM_ACCESS,
            scheduler.capability(),
        )
        val exactOccurrence = occurrence("overlay-volume-exact", 9)
        val scheduleResult = scheduler.scheduleExact(exactOccurrence)
        if (expectedExact) {
            assertEquals(AlarmScheduleResult.Scheduled(exactOccurrence), scheduleResult)
            scheduler.cancel(exactOccurrence.alarmId)
        } else {
            assertEquals(AlarmScheduleResult.NeedsExactAlarmAccess, scheduleResult)
        }

        val permissionReader = AndroidAlarmNotificationPermissionReader(context)
        assertEquals(expectedNotification, permissionReader.isGranted())

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val fullScreenAccess = AlarmFullScreenAccess(context)
        assertEquals(expectedFullScreen, fullScreenAccess.isGranted())
        assertEquals(manager.canUseFullScreenIntent(), fullScreenAccess.isGranted())

        val overlayAccess = AlarmOverlayAccess(context)
        assertEquals(expectedOverlay, overlayAccess.isGranted())
        assertEquals(Settings.canDrawOverlays(context), overlayAccess.isGranted())
        val overlayIntent = overlayAccess.createSettingsIntent()
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, overlayIntent.action)
        assertEquals(context.packageName, overlayIntent.data?.schemeSpecificPart)

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES,
        )
        assertTrue(packageInfo.requestedPermissions.orEmpty().contains(Manifest.permission.SYSTEM_ALERT_WINDOW))

        val payload = AlarmRingingPayload(
            alarmId = "overlay-volume-ring",
            ruleRevision = 4L,
            title = "Test sveglia",
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            isPrayer = false,
            occurrenceToken = "modern-overlay-token",
        )
        val urgent = AlarmRingingNotificationFactory.build(context, payload, fullScreenAccess)
        assertEquals(Notification.CATEGORY_ALARM, urgent.category)
        assertNotNull(urgent.contentIntent)
        assertNull(urgent.headsUpContentView)
        assertNull(urgent.bigContentView)
        assertTrue(urgent.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(2, urgent.actions.size)
        assertEquals("Interrompi", urgent.actions[0].title.toString())
        assertEquals("Rinvia", urgent.actions[1].title.toString())
        if (expectedFullScreen) assertNotNull(urgent.fullScreenIntent) else assertNull(urgent.fullScreenIntent)

        AlarmRingingNotificationFactory.ensureChannels(context)
        val customChannel = manager.getNotificationChannel(AlarmRingingNotificationFactory.CHANNEL_CUSTOM)
        assertNotNull(customChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, customChannel.importance)
        assertNull(customChannel.sound)

        var starts = 0
        val delivery = AndroidAlarmNotificationDelivery(
            context = context,
            permissionReader = permissionReader,
            ringingStarter = AlarmRingingStarter { _, _ -> starts += 1 },
        )
        val customRule = AlarmRule(
            alarmId = "overlay-volume-custom",
            revision = 3,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom("Test sveglia", LocalTime.NOON),
        )
        val deliveryResult = delivery.deliver(customRule, occurrence(customRule.alarmId, customRule.revision))
        if (expectedNotification) {
            assertEquals(AlarmNotificationDeliveryResult.DELIVERED, deliveryResult)
            assertEquals(1, starts)
        } else {
            assertEquals(AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION, deliveryResult)
            assertEquals(0, starts)
        }

        val service = packageInfo.services?.firstOrNull { it.name == AlarmRingingService::class.java.name }
        assertNotNull(service)
        assertFalse(service!!.exported)
        assertTrue((service.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) != 0)

        val volume = AlarmVolumeController(context).read()
        assertTrue(volume.current in volume.min..volume.max)
        assertTrue(volume.percent in 0..100)
    }

    private fun occurrence(id: String, revision: Long) = AlarmOccurrence(
        alarmId = id,
        ruleRevision = revision,
        triggerAt = Instant.now().plusSeconds(3_600),
        occurrenceToken = "token-$id-$revision",
    )
}
