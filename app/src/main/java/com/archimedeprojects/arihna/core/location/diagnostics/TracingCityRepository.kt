package com.archimedeprojects.arihna.core.location.diagnostics

import android.os.SystemClock
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.prayer.model.Coordinates

class TracingCityRepository(
    private val delegate: CityRepository,
) : CityRepository {
    override suspend fun search(query: String): List<CitySearchResult> = delegate.search(query)

    override suspend fun findById(id: Long): ManualCity? = delegate.findById(id)

    override suspend fun nearest(coordinates: Coordinates): CitySearchResult? {
        val start = SystemClock.elapsedRealtime()
        LocationDiagnosticTrace.record(
            "CITY_NEAREST_ENTER",
            "elapsedMs=$start",
        )
        return try {
            delegate.nearest(coordinates).also { result ->
                LocationDiagnosticTrace.record(
                    "CITY_NEAREST_RETURN",
                    "latencyMs=${SystemClock.elapsedRealtime() - start} found=${result != null} name=${result?.name}",
                )
            }
        } catch (error: Throwable) {
            LocationDiagnosticTrace.record(
                "CITY_NEAREST_THROW",
                "latencyMs=${SystemClock.elapsedRealtime() - start} ${error.javaClass.simpleName}: ${error.message}",
            )
            throw error
        }
    }
}
