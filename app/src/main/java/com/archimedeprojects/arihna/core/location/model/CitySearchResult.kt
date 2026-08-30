package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates

/** A discoverable catalog row that does not require successful ZoneId materialization. */
interface CitySearchResult {
    val id: Long
    val name: String
    val regionName: String?
    val countryName: String
    val countryCode: String
    val coordinates: Coordinates
    val timeZoneId: String
    val timeZoneSupported: Boolean

    val displayName: String
        get() = listOfNotNull(
            name,
            regionName?.takeIf { it.isNotBlank() },
            countryName,
        ).distinct().joinToString(", ")
}

/** Catalog result used when timezone materialization is deliberately deferred. */
data class CatalogCitySearchResult(
    override val id: Long,
    override val name: String,
    override val regionName: String?,
    override val countryName: String,
    override val countryCode: String,
    override val coordinates: Coordinates,
    override val timeZoneId: String,
    override val timeZoneSupported: Boolean,
) : CitySearchResult
