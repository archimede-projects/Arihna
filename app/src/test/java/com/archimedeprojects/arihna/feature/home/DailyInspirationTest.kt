package com.archimedeprojects.arihna.feature.home

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyInspirationTest {
    @Test
    fun selectionIsStableForSameCivilDateAndReferencesAreExplicit() {
        val date = LocalDate.of(2026, 9, 6)
        assertEquals(dailyInspirationFor(date), dailyInspirationFor(date))
        assertTrue(curatedDailyInspirations.all { it.reference.isNotBlank() })
        assertTrue(curatedDailyInspirations.all { it.text.any { character -> character in '\u0600'..'\u06FF' } })

        val originalArihna = curatedDailyInspirations.filter { it.reference == ARIHNA_REMINDER_REFERENCE }
        assertTrue(originalArihna.isNotEmpty())
        assertTrue(originalArihna.all { it.kind == "تذكير أريهنا" })

        val sourced = curatedDailyInspirations.filterNot { it.reference == ARIHNA_REMINDER_REFERENCE }
        assertTrue(sourced.isNotEmpty())
        assertTrue(
            sourced.all {
                it.reference.startsWith("Corano ") || it.reference.startsWith("Sahih al-Bukhari ")
            },
        )
    }

    @Test
    fun inspirationCorpusHasSixtyUniqueDaysWithoutExactRepeats() {
        assertTrue(curatedDailyInspirations.size >= 60)
        assertEquals(curatedDailyInspirations.size, curatedDailyInspirations.map { it.text }.toSet().size)

        val start = LocalDate.of(2026, 1, 1)
        val sixtyDays = (0L until 60L).map { dailyInspirationFor(start.plusDays(it)) }
        assertEquals(60, sixtyDays.map { it.text }.toSet().size)
    }

    @Test
    fun shareTextContainsDisplayedTextReferenceAndArihnaAttribution() {
        val inspiration = dailyInspirationFor(LocalDate.of(2026, 9, 6))
        val shared = inspiration.shareText()
        assertTrue(shared.contains(inspiration.text))
        assertTrue(shared.contains(inspiration.reference))
        assertTrue(shared.contains("Arihna"))
    }

    @Test
    fun positiveDailyActionsAreVariedStableArabicAndNeverPretendToBeScripture() {
        val date = LocalDate.of(2026, 9, 7)
        val first = dailyActionFor(date)
        val second = dailyActionFor(date)
        assertEquals(first, second)

        assertTrue(curatedDailyActions.size >= 30)
        assertEquals(curatedDailyActions.size, curatedDailyActions.map { it.arabic }.toSet().size)
        assertEquals(curatedDailyActions.size, curatedDailyActions.map { it.italian }.toSet().size)
        assertTrue(curatedDailyActions.all { it.arabic.any { character -> character in '\u0600'..'\u06FF' } })
        assertTrue(curatedDailyActions.all { it.italian.isNotBlank() })
        assertTrue(curatedDailyActions.none { it.italian.startsWith("Corano ") || it.italian.startsWith("Sahih") })
    }
}
