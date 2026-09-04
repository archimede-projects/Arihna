package com.archimedeprojects.arihna.feature.alarms.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmOccurrenceResolverTest {
    @Test
    fun oneShotCustomUsesNextDeviceLocalOccurrence() {
        val rule = customRule(time = LocalTime.of(7, 30))
        val zone = ZoneId.of("Europe/Rome")

        val sameDay = AlarmOccurrenceResolver.resolveCustom(
            rule = rule,
            now = Instant.parse("2026-09-04T04:00:00Z"),
            deviceZoneId = zone,
        )
        val nextDay = AlarmOccurrenceResolver.resolveCustom(
            rule = rule,
            now = Instant.parse("2026-09-04T06:00:00Z"),
            deviceZoneId = zone,
        )

        assertEquals(Instant.parse("2026-09-04T05:30:00Z"), sameDay?.triggerAt)
        assertEquals(Instant.parse("2026-09-05T05:30:00Z"), nextDay?.triggerAt)
    }

    @Test
    fun weekdayRecurrenceChoosesNextSelectedWeekday() {
        val rule = customRule(
            time = LocalTime.of(8, 0),
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )

        val occurrence = AlarmOccurrenceResolver.resolveCustom(
            rule,
            now = Instant.parse("2026-09-04T12:00:00Z"), // Friday
            deviceZoneId = ZoneId.of("UTC"),
        )

        assertEquals(Instant.parse("2026-09-07T08:00:00Z"), occurrence?.triggerAt)
    }

    @Test
    fun deviceZoneChangeRecomputesWallClockWithoutChangingRule() {
        val rule = customRule(time = LocalTime.of(9, 0))
        val now = Instant.parse("2026-09-04T00:00:00Z")

        val rome = AlarmOccurrenceResolver.resolveCustom(rule, now, ZoneId.of("Europe/Rome"))
        val tokyo = AlarmOccurrenceResolver.resolveCustom(rule, now, ZoneId.of("Asia/Tokyo"))

        assertEquals(LocalTime.of(9, 0), (rule.definition as AlarmDefinition.Custom).localTime)
        assertEquals(Instant.parse("2026-09-04T07:00:00Z"), rome?.triggerAt)
        assertEquals(Instant.parse("2026-09-05T00:00:00Z"), tokyo?.triggerAt)
        assertNotEquals(rome?.triggerAt, tokyo?.triggerAt)
    }

    @Test
    fun springForwardGapUsesFirstValidInstantAfterGap() {
        val rule = customRule(time = LocalTime.of(2, 30))
        val occurrence = AlarmOccurrenceResolver.resolveCustom(
            rule,
            now = Instant.parse("2026-03-28T12:00:00Z"),
            deviceZoneId = ZoneId.of("Europe/Rome"),
        )

        assertEquals(Instant.parse("2026-03-29T01:00:00Z"), occurrence?.triggerAt)
    }

    @Test
    fun fallBackOverlapUsesEarlierOffsetAndFiresOnce() {
        val rule = customRule(time = LocalTime.of(2, 30))
        val occurrence = AlarmOccurrenceResolver.resolveCustom(
            rule,
            now = Instant.parse("2026-10-24T12:00:00Z"),
            deviceZoneId = ZoneId.of("Europe/Rome"),
        )

        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), occurrence?.triggerAt)
    }

    @Test
    fun prayerOffsetCanCrossCivilMidnight() {
        val rule = AlarmRule(
            alarmId = "isha-late",
            revision = 4,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.PrayerLinked(
                prayer = AlarmPrayer.ISHA,
                offsetMinutes = 90,
            ),
        )
        val occurrence = AlarmOccurrenceResolver.resolvePrayer(
            rule = rule,
            prayerScheduleInstants = listOf(Instant.parse("2026-09-04T22:45:00Z")),
            now = Instant.parse("2026-09-04T20:00:00Z"),
        )

        assertEquals(Instant.parse("2026-09-05T00:15:00Z"), occurrence?.triggerAt)
    }

    @Test
    fun prayerResolverSkipsPastShiftedCandidates() {
        val rule = AlarmRule(
            alarmId = "fajr-before",
            revision = 1,
            enabled = true,
            soundProfile = AlarmSoundProfile.SILENT,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.FAJR, -20),
        )

        val occurrence = AlarmOccurrenceResolver.resolvePrayer(
            rule,
            prayerScheduleInstants = listOf(
                Instant.parse("2026-09-04T04:00:00Z"),
                Instant.parse("2026-09-05T04:01:00Z"),
            ),
            now = Instant.parse("2026-09-04T04:00:00Z"),
        )

        assertEquals(Instant.parse("2026-09-05T03:41:00Z"), occurrence?.triggerAt)
    }

    @Test
    fun disabledRuleNeverResolves() {
        val disabled = customRule(LocalTime.NOON).copy(enabled = false)
        assertNull(
            AlarmOccurrenceResolver.resolveCustom(
                disabled,
                Instant.parse("2026-09-04T00:00:00Z"),
                ZoneId.of("UTC"),
            ),
        )
    }

    @Test
    fun occurrenceTokenIsStableForSameOccurrenceAndChangesWithRevision() {
        val instant = Instant.parse("2026-09-04T08:00:00Z")
        val first = AlarmOccurrenceTokens.create("alarm-1", 7, instant)
        val again = AlarmOccurrenceTokens.create("alarm-1", 7, instant)
        val newer = AlarmOccurrenceTokens.create("alarm-1", 8, instant)

        assertEquals(first, again)
        assertNotEquals(first, newer)
        assertEquals(64, first.length)
        assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
    }

    private fun customRule(
        time: LocalTime,
        weekdays: Set<DayOfWeek> = emptySet(),
    ) = AlarmRule(
        alarmId = "custom",
        revision = 1,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.Custom(
            label = "Sveglia",
            localTime = time,
            weekdays = weekdays,
        ),
    )
}
