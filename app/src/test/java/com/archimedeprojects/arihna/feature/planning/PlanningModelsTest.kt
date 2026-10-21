package com.archimedeprojects.arihna.feature.planning

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningModelsTest {
    @Test fun december23IsScheduledMonthsAheadNotTheNextWednesday() {
        val date = LocalDate.of(2026, 12, 23)
        val time = LocalTime.of(8, 15)
        val now = Instant.parse("2026-10-21T07:00:00Z")
        assertTrue(PlanningTime.validFuture(date, time, now, ZoneId.of("Europe/Rome")))
        assertEquals(Instant.parse("2026-12-23T07:15:00Z"), PlanningTime.instant(date, time, ZoneId.of("Europe/Rome")))
    }

    @Test fun pastCalendarDateIsRejectedWithoutSilentFallback() {
        assertFalse(PlanningTime.validFuture(
            LocalDate.of(2026, 10, 20), LocalTime.of(8, 0),
            Instant.parse("2026-10-21T07:00:00Z"), ZoneId.of("Europe/Rome"),
        ))
    }

    @Test fun timezoneChangePreservesCivilDateAndTime() {
        val date = LocalDate.of(2026, 12, 23)
        val time = LocalTime.of(10, 0)
        val rome = PlanningTime.instant(date, time, ZoneId.of("Europe/Rome"))
        val tokyo = PlanningTime.instant(date, time, ZoneId.of("Asia/Tokyo"))
        assertEquals(Instant.parse("2026-12-23T09:00:00Z"), rome)
        assertEquals(Instant.parse("2026-12-23T01:00:00Z"), tokyo)
    }

    @Test fun dstGapOverlapHaveDeterministicPolicy() {
        val rome = ZoneId.of("Europe/Rome")
        assertEquals(
            Instant.parse("2026-03-29T01:00:00Z"),
            PlanningTime.instant(LocalDate.of(2026, 3, 29), LocalTime.of(2, 30), rome),
        )
        assertEquals(
            Instant.parse("2026-10-25T00:30:00Z"),
            PlanningTime.instant(LocalDate.of(2026, 10, 25), LocalTime.of(2, 30), rome),
        )
    }

    @Test fun codecRoundTripKeepsAllTypesAndSpecialUnicode() {
        val items = listOf(
            PlanningItem(id = "planning-1", revision = 1, type = PlanningType.TASK, title = "Da fare | عمل", date = null),
            PlanningItem(id = "planning-2", revision = 3, type = PlanningType.REMINDER, title = "Ricorda", note = "Telefono\nmedico", date = LocalDate.of(2026, 12, 23), time = LocalTime.of(10, 0)),
            PlanningItem(
                id = "planning-3", revision = 2, type = PlanningType.ALARM, title = "Viaggio",
                date = LocalDate.of(2026, 12, 23), time = LocalTime.of(6, 30),
                sound = AlarmSoundProfile.SYSTEM_DEFAULT,
                ringtoneUri = "arihna://alarm/classic-vintage", ringtoneTitle = "Sveglia classica Arihna",
                volume = 37, delivered = true,
            ),
        )
        assertEquals(items, PlanningCodec.decode(PlanningCodec.encode(items)))
        assertEquals(emptyList<PlanningItem>(), PlanningCodec.decode("invalid data"))
    }

    @Test fun completionStatusCannotBeConfusedWithPending() {
        val x = PlanningItem(id = "planning-7", revision = 1, type = PlanningType.ALARM, title = "Test", date = LocalDate.of(2026, 12, 23), time = LocalTime.NOON)
        assertTrue(x.pending)
        assertFalse(x.copy(delivered = true).pending)
        assertFalse(x.copy(complete = true).pending)
    }
}
