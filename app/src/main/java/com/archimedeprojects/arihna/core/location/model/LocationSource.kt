package com.archimedeprojects.arihna.core.location.model

import java.time.Instant

sealed interface LocationSource {
    data class Device(
        val capturedAt: Instant,
        val accuracyMeters: Float?,
    ) : LocationSource

    data class Manual(
        val cityId: Long,
    ) : LocationSource
}
