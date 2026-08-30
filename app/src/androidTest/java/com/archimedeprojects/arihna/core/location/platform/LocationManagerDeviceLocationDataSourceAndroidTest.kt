package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.LocationManager
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.core.location.domain.LocationUpdatePolicy
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import java.io.FileInputStream
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        shell("settings put secure location_mode 3")
        grantCoarseAndMockLocationAppOp(targetContext.packageName)
        grantCoarseAndMockLocationAppOp(testContext.packageName)
        runCatching { locationManager.removeTestProvider(providerName) }
        locationManager.addTestProvider(
            providerName,
            false,
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
        assertTrue(
            "Location services must be enabled before each STEP 5 test",
            waitForLocationEnabled(expected = true),
        )
        assertTrue("STEP 5 test provider must be enabled", locationManager.isProviderEnabled(providerName))
    }

    @After
    fun tearDown() {
        runCatching { locationManager.setTestProviderEnabled(providerName, false) }
        runCatching { locationManager.removeTestProvider(providerName) }
        shell("settings put secure location_mode 3")
    }

    @Test
    fun currentLocationRegistersWithFrameworkAndCancellationRemovesRequest() = runBlocking {
        // API28 emulator/test-provider limitation: setTestProviderLocation() can store a complete
        // synthetic fix while Android's app-facing COARSE path still withholds it because the mock
        // does not reliably carry the production no-GPS payload (Location.EXTRA_NO_GPS_LOCATION).
        // Therefore this automated case verifies the real framework/AndroidX request lifecycle only.
        // End-to-end COARSE delivery/mapping was verified separately on a real Galaxy S25.
        val awaiting = async(Dispatchers.Default) { dataSource().getCurrentLocation() }

        val registration = waitForRegistration(expectedPresent = true)
        assertTrue(
            "getCurrentLocation must register a real LocationManager request on API28",
            registration.isNotEmpty(),
        )

        awaiting.cancelAndJoin()

        val afterCancellation = waitForRegistration(expectedPresent = false)
        assertTrue(
            "Cancelling getCurrentLocation must remove its LocationManager request",
            afterCancellation.isEmpty(),
        )
    }

    @Test
    fun foregroundFlowRegistersApprovedIntervalAndCancellationRemovesListener() = runBlocking {
        // Same API28 COARSE mock-delivery limitation as the current-location case above: do not
        // require a synthetic callback the emulator cannot faithfully expose to a coarse-only app.
        // Keep the test active (never @Ignore): prove registration, the approved 15-minute request,
        // and callbackFlow cancellation -> LocationManagerCompat.removeUpdates().
        val awaiting = async(Dispatchers.Default) {
            dataSource().observeSignificantUpdates().first()
        }

        val registration = waitForRegistration(expectedPresent = true)
        assertTrue(
            "Foreground flow must register a real LocationManager listener",
            registration.isNotEmpty(),
        )
        assertTrue(
            "Foreground registration must carry the approved 15-minute interval",
            registration.any { "requested=+15m0s0ms" in it },
        )

        awaiting.cancelAndJoin()

        val afterCancellation = waitForRegistration(expectedPresent = false)
        assertTrue(
            "Cancelling foreground collection must unregister the LocationManager listener",
            afterCancellation.isEmpty(),
        )
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
        shell("settings put secure location_mode 0")
        val environment = AndroidLocationEnvironment(targetContext)

        assertFalse(environment.isLocationServicesEnabled())
        val result = dataSource().getCurrentLocation()
        val unavailable = result as DeviceLocationResult.Unavailable
        assertEquals(LocationFailure.NO_PROVIDER, unavailable.reason)
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

    private suspend fun waitForRegistration(expectedPresent: Boolean): List<String> {
        repeat(REGISTRATION_POLL_ATTEMPTS) {
            val lines = currentRegistrationLines()
            if (lines.isNotEmpty() == expectedPresent) return lines
            delay(REGISTRATION_POLL_INTERVAL_MILLIS)
        }
        return currentRegistrationLines()
    }

    private fun currentRegistrationLines(): List<String> {
        val marker = "UpdateRecord[$providerName ${targetContext.packageName}("
        return shell("dumpsys location")
            .lineSequence()
            .map(String::trim)
            .filter { marker in it }
            .toList()
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

    private fun grantCoarseAndMockLocationAppOp(packageName: String) {
        shell("pm grant $packageName ${Manifest.permission.ACCESS_COARSE_LOCATION}")
        shell("appops set $packageName android:mock_location allow")
    }

    private fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
        }
    }

    private companion object {
        const val REGISTRATION_POLL_ATTEMPTS = 50
        const val REGISTRATION_POLL_INTERVAL_MILLIS = 100L
    }
}
