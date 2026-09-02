package com.archimedeprojects.arihna.feature.settings

import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSettingsPresentationTest {
    @Test
    fun readyDeviceAndManualExposeSourceSpecificCopy() {
        val device = LocationResolutionState.Ready(
            location = selectedDevice(),
            freshness = LocationFreshness.FRESH,
        ).toPresentation()
        val manual = LocationResolutionState.Ready(
            location = selectedManual(),
            freshness = null,
        ).toPresentation()

        assertEquals("Posizione dispositivo pronta", device.title)
        assertEquals("Europe/Rome", device.zoneId)
        assertEquals("FRESH", device.freshness)
        assertEquals("Città manuale attiva", manual.title)
        assertEquals("Asia/Riyadh", manual.zoneId)
        assertEquals(null, manual.freshness)
    }

    @Test
    fun permissionAndServicesStatesExposeRecoveryActions() {
        val deniedRetry = LocationResolutionState.PermissionDenied(
            canRequestAgain = true,
            cachedLocation = null,
        ).toPresentation()
        val deniedPermanent = LocationResolutionState.PermissionDenied(
            canRequestAgain = false,
            cachedLocation = selectedDevice(),
        ).toPresentation()
        val services = LocationResolutionState.LocationServicesDisabled(
            cachedLocation = selectedDevice(),
        ).toPresentation()

        assertFalse(deniedRetry.showAppSettingsAction)
        assertTrue(deniedPermanent.showAppSettingsAction)
        assertEquals("CACHED", deniedPermanent.freshness)
        assertTrue(services.showLocationSettingsAction)
        assertTrue(services.message.contains("CACHED"))
    }

    @Test
    fun everyUnavailableReasonHasSpecificUnderstandableMessage() {
        val expectedTitles = mapOf(
            LocationFailure.TIMEOUT to "Posizione non ricevuta",
            LocationFailure.NO_PROVIDER to "Posizione non disponibile",
            LocationFailure.INVALID_FIX to "Posizione non valida",
            LocationFailure.CITY_NOT_FOUND to "Città non trovata",
            LocationFailure.CITY_DATASET_UNAVAILABLE to "Archivio città non disponibile",
            LocationFailure.UNSUPPORTED_TIME_ZONE to "Fuso orario non supportato",
            LocationFailure.PERSISTENCE_ERROR to "Impossibile salvare la posizione",
        )

        expectedTitles.forEach { (failure, title) ->
            val presentation = LocationResolutionState.Unavailable(
                reason = failure,
                cachedLocation = null,
            ).toPresentation()
            assertEquals(title, presentation.title)
            assertTrue(presentation.message.isNotBlank())
        }

        val timeout = LocationResolutionState.Unavailable(
            reason = LocationFailure.TIMEOUT,
            cachedLocation = null,
        ).toPresentation()
        assertTrue(timeout.message.contains("30 secondi"))

        val unsupported = LocationResolutionState.Unavailable(
            reason = LocationFailure.UNSUPPORTED_TIME_ZONE,
            cachedLocation = null,
        ).toPresentation()
        assertTrue(unsupported.message.contains("fuso orario"))
    }

    @Test
    fun activeModeLabelsAreExplicit() {
        assertEquals("Non configurata", LocationModeUi.Unconfigured.label())
        assertEquals("Device", LocationModeUi.Device.label())
        assertEquals("Manuale", LocationModeUi.Manual.label())
    }

    private fun selectedDevice() = SelectedLocation(
        source = LocationSource.Device(
            capturedAt = Instant.parse("2026-08-30T10:44:05.566Z"),
            accuracyMeters = 2_000f,
        ),
        coordinates = Coordinates(44.8, 11.0),
        zoneId = ZoneId.of("Europe/Rome"),
        displayName = "Ferrara, Italia",
    )

    private fun selectedManual() = SelectedLocation(
        source = LocationSource.Manual(cityId = 104515L),
        coordinates = Coordinates(21.4225, 39.8262),
        zoneId = ZoneId.of("Asia/Riyadh"),
        displayName = "Makkah, Saudi Arabia",
    )
}
