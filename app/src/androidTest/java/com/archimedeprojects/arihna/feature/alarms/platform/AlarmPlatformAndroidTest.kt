package com.archimedeprojects.arihna.feature.alarms.platform

import android.Manifest
import android.content.ComponentName
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmPlatformAndroidTest {
    @Test
    fun api28ExactScheduleAndCancelPathWorksWithoutSpecialAccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(28, Build.VERSION.SDK_INT)
        val backend = AndroidExactAlarmBackend(context)
        val scheduler = DefaultAlarmPlatformScheduler(backend)
        val occurrence = occurrence("api28-path", revision = 1)

        assertTrue(backend.hasExactAlarmAccess())
        assertEquals(ExactAlarmCapability.READY, scheduler.capability())
        val result = scheduler.scheduleExact(occurrence)
        assertEquals(AlarmScheduleResult.Scheduled(occurrence), result)
        scheduler.cancel(occurrence.alarmId)
    }

    @Test
    fun pendingIntentIdentityIsStablePerAlarmIdAndDistinctAcrossIds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = AlarmPendingIntentFactory.occurrence(context, occurrence("same", 1))
        val sameIdNewRevision = AlarmPendingIntentFactory.occurrence(context, occurrence("same", 2))
        val other = AlarmPendingIntentFactory.occurrence(context, occurrence("other", 1))

        assertEquals(first, sameIdNewRevision)
        assertNotEquals(first, other)

        AndroidExactAlarmBackend(context).cancel("same")
        AndroidExactAlarmBackend(context).cancel("other")
    }

    @Test
    fun api28HasNoExactAlarmSettingsIntent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(28, Build.VERSION.SDK_INT)
        assertNull(ExactAlarmAccessIntentFactory(context).create())
    }

    @Test
    fun manifestDeclaresOnlyAuthorizedAlarmPlatformComponentsAndPermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0x00001000)
        val permissions = packageInfo.requestedPermissions?.toSet().orEmpty()

        assertTrue(Manifest.permission.ACCESS_COARSE_LOCATION in permissions)
        assertTrue(Manifest.permission.SCHEDULE_EXACT_ALARM in permissions)
        assertTrue(Manifest.permission.RECEIVE_BOOT_COMPLETED in permissions)
        assertFalse("android.permission.USE_EXACT_ALARM" in permissions)
        assertFalse(Manifest.permission.ACCESS_FINE_LOCATION in permissions)
        assertFalse(Manifest.permission.ACCESS_BACKGROUND_LOCATION in permissions)

        context.packageManager.getReceiverInfo(
            ComponentName(context, AlarmOccurrenceReceiver::class.java),
            0,
        )
        context.packageManager.getReceiverInfo(
            ComponentName(context, AlarmSystemEventReceiver::class.java),
            0,
        )
    }

    private fun occurrence(alarmId: String, revision: Long) = AlarmOccurrence(
        alarmId = alarmId,
        ruleRevision = revision,
        triggerAt = Instant.now().plusSeconds(3_600),
        occurrenceToken = "token-$revision",
    )
}
