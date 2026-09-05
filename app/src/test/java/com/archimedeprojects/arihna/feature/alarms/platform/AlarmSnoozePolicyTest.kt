package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSnoozePolicyTest {
    @Test
    fun snoozeDelayIsExactlyFiveMinutes() {
        val now = Instant.parse("2026-09-05T13:00:00Z")
        assertEquals(
            Instant.parse("2026-09-05T13:05:00Z"),
            AlarmSnoozePolicy.triggerAt(now),
        )
        assertEquals(300_000L, AlarmSnoozePolicy.DELAY_MILLIS)
    }

    @Test
    fun recurringAlarmReplaysOnlyWhileSameRevisionRemainsEnabled() {
        val payload = payload(alarmId = "custom-recurring", revision = 4L)
        val current = AlarmRule(
            alarmId = payload.alarmId,
            revision = 4L,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom(
                label = "Lavoro",
                localTime = LocalTime.of(7, 30),
                weekdays = setOf(DayOfWeek.MONDAY),
            ),
        )
        assertTrue(AlarmSnoozePolicy.canReplay(payload, current))
        assertFalse(AlarmSnoozePolicy.canReplay(payload, current.copy(revision = 5L, enabled = false)))
        assertFalse(AlarmSnoozePolicy.canReplay(payload, current.copy(revision = 5L, enabled = true)))
    }

    @Test
    fun deliveredOneShotMayReplayAfterAtomicAutoDisableButNotAfterLaterEdit() {
        val payload = payload(alarmId = "custom-one-shot", revision = 8L)
        val autoDisabled = AlarmRule(
            alarmId = payload.alarmId,
            revision = 9L,
            enabled = false,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom(
                label = "Farmaco",
                localTime = LocalTime.of(15, 3),
            ),
        )
        assertTrue(AlarmSnoozePolicy.canReplay(payload, autoDisabled))
        assertFalse(AlarmSnoozePolicy.canReplay(payload, autoDisabled.copy(revision = 10L)))
    }

    @Test
    fun diagnosticAlarmCanBeSnoozedWithoutPersistedRule() {
        assertTrue(
            AlarmSnoozePolicy.canReplay(
                payload(alarmId = "diagnostic-system-alarm", revision = 1L),
                rule = null,
            ),
        )
    }

    private fun payload(alarmId: String, revision: Long) = AlarmRingingPayload(
        alarmId = alarmId,
        ruleRevision = revision,
        title = "Test",
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        isPrayer = false,
        occurrenceToken = "token",
    )
}
