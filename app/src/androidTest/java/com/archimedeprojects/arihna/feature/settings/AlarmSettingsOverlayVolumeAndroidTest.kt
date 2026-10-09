package com.archimedeprojects.arihna.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlarmSettingsOverlayVolumeAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsDoNotExposeGlobalAlarmVolumeControl() {
        composeRule.setContent {
            ArihnaTheme {
                LocationSettingsScreen(
                    contentPadding = PaddingValues(0.dp),
                    uiState = LocationSettingsUiState(),
                    onUseDevice = {},
                    onDismissRationale = {},
                    onConfirmRationale = {},
                    onSearchQueryChanged = {},
                    onSelectCity = {},
                    onOpenAppSettings = {},
                    onOpenLocationSettings = {},
                )
            }
        }

        assertTrue(composeRule.onAllNodesWithText("Volume sveglia").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Volume globale delle sveglie del telefono").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("settings-alarm-volume-slider").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("settings-test-alarm-one-minute").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-test-adhan-one-minute").assertIsDisplayed()
    }
}
