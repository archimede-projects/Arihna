package com.archimedeprojects.arihna.core.location.model

sealed interface DeviceLocationResult {
    data class Success(val fix: DeviceLocationFix) : DeviceLocationResult
    data class Unavailable(val reason: LocationFailure) : DeviceLocationResult
}
