package com.archimedeprojects.arihna.feature.qibla

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.qibla.heading.HeadingQuality
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSource
import com.archimedeprojects.arihna.core.qibla.heading.HeadingUnavailableReason
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.qibla.domain.QiblaState
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QiblaScreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noLocationShowsSettingsCtaWithoutInventedDegrees() {
        var calls = 0
        setScreen(
            state = QiblaState.NoLocation(LocationResolutionState.Unconfigured),
            onOpenLocationSettings = { calls += 1 },
        )

        composeRule.onNodeWithText("Posizione necessaria").assertIsDisplayed()
        composeRule.onNodeWithText("Configura posizione").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, calls) }
        assertTextAbsent("Qibla 0°")
        assertTextAbsent("--:--")
    }

    @Test
    fun manualLocationShowsProminentStaticBearingNoticeAndNoLiveClaim() {
        setScreen(
            state = QiblaState.StaticBearing(
                location = manualLocation(),
                bearingTrueDegrees = 123.276,
            ),
        )

        composeRule.onNodeWithText("Roma, Italia").assertIsDisplayed()
        composeRule.onNodeWithText("Posizione manuale").assertIsDisplayed()
        composeRule.onNodeWithText("Qibla 123°").assertIsDisplayed()
        composeRule.onNodeWithText("Bussola statica").assertIsDisplayed()
        composeRule.onNodeWithText(
            "La direzione è calcolata per la città selezionata. Per usare la bussola live e allineare il telefono, scegli la posizione del dispositivo.",
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-static-direction").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-static-cardinal-rose").assertIsDisplayed()
        assertTextAbsent("Bussola live")
    }

    @Test
    fun liveCompassShowsBearingAccuracySourceCachedProvenanceAndLiveRose() {
        setScreen(
            state = QiblaState.LiveCompass(
                location = deviceLocation(),
                bearingTrueDegrees = 123.276,
                deviceHeadingTrueDegrees = 100.0,
                relativeQiblaDirectionDegrees = 23.276,
                quality = HeadingQuality.HIGH,
                estimatedAccuracyDegrees = 4.2,
                headingSource = HeadingSource.ROTATION_VECTOR,
            ),
        )

        composeRule.onNodeWithText("Pegognaga, Italia").assertIsDisplayed()
        composeRule.onNodeWithText("Posizione dispositivo").assertIsDisplayed()
        composeRule.onNodeWithText("Posizione memorizzata (CACHED)").assertIsDisplayed()
        composeRule.onNodeWithText("Qibla 123°").assertIsDisplayed()
        composeRule.onNodeWithText("Bussola live").assertIsDisplayed()
        composeRule.onNodeWithText("Accuratezza bussola: alta").assertIsDisplayed()
        composeRule.onNodeWithText("Accuratezza stimata: ±4°").assertIsDisplayed()
        composeRule.onNodeWithText("Sensore: rotation vector").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-live-direction").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-live-cardinal-rose").assertIsDisplayed()
    }

    @Test
    fun lowQualityShowsCalibrationGuidance() {
        setScreen(
            state = QiblaState.LiveCompass(
                location = deviceLocation(),
                bearingTrueDegrees = 123.276,
                deviceHeadingTrueDegrees = 100.0,
                relativeQiblaDirectionDegrees = 23.276,
                quality = HeadingQuality.LOW,
                estimatedAccuracyDegrees = null,
                headingSource = HeadingSource.ACCELEROMETER_MAGNETIC_FIELD,
            ),
        )

        composeRule.onNodeWithText(
            "Accuratezza bassa: allontanati da metallo o interferenze e muovi il telefono per calibrare.",
        ).assertIsDisplayed()
        assertTextAbsent("Accuratezza stimata:")
    }

    @Test
    fun sensorUnavailableKeepsNumericBearingWithoutLiveMarker() {
        setScreen(
            state = QiblaState.SensorUnavailable(
                location = deviceLocation(),
                bearingTrueDegrees = 123.276,
                reason = HeadingUnavailableReason.NO_SUPPORTED_SENSOR,
            ),
        )

        composeRule.onNodeWithText("Qibla 123°").assertIsDisplayed()
        composeRule.onNodeWithText("Bussola live non disponibile").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Nessun sensore di orientamento supportato è disponibile su questo dispositivo.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Il bearing numerico resta valido rispetto al nord vero.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-static-direction").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Bussola live", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun setScreen(
        state: QiblaState,
        onOpenLocationSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            ArihnaTheme {
                QiblaScreen(
                    contentPadding = PaddingValues(0.dp),
                    state = state,
                    onOpenLocationSettings = onOpenLocationSettings,
                )
            }
        }
    }

    private fun assertTextAbsent(text: String) {
        assertTrue(
            "Expected no node with text '$text'",
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun manualLocation() = SelectedLocation(
        source = LocationSource.Manual(cityId = 1L),
        coordinates = Coordinates(41.9028, 12.4964),
        zoneId = ZoneId.of("Europe/Rome"),
        displayName = "Roma, Italia",
    )

    private fun deviceLocation() = SelectedLocation(
        source = LocationSource.Device(
            capturedAt = Instant.parse("2026-09-03T04:00:00Z"),
            accuracyMeters = 2_000f,
        ),
        coordinates = Coordinates(44.99, 10.85),
        zoneId = ZoneId.of("Europe/Rome"),
        displayName = "Pegognaga, Italia",
        freshness = LocationFreshness.CACHED,
    )
}
