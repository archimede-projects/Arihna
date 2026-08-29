package com.archimedeprojects.arihna.core.location.data

import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.prayer.model.Coordinates

interface CityRepository {
    suspend fun search(query: String): List<ManualCity>
    suspend fun findById(id: Long): ManualCity?
    suspend fun nearest(coordinates: Coordinates): ManualCity?
}
