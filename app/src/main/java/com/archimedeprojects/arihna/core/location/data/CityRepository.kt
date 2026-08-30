package com.archimedeprojects.arihna.core.location.data

import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.prayer.model.Coordinates

interface CityRepository {
    suspend fun search(query: String): List<CitySearchResult>

    /**
     * Materializes a selectable manual city.
     *
     * Returns null when the city id does not exist. Throws [UnsupportedCityTimeZoneException]
     * when the catalog row exists but the current platform cannot resolve its authoritative
     * timezone natively or through an approved compatibility mapping.
     */
    suspend fun findById(id: Long): ManualCity?

    /** Returns the nearest discoverable catalog row without requiring timezone materialization. */
    suspend fun nearest(coordinates: Coordinates): CitySearchResult?
}
