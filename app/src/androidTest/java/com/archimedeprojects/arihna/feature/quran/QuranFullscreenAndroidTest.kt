package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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

    private fun waitForExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForMissing(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
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
        waitForExists("quran-reading-fullscreen")
        composeRule.onNodeWithTag("quran-reading-fullscreen")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForExists("quran-fullscreen-reader")
        waitForExists("quran-fullscreen-close")
        composeRule.runOnIdle { assertTrue(immersive) }

        composeRule.onNodeWithTag("quran-fullscreen-close")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForMissing("quran-fullscreen-reader")
        waitForExists("quran-reading-fullscreen")
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
        waitForExists("quran-style-menu")
        composeRule.onNodeWithTag("quran-style-menu")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForExists("quran-style-clean")
        composeRule.onNodeWithTag("quran-style-clean")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle {
            assertEquals(MushafVisualStyle.CLEAN, QuranReadingPrefs.visualStyle(context))
        }

        waitForExists("quran-reading-fullscreen")
        composeRule.onNodeWithTag("quran-reading-fullscreen")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForExists("quran-fullscreen-reader")
        waitForExists("quran-fullscreen-chrome")
        waitForExists("quran-fullscreen-page-context")
        waitForExists("quran-fullscreen-bottom-context")

        waitForExists("quran-zoomable-page-$pageNumber")
        composeRule.onNodeWithTag("quran-zoomable-page-$pageNumber")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForMissing("quran-fullscreen-chrome")
        composeRule.onNodeWithTag("quran-zoomable-page-$pageNumber")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForExists("quran-fullscreen-chrome")
    }
}
