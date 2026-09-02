package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.location.model.ManualCitySnapshot
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationCacheFirstRestoreTest {
    @Test
    fun persistedDeviceFixReturnsCachedReadyWithoutCallingProvider() = runBlocking {
        val cached = fix()
        val device = FakeDeviceLocationDataSource()
        val coordinator = coordinator(
            device = device,
            preferences = FakePreferencesRepository(LocationPreference.Device, cached),
        )

        val state = coordinator.restorePersistedState(
            permissionState = LocationPermissionState.Granted,
            locationServicesEnabled = true,
        )

        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.CACHED, ready.freshness)
        assertEquals(cached.coordinates, ready.location.coordinates)
        assertEquals(0, device.currentCalls)
        assertEquals(0, device.lastKnownCalls)
    }

    @Test
    fun deviceModeWithoutPersistedFixReturnsResolvingWithoutCallingProvider() = runBlocking {
        val device = FakeDeviceLocationDataSource()
        val coordinator = coordinator(
            device = device,
            preferences = FakePreferencesRepository(LocationPreference.Device),
        )

        val state = coordinator.restorePersistedState(
            permissionState = LocationPermissionState.Granted,
            locationServicesEnabled = true,
        )

        assertEquals(LocationResolutionState.Resolving, state)
        assertEquals(0, device.currentCalls)
        assertEquals(0, device.lastKnownCalls)
    }

    @Test
    fun permissionDeniedRemainsExplicitAndCarriesCachedLocation() = runBlocking {
        val cached = fix()
        val device = FakeDeviceLocationDataSource()
        val coordinator = coordinator(
            device = device,
            preferences = FakePreferencesRepository(LocationPreference.Device, cached),
        )

        val state = coordinator.restorePersistedState(
            permissionState = LocationPermissionState.Denied(canRequestAgain = false),
            locationServicesEnabled = true,
        )

        val denied = state as LocationResolutionState.PermissionDenied
        assertEquals(false, denied.canRequestAgain)
        assertEquals(cached.coordinates, denied.cachedLocation?.coordinates)
        assertEquals(0, device.currentCalls)
        assertEquals(0, device.lastKnownCalls)
    }

    private fun coordinator(
        device: FakeDeviceLocationDataSource,
        preferences: FakePreferencesRepository,
    ) = LocationCoordinator(
        deviceLocationDataSource = device,
        cityRepository = FakeCityRepository(),
        preferencesRepository = preferences,
    )

    private fun fix() = DeviceLocationFix(
        coordinates = Coordinates(41.9028, 12.4964),
        zoneId = ZoneId.of("Europe/Rome"),
        capturedAt = Instant.parse("2026-09-02T08:00:00Z"),
        accuracyMeters = 2_000f,
    )

    private class FakeDeviceLocationDataSource : DeviceLocationDataSource {
        var currentCalls: Int = 0
        var lastKnownCalls: Int = 0
        private val updates = MutableSharedFlow<DeviceLocationFix>()

        override suspend fun getCurrentLocation(): DeviceLocationResult {
            currentCalls += 1
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        override suspend fun getLastKnownLocation(): DeviceLocationResult {
            lastKnownCalls += 1
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = updates
    }

    private class FakePreferencesRepository(
        initialPreference: LocationPreference,
        initialCachedFix: DeviceLocationFix? = null,
    ) : LocationPreferencesRepository {
        private val preferenceState = MutableStateFlow(initialPreference)
        private val cachedState = MutableStateFlow(initialCachedFix)

        override val preference: Flow<LocationPreference> = preferenceState
        override val cachedDeviceFix: Flow<DeviceLocationFix?> = cachedState

        override suspend fun selectDevice() {
            preferenceState.value = LocationPreference.Device
        }

        override suspend fun selectManual(city: ManualCity) {
            preferenceState.value = LocationPreference.Manual(ManualCitySnapshot.from(city))
        }

        override suspend fun saveDeviceFix(fix: DeviceLocationFix) {
            cachedState.value = fix
        }
    }

    private class FakeCityRepository : CityRepository {
        override suspend fun search(query: String): List<CitySearchResult> = emptyList()
        override suspend fun findById(id: Long): ManualCity? = null
        override suspend fun nearest(coordinates: Coordinates): CitySearchResult? = null
    }
}
