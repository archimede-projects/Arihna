package com.archimedeprojects.arihna.core.location.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.domain.LocationCoordinator
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.location.model.ManualCitySnapshot
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesDataStoreLocationPreferencesRepositoryAndroidTest {
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesDataStoreLocationPreferencesRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.filesDir, "datastore/location-${UUID.randomUUID()}.preferences_pb")
        file.parentFile?.mkdirs()
        file.delete()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        repository = PreferencesDataStoreLocationPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun unsetDeviceManualRoundTripAndSwitchAreAtomic() = runBlocking {
        assertEquals(LocationPreference.Unset, repository.preference.first())
        assertNull(repository.cachedDeviceFix.first())
        assertNoGeographicDefaults(dataStore.data.first())

        repository.selectDevice()
        assertEquals(LocationPreference.Device, repository.preference.first())
        assertManualKeysCleared(dataStore.data.first())

        val city = makkah()
        repository.selectManual(city)
        assertEquals(
            LocationPreference.Manual(ManualCitySnapshot.from(city)),
            repository.preference.first(),
        )

        repository.selectDevice()
        val afterDeviceSwitch = dataStore.data.first()
        assertEquals(LocationPreference.Device, LocationPreferencesCodec.decodePreference(afterDeviceSwitch))
        assertManualKeysCleared(afterDeviceSwitch)

        repository.selectManual(city)
        assertEquals(
            LocationPreference.Manual(ManualCitySnapshot.from(city)),
            repository.preference.first(),
        )
    }

    @Test
    fun cachedDeviceFixRoundTripsWithCoordinatesTimezoneTimestampAndAccuracy() = runBlocking {
        val fix = deviceFix()

        repository.saveDeviceFix(fix)

        assertEquals(fix, repository.cachedDeviceFix.first())
        assertEquals("Europe/Rome", dataStore.data.first()[LocationPreferencesCodec.deviceTimeZoneIdKey])
    }

    @Test
    fun incompleteManualRecordFailsWithPersistenceErrorWithoutSelectedLocation() = runBlocking {
        dataStore.edit { preferences ->
            preferences.clear()
            preferences[LocationPreferencesCodec.modeKey] = "MANUAL"
            preferences[LocationPreferencesCodec.manualIdKey] = 104515
            preferences[LocationPreferencesCodec.manualNameKey] = "Makkah"
        }

        val coordinator = LocationCoordinator(
            deviceLocationDataSource = unusedDeviceLocationSource(),
            cityRepository = emptyCityRepository(),
            preferencesRepository = repository,
        )

        val state = coordinator.restore(
            permissionState = LocationPermissionState.Granted,
            locationServicesEnabled = true,
        )

        assertEquals(
            LocationResolutionState.Unavailable(LocationFailure.PERSISTENCE_ERROR, null),
            state,
        )
    }

    @Test
    fun incompleteActiveDeviceCacheFailsWithPersistenceErrorWithoutFallback() = runBlocking {
        repository.selectDevice()
        dataStore.edit { preferences ->
            preferences[LocationPreferencesCodec.deviceLatitudeKey] = 41.9028
        }

        val coordinator = LocationCoordinator(
            deviceLocationDataSource = unusedDeviceLocationSource(),
            cityRepository = emptyCityRepository(),
            preferencesRepository = repository,
        )

        val state = coordinator.restore(
            permissionState = LocationPermissionState.Denied(canRequestAgain = false),
            locationServicesEnabled = false,
        )

        assertEquals(
            LocationResolutionState.Unavailable(LocationFailure.PERSISTENCE_ERROR, null),
            state,
        )
    }

    @Test
    fun emptyStoreNeverWritesOrReadsGeographicDefaults() = runBlocking {
        val raw = dataStore.data.first()

        assertEquals(LocationPreference.Unset, repository.preference.first())
        assertNull(repository.cachedDeviceFix.first())
        assertTrue(raw.asMap().isEmpty())
        assertNoGeographicDefaults(raw)

        repository.selectDevice()
        val deviceRaw = dataStore.data.first()
        assertFalse(deviceRaw.asMap().keys.contains(LocationPreferencesCodec.deviceLatitudeKey))
        assertFalse(deviceRaw.asMap().keys.contains(LocationPreferencesCodec.deviceLongitudeKey))
        assertFalse(deviceRaw.asMap().keys.contains(LocationPreferencesCodec.deviceTimeZoneIdKey))
    }

    private fun assertNoGeographicDefaults(preferences: Preferences) {
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualLatitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualLongitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualTimeZoneIdKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.deviceLatitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.deviceLongitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.deviceTimeZoneIdKey))
    }

    private fun assertManualKeysCleared(preferences: Preferences) {
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualIdKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualNameKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualRegionKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualCountryNameKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualCountryCodeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualLatitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualLongitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualTimeZoneIdKey))
    }

    private fun makkah() = ManualCity(
        id = 104515,
        name = "Makkah",
        regionName = "Makkah Province",
        countryName = "Saudi Arabia",
        countryCode = "SA",
        coordinates = Coordinates(21.427009, 39.828685),
        zoneId = ZoneId.of("Asia/Riyadh"),
    )

    private fun deviceFix() = DeviceLocationFix(
        coordinates = Coordinates(41.9028, 12.4964),
        zoneId = ZoneId.of("Europe/Rome"),
        capturedAt = Instant.parse("2026-08-29T10:00:00Z"),
        accuracyMeters = 1500f,
    )

    private fun unusedDeviceLocationSource() = object : DeviceLocationDataSource {
        override suspend fun getCurrentLocation(): DeviceLocationResult = error("Device source must not be called")
        override suspend fun getLastKnownLocation(): DeviceLocationResult = error("Device source must not be called")
        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = emptyFlow()
    }

    private fun emptyCityRepository() = object : CityRepository {
        override suspend fun search(query: String): List<ManualCity> = emptyList()
        override suspend fun findById(id: Long): ManualCity? = null
        override suspend fun nearest(coordinates: Coordinates): ManualCity? = null
    }
}
