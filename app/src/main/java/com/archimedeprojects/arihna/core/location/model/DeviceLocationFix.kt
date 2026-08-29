package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant
import java.time.ZoneId

data class DeviceLocationFix(
    val coordinates: Coordinates,
    val zoneId: ZoneId,
    val capturedAt: Instant,
    val accuracyMeters: Float?,
) {
    val isValid: Boolean
        get() = coordinates.isValid &&
            (accuracyMeters == null || (accuracyMeters.isFinite() && accuracyMeters >= 0f))
}
