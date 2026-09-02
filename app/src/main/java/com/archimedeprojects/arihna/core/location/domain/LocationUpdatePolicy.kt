package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Duration
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationUpdatePolicy(
    val significantDistanceMeters: Double = 5_000.0,
    val minimumForegroundUpdateInterval: Duration = Duration.ofMinutes(15),
    val currentFixTimeout: Duration = Duration.ofSeconds(30),
) {
    init {
        require(significantDistanceMeters > 0.0)
        require(!minimumForegroundUpdateInterval.isNegative && !minimumForegroundUpdateInterval.isZero)
        require(!currentFixTimeout.isNegative && !currentFixTimeout.isZero)
    }

    fun shouldAccept(previous: DeviceLocationFix?, candidate: DeviceLocationFix): Boolean {
        if (!candidate.isValid) return false
        if (previous == null || !previous.isValid) return true
        if (previous.zoneId != candidate.zoneId) return true
        return distanceMeters(previous.coordinates, candidate.coordinates) >= significantDistanceMeters
    }

    fun distanceMeters(first: Coordinates, second: Coordinates): Double {
        if (!first.isValid || !second.isValid) return Double.POSITIVE_INFINITY

        val lat1 = Math.toRadians(first.latitude)
        val lat2 = Math.toRadians(second.latitude)
        val deltaLat = lat2 - lat1
        val deltaLon = Math.toRadians(second.longitude - first.longitude)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_008.8
    }
}
