package com.archimedeprojects.arihna.core.location.model

sealed interface DeviceLocationResult {
    data class Success(
        val fix: DeviceLocationFix,
        val freshness: LocationFreshness = LocationFreshness.FRESH,
    ) : DeviceLocationResult

    /** Real cached coordinates/timestamp without an invented historical timezone. */
    data class Cached(
        val location: CachedDeviceLocation,
    ) : DeviceLocationResult

    data class Unavailable(val reason: LocationFailure) : DeviceLocationResult
}
