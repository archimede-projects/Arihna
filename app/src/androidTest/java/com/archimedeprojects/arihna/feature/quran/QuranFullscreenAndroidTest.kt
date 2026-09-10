package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
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
        composeRule.waitUntil(timeoutMillis = 10_000) {
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
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        var immersive = false
        composeRule.setContent {
            QuranPlaceholderScreen(
                contentPadding = PaddingValues(0.dp),
                onImmersiveChanged = { immersive = it },
            )
        }

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
    fun readingButtonStaysTouchableWhilePremiumPreviewIsVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, 11)
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }

        waitForExists("quran-mushaf-toolbar")
        waitForExists("quran-premium-page-frame")
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val previewNodes = composeRule.onAllNodesWithTag("quran-premium-page-frame").fetchSemanticsNodes()
        assertTrue("at least one premium preview must intersect the visible root", previewNodes.any { node ->
            val bounds = node.boundsInRoot
            bounds.left < rootBounds.right && bounds.right > rootBounds.left &&
                bounds.top < rootBounds.bottom && bounds.bottom > rootBounds.top
        })
        composeRule.onNodeWithTag("quran-reading-fullscreen")
            .assertIsDisplayed()
            .performTouchInput { click() }
        waitForExists("quran-fullscreen-reader")
    }

    @Test
    fun fullscreenTopBarHonorsInjectedSafeInset() {
        composeRule.setContent {
            FullscreenMushafReader(
                startPage = 0,
                style = MushafVisualStyle.CLASSIC,
                riwaya = QuranRiwaya.HAFS,
                onDismiss = {},
                onPageChanged = {},
                topBarInsets = WindowInsets(top = 72.dp),
            )
        }
        waitForExists("quran-fullscreen-chrome")
        val density = composeRule.density
        val top = composeRule.onNodeWithTag("quran-fullscreen-chrome").fetchSemanticsNode().boundsInRoot.top
        val expected = with(density) { 72.dp.toPx() }
        assertTrue("fullscreen chrome top=$top expected >= $expected", top >= expected)
    }

    @Test
    fun warshSwitchKeepsBookmarksSeparatedByRiwaya() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, 0)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.WARSH, 0)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.HAFS, 0, false)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.WARSH, 0, false)

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-mushaf-riwaya-hafs")
        composeRule.onNodeWithTag("quran-bookmark-toggle").performClick()
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 0))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.WARSH, 0))
        }

        composeRule.onNodeWithTag("quran-mode-warsh").performClick()
        waitForExists("quran-mushaf-riwaya-warsh")
        waitForExists("quran-mushaf-page-1")
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 0))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.WARSH, 0))
        }

        composeRule.onNodeWithTag("quran-bookmark-toggle").performClick()
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 0))
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.WARSH, 0))
        }
    }

    @Test
    fun eachRiwayaRestoresOwnLastPageAndRecentHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, 11)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.WARSH, 22)
        assertEquals(11, QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS))
        assertEquals(22, QuranReadingPrefs.lastPage(context, QuranRiwaya.WARSH))
        assertEquals(11, QuranReadingPrefs.recentPages(context, QuranRiwaya.HAFS).first())
        assertEquals(22, QuranReadingPrefs.recentPages(context, QuranRiwaya.WARSH).first())

        QuranReadingPrefs.setMode(context, QuranReadingMode.WARSH_NAFI)
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-mushaf-riwaya-warsh")
        waitForExists("quran-mushaf-page-23")
        composeRule.onNodeWithTag("quran-mode-hafs").performClick()
        waitForExists("quran-mushaf-riwaya-hafs")
        waitForExists("quran-mushaf-page-12")
    }

    @Test
    fun indexLargeTargetsAreClickable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }

        composeRule.onNodeWithTag("quran-surah-selector").performClick()
        composeRule.onNodeWithTag("quran-explorer-juz").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-explorer-hizb").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-shortcut-bookmarks").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-shortcut-recent").assertIsDisplayed().performClick()
    }

    @Test
    fun warshIndexDoesNotPretendHafsJuzHizbBoundaries() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.WARSH_NAFI)
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        composeRule.onNodeWithTag("quran-surah-selector").performClick()
        waitForExists("quran-warsh-boundaries-unavailable")
        composeRule.onNodeWithTag("quran-warsh-boundaries-unavailable").assertIsDisplayed()
    }

    @Test
    fun premiumStylePersistsAndFullscreenChromeToggles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        QuranReadingPrefs.setVisualStyle(context, MushafVisualStyle.CLASSIC)
        val pageNumber = QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS) + 1

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
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


    @Test
    fun tajwidBetaUsesAuthoritativePagePagerInlineLayoutAndSeparatePageBookmarks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_TAJWID)
        QuranReadingPrefs.recordVisitedTajwidPage(context, 1)
        QuranReadingPrefs.setTajwidPageBookmarked(context, 1, false)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.HAFS, 1, false)

        val starts = MushafRepository.hafsPageStarts(context)
        assertEquals(604, starts.size)
        assertEquals(1 to 1, starts[0])
        assertEquals(2 to 1, starts[1])
        assertEquals(1, MushafRepository.hafsPageIndexForAyah(context, 2, 1))

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-tajwid-beta-reader")
        waitForExists("quran-tajwid-rtl-pager")
        waitForExists("quran-tajwid-page-2")
        composeRule.onNodeWithTag("quran-tajwid-page-context").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-tajwid-beta-disclaimer").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-tajwid-page-surface").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-mode-tabs").assertIsDisplayed()
        listOf("quran-mode-hafs", "quran-mode-tajwid", "quran-mode-warsh", "quran-mode-easy").forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        assertEquals(0, composeRule.onAllNodesWithTag("quran-tajwid-ayah-2-1").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("quran-tajwid-legend-toggle").performClick()
        waitForExists("quran-tajwid-legend")
        composeRule.onNodeWithTag("quran-tajwid-bookmark").performClick()
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isTajwidPageBookmarked(context, 1))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 1))
        }
    }

    @Test
    fun tajwidFullscreenIsImmersiveAndKeepsZoomControls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_TAJWID)
        QuranReadingPrefs.recordVisitedTajwidPage(context, 1)
        var immersive = false
        composeRule.setContent {
            QuranPlaceholderScreen(
                contentPadding = PaddingValues(0.dp),
                onImmersiveChanged = { immersive = it },
            )
        }
        waitForExists("quran-tajwid-fullscreen")
        composeRule.onNodeWithTag("quran-tajwid-fullscreen").performClick()
        waitForExists("quran-tajwid-fullscreen-reader")
        composeRule.onNodeWithTag("quran-tajwid-fullscreen-disclaimer").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-tajwid-fullscreen-zoom-in").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-tajwid-fullscreen-zoom-out").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(immersive) }
        composeRule.onNodeWithTag("quran-tajwid-fullscreen-close").performClick()
        waitForMissing("quran-tajwid-fullscreen-reader")
        composeRule.runOnIdle { assertFalse(immersive) }
    }

    @Test
    fun tajwidAyahMarkerIsInlineWithTheAnnotatedAyahText() {
        val ayah = QuranAyah(2, 1, "الم")
        val decorated = buildTajwidAnnotatedPageText(listOf(ayah))
        assertTrue(decorated.text.contains(ayah.text + " ۝١"))
        assertTrue(decorated.text.startsWith(ayah.text))
    }

}
