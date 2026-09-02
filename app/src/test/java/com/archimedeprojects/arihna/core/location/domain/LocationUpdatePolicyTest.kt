package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUpdatePolicyTest {
    private val policy = LocationUpdatePolicy()

    @Test
    fun defaultsMatchApprovedLocationPolicy() {
        assertTrue(policy.significantDistanceMeters == 5_000.0)
        assertTrue(policy.minimumForegroundUpdateInterval == Duration.ofMinutes(15))
        assertTrue(policy.currentFixTimeout == Duration.ofSeconds(30))
    }

    @Test
    fun firstValidFixIsAlwaysAccepted() {
        assertTrue(policy.shouldAccept(null, fix(41.9028, 12.4964, "Europe/Rome")))
    }

    @Test
    fun movementBelowFiveKilometersIsNotSignificant() {
        val previous = fix(41.9028, 12.4964, "Europe/Rome")
        val candidate = fix(41.9200, 12.4964, "Europe/Rome")

        assertTrue(policy.distanceMeters(previous.coordinates, candidate.coordinates) < 5_000.0)
        assertFalse(policy.shouldAccept(previous, candidate))
    }

    @Test
    fun movementAtLeastFiveKilometersIsSignificant() {
        val previous = fix(41.9028, 12.4964, "Europe/Rome")
        val candidate = fix(41.9600, 12.4964, "Europe/Rome")

        assertTrue(policy.distanceMeters(previous.coordinates, candidate.coordinates) >= 5_000.0)
        assertTrue(policy.shouldAccept(previous, candidate))
    }

    @Test
    fun timezoneChangeIsSignificantEvenBelowFiveKilometers() {
        val previous = fix(41.9028, 12.4964, "Europe/Rome")
        val candidate = fix(41.9030, 12.4965, "Europe/Paris")

        assertTrue(policy.distanceMeters(previous.coordinates, candidate.coordinates) < 5_000.0)
        assertTrue(policy.shouldAccept(previous, candidate))
    }

    private fun fix(latitude: Double, longitude: Double, zone: String) = DeviceLocationFix(
        coordinates = Coordinates(latitude, longitude),
        zoneId = ZoneId.of(zone),
        capturedAt = Instant.parse("2026-08-29T10:00:00Z"),
        accuracyMeters = 1_000f,
    )
}
