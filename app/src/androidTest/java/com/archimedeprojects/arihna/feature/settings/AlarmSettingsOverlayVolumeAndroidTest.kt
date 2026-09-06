package com.archimedeprojects.arihna.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlarmSettingsOverlayVolumeAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsHideCapabilityRowsWhileKeepingRealAlarmVolume() {
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
                    alarmSettings = AlarmSettingsPresentation(
                        notificationReady = true,
                        exactReady = true,
                        fullScreenReady = true,
                        overlayReady = false,
                        alarmVolumeState = AlarmVolumeState(current = 8, min = 0, max = 15),
                    ),
                )
            }
        }

        assertTrue(composeRule.onAllNodesWithText("Popup sveglia").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Notifiche").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Allarmi esatti").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Schermo intero").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Volume sveglia").assertIsDisplayed()
        composeRule.onNodeWithText("53%").assertIsDisplayed()
        composeRule.onNodeWithText("Volume globale delle sveglie del telefono").assertIsDisplayed()
    }
}
