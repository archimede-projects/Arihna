package com.archimedeprojects.arihna.feature.settings

import androidx.lifecycle.SavedStateHandle
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.domain.LocationCoordinator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationCacheFirstForegroundTest {
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
    fun cachedDeviceFixIsReadyBeforeForegroundRevalidationCompletes() = runTest(dispatcher) {
        val cached = fix(41.9028, 12.4964, "2026-09-02T08:00:00Z")
        val moved = fix(42.0500, 12.4964, "2026-09-02T13:00:00Z")
        val preferences = FakePreferencesRepository(
            initialPreference = LocationPreference.Device,
            initialCachedFix = cached,
        )
        val device = FakeDeviceLocationDataSource().apply {
            currentDelayMillis = 1_000L
            currentResult = DeviceLocationResult.Success(moved)
        }
        val viewModel = viewModel(preferences, device)

        viewModel.onForeground(LocationPermissionState.Granted, locationServicesEnabled = true)
        runCurrent()

        val immediate = viewModel.uiState.value.resolutionState as LocationResolutionState.Ready
        assertEquals(LocationFreshness.CACHED, immediate.freshness)
        assertEquals(cached.coordinates, immediate.location.coordinates)
        assertEquals(LocationModeUi.Device, viewModel.uiState.value.activeMode)
        assertEquals(1, device.currentCalls)

        advanceTimeBy(1_000L)
        advanceUntilIdle()

        val refreshed = viewModel.uiState.value.resolutionState as LocationResolutionState.Ready
        assertEquals(LocationFreshness.FRESH, refreshed.freshness)
        assertEquals(moved.coordinates, refreshed.location.coordinates)
        viewModel.onBackground()
    }

    @Test
    fun failedRevalidationDoesNotReplaceAlreadyReadyCachedState() = runTest(dispatcher) {
        val cached = fix(41.9028, 12.4964, "2026-09-02T08:00:00Z")
        val invalid = cached.copy(
            coordinates = Coordinates(Double.NaN, 12.4964),
            capturedAt = Instant.parse("2026-09-02T13:00:00Z"),
        )
        val preferences = FakePreferencesRepository(
            initialPreference = LocationPreference.Device,
            initialCachedFix = cached,
        )
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Success(invalid)
        }
        val viewModel = viewModel(preferences, device)

        viewModel.onForeground(LocationPermissionState.Granted, locationServicesEnabled = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value.resolutionState
        assertTrue(state is LocationResolutionState.Ready)
        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.CACHED, ready.freshness)
        assertEquals(cached.coordinates, ready.location.coordinates)
        viewModel.onBackground()
    }

    @Test
    fun deviceModeWithoutCacheStillShowsResolvingWhileCurrentIsPending() = runTest(dispatcher) {
        val preferences = FakePreferencesRepository(initialPreference = LocationPreference.Device)
        val device = FakeDeviceLocationDataSource().apply {
            currentDelayMillis = 1_000L
            currentResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }
        val viewModel = viewModel(preferences, device)

        viewModel.onForeground(LocationPermissionState.Granted, locationServicesEnabled = true)
        runCurrent()

        assertEquals(LocationResolutionState.Resolving, viewModel.uiState.value.resolutionState)
        assertEquals(1, device.currentCalls)
        viewModel.onBackground()
    }

    private fun viewModel(
        preferences: FakePreferencesRepository,
        device: FakeDeviceLocationDataSource,
    ): LocationSettingsViewModel {
        val cities = FakeCityRepository()
        return LocationSettingsViewModel(
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
    }

    private fun fix(latitude: Double, longitude: Double, capturedAt: String) = DeviceLocationFix(
        coordinates = Coordinates(latitude, longitude),
        zoneId = ZoneId.of("Europe/Rome"),
        capturedAt = Instant.parse(capturedAt),
        accuracyMeters = 2_000f,
    )

    private class FakeDeviceLocationDataSource : DeviceLocationDataSource {
        var currentDelayMillis: Long = 0L
        var currentCalls: Int = 0
        var currentResult: DeviceLocationResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        private val updates = MutableSharedFlow<DeviceLocationFix>()

        override suspend fun getCurrentLocation(): DeviceLocationResult {
            currentCalls += 1
            if (currentDelayMillis > 0L) delay(currentDelayMillis)
            return currentResult
        }

        override suspend fun getLastKnownLocation(): DeviceLocationResult =
            DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)

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
