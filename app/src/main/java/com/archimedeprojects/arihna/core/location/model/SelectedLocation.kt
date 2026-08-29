package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.ZoneId

data class SelectedLocation(
    val source: LocationSource,
    val coordinates: Coordinates,
    val zoneId: ZoneId,
    val displayName: String,
) {
    val isValid: Boolean
        get() = coordinates.isValid && displayName.isNotBlank()
}
