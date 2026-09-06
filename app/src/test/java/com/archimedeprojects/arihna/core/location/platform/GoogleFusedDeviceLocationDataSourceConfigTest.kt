package com.archimedeprojects.arihna.core.location.platform

import com.archimedeprojects.arihna.core.location.domain.LocationUpdatePolicy
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleFusedDeviceLocationDataSourceConfigTest {
    @Test
    fun foregroundFusedRequestKeepsDomainIntervalWithoutPlatformDistanceFilter() {
        val policy = LocationUpdatePolicy(
            significantDistanceMeters = 5_000.0,
            minimumForegroundUpdateInterval = Duration.ofMinutes(15),
            currentFixTimeout = Duration.ofSeconds(30),
        )

        val spec = fusedForegroundRequestSpec(policy)

        assertEquals(Duration.ofMinutes(15).toMillis(), spec.intervalMillis)
        assertEquals(0f, spec.minDistanceMeters)
    }
}
