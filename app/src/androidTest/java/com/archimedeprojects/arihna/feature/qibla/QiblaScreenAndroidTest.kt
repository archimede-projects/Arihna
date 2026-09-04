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
    @get:Rule val composeRule = createComposeRule()

    @Test fun noLocationShowsSettingsCtaWithoutInventedDegrees() {
        var calls = 0
        setScreen(QiblaState.NoLocation(LocationResolutionState.Unconfigured)) { calls += 1 }
        composeRule.onNodeWithText("Posizione necessaria").assertIsDisplayed()
        composeRule.onNodeWithText("Configura posizione").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, calls) }
        assertTextAbsent("Qibla 0°")
    }

    @Test fun manualLocationRemainsStaticAndHasNoLiveInstrumentation() {
        setScreen(QiblaState.StaticBearing(manualLocation(), 123.276))
        composeRule.onNodeWithText("Qibla 123°").assertIsDisplayed()
        composeRule.onNodeWithText("Bussola statica").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-static-compass").assertIsDisplayed()
        assertTextAbsent("Bussola live")
        assertTextAbsent("Intensità campo magnetico")
    }

    @Test fun liveCompassShowsFullDialHeadingAndInstrumentation() {
        setScreen(
            QiblaState.LiveCompass(
                location = deviceLocation(),
                bearingTrueDegrees = 123.276,
                deviceHeadingTrueDegrees = 349.0,
                relativeQiblaDirectionDegrees = 134.276,
                quality = HeadingQuality.HIGH,
                estimatedAccuracyDegrees = 4.2,
                headingSource = HeadingSource.ROTATION_VECTOR,
                declinationDegrees = 0.8,
                magneticFieldMicroTesla = 47.0,
            ),
        )
        composeRule.onNodeWithText("Posizione memorizzata (CACHED)").assertIsDisplayed()
        composeRule.onNodeWithText("Qibla 123°").assertIsDisplayed()
        composeRule.onNodeWithText("349° N").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-sacred-banner").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-bearing-banner").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-live-heading").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-kaaba-mark").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-full-compass").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-live-cardinal-rose").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-live-direction").assertIsDisplayed()
        composeRule.onNodeWithText("47 µT").assertIsDisplayed()
        composeRule.onNodeWithText("Normale").assertIsDisplayed()
        composeRule.onNodeWithText("Alta").assertIsDisplayed()
        composeRule.onNodeWithText("Rotation Vector").assertIsDisplayed()
        composeRule.onNodeWithText("0.8° E").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-precision-guidance").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-single-viewport").assertIsDisplayed()
    }

    @Test fun highMagneticFieldShowsInterferenceAdvisory() {
        setScreen(
            QiblaState.LiveCompass(
                location = deviceLocation(),
                bearingTrueDegrees = 123.276,
                deviceHeadingTrueDegrees = 90.0,
                relativeQiblaDirectionDegrees = 33.276,
                quality = HeadingQuality.LOW,
                estimatedAccuracyDegrees = null,
                headingSource = HeadingSource.ACCELEROMETER_MAGNETIC_FIELD,
                declinationDegrees = 0.8,
                magneticFieldMicroTesla = 120.0,
            ),
        )
        composeRule.onNodeWithText("Bassa").assertIsDisplayed()
        composeRule.onNodeWithText("Possibile interferenza magnetica").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-interference-banner").assertIsDisplayed()
    }

    @Test fun sensorUnavailableKeepsNumericBearingWithoutLiveMarker() {
        setScreen(QiblaState.SensorUnavailable(deviceLocation(), 123.276, HeadingUnavailableReason.NO_SUPPORTED_SENSOR))
        composeRule.onNodeWithText("Qibla 123°").assertIsDisplayed()
        composeRule.onNodeWithText("Bussola live non disponibile").assertIsDisplayed()
        composeRule.onNodeWithTag("qibla-static-direction").assertIsDisplayed()
    }

    private fun setScreen(state: QiblaState, onOpenLocationSettings: () -> Unit = {}) {
        composeRule.setContent { ArihnaTheme { QiblaScreen(PaddingValues(0.dp), state, onOpenLocationSettings) } }
    }

    private fun assertTextAbsent(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }

    private fun manualLocation() = SelectedLocation(
        LocationSource.Manual(1L),
        Coordinates(41.9028, 12.4964),
        ZoneId.of("Europe/Rome"),
        "Roma, Italia",
    )

    private fun deviceLocation() = SelectedLocation(
        source = LocationSource.Device(Instant.parse("2026-09-03T04:00:00Z"), 2_000f),
        coordinates = Coordinates(44.99, 10.85),
        zoneId = ZoneId.of("Europe/Rome"),
        displayName = "Pegognaga, Italia",
        freshness = LocationFreshness.CACHED,
    )
}
