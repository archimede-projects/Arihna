package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.ZoneId

data class ManualCity(
    val id: Long,
    val name: String,
    val regionName: String?,
    val countryName: String,
    val countryCode: String,
    val coordinates: Coordinates,
    val zoneId: ZoneId,
) {
    val isValid: Boolean
        get() = id > 0 &&
            name.isNotBlank() &&
            countryName.isNotBlank() &&
            countryCode.isNotBlank() &&
            coordinates.isValid

    val displayName: String
        get() = listOfNotNull(name, regionName?.takeIf { it.isNotBlank() }, countryName)
            .distinct()
            .joinToString(", ")
}
