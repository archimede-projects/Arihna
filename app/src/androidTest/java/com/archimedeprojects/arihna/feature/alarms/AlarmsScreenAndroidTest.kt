package com.archimedeprojects.arihna.feature.alarms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlarmsScreenAndroidTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun alarmsSurfaceContainsOnlyPersonalAlarmsAndNoCapabilityOrPrayerPanel() {
        val custom = customRule()
        val prayer = AlarmRule(
            alarmId = "prayer-fajr",
            revision = 1,
            enabled = true,
            soundProfile = AlarmSoundProfile.ADHAN,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.FAJR),
        )
        setScreen(AlarmsUiState(rules = listOf(custom, prayer)))

        composeRule.onNodeWithTag("alarms-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-custom-row-custom-visible").assertIsDisplayed()
        composeRule.onNodeWithText("Farmaco  •  Singola").assertIsDisplayed()
        assertTextAbsent("Prontezza sveglie")
        assertTextAbsent("Preghiere")
        assertTextAbsent("Fajr")
    }

    @Test
    fun compactAlarmRowOpensEditingAndSwitchRemainsIndependent() {
        val custom = customRule()
        var newClicks = 0
        var edited: AlarmRule? = null
        var toggled: AlarmRule? = null
        setScreen(
            state = AlarmsUiState(rules = listOf(custom)),
            onNew = { newClicks += 1 },
            onEdit = { edited = it },
            onToggle = { toggled = it },
        )

        composeRule.onNodeWithTag("alarm-new").performClick()
        composeRule.onNodeWithTag("alarm-custom-row-custom-visible").performClick()
        composeRule.runOnIdle {
            assertEquals(1, newClicks)
            assertEquals("custom-visible", edited?.alarmId)
            assertNull(toggled)
        }

        edited = null
        composeRule.onNodeWithTag("alarm-custom-switch-custom-visible").performClick()
        composeRule.runOnIdle {
            assertEquals("custom-visible", toggled?.alarmId)
            assertNull(edited)
        }
    }

    @Test
    fun compactAlarmRowHidesManagementActionsAndRingtoneName() {
        val custom = customRule().copy(
            ringtoneUri = "content://alarm/42",
            ringtoneTitle = "Morning Flower",
        )
        setScreen(AlarmsUiState(rules = listOf(custom)))

        composeRule.onNodeWithTag("alarm-custom-row-custom-visible").assertIsDisplayed()
        assertTextAbsent("Modifica")
        assertTextAbsent("Elimina")
        assertTextAbsent("Morning Flower")
        assertTextAbsent("Suoneria telefono")
    }

    @Test
    fun soundUpdatedStatusIsNeverRenderedOnPersonalAlarmSurface() {
        setScreen(AlarmsUiState(message = "Suono aggiornato"))
        assertTextAbsent("Suono aggiornato")
        assertTrue(composeRule.onAllNodesWithText("Nessuna sveglia").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun existingAlarmEditorKeepsCompactControlsAndMovesDeleteInsideEditor() {
        var deleted: AlarmRule? = null
        composeRule.setContent {
            ArihnaTheme {
                CustomAlarmEditorDialog(
                    initialRule = customRule().copy(
                        ringtoneUri = "content://alarm/42",
                        ringtoneTitle = "Morning Flower",
                    ),
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _ -> },
                    onDelete = { deleted = it },
                )
            }
        }

        DayOfWeek.entries.forEach { day ->
            composeRule.onNodeWithTag("alarm-day-${day.name.lowercase()}").assertExists()
        }
        assertTextAbsent("Lun")
        assertTextAbsent("Mar")
        assertTextAbsent("Mer")
        assertTextAbsent("Gio")
        assertTextAbsent("Ven")
        assertTextAbsent("Sab")
        assertTextAbsent("Dom")

        composeRule.onNodeWithTag("alarm-ringtone-row").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-sound-switch").assertIsDisplayed()
        composeRule.onNodeWithText("Morning Flower").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-editor-cancel").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-editor-save").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-editor-delete").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("custom-visible", deleted?.alarmId) }
    }

    @Test
    fun newAlarmEditorDoesNotExposeDeleteAction() {
        composeRule.setContent {
            ArihnaTheme {
                CustomAlarmEditorDialog(
                    initialRule = null,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag("alarm-editor").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-editor-delete").assertDoesNotExist()
        composeRule.onNodeWithTag("alarm-editor-cancel").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-editor-save").assertIsDisplayed()
    }

    private fun customRule() = AlarmRule(
        alarmId = "custom-visible",
        revision = 3,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.Custom(
            label = "Farmaco",
            localTime = LocalTime.of(7, 30),
        ),
    )

    private fun setScreen(
        state: AlarmsUiState,
        onNew: () -> Unit = {},
        onEdit: (AlarmRule) -> Unit = {},
        onToggle: (AlarmRule) -> Unit = {},
    ) {
        composeRule.setContent {
            ArihnaTheme {
                AlarmsScreen(
                    contentPadding = PaddingValues(0.dp),
                    state = state,
                    onNew = onNew,
                    onEdit = onEdit,
                    onToggle = onToggle,
                )
            }
        }
    }

    private fun assertTextAbsent(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }
}
