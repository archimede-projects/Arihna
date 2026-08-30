package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates

/**
 * Lightweight catalog result that does not require successful ZoneId materialization.
 *
 * This keeps a city discoverable by search/nearest even when the current platform tzdata
 * cannot represent the city's modern IANA timezone. Successful manual selection still
 * requires CityRepository.findById() to materialize a [ManualCity].
 */
data class CitySearchResult(
    val id: Long,
    val name: String,
    val regionName: String?,
    val countryName: String,
    val countryCode: String,
    val coordinates: Coordinates,
    val timeZoneId: String,
    val timeZoneSupported: Boolean,
) {
    val displayName: String
        get() = listOfNotNull(
            name,
            regionName?.takeIf { it.isNotBlank() },
            countryName,
        ).joinToString(", ")
}
