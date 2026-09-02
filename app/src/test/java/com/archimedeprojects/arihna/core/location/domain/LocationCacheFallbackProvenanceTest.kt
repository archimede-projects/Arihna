package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.model.CachedDeviceLocation
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCacheFallbackProvenanceTest {
    private val capturedAt = Instant.parse("2026-08-31T15:16:15.258Z")
    private val coordinates = Coordinates(44.993, 10.86)

    @Test
    fun rawFrameworkCacheMatchesOnlySamePersistedCaptureAndCoordinates() {
        val persisted = persistedFix()
        val same = CachedDeviceLocation(coordinates, capturedAt, 2_000f)
        val differentTime = same.copy(capturedAt = capturedAt.plusSeconds(1))
        val differentCoordinates = same.copy(coordinates = Coordinates(45.0, 10.86))

        assertTrue(cachedFrameworkLocationMatchesPersistedFix(same, persisted))
        assertFalse(cachedFrameworkLocationMatchesPersistedFix(differentTime, persisted))
        assertFalse(cachedFrameworkLocationMatchesPersistedFix(differentCoordinates, persisted))
    }

    @Test
    fun readyFreshnessIsOwnedBySelectedLocation() {
        val selected = SelectedLocation(
            source = LocationSource.Device(capturedAt, 2_000f),
            coordinates = coordinates,
            zoneId = ZoneId.of("Europe/Rome"),
            displayName = "Pegognaga, Lombardia, Italy",
            freshness = LocationFreshness.CACHED,
        )
        val ready = LocationResolutionState.Ready(selected)

        assertEquals(LocationFreshness.CACHED, selected.freshness)
        assertEquals(LocationFreshness.CACHED, ready.freshness)
    }

    @Test
    fun manualSelectedLocationHasNoDeviceFreshness() {
        val selected = SelectedLocation(
            source = LocationSource.Manual(1L),
            coordinates = coordinates,
            zoneId = ZoneId.of("Europe/Rome"),
            displayName = "Pegognaga",
        )

        assertNull(selected.freshness)
    }

    private fun persistedFix() = DeviceLocationFix(
        coordinates = coordinates,
        zoneId = ZoneId.of("Europe/Rome"),
        capturedAt = capturedAt,
        accuracyMeters = 2_000f,
    )
}
