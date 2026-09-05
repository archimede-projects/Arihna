package com.archimedeprojects.arihna.feature.alarms.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmDiagnosticAndroidTest {
    @Test
    fun api28SchedulesAndCancelsBothIsolatedOneMinuteDiagnosticKinds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduler = AlarmDiagnosticTestScheduler(
            context = context,
            notificationPermissionReader = AlarmNotificationPermissionReader { true },
            fullScreenAccess = AlarmFullScreenAccess(context),
        )
        try {
            assertEquals(
                AlarmDiagnosticScheduleResult.SCHEDULED,
                scheduler.scheduleOneMinute(AlarmDiagnosticKind.SYSTEM_ALARM),
            )
            scheduler.cancel()
            assertEquals(
                AlarmDiagnosticScheduleResult.SCHEDULED,
                scheduler.scheduleOneMinute(AlarmDiagnosticKind.ADHAN),
            )
        } finally {
            scheduler.cancel()
        }
    }
}
