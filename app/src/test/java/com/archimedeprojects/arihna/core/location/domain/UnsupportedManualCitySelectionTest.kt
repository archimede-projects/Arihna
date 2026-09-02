package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.data.UnsupportedCityTimeZoneException
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UnsupportedManualCitySelectionTest {
    @Test
    fun unsupportedCatalogTimezoneReturnsControlledStateWithoutPersistence() = runBlocking {
        val preferences = RecordingPreferences()
        val coordinator = LocationCoordinator(
            deviceLocationDataSource = NoDeviceLocation(),
            cityRepository = UnsupportedNuukRepository(),
            preferencesRepository = preferences,
        )

        val state = coordinator.selectManual(cityId = 3_421_319L)

        val unavailable = state as LocationResolutionState.Unavailable
        assertEquals(LocationFailure.UNSUPPORTED_TIME_ZONE, unavailable.reason)
        assertFalse(preferences.manualSelectionWritten)
    }

    private class UnsupportedNuukRepository : CityRepository {
        override suspend fun search(query: String): List<CitySearchResult> = emptyList()

        override suspend fun findById(id: Long): ManualCity? {
            throw UnsupportedCityTimeZoneException(id, "America/Nuuk")
        }

        override suspend fun nearest(coordinates: Coordinates): CitySearchResult? = null
    }

    private class NoDeviceLocation : DeviceLocationDataSource {
        override suspend fun getCurrentLocation(): DeviceLocationResult =
            DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)

        override suspend fun getLastKnownLocation(): DeviceLocationResult =
            DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)

        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = emptyFlow()
    }

    private class RecordingPreferences : LocationPreferencesRepository {
        override val preference = MutableStateFlow<LocationPreference>(LocationPreference.Unset)
        override val cachedDeviceFix = MutableStateFlow<DeviceLocationFix?>(null)
        var manualSelectionWritten = false

        override suspend fun selectDevice() {
            preference.value = LocationPreference.Device
        }

        override suspend fun selectManual(city: ManualCity) {
            manualSelectionWritten = true
        }

        override suspend fun saveDeviceFix(fix: DeviceLocationFix) {
            cachedDeviceFix.value = fix
        }
    }
}
