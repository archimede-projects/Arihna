package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.ZoneId

data class ManualCity(
    override val id: Long,
    override val name: String,
    override val regionName: String?,
    override val countryName: String,
    override val countryCode: String,
    override val coordinates: Coordinates,
    val zoneId: ZoneId,
    /** Authoritative modern IANA id from GeoNames, even if [zoneId] uses API28 compatibility. */
    override val timeZoneId: String = zoneId.id,
) : CitySearchResult {
    override val timeZoneSupported: Boolean
        get() = true

    val isValid: Boolean
        get() = id > 0 &&
            name.isNotBlank() &&
            countryName.isNotBlank() &&
            countryCode.isNotBlank() &&
            timeZoneId.isNotBlank() &&
            coordinates.isValid
}
