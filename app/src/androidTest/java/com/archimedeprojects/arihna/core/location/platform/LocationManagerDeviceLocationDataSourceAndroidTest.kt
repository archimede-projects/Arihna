package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.os.CancellationSignal
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
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
    fun currentFixDeliveryIsBisectedAcrossFrameworkCompatAndBridge(): Unit = runBlocking {
        val diagnostics = mutableListOf("case=A current-fix three-level bisection")
        recordState(diagnostics, "initial")

        val freshPreInjected = inject(
            latitude = 41.9028,
            longitude = 12.4964,
            accuracyMeters = 120f,
            ageMillis = 0L,
        )
        recordLocation(diagnostics, "injectedFreshPreRequest", freshPreInjected)

        val lastKnown = runCatching { locationManager.getLastKnownLocation(providerName) }
            .onFailure { record(diagnostics, "lastKnownException=${it.stackTraceToString()}") }
            .getOrNull()
        recordLocation(diagnostics, "frameworkLastKnownFresh", lastKnown)

        val directFresh = awaitDirectCompatCurrentLocation(timeoutMillis = 5_000L)
        recordLocation(diagnostics, "compatCurrentFromFreshLastKnown", directFresh)
        recordRegistrationState(diagnostics, "after-fresh-compat-current")
        waitForRegistration(expectedPresent = false)

        val stale = inject(
            latitude = 41.9029,
            longitude = 12.49645,
            accuracyMeters = 130f,
            ageMillis = STALE_FIX_AGE_MILLIS,
        )
        recordLocation(diagnostics, "injectedStalePreRequest", stale)
        recordLocation(
            diagnostics,
            "frameworkLastKnownStale",
            runCatching { locationManager.getLastKnownLocation(providerName) }.getOrNull(),
        )

        val directPostRequest = startDirectCompatCurrentLocation()
        val directRegistration = waitForRegistration(expectedPresent = true)
        record(
            diagnostics,
            "compatCurrentWaitingRegistrationObserved=${directRegistration.isNotEmpty()} " +
                "lines=${directRegistration.joinToString(" | ").ifEmpty { "<none>" }}",
        )
        val freshAfterDirectRequest = inject(
            latitude = 41.9031,
            longitude = 12.4966,
            accuracyMeters = 140f,
            ageMillis = 0L,
        )
        recordLocation(diagnostics, "injectedFreshAfterCompatRequest", freshAfterDirectRequest)
        val directAfterRequest = withTimeoutOrNull(5_000L) { directPostRequest.callback.await() }
        recordLocation(diagnostics, "compatCurrentAfterFreshPostRequest", directAfterRequest)
        directPostRequest.cancellationSignal.cancel()
        val directRemoved = waitForRegistration(expectedPresent = false)
        record(
            diagnostics,
            "compatCurrentRegistrationRemoved=${directRemoved.isEmpty()}",
        )

        val staleBeforeBridge = inject(
            latitude = 41.9032,
            longitude = 12.4967,
            accuracyMeters = 150f,
            ageMillis = STALE_FIX_AGE_MILLIS,
        )
        recordLocation(diagnostics, "injectedStaleBeforeBridge", staleBeforeBridge)

        val bridgeAwaiting = async(Dispatchers.Default) {
            withTimeoutOrNull(5_000L) { dataSource().getCurrentLocation() }
        }
        val bridgeRegistration = waitForRegistration(expectedPresent = true)
        record(
            diagnostics,
            "bridgeCurrentWaitingRegistrationObserved=${bridgeRegistration.isNotEmpty()} " +
                "lines=${bridgeRegistration.joinToString(" | ").ifEmpty { "<none>" }}",
        )
        val freshAfterBridgeRequest = inject(
            latitude = 41.9033,
            longitude = 12.4968,
            accuracyMeters = 160f,
            ageMillis = 0L,
        )
        recordLocation(diagnostics, "injectedFreshAfterBridgeRequest", freshAfterBridgeRequest)
        recordBridgeCurrentResult(diagnostics, bridgeAwaiting.await())
        val bridgeRemoved = waitForRegistration(expectedPresent = false)
        record(diagnostics, "bridgeCurrentRegistrationRemoved=${bridgeRemoved.isEmpty()}")

        diagnosticOnlyFailure(diagnostics)
    }

    @Test
    fun foregroundFlowDeliveryIsBisectedAcrossCompatListenerAndBridge(): Unit = runBlocking {
        val diagnostics = mutableListOf("case=B foreground-flow three-level bisection")
        recordState(diagnostics, "initial")

        val requestSpec = foregroundLocationRequestSpec(
            LocationUpdatePolicy(
                significantDistanceMeters = 5_000.0,
                minimumForegroundUpdateInterval = Duration.ofMinutes(15),
                currentFixTimeout = Duration.ofSeconds(20),
            ),
        )
        val request = LocationRequestCompat.Builder(requestSpec.intervalMillis)
            .setMinUpdateIntervalMillis(requestSpec.intervalMillis)
            .setMinUpdateDistanceMeters(requestSpec.minDistanceMeters)
            .build()
        val directCallback = CompletableDeferred<Location>()
        val directListener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                directCallback.complete(location)
            }
        }

        LocationManagerCompat.requestLocationUpdates(
            locationManager,
            providerName,
            request,
            directExecutor,
            directListener,
        )
        val directRegistration = waitForRegistration(expectedPresent = true)
        record(
            diagnostics,
            "compatListenerRegistrationObserved=${directRegistration.isNotEmpty()} " +
                "lines=${directRegistration.joinToString(" | ").ifEmpty { "<none>" }}",
        )
        val directInjected = inject(
            latitude = 41.9030,
            longitude = 12.4965,
            accuracyMeters = 200f,
            ageMillis = 0L,
        )
        recordLocation(diagnostics, "injectedAfterCompatListenerRegistration", directInjected)
        val directDelivered = withTimeoutOrNull(5_000L) { directCallback.await() }
        recordLocation(diagnostics, "compatListenerCallback", directDelivered)
        LocationManagerCompat.removeUpdates(locationManager, directListener)
        val directRemoved = waitForRegistration(expectedPresent = false)
        record(diagnostics, "compatListenerRegistrationRemoved=${directRemoved.isEmpty()}")

        val dataSource = dataSource()
        val flowAwaiting = async(Dispatchers.Default) {
            withTimeoutOrNull(5_000L) { dataSource.observeSignificantUpdates().first() }
        }
        val flowRegistration = waitForRegistration(expectedPresent = true)
        record(
            diagnostics,
            "bridgeFlowRegistrationObserved=${flowRegistration.isNotEmpty()} " +
                "lines=${flowRegistration.joinToString(" | ").ifEmpty { "<none>" }}",
        )
        val flowInjected = inject(
            latitude = 41.9034,
            longitude = 12.4969,
            accuracyMeters = 210f,
            ageMillis = 0L,
        )
        recordLocation(diagnostics, "injectedAfterBridgeFlowRegistration", flowInjected)
        val flowFix = flowAwaiting.await()
        if (flowFix == null) {
            record(diagnostics, "bridgeFlowResult=<timeout-or-closed>")
        } else {
            record(
                diagnostics,
                "bridgeFlowResult=Fix latitude=${flowFix.coordinates.latitude} " +
                    "longitude=${flowFix.coordinates.longitude} accuracy=${flowFix.accuracyMeters} " +
                    "capturedAt=${flowFix.capturedAt} zone=${flowFix.zoneId}",
            )
        }
        val flowRemoved = waitForRegistration(expectedPresent = false)
        record(diagnostics, "bridgeFlowRegistrationRemoved=${flowRemoved.isEmpty()}")

        diagnosticOnlyFailure(diagnostics)
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

    private suspend fun awaitDirectCompatCurrentLocation(timeoutMillis: Long): Location? {
        val request = startDirectCompatCurrentLocation()
        return try {
            withTimeoutOrNull(timeoutMillis) { request.callback.await() }
        } finally {
            request.cancellationSignal.cancel()
        }
    }

    @Suppress("DEPRECATION")
    private fun startDirectCompatCurrentLocation(): CurrentLocationProbe {
        val callback = CompletableDeferred<Location?>()
        val cancellationSignal = CancellationSignal()
        LocationManagerCompat.getCurrentLocation(
            locationManager,
            providerName,
            cancellationSignal,
            directExecutor,
        ) { location ->
            callback.complete(location)
        }
        return CurrentLocationProbe(callback, cancellationSignal)
    }

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

    private fun recordRegistrationState(diagnostics: MutableList<String>, label: String) {
        val lines = currentRegistrationLines()
        record(
            diagnostics,
            "registration[$label] present=${lines.isNotEmpty()} " +
                "lines=${lines.joinToString(" | ").ifEmpty { "<none>" }}",
        )
    }

    private fun recordBridgeCurrentResult(
        diagnostics: MutableList<String>,
        result: DeviceLocationResult?,
    ) {
        when (result) {
            is DeviceLocationResult.Success -> record(
                diagnostics,
                "bridgeCurrentResult=Success latitude=${result.fix.coordinates.latitude} " +
                    "longitude=${result.fix.coordinates.longitude} accuracy=${result.fix.accuracyMeters} " +
                    "capturedAt=${result.fix.capturedAt} zone=${result.fix.zoneId}",
            )
            is DeviceLocationResult.Unavailable -> record(
                diagnostics,
                "bridgeCurrentResult=Unavailable reason=${result.reason}",
            )
            null -> record(diagnostics, "bridgeCurrentResult=<timeout>")
        }
    }

    private fun recordLocation(
        diagnostics: MutableList<String>,
        label: String,
        location: Location?,
    ) {
        if (location == null) {
            record(diagnostics, "$label=null")
            return
        }
        val wallAgeMillis = System.currentTimeMillis() - location.time
        val elapsedAgeMillis = if (location.elapsedRealtimeNanos > 0L) {
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        } else {
            null
        }
        record(
            diagnostics,
            "$label provider=${location.provider} latitude=${location.latitude} longitude=${location.longitude} " +
                "accuracy=${if (location.hasAccuracy()) location.accuracy else null} time=${location.time} " +
                "wallAgeMillis=$wallAgeMillis elapsedRealtimeNanos=${location.elapsedRealtimeNanos} " +
                "elapsedAgeMillis=$elapsedAgeMillis",
        )
    }

    private fun recordState(diagnostics: MutableList<String>, label: String) {
        record(
            diagnostics,
            "state[$label] locationEnabled=${locationManager.isLocationEnabled} " +
                "providerEnabled=${locationManager.isProviderEnabled(providerName)} " +
                "providerVisible=${providerName in locationManager.getProviders(true)}",
        )
    }

    @Suppress("DEPRECATION")
    private fun inject(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        ageMillis: Long,
    ): Location {
        val elapsedAgeNanos = ageMillis * 1_000_000L
        val location = Location(providerName).apply {
            this.latitude = latitude
            this.longitude = longitude
            accuracy = accuracyMeters
            time = System.currentTimeMillis() - ageMillis
            elapsedRealtimeNanos = (SystemClock.elapsedRealtimeNanos() - elapsedAgeNanos).coerceAtLeast(1L)
        }
        locationManager.setTestProviderLocation(providerName, location)
        return location
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

    private fun record(diagnostics: MutableList<String>, message: String) {
        diagnostics += message
        Log.i(DIAGNOSTIC_TAG, message)
        System.err.println("$DIAGNOSTIC_TAG $message")
    }

    private fun diagnosticOnlyFailure(diagnostics: List<String>): Nothing =
        throw AssertionError(
            "STEP5_DIAGNOSTIC_ONLY\n" + diagnostics.joinToString(separator = "\n"),
        )

    private fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
        }
    }

    private data class CurrentLocationProbe(
        val callback: CompletableDeferred<Location?>,
        val cancellationSignal: CancellationSignal,
    )

    private companion object {
        const val DIAGNOSTIC_TAG = "ARIHNA_STEP5_DIAG"
        const val STALE_FIX_AGE_MILLIS = 60_000L
        const val REGISTRATION_POLL_ATTEMPTS = 50
        const val REGISTRATION_POLL_INTERVAL_MILLIS = 100L
    }
}
