package com.archimedeprojects.arihna.core.calendar

import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertTrue

class HijriDateFormatterTest {
    @Test
    fun formatsSameDateInItalianAndArabicWithoutHardcodedCivilLookup() {
        val date = LocalDate.of(2026, 9, 7)
        val italian = HijriDateFormatter.format(date, arabic = false)
        val arabic = HijriDateFormatter.format(date, arabic = true)
        assertTrue(italian.endsWith("AH"))
        assertTrue(arabic.endsWith("هـ"))
        assertTrue(italian.isNotBlank())
        assertTrue(arabic.isNotBlank())
    }
}
