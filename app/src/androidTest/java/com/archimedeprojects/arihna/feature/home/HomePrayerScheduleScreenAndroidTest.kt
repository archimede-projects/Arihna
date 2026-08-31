package com.archimedeprojects.arihna.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.NextPrayerUiState
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleLocationSourceUi
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleLocationUi
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleUiState
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomePrayerScheduleScreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noLocationShowsMessageAndSettingsCtaWithoutPrayerValues() {
        var openSettingsCalls = 0
        setScreen(
            uiState = PrayerScheduleUiState.NoLocation(
                message = "Imposta una posizione per calcolare gli orari di preghiera.",
                locationState = LocationResolutionState.Unconfigured,
            ),
            onOpenLocationSettings = { openSettingsCalls += 1 },
        )

        composeRule.onNodeWithText("Imposta una posizione per calcolare gli orari di preghiera.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Configura posizione")
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, openSettingsCalls) }

        assertTextAbsent("Prossima preghiera")
        assertTextAbsent("Fajr")
        assertTextAbsent("--:--")
    }

    @Test
    fun readyShowsReadableLocationNextPrayerCountdownAndCompleteDay() {
        setScreen(uiState = readyState())

        composeRule.onNodeWithText("Roma, Italia").assertIsDisplayed()
        composeRule.onNodeWithText("Posizione manuale").assertIsDisplayed()
        composeRule.onNodeWithText("Prossima preghiera").assertIsDisplayed()
        composeRule.onNodeWithText("Dhuhr · 13:15").assertIsDisplayed()
        composeRule.onNodeWithText("Tra 01:02:03").assertIsDisplayed()
        composeRule.onNodeWithText("Metodo: Muslim World League (MWL)").assertIsDisplayed()
        composeRule.onNodeWithText("Fajr").assertIsDisplayed()
        composeRule.onNodeWithText("05:10").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Isha"))
        composeRule.onNodeWithText("Alba").assertIsDisplayed()
        composeRule.onNodeWithText("Dhuhr").assertIsDisplayed()
        composeRule.onNodeWithText("Asr").assertIsDisplayed()
        composeRule.onNodeWithText("Maghrib").assertIsDisplayed()
        composeRule.onNodeWithText("Isha").assertIsDisplayed()
        composeRule.onNodeWithText("22:05").assertIsDisplayed()
    }

    @Test
    fun calculationUnavailableShowsControlledErrorWithoutInventedTimes() {
        setScreen(
            uiState = PrayerScheduleUiState.CalculationUnavailable(
                message = "Orari di preghiera non disponibili per la posizione e le impostazioni selezionate.",
                reason = PrayerCalculationResult.Reason.ASTRONOMICAL_EVENT_UNAVAILABLE,
                selectedLocation = selectedManualLocation(),
            ),
        )

        composeRule.onNodeWithText("Orari non disponibili").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Orari di preghiera non disponibili per la posizione e le impostazioni selezionate.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Nessun orario viene mostrato finché il calcolo non torna disponibile.")
            .assertIsDisplayed()

        assertTextAbsent("Prossima preghiera")
        assertTextAbsent("Fajr")
        assertTextAbsent("--:--")
    }

    @Test
    fun loadingIsExplicitAndDoesNotExposePrayerValues() {
        setScreen(uiState = PrayerScheduleUiState.Loading)

        composeRule.onNodeWithText("Calcolo degli orari in corso…").assertIsDisplayed()
        assertTextAbsent("Prossima preghiera")
        assertTextAbsent("Fajr")
        assertTextAbsent("--:--")
    }

    private fun assertTextAbsent(text: String) {
        assertTrue(
            "Expected no node with text '$text'",
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun setScreen(
        uiState: PrayerScheduleUiState,
        onOpenLocationSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            ArihnaTheme {
                HomePrayerScheduleScreen(
                    contentPadding = PaddingValues(0.dp),
                    uiState = uiState,
                    onOpenLocationSettings = onOpenLocationSettings,
                )
            }
        }
    }

    private fun readyState(): PrayerScheduleUiState.Ready {
        val zoneId = ZoneId.of("Europe/Rome")
        val settings = PrayerCalculationSettings(
            method = PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE,
        )
        val times = PrayerTimes(
            fajr = Instant.parse("2026-08-31T03:10:00Z"),
            sunrise = Instant.parse("2026-08-31T04:35:00Z"),
            dhuhr = Instant.parse("2026-08-31T11:15:00Z"),
            asr = Instant.parse("2026-08-31T14:50:00Z"),
            maghrib = Instant.parse("2026-08-31T18:45:00Z"),
            isha = Instant.parse("2026-08-31T20:05:00Z"),
        )
        val selectedLocation = selectedManualLocation()

        return PrayerScheduleUiState.Ready(
            localDate = LocalDate.of(2026, 8, 31),
            location = PrayerScheduleLocationUi(
                displayName = "Roma, Italia",
                source = PrayerScheduleLocationSourceUi.MANUAL,
            ),
            selectedLocation = selectedLocation,
            settings = settings,
            today = PrayerDay(
                date = LocalDate.of(2026, 8, 31),
                zoneId = zoneId,
                coordinates = selectedLocation.coordinates,
                settings = settings,
                times = times,
            ),
            nextPrayer = NextPrayerUiState(
                prayer = PrayerName.DHUHR,
                time = times.dhuhr,
                remaining = Duration.ofHours(1).plusMinutes(2).plusSeconds(3),
            ),
        )
    }

    private fun selectedManualLocation(): SelectedLocation = SelectedLocation(
        source = LocationSource.Manual(cityId = 1L),
        coordinates = Coordinates(41.9028, 12.4964),
        zoneId = ZoneId.of("Europe/Rome"),
        displayName = "Roma, Italia",
    )
}
