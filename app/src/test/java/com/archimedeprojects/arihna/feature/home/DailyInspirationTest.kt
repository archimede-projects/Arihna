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
        assertTrue(
            curatedDailyInspirations.all {
                it.reference.startsWith("Corano ") || it.reference.startsWith("Sahih al-Bukhari ")
            },
        )
        assertTrue(curatedDailyInspirations.all { it.text.any { character -> character in '\u0600'..'\u06FF' } })
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
    fun positiveDailyActionIsStableArabicAndNeverPretendsToBeScripture() {
        val date = LocalDate.of(2026, 9, 7)
        val first = dailyActionFor(date)
        val second = dailyActionFor(date)
        assertEquals(first, second)
        assertTrue(curatedDailyActions.isNotEmpty())
        assertTrue(curatedDailyActions.all { it.arabic.any { character -> character in '\u0600'..'\u06FF' } })
        assertTrue(curatedDailyActions.all { it.italian.isNotBlank() })
        assertTrue(curatedDailyActions.none { it.italian.startsWith("Corano ") || it.italian.startsWith("Sahih") })
    }
}
