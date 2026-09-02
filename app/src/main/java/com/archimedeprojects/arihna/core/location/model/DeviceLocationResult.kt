package com.archimedeprojects.arihna.core.location.model

sealed interface DeviceLocationResult {
    data class Success(
        val fix: DeviceLocationFix,
        val freshness: LocationFreshness = LocationFreshness.FRESH,
    ) : DeviceLocationResult

    data class Unavailable(val reason: LocationFailure) : DeviceLocationResult
}
