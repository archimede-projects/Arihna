package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuranPostStableHardeningAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun waitForExists(tag: String, timeoutMillis: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForMissing(tag: String, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun longSessionHistoriesStayBoundedSeparatedAndClamped() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        repeat(64) { index ->
            QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, (index * 17) % 604)
            QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.WARSH, (index * 19) % 604)
            QuranReadingPrefs.recordVisitedTajwidPage(context, (index * 23) % 604)
        }

        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, 9_999)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.WARSH, -9_999)
        QuranReadingPrefs.recordVisitedTajwidPage(context, 9_999)

        assertEquals(603, QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS))
        assertEquals(0, QuranReadingPrefs.lastPage(context, QuranRiwaya.WARSH))
        assertEquals(603, QuranReadingPrefs.lastTajwidPage(context))

        val hafsRecent = QuranReadingPrefs.recentPages(context, QuranRiwaya.HAFS)
        val warshRecent = QuranReadingPrefs.recentPages(context, QuranRiwaya.WARSH)
        val tajwidRecent = QuranReadingPrefs.recentTajwidPages(context)

        listOf(hafsRecent, warshRecent, tajwidRecent).forEach { recent ->
            assertEquals(8, recent.size)
            assertEquals(recent.size, recent.distinct().size)
            assertTrue(recent.all { it in 0..603 })
        }
        assertEquals(603, hafsRecent.first())
        assertEquals(0, warshRecent.first())
        assertEquals(603, tajwidRecent.first())
        assertFalse(hafsRecent == warshRecent)
    }

    @Test
    fun repeatedModeSwitchingRestoresIndependentPagesAndBookmarks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val hafsPage = 10
        val warshPage = 20
        val tajwidPage = 30

        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, hafsPage)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.WARSH, warshPage)
        QuranReadingPrefs.recordVisitedTajwidPage(context, tajwidPage)
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)

        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.HAFS, hafsPage, true)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.HAFS, warshPage, false)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.WARSH, warshPage, true)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.WARSH, hafsPage, false)
        QuranReadingPrefs.setTajwidPageBookmarked(context, tajwidPage, true)
        QuranReadingPrefs.setTajwidPageBookmarked(context, hafsPage, false)

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-mushaf-riwaya-hafs")
        waitForExists("quran-mushaf-page-${hafsPage + 1}")

        repeat(5) {
            composeRule.onNodeWithTag("quran-mode-tajwid").performClick()
            waitForExists("quran-tajwid-beta-reader")
            waitForExists("quran-tajwid-page-${tajwidPage + 1}")

            composeRule.onNodeWithTag("quran-mode-warsh").performClick()
            waitForExists("quran-mushaf-riwaya-warsh")
            waitForExists("quran-mushaf-page-${warshPage + 1}")

            composeRule.onNodeWithTag("quran-mode-hafs").performClick()
            waitForExists("quran-mushaf-riwaya-hafs")
            waitForExists("quran-mushaf-page-${hafsPage + 1}")
        }

        composeRule.runOnIdle {
            assertEquals(QuranReadingMode.HAFS_UTHMANI, QuranReadingPrefs.mode(context))
            assertEquals(hafsPage, QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS))
            assertEquals(warshPage, QuranReadingPrefs.lastPage(context, QuranRiwaya.WARSH))
            assertEquals(tajwidPage, QuranReadingPrefs.lastTajwidPage(context))

            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, hafsPage))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, warshPage))
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.WARSH, warshPage))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.WARSH, hafsPage))
            assertTrue(QuranReadingPrefs.isTajwidPageBookmarked(context, tajwidPage))
            assertFalse(QuranReadingPrefs.isTajwidPageBookmarked(context, hafsPage))
        }
    }

    @Test
    fun repeatedFullscreenCyclesResetImmersiveAndKeepPerModePageState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val hafsPage = 41
        val warshPage = 52
        val tajwidPage = 63
        var immersive = false

        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, hafsPage)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.WARSH, warshPage)
        QuranReadingPrefs.recordVisitedTajwidPage(context, tajwidPage)
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)

        composeRule.setContent {
            QuranPlaceholderScreen(
                contentPadding = PaddingValues(0.dp),
                onImmersiveChanged = { immersive = it },
            )
        }

        waitForExists("quran-mushaf-page-${hafsPage + 1}")
        repeat(2) {
            composeRule.onNodeWithTag("quran-reading-fullscreen").performClick()
            waitForExists("quran-fullscreen-reader")
            composeRule.runOnIdle { assertTrue(immersive) }
            composeRule.onNodeWithTag("quran-fullscreen-close").performClick()
            waitForMissing("quran-fullscreen-reader")
            waitForExists("quran-mushaf-page-${hafsPage + 1}")
            composeRule.runOnIdle { assertFalse(immersive) }
        }

        composeRule.onNodeWithTag("quran-mode-tajwid").performClick()
        waitForExists("quran-tajwid-page-${tajwidPage + 1}")
        repeat(2) {
            composeRule.onNodeWithTag("quran-tajwid-fullscreen").performClick()
            waitForExists("quran-tajwid-fullscreen-reader")
            composeRule.runOnIdle { assertTrue(immersive) }
            composeRule.onNodeWithTag("quran-tajwid-fullscreen-close").performClick()
            waitForMissing("quran-tajwid-fullscreen-reader")
            waitForExists("quran-tajwid-page-${tajwidPage + 1}")
            composeRule.runOnIdle { assertFalse(immersive) }
        }

        composeRule.onNodeWithTag("quran-mode-warsh").performClick()
        waitForExists("quran-mushaf-page-${warshPage + 1}")
        repeat(2) {
            composeRule.onNodeWithTag("quran-reading-fullscreen").performClick()
            waitForExists("quran-fullscreen-reader")
            composeRule.runOnIdle { assertTrue(immersive) }
            composeRule.onNodeWithTag("quran-fullscreen-close").performClick()
            waitForMissing("quran-fullscreen-reader")
            waitForExists("quran-mushaf-page-${warshPage + 1}")
            composeRule.runOnIdle { assertFalse(immersive) }
        }

        composeRule.runOnIdle {
            assertEquals(hafsPage, QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS))
            assertEquals(warshPage, QuranReadingPrefs.lastPage(context, QuranRiwaya.WARSH))
            assertEquals(tajwidPage, QuranReadingPrefs.lastTajwidPage(context))
            assertFalse(immersive)
        }
    }
}
