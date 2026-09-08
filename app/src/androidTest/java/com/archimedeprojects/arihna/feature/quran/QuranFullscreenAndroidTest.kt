package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
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

class QuranFullscreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun waitForDisplayed(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun readingButtonOpensAndClosesRealFullscreenReader() {
        var immersive = false
        composeRule.setContent {
            QuranPlaceholderScreen(
                contentPadding = PaddingValues(0.dp),
                onImmersiveChanged = { immersive = it },
            )
        }

        composeRule.onNodeWithTag("quran-mode-hafs").performClick()
        composeRule.onNodeWithTag("quran-reading-fullscreen").assertIsDisplayed().performClick()
        waitForDisplayed("quran-fullscreen-reader")
        waitForDisplayed("quran-fullscreen-close")
        composeRule.runOnIdle { assertTrue(immersive) }

        composeRule.onNodeWithTag("quran-fullscreen-close").performClick()
        waitForDisplayed("quran-reading-fullscreen")
        composeRule.runOnIdle { assertFalse(immersive) }
    }

    @Test
    fun indexLargeTargetsAreClickable() {
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }

        composeRule.onNodeWithTag("quran-surah-selector").performClick()
        composeRule.onNodeWithTag("quran-explorer-juz").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-explorer-hizb").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-shortcut-bookmarks").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-shortcut-recent").assertIsDisplayed().performClick()
    }

    @Test
    fun premiumStylePersistsAndFullscreenChromeToggles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setVisualStyle(context, MushafVisualStyle.CLASSIC)
        val pageNumber = QuranReadingPrefs.lastPage(context) + 1

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        composeRule.onNodeWithTag("quran-mode-hafs").performClick()
        composeRule.onNodeWithTag("quran-style-menu").assertIsDisplayed().performClick()
        waitForDisplayed("quran-style-clean")
        composeRule.onNodeWithTag("quran-style-clean").performClick()
        composeRule.runOnIdle {
            assertEquals(MushafVisualStyle.CLEAN, QuranReadingPrefs.visualStyle(context))
        }

        composeRule.onNodeWithTag("quran-reading-fullscreen").performClick()
        waitForDisplayed("quran-fullscreen-chrome")
        waitForDisplayed("quran-fullscreen-page-context")
        waitForDisplayed("quran-fullscreen-bottom-context")

        composeRule.onNodeWithTag("quran-zoomable-page-$pageNumber").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("quran-fullscreen-chrome").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithTag("quran-fullscreen-chrome").assertCountEquals(0)
        composeRule.onNodeWithTag("quran-zoomable-page-$pageNumber").performClick()
        waitForDisplayed("quran-fullscreen-chrome")
    }
}
