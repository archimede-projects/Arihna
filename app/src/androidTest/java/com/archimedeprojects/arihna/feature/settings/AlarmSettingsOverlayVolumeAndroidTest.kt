package com.archimedeprojects.arihna.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeState
import org.junit.Rule
import org.junit.Test

class AlarmSettingsOverlayVolumeAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsExposeOverlaySpecialAccessAndRealAlarmVolume() {
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

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Popup sveglia"))
        composeRule.onNodeWithText("Popup sveglia").assertIsDisplayed()
        composeRule.onNodeWithText("Da autorizzare").assertIsDisplayed()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Volume sveglia"))
        composeRule.onNodeWithText("Volume sveglia").assertIsDisplayed()
        composeRule.onNodeWithText("53%").assertIsDisplayed()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Volume globale delle sveglie del telefono"))
        composeRule.onNodeWithText("Volume globale delle sveglie del telefono")
            .assertIsDisplayed()
    }
}
