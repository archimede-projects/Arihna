from pathlib import Path

path = Path('app/src/test/java/com/archimedeprojects/arihna/core/location/domain/LocationCoordinatorTest.kt')
text = path.read_text()
text = text.replace(
    'import com.archimedeprojects.arihna.core.location.model.LocationFreshness\nimport com.archimedeprojects.arihna.core.location.model.LocationFreshness\n',
    'import com.archimedeprojects.arihna.core.location.model.LocationFreshness\n',
    1,
)
old_timeout = '''    @Test
    fun timeoutWithCacheReturnsControlledUnavailableWithCachedLocation() = runBlocking {
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

        val unavailable = state as LocationResolutionState.Unavailable
        assertEquals(LocationFailure.TIMEOUT, unavailable.reason)
        assertEquals(romeFix().coordinates, unavailable.cachedLocation?.coordinates)
    }
'''
new_timeout = '''    @Test
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
'''
if old_timeout not in text:
    raise SystemExit('old timeout test not found')
text = text.replace(old_timeout, new_timeout, 1)
old_provider = '''    @Test
    fun providerUnavailableFallsBackOnlyToRealCache() = runBlocking {
        val device = FakeDeviceLocationDataSource().apply {
            currentResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }
        val withCache = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device, romeFix()),
        ).resolveDevice(LocationPermissionState.Granted, true) as LocationResolutionState.Unavailable
        val withoutCache = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device),
        ).resolveDevice(LocationPermissionState.Granted, true) as LocationResolutionState.Unavailable

        assertEquals(LocationFailure.NO_PROVIDER, withCache.reason)
        assertEquals(romeFix().coordinates, withCache.cachedLocation?.coordinates)
        assertEquals(LocationFailure.NO_PROVIDER, withoutCache.reason)
        assertNull(withoutCache.cachedLocation)
    }
'''
new_provider = '''    @Test
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
'''
if old_provider not in text:
    raise SystemExit('old provider test not found')
text = text.replace(old_provider, new_provider, 1)
path.write_text(text)

unsupported = Path('app/src/test/java/com/archimedeprojects/arihna/core/location/domain/UnsupportedManualCitySelectionTest.kt')
unsupported_text = unsupported.read_text()
old_fake = '''    private class NoDeviceLocation : DeviceLocationDataSource {
        override suspend fun getCurrentLocation(): DeviceLocationResult =
            DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)

        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = emptyFlow()
    }
'''
new_fake = '''    private class NoDeviceLocation : DeviceLocationDataSource {
        override suspend fun getCurrentLocation(): DeviceLocationResult =
            DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)

        override suspend fun getLastKnownLocation(): DeviceLocationResult =
            DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)

        override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = emptyFlow()
    }
'''
if old_fake not in unsupported_text:
    raise SystemExit('NoDeviceLocation fake not found')
unsupported.write_text(unsupported_text.replace(old_fake, new_fake, 1))
