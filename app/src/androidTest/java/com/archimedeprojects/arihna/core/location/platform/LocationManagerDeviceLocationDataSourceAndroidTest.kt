package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.core.location.domain.LocationUpdatePolicy
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import java.io.FileInputStream
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Executor
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationManagerDeviceLocationDataSourceAndroidTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext: Context = instrumentation.targetContext
    private val testContext: Context = instrumentation.context
    private val locationManager: LocationManager =
        targetContext.getSystemService(LocationManager::class.java)
    private val providerName = "arihna_step5_${UUID.randomUUID()}"
    private val directExecutor = Executor { command -> command.run() }
    private val fixedZone = ZoneId.of("Europe/Rome")
    private val fixedClock = Clock.fixed(Instant.parse("2026-08-30T06:30:00Z"), ZoneId.of("UTC"))

    @Before
    fun setUp() {
        setApi28LocationProvidersEnabled(true)
        grantCoarseAndMockLocationAppOp(targetContext.packageName)
        grantCoarseAndMockLocationAppOp(testContext.packageName)
        runCatching { locationManager.removeTestProvider(providerName) }
        locationManager.addTestProvider(
            providerName,
            true,
            false,
            false,
            false,
            true,
            true,
            true,
            Criteria.POWER_LOW,
            Criteria.ACCURACY_COARSE,
        )
        locationManager.setTestProviderEnabled(providerName, true)

        assertTrue("Location services must be enabled before each STEP 5 test", waitForLocationEnabled(true))
        assertTrue("Test provider must be enabled", locationManager.isProviderEnabled(providerName))
        assertTrue(
            "The API 28 test provider must be visible with ACCESS_COARSE_LOCATION only",
            providerName in locationManager.getProviders(true),
        )
    }

    @After
    fun tearDown() {
        runCatching { locationManager.setTestProviderEnabled(providerName, false) }
        runCatching { locationManager.removeTestProvider(providerName) }
        setApi28LocationProvidersEnabled(true)
    }

    @Test
    fun currentFixComesFromRealLocationManagerBridgeAndCapturesCurrentZone() = runBlocking {
        inject(latitude = 41.9028, longitude = 12.4964, accuracyMeters = 120f)
        val dataSource = dataSource()

        val result = withTimeout(5_000) { dataSource.getCurrentLocation() }
        val fix = requireSuccess(result)

        assertEquals(41.9028, fix.coordinates.latitude, 0.0)
        assertEquals(12.4964, fix.coordinates.longitude, 0.0)
        assertEquals(fixedZone, fix.zoneId)
        assertEquals(120f, fix.accuracyMeters)
        assertTrue(fix.capturedAt.toEpochMilli() > 0L)
    }

    @Test
    fun foregroundFlowReceivesInjectedFrameworkLocationAndCancelsCleanly() = runBlocking {
        val dataSource = dataSource()
        val awaiting = async {
            withTimeout(5_000) { dataSource.observeSignificantUpdates().first() }
        }
        delay(150)

        inject(latitude = 41.9030, longitude = 12.4965, accuracyMeters = 200f)
        val fix = awaiting.await()

        assertEquals(41.9030, fix.coordinates.latitude, 0.0)
        assertEquals(12.4965, fix.coordinates.longitude, 0.0)
        assertEquals(fixedZone, fix.zoneId)
        assertEquals(200f, fix.accuracyMeters)

        // first() cancels collection, which drives callbackFlow awaitClose -> removeUpdates.
        // A later framework injection must therefore not require a live collector/listener.
        inject(latitude = 41.9040, longitude = 12.4970, accuracyMeters = 220f)
    }

    @Test
    fun noSelectedCoarseProviderReturnsControlledNoProvider() = runBlocking {
        val dataSource = LocationManagerDeviceLocationDataSource(
            context = targetContext,
            locationManager = locationManager,
            callbackExecutor = directExecutor,
            zoneIdProvider = { fixedZone },
            clock = fixedClock,
            providerSelector = { null },
            updatePolicy = LocationUpdatePolicy(),
        )

        val result = dataSource.getCurrentLocation()

        val unavailable = result as DeviceLocationResult.Unavailable
        assertEquals(LocationFailure.NO_PROVIDER, unavailable.reason)
    }

    @Test
    fun disabledLocationServicesAreExposedWithoutFabricatedFix() = runBlocking {
        try {
            setApi28LocationProvidersEnabled(false)
            assertTrue(
                "API 28 location provider toggle must make LocationManager report services disabled",
                waitForLocationEnabled(false),
            )
            assertFalse(locationManager.isLocationEnabled)
            assertFalse(AndroidLocationEnvironment(targetContext).isLocationServicesEnabled())

            val result = dataSource().getCurrentLocation()
            val unavailable = result as DeviceLocationResult.Unavailable
            assertEquals(LocationFailure.NO_PROVIDER, unavailable.reason)
        } finally {
            setApi28LocationProvidersEnabled(true)
            assertTrue(
                "API 28 location providers must be restored after the disabled-services test",
                waitForLocationEnabled(true),
            )
        }
    }

    @Test
    fun manifestAndRuntimePermissionRemainCoarseOnly() {
        val requested = targetContext.packageManager
            .getPackageInfo(targetContext.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toSet()
            .orEmpty()

        assertTrue(Manifest.permission.ACCESS_COARSE_LOCATION in requested)
        assertFalse(Manifest.permission.ACCESS_FINE_LOCATION in requested)
        assertFalse(Manifest.permission.ACCESS_BACKGROUND_LOCATION in requested)
        assertTrue(AndroidLocationEnvironment(targetContext).isCoarsePermissionGranted())
    }

    private fun dataSource(): LocationManagerDeviceLocationDataSource =
        LocationManagerDeviceLocationDataSource(
            context = targetContext,
            locationManager = locationManager,
            callbackExecutor = directExecutor,
            zoneIdProvider = { fixedZone },
            clock = fixedClock,
            providerSelector = { providerName },
            updatePolicy = LocationUpdatePolicy(
                significantDistanceMeters = 5_000.0,
                minimumForegroundUpdateInterval = Duration.ofMinutes(15),
                currentFixTimeout = Duration.ofSeconds(20),
            ),
        )

    private fun requireSuccess(result: DeviceLocationResult): DeviceLocationFix =
        when (result) {
            is DeviceLocationResult.Success -> result.fix
            is DeviceLocationResult.Unavailable -> throw AssertionError(
                "Expected DeviceLocationResult.Success but was Unavailable(${result.reason})",
            )
        }

    @Suppress("DEPRECATION")
    private fun inject(latitude: Double, longitude: Double, accuracyMeters: Float) {
        val location = Location(providerName).apply {
            this.latitude = latitude
            this.longitude = longitude
            accuracy = accuracyMeters
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
        }
        locationManager.setTestProviderLocation(providerName, location)
    }

    private fun grantCoarseAndMockLocationAppOp(packageName: String) {
        shell("pm grant $packageName ${Manifest.permission.ACCESS_COARSE_LOCATION}")
        shell("appops set $packageName android:mock_location allow")
    }

    private fun setApi28LocationProvidersEnabled(enabled: Boolean) {
        val operator = if (enabled) "+" else "-"
        shell("settings put secure location_providers_allowed ${operator}gps")
        shell("settings put secure location_providers_allowed ${operator}network")
    }

    private fun waitForLocationEnabled(expected: Boolean): Boolean {
        repeat(30) {
            if (runCatching { locationManager.isLocationEnabled }.getOrDefault(!expected) == expected) {
                return true
            }
            Thread.sleep(100)
        }
        return runCatching { locationManager.isLocationEnabled }.getOrDefault(!expected) == expected
    }

    private fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
        }
    }
}
