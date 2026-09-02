package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant

/** Raw real framework cache. Android does not carry the historical ZoneId with this record. */
data class CachedDeviceLocation(
    val coordinates: Coordinates,
    val capturedAt: Instant,
    val accuracyMeters: Float?,
) {
    val isValid: Boolean
        get() = coordinates.isValid && (accuracyMeters == null || (accuracyMeters.isFinite() && accuracyMeters >= 0f))
}
