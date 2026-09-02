package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.location.model.ManualCitySnapshot
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCoordinatorTest {
    @Test
    fun freshFirstFixIsPersistedAndReturnedFresh() = runBlocking {
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Success(romeFix())
        }
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Device)
        val coordinator = coordinator(device = device, preferences = preferences)

        val state = coordinator.resolveDevice(LocationPermissionState.Granted, locationServicesEnabled = true)

        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.FRESH, ready.freshness)
        assertEquals(romeFix().coordinates, ready.location.coordinates)
        assertEquals(romeFix(), preferences.cachedState.value)
        assertEquals(1, device.currentCalls)
    }

    @Test
    fun freshFixBelowFiveKilometersKeepsAcceptedCachedLocation() = runBlocking {
        val cached = romeFix()
        val nearby = cached.copy(
            coordinates = Coordinates(41.9200, 12.4964),
            capturedAt = Instant.parse("2026-08-29T11:00:00Z"),
        )
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Success(nearby)
        }
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Device, cached)
        val coordinator = coordinator(device = device, preferences = preferences)

        val state = coordinator.resolveDevice(LocationPermissionState.Granted, true)

        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.CACHED, ready.freshness)
        assertEquals(cached.coordinates, ready.location.coordinates)
        assertEquals(cached, preferences.cachedState.value)
    }

    @Test
    fun freshFixAboveFiveKilometersReplacesCache() = runBlocking {
        val cached = romeFix()
        val moved = cached.copy(
            coordinates = Coordinates(42.0500, 12.4964),
            capturedAt = Instant.parse("2026-08-29T11:00:00Z"),
        )
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Success(moved)
        }
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Device, cached)
        val coordinator = coordinator(device = device, preferences = preferences)

        val state = coordinator.resolveDevice(LocationPermissionState.Granted, true)

        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.FRESH, ready.freshness)
        assertEquals(moved.coordinates, ready.location.coordinates)
        assertEquals(moved, preferences.cachedState.value)
    }

    @Test
    fun timezoneChangeReplacesCacheEvenWithoutFiveKilometerMovement() = runBlocking {
        val cached = romeFix()
        val changedZone = cached.copy(
            coordinates = Coordinates(41.9030, 12.4965),
            zoneId = ZoneId.of("Europe/Paris"),
            capturedAt = Instant.parse("2026-08-29T11:00:00Z"),
        )
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Success(changedZone)
        }
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Device, cached)
        val coordinator = coordinator(device = device, preferences = preferences)

        val state = coordinator.resolveDevice(LocationPermissionState.Granted, true)

        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.FRESH, ready.freshness)
        assertEquals(ZoneId.of("Europe/Paris"), ready.location.zoneId)
        assertEquals(changedZone, preferences.cachedState.value)
    }

    @Test
    fun timeoutWithPersistedCacheReturnsCachedReady() = runBlocking {
        val device = FakeDeviceLocationDataSource().apply {
            delayMillis = 100
            currentResult = DeviceLocationResult.Success(romeFix())
        }
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Device, romeFix())
        val coordinator = coordinator(
            device = device,
            preferences = preferences,
            policy = LocationUpdatePolicy(currentFixTimeout = Duration.ofMillis(10)),
        )

        val state = coordinator.resolveDevice(LocationPermissionState.Granted, true)

        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.CACHED, ready.freshness)
        assertEquals(romeFix().coordinates, ready.location.coordinates)
    }

    @Test
    fun timeoutUsesRealLastKnownAsCachedReady() = runBlocking {
        val lastKnown = romeFix().copy(capturedAt = Instant.parse("2026-08-29T09:30:00Z"))
        val device = FakeDeviceLocationDataSource().apply {
            delayMillis = 100
            currentResult = DeviceLocationResult.Success(romeFix())
            lastKnownResult = DeviceLocationResult.Success(lastKnown, LocationFreshness.CACHED)
        }
        val coordinator = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device),
            policy = LocationUpdatePolicy(currentFixTimeout = Duration.ofMillis(10)),
        )

        val state = coordinator.resolveDevice(LocationPermissionState.Granted, true)

        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.CACHED, ready.freshness)
        assertEquals(lastKnown.coordinates, ready.location.coordinates)
        assertEquals(1, device.lastKnownCalls)
    }

    @Test
    fun timeoutWithoutCacheNeverInventsLocation() = runBlocking {
        val device = FakeDeviceLocationDataSource().apply {
            delayMillis = 100
            currentResult = DeviceLocationResult.Success(romeFix())
        }
        val coordinator = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device),
            policy = LocationUpdatePolicy(currentFixTimeout = Duration.ofMillis(10)),
        )

        val state = coordinator.resolveDevice(LocationPermissionState.Granted, true)

        val unavailable = state as LocationResolutionState.Unavailable
        assertEquals(LocationFailure.TIMEOUT, unavailable.reason)
        assertNull(unavailable.cachedLocation)
    }

    @Test
    fun providerUnavailableUsesPersistedRealCacheButNeverInventsOne() = runBlocking {
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }
        val withCache = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device, romeFix()),
        ).resolveDevice(LocationPermissionState.Granted, true) as LocationResolutionState.Ready
        val withoutCache = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device),
        ).resolveDevice(LocationPermissionState.Granted, true) as LocationResolutionState.Unavailable

        assertEquals(LocationFreshness.CACHED, withCache.freshness)
        assertEquals(romeFix().coordinates, withCache.location.coordinates)
        assertEquals(LocationFailure.NO_PROVIDER, withoutCache.reason)
        assertNull(withoutCache.cachedLocation)
    }

    @Test
    fun permissionDeniedDoesNotCallDeviceProviderAndPreservesCacheIfPresent() = runBlocking {
        val device = FakeDeviceLocationDataSource()
        val state = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device, romeFix()),
        ).resolveDevice(LocationPermissionState.Denied(canRequestAgain = false), true)

        val denied = state as LocationResolutionState.PermissionDenied
        assertEquals(false, denied.canRequestAgain)
        assertEquals(romeFix().coordinates, denied.cachedLocation?.coordinates)
        assertEquals(0, device.currentCalls)
    }

    @Test
    fun permissionDeniedWithoutCacheHasNoCoordinates() = runBlocking {
        val state = coordinator(
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device),
        ).resolveDevice(LocationPermissionState.Denied(canRequestAgain = true), true)

        val denied = state as LocationResolutionState.PermissionDenied
        assertTrue(denied.canRequestAgain)
        assertNull(denied.cachedLocation)
    }

    @Test
    fun disabledLocationServicesPreserveOnlyExistingRealCache() = runBlocking {
        val withCache = coordinator(
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device, romeFix()),
        ).resolveDevice(LocationPermissionState.Granted, false) as LocationResolutionState.LocationServicesDisabled
        val withoutCache = coordinator(
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device),
        ).resolveDevice(LocationPermissionState.Granted, false) as LocationResolutionState.LocationServicesDisabled

        assertEquals(romeFix().coordinates, withCache.cachedLocation?.coordinates)
        assertNull(withoutCache.cachedLocation)
    }

    @Test
    fun invalidFreshFixIsRejectedAndPreviousRealFixRemainsAvailable() = runBlocking {
        val invalid = romeFix().copy(coordinates = Coordinates(Double.NaN, 12.4964))
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Success(invalid)
        }
        val state = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device, romeFix()),
        ).resolveDevice(LocationPermissionState.Granted, true)

        val unavailable = state as LocationResolutionState.Unavailable
        assertEquals(LocationFailure.INVALID_FIX, unavailable.reason)
        assertEquals(romeFix().coordinates, unavailable.cachedLocation?.coordinates)
    }

    @Test
    fun switchingManualThenDevicePersistsExplicitPreferenceAndResolvesEachSource() = runBlocking {
        val makkah = makkahCity()
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Success(romeFix())
        }
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Unset)
        val cityRepository = FakeCityRepository(listOf(makkah, romeCity()))
        val coordinator = LocationCoordinator(device, cityRepository, preferences)

        val manual = coordinator.selectManual(makkah) as LocationResolutionState.Ready
        assertTrue(preferences.preferenceState.value is LocationPreference.Manual)
        assertTrue(manual.location.source is LocationSource.Manual)
        assertEquals(ZoneId.of("Asia/Riyadh"), manual.location.zoneId)

        val deviceState = coordinator.selectDevice(LocationPermissionState.Granted, true) as LocationResolutionState.Ready
        assertEquals(LocationPreference.Device, preferences.preferenceState.value)
        assertTrue(deviceState.location.source is LocationSource.Device)
        assertEquals(romeFix().coordinates, deviceState.location.coordinates)
    }

    @Test
    fun manualSelectionSurvivesCoordinatorRecreationViaPersistedSnapshot() = runBlocking {
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Unset)
        val first = coordinator(preferences = preferences)
        first.selectManual(makkahCity())

        val recreated = coordinator(preferences = preferences)
        val state = recreated.restore(LocationPermissionState.NotRequested, false)

        val ready = state as LocationResolutionState.Ready
        assertTrue(ready.location.source is LocationSource.Manual)
        assertEquals(makkahCity().coordinates, ready.location.coordinates)
        assertEquals(ZoneId.of("Asia/Riyadh"), ready.location.zoneId)
        assertNull(ready.freshness)
    }

    @Test
    fun malformedPersistedManualSnapshotReturnsPersistenceErrorWithoutFallback() = runBlocking {
        val malformed = ManualCitySnapshot(
            id = 1,
            name = "Broken",
            regionName = null,
            countryName = "Nowhere",
            countryCode = "XX",
            latitude = 10.0,
            longitude = 20.0,
            timeZoneId = "Mars/Olympus",
        )
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Manual(malformed))

        val state = coordinator(preferences = preferences)
            .restore(LocationPermissionState.NotRequested, false)

        val unavailable = state as LocationResolutionState.Unavailable
        assertEquals(LocationFailure.PERSISTENCE_ERROR, unavailable.reason)
        assertNull(unavailable.cachedLocation)
    }

    @Test
    fun preferenceReadFailureReturnsControlledPersistenceError() = runBlocking {
        val preferences = FakeLocationPreferencesRepository(LocationPreference.Unset).apply {
            failPreferenceRead = true
        }

        val state = coordinator(preferences = preferences)
            .restore(LocationPermissionState.NotRequested, false)

        val unavailable = state as LocationResolutionState.Unavailable
        assertEquals(LocationFailure.PERSISTENCE_ERROR, unavailable.reason)
        assertNull(unavailable.cachedLocation)
    }

    @Test
    fun selectingMissingManualCityReturnsCityNotFound() = runBlocking {
        val state = coordinator(cityRepository = FakeCityRepository(emptyList()))
            .selectManual(cityId = 999)

        val unavailable = state as LocationResolutionState.Unavailable
        assertEquals(LocationFailure.CITY_NOT_FOUND, unavailable.reason)
    }

    private fun coordinator(
        device: FakeDeviceLocationDataSource = FakeDeviceLocationDataSource(),
        cityRepository: FakeCityRepository = FakeCityRepository(listOf(romeCity(), makkahCity())),
        preferences: FakeLocationPreferencesRepository = FakeLocationPreferencesRepository(LocationPreference.Unset),
        policy: LocationUpdatePolicy = LocationUpdatePolicy(),
    ) = LocationCoordinator(device, cityRepository, preferences, policy)

    private fun romeFix() = DeviceLocationFix(
        coordinates = Coordinates(41.9028, 12.4964),
        zoneId = ZoneId.of("Europe/Rome"),
        capturedAt = Instant.parse("2026-08-29T10:00:00Z"),
        accuracyMeters = 1_500f,
    )

    private fun romeCity() = ManualCity(
        id = 3169070,
        name = "Rome",
        regionName = "Lazio",
        countryName = "Italy",
        countryCode = "IT",
        coordinates = Coordinates(41.8919, 12.5113),
        zoneId = ZoneId.of("Europe/Rome"),
    )

    private fun makkahCity() = ManualCity(
        id = 104515,
        name = "Makkah",
        regionName = "Makkah Province",
        countryName = "Saudi Arabia",
        countryCode = "SA",
        coordinates = Coordinates(21.427009, 39.828685),
        zoneId = ZoneId.of("Asia/Riyadh"),
    )

    private class FakeDeviceLocationDataSource : DeviceLocationDataSource {
        var currentResult: DeviceLocationResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        var delayMillis: Long = 0
        var currentCalls: Int = 0
        var lastKnownResult: DeviceLocationResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        var lastKnownCalls: Int = 0
        private val updates = MutableSharedFlow<DeviceLocationFix>()

        override suspend fun getLastKnownLocation(): DeviceLocationResult {
            lastKnownCalls += 1
            return lastKnownResult
        }

        override suspend fun getCurrentLocation(): DeviceLocationResult {
            currentCalls += 1
            if (delayMillis > 0) delay(delayMillis)
            return currentResult
        }

        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = updates
    }

    private class FakeCityRepository(
        cities: List<ManualCity>,
    ) : CityRepository {
        private val byId = cities.associateBy { it.id }

        override suspend fun search(query: String): List<ManualCity> = byId.values
            .filter { it.name.contains(query, ignoreCase = true) }

        override suspend fun findById(id: Long): ManualCity? = byId[id]

        override suspend fun nearest(coordinates: Coordinates): ManualCity? = byId.values.minByOrNull {
            val lat = it.coordinates.latitude - coordinates.latitude
            val lon = it.coordinates.longitude - coordinates.longitude
            lat * lat + lon * lon
        }
    }

    private class FakeLocationPreferencesRepository(
        initialPreference: LocationPreference,
        initialCachedFix: DeviceLocationFix? = null,
    ) : LocationPreferencesRepository {
        val preferenceState = MutableStateFlow(initialPreference)
        val cachedState = MutableStateFlow(initialCachedFix)
        var failPreferenceRead: Boolean = false

        override val preference: Flow<LocationPreference>
            get() = if (failPreferenceRead) {
                flow { throw IllegalStateException("malformed persisted preference") }
            } else {
                preferenceState
            }

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
}
