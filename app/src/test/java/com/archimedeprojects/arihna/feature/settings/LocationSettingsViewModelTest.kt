package com.archimedeprojects.arihna.feature.settings

import androidx.lifecycle.SavedStateHandle
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.data.UnsupportedCityTimeZoneException
import com.archimedeprojects.arihna.core.location.domain.LocationCoordinator
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.location.model.ManualCitySnapshot
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoreSearchAndManualSelectionUseExistingDomainBoundaries() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository()
        val cities = FakeCityRepository().apply {
            searchResults = listOf(roma())
            citiesById[roma().id] = roma()
        }
        val device = FakeDeviceLocationDataSource()
        val viewModel = viewModel(preferences, cities, device)

        viewModel.onForeground(LocationPermissionState.NotRequested, locationServicesEnabled = true)
        advanceUntilIdle()
        assertEquals(LocationResolutionState.Unconfigured, viewModel.uiState.value.resolutionState)
        assertEquals(LocationModeUi.Unconfigured, viewModel.uiState.value.activeMode)

        viewModel.onSearchQueryChanged("Roma")
        advanceTimeBy(250)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.searchResults.size)
        assertEquals("Roma", viewModel.uiState.value.searchResults.single().name)

        viewModel.selectManual(roma().id)
        advanceUntilIdle()
        val ready = viewModel.uiState.value.resolutionState as LocationResolutionState.Ready
        assertEquals("Roma, Lazio, Italia", ready.location.displayName)
        assertEquals(ZoneId.of("Europe/Rome"), ready.location.zoneId)
        assertEquals(LocationModeUi.Manual, viewModel.uiState.value.activeMode)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())

        viewModel.onBackground()
    }

    @Test
    fun rationaleAndDeniedDeviceStateNeverNeedAnAutomaticPrompt() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository()
        val viewModel = viewModel(
            preferences = preferences,
            cities = FakeCityRepository(),
            device = FakeDeviceLocationDataSource(),
        )

        assertFalse(viewModel.uiState.value.rationaleVisible)
        assertFalse(viewModel.hasRequestedPermissionBefore())

        viewModel.onUseDeviceClick()
        assertTrue(viewModel.uiState.value.rationaleVisible)
        assertFalse(viewModel.hasRequestedPermissionBefore())

        viewModel.markPermissionRequestStarted()
        assertFalse(viewModel.uiState.value.rationaleVisible)
        assertTrue(viewModel.hasRequestedPermissionBefore())

        viewModel.selectDevice(
            permissionState = LocationPermissionState.Denied(canRequestAgain = false),
            locationServicesEnabled = true,
        )
        advanceUntilIdle()

        val denied = viewModel.uiState.value.resolutionState as LocationResolutionState.PermissionDenied
        assertFalse(denied.canRequestAgain)
        assertEquals(LocationModeUi.Device, viewModel.uiState.value.activeMode)
        assertEquals(LocationPreference.Device, preferences.preference.first())

        viewModel.onBackground()
    }

    @Test
    fun unsupportedManualTimezoneSurfacesControlledDomainFailure() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository()
        val cities = FakeCityRepository().apply {
            unsupportedIds += 999L
        }
        val viewModel = viewModel(
            preferences = preferences,
            cities = cities,
            device = FakeDeviceLocationDataSource(),
        )

        viewModel.selectManual(999L)
        advanceUntilIdle()

        val unavailable = viewModel.uiState.value.resolutionState as LocationResolutionState.Unavailable
        assertEquals(LocationFailure.UNSUPPORTED_TIME_ZONE, unavailable.reason)
        assertEquals(LocationModeUi.Unconfigured, viewModel.uiState.value.activeMode)
        viewModel.onBackground()
    }

    @Test
    fun foregroundDeviceUpdatesPassBackThroughCoordinatorAndStopOnBackground() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository(initialPreference = LocationPreference.Device)
        val cities = FakeCityRepository()
        val firstFix = deviceFix(44.80, 11.00, "Europe/Rome", "2026-08-30T10:44:05Z")
        val secondFix = deviceFix(45.50, 11.00, "Europe/Rome", "2026-08-30T11:10:00Z")
        val thirdFix = deviceFix(46.20, 11.00, "Europe/Rome", "2026-08-30T11:30:00Z")
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Success(firstFix)
        }
        val viewModel = viewModel(preferences, cities, device)

        viewModel.onForeground(LocationPermissionState.Granted, locationServicesEnabled = true)
        advanceUntilIdle()
        assertEquals(firstFix, preferences.cachedDeviceFix.first())
        assertEquals(LocationModeUi.Device, viewModel.uiState.value.activeMode)

        device.updates.emit(secondFix)
        advanceUntilIdle()
        assertEquals(secondFix, preferences.cachedDeviceFix.first())

        viewModel.onBackground()
        advanceUntilIdle()
        device.updates.emit(thirdFix)
        advanceUntilIdle()
        assertEquals(secondFix, preferences.cachedDeviceFix.first())
    }

    private fun viewModel(
        preferences: FakePreferencesRepository,
        cities: FakeCityRepository,
        device: FakeDeviceLocationDataSource,
    ): LocationSettingsViewModel = LocationSettingsViewModel(
        coordinator = LocationCoordinator(
            deviceLocationDataSource = device,
            cityRepository = cities,
            preferencesRepository = preferences,
        ),
        cityRepository = cities,
        deviceLocationDataSource = device,
        preferencesRepository = preferences,
        savedStateHandle = SavedStateHandle(),
    )

    private fun roma() = ManualCity(
        id = 3169070L,
        name = "Roma",
        regionName = "Lazio",
        countryName = "Italia",
        countryCode = "IT",
        coordinates = Coordinates(41.89193, 12.51133),
        zoneId = ZoneId.of("Europe/Rome"),
    )

    private fun deviceFix(
        latitude: Double,
        longitude: Double,
        zone: String,
        capturedAt: String,
    ) = DeviceLocationFix(
        coordinates = Coordinates(latitude, longitude),
        zoneId = ZoneId.of(zone),
        capturedAt = Instant.parse(capturedAt),
        accuracyMeters = 2_000f,
    )

    private class FakeDeviceLocationDataSource : DeviceLocationDataSource {
        var currentResult: DeviceLocationResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        val updates = MutableSharedFlow<DeviceLocationFix>(extraBufferCapacity = 4)

        override suspend fun getCurrentLocation(): DeviceLocationResult = currentResult
        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = updates
    }

    private class FakePreferencesRepository(
        initialPreference: LocationPreference = LocationPreference.Unset,
    ) : LocationPreferencesRepository {
        private val preferenceState = MutableStateFlow(initialPreference)
        private val cachedState = MutableStateFlow<DeviceLocationFix?>(null)

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
        var searchResults: List<CitySearchResult> = emptyList()
        val citiesById = mutableMapOf<Long, ManualCity>()
        val unsupportedIds = mutableSetOf<Long>()

        override suspend fun search(query: String): List<CitySearchResult> = searchResults

        override suspend fun findById(id: Long): ManualCity? {
            if (id in unsupportedIds) {
                throw UnsupportedCityTimeZoneException(id, "America/Nuuk")
            }
            return citiesById[id]
        }

        override suspend fun nearest(coordinates: Coordinates): CitySearchResult? = null
    }
}
