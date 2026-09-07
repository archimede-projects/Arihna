package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuranFullscreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

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
        composeRule.onNodeWithTag("quran-fullscreen-reader").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-fullscreen-close").assertIsDisplayed()
        composeRule.runOnIdle { assertTrue(immersive) }

        composeRule.onNodeWithTag("quran-fullscreen-close").performClick()
        // Returning to the normal reader proves the fullscreen branch was removed.
        composeRule.onNodeWithTag("quran-reading-fullscreen").assertIsDisplayed()
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
}
