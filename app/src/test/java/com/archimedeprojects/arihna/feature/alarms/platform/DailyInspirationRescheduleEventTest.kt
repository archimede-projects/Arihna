package com.archimedeprojects.arihna.feature.alarms.platform

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyInspirationRescheduleEventTest {
    @Test
    fun dailyInspirationReschedulesForClockEnvironmentChanges() {
        assertTrue(AlarmSystemEventReceiver.shouldRescheduleDailyInspiration(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(AlarmSystemEventReceiver.shouldRescheduleDailyInspiration(Intent.ACTION_TIME_CHANGED))
        assertTrue(AlarmSystemEventReceiver.shouldRescheduleDailyInspiration(Intent.ACTION_TIMEZONE_CHANGED))
        assertTrue(AlarmSystemEventReceiver.shouldRescheduleDailyInspiration(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertFalse(
            AlarmSystemEventReceiver.shouldRescheduleDailyInspiration(
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            ),
        )
    }
}
