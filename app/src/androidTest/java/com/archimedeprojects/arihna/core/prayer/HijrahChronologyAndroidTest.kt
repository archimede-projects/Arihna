package com.archimedeprojects.arihna.core.prayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HijrahChronologyAndroidTest {
    @Test
    fun ummAlQuraChronologyIsAvailableAndIdentified() {
        val chronology = HijrahChronology.INSTANCE

        assertEquals("Hijrah-umalqura", chronology.id)
        assertTrue(chronology.calendarType.contains("islamic", ignoreCase = true))
    }

    @Test
    fun knownGregorianDatesResolveToExpectedIslamicMonths() {
        assertHijrahMonth(LocalDate.of(2024, 2, 20), expectedMonth = 8) // Sha'ban 1445
        assertHijrahMonth(LocalDate.of(2024, 3, 20), expectedMonth = 9) // Ramadan 1445
        assertHijrahMonth(LocalDate.of(2024, 4, 20), expectedMonth = 10) // Shawwal 1445
        assertHijrahMonth(LocalDate.of(2016, 6, 15), expectedMonth = 9) // Ramadan 1437
    }

    private fun assertHijrahMonth(date: LocalDate, expectedMonth: Int) {
        val hijrahDate = HijrahChronology.INSTANCE.date(date)
        assertEquals(
            "Unexpected Hijri month for $date: $hijrahDate",
            expectedMonth,
            hijrahDate.get(ChronoField.MONTH_OF_YEAR),
        )
    }
}
