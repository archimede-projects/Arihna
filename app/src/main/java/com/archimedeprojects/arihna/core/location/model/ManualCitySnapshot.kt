package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates

data class ManualCitySnapshot(
    val id: Long,
    val name: String,
    val regionName: String?,
    val countryName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    /** Authoritative modern IANA id, never a legacy compatibility id. */
    val timeZoneId: String,
) {
    fun toManualCityOrNull(): ManualCity? {
        val coordinates = Coordinates(latitude, longitude)
        if (id <= 0 || name.isBlank() || countryName.isBlank() || countryCode.isBlank() || !coordinates.isValid) {
            return null
        }

        val zoneId = VerifiedTimeZoneCompatibility.resolveOrNull(timeZoneId) ?: return null

        return ManualCity(
            id = id,
            name = name,
            regionName = regionName,
            countryName = countryName,
            countryCode = countryCode,
            coordinates = coordinates,
            zoneId = zoneId,
            timeZoneId = timeZoneId,
        )
    }

    companion object {
        fun from(city: ManualCity): ManualCitySnapshot = ManualCitySnapshot(
            id = city.id,
            name = city.name,
            regionName = city.regionName,
            countryName = city.countryName,
            countryCode = city.countryCode,
            latitude = city.coordinates.latitude,
            longitude = city.coordinates.longitude,
            timeZoneId = city.timeZoneId,
        )
    }
}
