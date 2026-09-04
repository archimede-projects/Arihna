package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAlarmPlatformSchedulerTest {
    @Test
    fun readyCapabilitySchedulesExactlyOnce() {
        val backend = FakeBackend(granted = true)
        val scheduler = DefaultAlarmPlatformScheduler(backend)
        val occurrence = occurrence()

        val result = scheduler.scheduleExact(occurrence)

        assertEquals(ExactAlarmCapability.READY, scheduler.capability())
        assertEquals(listOf(occurrence), backend.scheduled)
        assertEquals(AlarmScheduleResult.Scheduled(occurrence), result)
    }

    @Test
    fun deniedExactAccessNeverCallsScheduleAndHasNoInexactFallback() {
        val backend = FakeBackend(granted = false)
        val scheduler = DefaultAlarmPlatformScheduler(backend)

        val result = scheduler.scheduleExact(occurrence())

        assertEquals(ExactAlarmCapability.NEEDS_EXACT_ALARM_ACCESS, scheduler.capability())
        assertSame(AlarmScheduleResult.NeedsExactAlarmAccess, result)
        assertTrue(backend.scheduled.isEmpty())
    }

    @Test
    fun cancelDelegatesStableAlarmId() {
        val backend = FakeBackend(granted = true)
        val scheduler = DefaultAlarmPlatformScheduler(backend)

        scheduler.cancel("alarm:work")
        scheduler.cancel("alarm:work")

        assertEquals(listOf("alarm:work", "alarm:work"), backend.cancelled)
    }

    private fun occurrence() = AlarmOccurrence(
        alarmId = "alarm:work",
        ruleRevision = 3,
        triggerAt = Instant.parse("2026-09-05T06:00:00Z"),
        occurrenceToken = "abc",
    )

    private class FakeBackend(
        private val granted: Boolean,
    ) : ExactAlarmBackend {
        val scheduled = mutableListOf<AlarmOccurrence>()
        val cancelled = mutableListOf<String>()

        override fun hasExactAlarmAccess(): Boolean = granted

        override fun scheduleRtcWakeupExact(occurrence: AlarmOccurrence) {
            scheduled += occurrence
        }

        override fun cancel(alarmId: String) {
            cancelled += alarmId
        }
    }
}
