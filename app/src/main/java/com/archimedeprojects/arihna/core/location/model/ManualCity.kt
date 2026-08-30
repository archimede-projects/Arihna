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
    /** Authoritative modern IANA id from GeoNames, even if [zoneId] uses API28 compatibility. */
    val timeZoneId: String = zoneId.id,
) {
    val isValid: Boolean
        get() = id > 0 &&
            name.isNotBlank() &&
            countryName.isNotBlank() &&
            countryCode.isNotBlank() &&
            timeZoneId.isNotBlank() &&
            coordinates.isValid

    val displayName: String
        get() = listOfNotNull(name, regionName?.takeIf { it.isNotBlank() }, countryName)
            .distinct()
            .joinToString(", ")
}
