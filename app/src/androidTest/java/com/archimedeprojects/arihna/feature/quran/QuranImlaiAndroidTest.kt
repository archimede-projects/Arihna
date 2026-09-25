package com.archimedeprojects.arihna.feature.quran

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuranImlaiAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun waitForExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun imlaiCorpusIsCompleteAndStructurallyAlignedWithPinnedHafs() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uthmani = QuranCorpus.load(context)
        val imlai = QuranCorpus.loadImlai(context)

        assertEquals(6236, imlai.ayahs.size)
        assertEquals(114, imlai.surahNumbers.size)
        assertEquals(
            uthmani.ayahs.map { it.surah to it.ayah },
            imlai.ayahs.map { it.surah to it.ayah },
        )
        assertEquals(6236, imlai.ayahs.map { it.surah to it.ayah }.toSet().size)
        assertEquals(1 to 1, imlai.ayahs.first().let { it.surah to it.ayah })
        assertEquals(114 to 6, imlai.ayahs.last().let { it.surah to it.ayah })
        assertTrue(imlai.ayahs.first().text.isNotBlank())
        assertTrue(imlai.ayahs.first().text.any { it.code in 0x064B..0x065F })
    }

    @Test
    fun legacyEasyPreferenceMigratesToHafsImlai() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("arihna_quran_reader", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("quran_reading_mode_v1", "EASY")
            .remove("quran_writing_style_v1")
            .commit()

        assertEquals(QuranReadingMode.HAFS_UTHMANI, QuranReadingPrefs.mode(context))
        assertEquals(QuranWritingStyle.IMLAI, QuranReadingPrefs.writingStyle(context))
        assertEquals(
            QuranReadingMode.HAFS_UTHMANI.name,
            prefs.getString("quran_reading_mode_v1", null),
        )
    }

    @Test
    fun imlaiKeepsHafsPageJumpBookmarkFullscreenAndSwitchState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        QuranReadingPrefs.setWritingStyle(context, QuranWritingStyle.IMLAI)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, 5)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.HAFS, 5, false)

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }

        waitForExists("quran-imlai-reader")
        waitForExists("quran-imlai-page-6")
        composeRule.onNodeWithTag("quran-writing-imlai").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-imlai-page-context").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-imlai-source-context").assertIsDisplayed()

        composeRule.onNodeWithTag("quran-imlai-bookmark").performClick()
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 5))
        }

        composeRule.onNodeWithTag("quran-surah-selector").performClick()
        waitForExists("quran-page-jump-input")
        composeRule.onNodeWithTag("quran-page-jump-input").performTextInput("321")
        composeRule.onNodeWithTag("quran-page-jump-go").performClick()
        waitForExists("quran-imlai-page-321")
        composeRule.runOnIdle {
            assertEquals(320, QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS))
        }

        composeRule.onNodeWithTag("quran-imlai-fullscreen").performClick()
        waitForExists("quran-imlai-fullscreen-reader")
        composeRule.onNodeWithTag("quran-imlai-fullscreen-page-context").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-imlai-fullscreen-close").performClick()
        waitForExists("quran-imlai-reader")

        composeRule.onNodeWithTag("quran-mode-tajwid").performClick()
        waitForExists("quran-tajwid-beta-reader")
        composeRule.onNodeWithTag("quran-mode-warsh").performClick()
        waitForExists("quran-mushaf-riwaya-warsh")
        composeRule.onNodeWithTag("quran-writing-imlai").performClick()
        waitForExists("quran-imlai-page-321")

        composeRule.runOnIdle {
            assertEquals(QuranReadingMode.HAFS_UTHMANI, QuranReadingPrefs.mode(context))
            assertEquals(QuranWritingStyle.IMLAI, QuranReadingPrefs.writingStyle(context))
            assertEquals(320, QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS))
            assertFalse(QuranReadingPrefs.isTajwidPageBookmarked(context, 320))
        }
    }
}
