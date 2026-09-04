package com.archimedeprojects.arihna.feature.alarms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlarmsScreenAndroidTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun missingCapabilitiesAreExplicitAndNeverFabricatedAsReady() {
        setScreen(AlarmsUiState(exactAlarmReady = false, notificationReady = false))

        composeRule.onNodeWithTag("alarm-capabilities").assertIsDisplayed()
        composeRule.onNodeWithText("Notifiche: autorizzazione richiesta").assertIsDisplayed()
        composeRule.onNodeWithText("Allarmi esatti: accesso richiesto").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-notification-permission-action").assertExists()
        composeRule.onNodeWithTag("alarm-exact-access-action").assertExists()
        composeRule.onNodeWithText("Notifiche: pronte").assertDoesNotExist()
        composeRule.onNodeWithText("Allarmi esatti: pronti").assertDoesNotExist()
    }

    @Test
    fun allFivePrayerControlsArePresent() {
        setScreen(AlarmsUiState(exactAlarmReady = true, notificationReady = true))

        AlarmPrayer.entries.forEach { prayer ->
            composeRule.onNodeWithTag("alarm-prayer-${prayer.name.lowercase()}").assertExists()
            composeRule.onNodeWithTag("alarm-prayer-${prayer.name.lowercase()}-switch").assertExists()
        }
    }

    @Test
    fun customCreatorSendsLabelAndLocalTimeTextWithoutInventingSuccess() {
        var captured: Pair<String, String>? = null
        setScreen(
            state = AlarmsUiState(exactAlarmReady = true, notificationReady = true),
            onCreateCustom = { label, time -> captured = label to time },
        )

        composeRule.onNodeWithTag("alarm-custom-label").performScrollTo().performTextInput("Farmaco")
        composeRule.onNodeWithTag("alarm-custom-time").performTextInput("07:30")
        composeRule.onNodeWithTag("alarm-custom-add").performClick()

        composeRule.runOnIdle { assertEquals("Farmaco" to "07:30", captured) }
        composeRule.onNodeWithText("Sveglia salvata").assertDoesNotExist()
    }

    @Test
    fun persistedCustomRuleIsRenderedWithItsState() {
        val rule = AlarmRule(
            alarmId = "custom-visible",
            revision = 3,
            enabled = true,
            soundProfile = AlarmSoundProfile.SILENT,
            definition = AlarmDefinition.Custom(
                label = "Farmaco",
                localTime = LocalTime.of(7, 30),
            ),
        )
        setScreen(
            AlarmsUiState(
                rules = listOf(rule),
                exactAlarmReady = true,
                notificationReady = true,
            ),
        )

        composeRule.onNodeWithTag("alarm-custom-row-custom-visible").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Farmaco").assertExists()
        composeRule.onNodeWithText("07:30 • Silenzioso").assertExists()
        composeRule.onNodeWithTag("alarm-custom-switch-custom-visible").assertExists()
    }

    private fun setScreen(
        state: AlarmsUiState,
        onCreateCustom: (String, String) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            ArihnaTheme {
                AlarmsScreen(
                    contentPadding = PaddingValues(0.dp),
                    state = state,
                    onRequestNotificationPermission = {},
                    onRequestExactAlarmAccess = {},
                    onPrayerEnabled = { _, _ -> },
                    onCreateCustom = onCreateCustom,
                    onToggleRule = {},
                    onDeleteRule = {},
                    onToggleSound = {},
                )
            }
        }
    }
}
