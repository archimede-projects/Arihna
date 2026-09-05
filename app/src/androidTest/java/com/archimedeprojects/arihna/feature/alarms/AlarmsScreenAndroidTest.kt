package com.archimedeprojects.arihna.feature.alarms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlarmsScreenAndroidTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun missingCapabilitiesAreExplicitAndNeverFabricatedAsReady() {
        setScreen(AlarmsUiState(exactAlarmReady = false, notificationReady = false), fullScreenReady = false)

        composeRule.onNodeWithTag("alarm-capabilities").assertIsDisplayed()
        composeRule.onNodeWithText("Notifiche").assertIsDisplayed()
        composeRule.onNodeWithText("Allarmi esatti").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-notification-permission-action").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-exact-access-action").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-fullscreen-access-action").performScrollTo().assertIsDisplayed()
        assertTextAbsent("Schermo intero: pronto")
    }

    @Test
    fun allFivePrayerControlsArePresent() {
        setScreen(AlarmsUiState(exactAlarmReady = true, notificationReady = true))

        AlarmPrayer.entries.forEach { prayer ->
            composeRule.onNodeWithTag("alarm-prayer-${prayer.name.lowercase()}").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("alarm-prayer-${prayer.name.lowercase()}-switch").assertIsDisplayed()
        }
    }

    @Test
    fun prayerSoundButtonOpensExplicitPickerAndSelectsAdhan() {
        val rule = AlarmRule(
            alarmId = "prayer-dhuhr",
            revision = 2,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.DHUHR),
        )
        var selected: AlarmSoundProfile? = null
        setScreen(
            state = AlarmsUiState(rules = listOf(rule), exactAlarmReady = true, notificationReady = true),
            onSoundSelected = { _, profile -> selected = profile },
        )

        composeRule.onNodeWithTag("alarm-prayer-dhuhr-sound").performScrollTo().performClick()
        composeRule.onNodeWithTag("alarm-sound-picker").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-sound-option-adhan").performClick()
        composeRule.runOnIdle { assertEquals(AlarmSoundProfile.ADHAN, selected) }
    }

    @Test
    fun customCreatorSendsLabelAndLocalTimeTextWithoutInventingSuccess() {
        var captured: Pair<String, String>? = null
        setScreen(
            state = AlarmsUiState(exactAlarmReady = true, notificationReady = true),
            onCreateCustom = { label, time -> captured = label to time },
        )

        composeRule.onNodeWithTag("alarm-custom-label").performScrollTo().assertIsDisplayed().performTextInput("Farmaco")
        composeRule.onNodeWithTag("alarm-custom-time").performScrollTo().assertIsDisplayed().performTextInput("07:30")
        composeRule.onNodeWithTag("alarm-custom-add").performScrollTo().assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals("Farmaco" to "07:30", captured) }
        assertTextAbsent("Sveglia salvata")
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
        composeRule.onNodeWithText("Farmaco").assertIsDisplayed()
        composeRule.onNodeWithText("07:30 • Silenzioso").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-custom-switch-custom-visible").assertIsDisplayed()
    }

    private fun setScreen(
        state: AlarmsUiState,
        fullScreenReady: Boolean = true,
        onCreateCustom: (String, String) -> Unit = { _, _ -> },
        onSoundSelected: (AlarmRule, AlarmSoundProfile) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            ArihnaTheme {
                AlarmsScreen(
                    contentPadding = PaddingValues(0.dp),
                    state = state,
                    fullScreenReady = fullScreenReady,
                    onRequestNotificationPermission = {},
                    onRequestExactAlarmAccess = {},
                    onRequestFullScreenAccess = {},
                    onPrayerEnabled = { _, _ -> },
                    onCreateCustom = onCreateCustom,
                    onToggleRule = {},
                    onDeleteRule = {},
                    onSoundSelected = onSoundSelected,
                )
            }
        }
    }

    private fun assertTextAbsent(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }
}
