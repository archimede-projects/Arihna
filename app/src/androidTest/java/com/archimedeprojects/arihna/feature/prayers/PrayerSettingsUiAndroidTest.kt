package com.archimedeprojects.arihna.feature.prayers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimeAdjustments
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerAdjustmentTarget
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleLocationSourceUi
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleLocationUi
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrayerSettingsUiAndroidTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun dedicatedSettingsPageExposesControlsAndAdjustmentActions() {
        var adjusted: Pair<PrayerAdjustmentTarget, Int>? = null
        var resetCalls = 0
        composeRule.setContent {
            ArihnaTheme {
                PrayerSettingsScreen(
                    contentPadding = PaddingValues(0.dp),
                    settings = PrayerCalculationSettings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
                    onBack = {},
                    onMethod = {},
                    onAsrMethod = {},
                    onHighLatitudeRule = {},
                    onAdjust = { target, delta -> adjusted = target to delta },
                    onReset = { resetCalls += 1 },
                )
            }
        }
        composeRule.onNodeWithTag("prayer-settings-screen").assertIsDisplayed()
        composeRule.onNodeWithText("Metodo di calcolo").assertIsDisplayed()
        composeRule.onNodeWithText("Metodo Asr").assertIsDisplayed()
        composeRule.onNodeWithTag("prayer-settings-asr-standard").assertIsSelected()
        composeRule.onNodeWithTag("prayer-settings-asr-hanafi").assertIsNotSelected()
        composeRule.onNodeWithText("Alte latitudini").assertIsDisplayed()
        composeRule.onNodeWithText("Correzioni manuali (minuti)").assertIsDisplayed()
        composeRule.onNodeWithTag("prayer-setting-adjust-fajr-plus").performClick()
        composeRule.runOnIdle { assertEquals(PrayerAdjustmentTarget.FAJR to 1, adjusted) }
        composeRule.onNodeWithTag("prayer-settings-reset").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, resetCalls) }
    }

    @Test
    fun orariShowsAppliedCorrectionAndSettingsEntryPoint() {
        var settingsCalls = 0
        val zone = ZoneId.of("Europe/Rome")
        val settings = PrayerCalculationSettings(
            method = PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE,
            adjustments = PrayerTimeAdjustments(fajrMinutes = 3),
        )
        val selected = SelectedLocation(
            source = LocationSource.Manual(1L),
            coordinates = Coordinates(41.9028, 12.4964),
            zoneId = zone,
            displayName = "Roma, Italia",
        )
        val times = PrayerTimes(
            fajr = Instant.parse("2026-10-16T03:42:00Z"),
            sunrise = Instant.parse("2026-10-16T05:10:00Z"),
            dhuhr = Instant.parse("2026-10-16T11:00:00Z"),
            asr = Instant.parse("2026-10-16T14:30:00Z"),
            maghrib = Instant.parse("2026-10-16T17:20:00Z"),
            isha = Instant.parse("2026-10-16T18:45:00Z"),
        )
        val state = PrayerScheduleUiState.Ready(
            localDate = LocalDate.of(2026, 10, 16),
            location = PrayerScheduleLocationUi("Roma, Italia", PrayerScheduleLocationSourceUi.MANUAL),
            selectedLocation = selected,
            settings = settings,
            today = PrayerDay(
                date = LocalDate.of(2026, 10, 16),
                zoneId = zone,
                coordinates = selected.coordinates,
                settings = settings,
                times = times,
            ),
            nextPrayer = null,
        )
        composeRule.setContent {
            ArihnaTheme {
                PrayerTimesScreen(
                    contentPadding = PaddingValues(0.dp),
                    scheduleState = state,
                    rules = emptyList(),
                    onEnabled = { _, _ -> },
                    onOpenSound = {},
                    onOpenSettings = { settingsCalls += 1 },
                )
            }
        }
        composeRule.onNodeWithTag("prayer-correction-fajr").assertTextEquals("+3 min")
        composeRule.onNodeWithTag("prayer-settings-open").performClick()
        composeRule.runOnIdle { assertEquals(1, settingsCalls) }
    }
}
