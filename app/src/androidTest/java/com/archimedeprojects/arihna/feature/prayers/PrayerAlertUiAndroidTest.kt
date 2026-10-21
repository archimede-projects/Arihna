package com.archimedeprojects.arihna.feature.prayers

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.domain.AdhanVariant
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrayerAlertUiAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun prayerDialogExposesIndependentVolumeAndDedicatedTwoTakbirChoice() {
        var savedUri: String? = null
        var savedVolume = -1
        val rule = AlarmRule(
            alarmId = "prayer-fajr",
            revision = 1L,
            enabled = true,
            soundProfile = AlarmSoundProfile.ADHAN,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.FAJR),
            ringtoneUri = AdhanVariant.CLASSIC.storageValue,
            ringtoneTitle = AdhanVariant.CLASSIC.displayName,
        )

        composeRule.setContent {
            ArihnaTheme {
                PrayerSoundDialog(
                    rule = rule,
                    initialVolumePercent = 100,
                    onDismiss = {},
                    onSave = { _, uri, _, volume ->
                        savedUri = uri
                        savedVolume = volume
                    },
                )
            }
        }

        composeRule.onNodeWithTag("prayer-volume-slider-fajr").assertIsDisplayed()
        composeRule.onNodeWithTag("prayer-volume-value-fajr").assertTextEquals("100%")
        composeRule.onNodeWithTag("prayer-volume-slider-fajr")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(40f)
            }
        composeRule.onNodeWithTag("prayer-volume-value-fajr").assertTextEquals("40%")

        composeRule.onNodeWithTag("prayer-sound-choose-adhan").performClick()
        composeRule.onNodeWithText("Takbīr Makkah · Allahu Akbar ×2").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Indietro").performClick()
        composeRule.onNodeWithText("Conferma").performClick()

        composeRule.runOnIdle {
            assertEquals(AdhanVariant.TAKBIR_X2.storageValue, savedUri)
            assertEquals(40, savedVolume)
        }
    }
    @Test
    fun prayerAdhanCanBeReplacedByPerPrayerVibrationOnly() {
        var chosen: AlarmSoundProfile? = null
        val rule = AlarmRule(
            alarmId = "prayer-isha",
            revision = 5L,
            enabled = true,
            soundProfile = AlarmSoundProfile.ADHAN,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.ISHA),
            ringtoneUri = AdhanVariant.TAKBIR_X2.storageValue,
            ringtoneTitle = AdhanVariant.TAKBIR_X2.displayName,
        )
        composeRule.setContent {
            ArihnaTheme {
                PrayerSoundDialog(
                    rule = rule,
                    initialVolumePercent = 65,
                    onDismiss = {},
                    onSave = { profile, _, _, _ -> chosen = profile },
                )
            }
        }
        composeRule.onNodeWithText("Solo vibrazione").performClick()
        composeRule.onNodeWithText("Conferma").performClick()
        composeRule.runOnIdle { assertEquals(AlarmSoundProfile.VIBRATION, chosen) }
    }

    @Test
    fun androidRingtoneProfileShowsConcreteSelectedNameAndChangeAction() {
        val rule = AlarmRule(
            alarmId = "prayer-dhuhr",
            revision = 1L,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.DHUHR),
            ringtoneUri = "content://media/internal/audio/media/42",
            ringtoneTitle = "Morning Flower",
        )
        composeRule.setContent {
            ArihnaTheme {
                PrayerSoundDialog(
                    rule = rule,
                    initialVolumePercent = 80,
                    onDismiss = {},
                    onSave = { _, _, _, _ -> },
                )
            }
        }
        composeRule.onNodeWithText("Suoneria Android").assertIsDisplayed()
        composeRule.onNodeWithText("Morning Flower").assertIsDisplayed()
        composeRule.onNodeWithTag("prayer-sound-change-ringtone").assertIsDisplayed()
    }

}
