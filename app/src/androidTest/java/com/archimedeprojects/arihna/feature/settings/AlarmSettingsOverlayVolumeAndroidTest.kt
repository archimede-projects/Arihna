package com.archimedeprojects.arihna.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlarmSettingsOverlayVolumeAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsHideCapabilityRowsAndGlobalAlarmVolume() {
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
                    ),
                )
            }
        }

        assertTrue(composeRule.onAllNodesWithText("Popup sveglia").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Notifiche").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Allarmi esatti").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Schermo intero").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Volume sveglia").fetchSemanticsNodes().isEmpty())
        assertTrue(
            composeRule.onAllNodesWithText("Volume globale delle sveglie del telefono")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }
}
