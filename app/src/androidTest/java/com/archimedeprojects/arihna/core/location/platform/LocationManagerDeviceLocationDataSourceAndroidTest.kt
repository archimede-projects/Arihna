package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
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
    }

    @After
    fun tearDown() {
        runCatching { locationManager.setTestProviderEnabled(providerName, false) }
        runCatching { locationManager.removeTestProvider(providerName) }
        shell("settings put secure location_mode 3")
    }

    @Test
    fun currentFixComesFromRealLocationManagerBridgeAndCapturesCurrentZone(): Unit = runBlocking {
        val diagnostics = mutableListOf("case=A currentFix")
        recordState(diagnostics, "before-inject")

        inject(latitude = 41.9028, longitude = 12.4964, accuracyMeters = 120f)
        recordState(diagnostics, "after-inject")
        recordLastKnownLocation(diagnostics)

        val result = runCatching {
            withTimeout(5_000) { dataSource().getCurrentLocation() }
        }
        result.exceptionOrNull()?.let { throwable ->
            record(diagnostics, "bridgeException=${throwable.stackTraceToString()}")
        }
        when (val value = result.getOrNull()) {
            is DeviceLocationResult.Success -> {
                record(
                    diagnostics,
                    "bridgeResult=Success latitude=${value.fix.coordinates.latitude} " +
                        "longitude=${value.fix.coordinates.longitude} zone=${value.fix.zoneId} " +
                        "accuracy=${value.fix.accuracyMeters} capturedAt=${value.fix.capturedAt}",
                )
            }
            is DeviceLocationResult.Unavailable -> {
                record(diagnostics, "bridgeResult=Unavailable reason=${value.reason}")
            }
            null -> record(diagnostics, "bridgeResult=<none>")
        }

        diagnosticOnlyFailure(diagnostics)
    }

    @Test
    fun foregroundFlowReceivesInjectedFrameworkLocationAndCancelsCleanly(): Unit = runBlocking {
        val diagnostics = mutableListOf("case=B foregroundFlow")
        recordState(diagnostics, "before-framework-probe")
        probeFrameworkUpdateRegistration(diagnostics)
        recordState(diagnostics, "before-flow")

        val dataSource = dataSource()
        val awaiting = async {
            runCatching {
                withTimeout(5_000) { dataSource.observeSignificantUpdates().firstOrNull() }
            }
        }
        delay(150)

        recordState(diagnostics, "before-inject")
        inject(latitude = 41.9030, longitude = 12.4965, accuracyMeters = 200f)
        recordState(diagnostics, "after-inject")
        recordLastKnownLocation(diagnostics)

        val flowResult = awaiting.await()
        flowResult.exceptionOrNull()?.let { throwable ->
            record(diagnostics, "flowException=${throwable.stackTraceToString()}")
        }
        val fix = flowResult.getOrNull()
        if (fix == null) {
            record(diagnostics, "flowResult=<closed-without-element>")
        } else {
            record(
                diagnostics,
                "flowResult=Fix latitude=${fix.coordinates.latitude} longitude=${fix.coordinates.longitude} " +
                    "zone=${fix.zoneId} accuracy=${fix.accuracyMeters} capturedAt=${fix.capturedAt}",
            )
        }
        recordState(diagnostics, "after-flow")

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
    fun disabledLocationServicesAreExposedWithoutFabricatedFix(): Unit = runBlocking {
        val diagnostics = mutableListOf("case=C disabledLocationServices")
        recordState(diagnostics, "before-location-mode-write")

        val directModeWriteOutput = shell("settings put secure location_mode 0").trim()
        record(
            diagnostics,
            "directLocationModeWriteOutput=${directModeWriteOutput.ifEmpty { "<empty>" }}",
        )
        delay(500)
        recordState(diagnostics, "after-location-mode-write")
        val enabledAfterDirectModeWrite = runCatching { locationManager.isLocationEnabled }
            .getOrElse { throwable ->
                record(diagnostics, "isLocationEnabledAfterModeWriteException=${throwable.stackTraceToString()}")
                true
            }
        record(diagnostics, "enabledAfterDirectLocationModeWrite=$enabledAfterDirectModeWrite")

        try {
            if (enabledAfterDirectModeWrite) {
                record(
                    diagnostics,
                    "api28Fallback=disable gps/network through secure location_providers_allowed",
                )
                setApi28LocationProvidersEnabled(false, diagnostics)
                val disabledSettled = waitForLocationEnabled(expected = false)
                record(diagnostics, "api28ProviderToggleReachedDisabled=$disabledSettled")
                recordState(diagnostics, "after-api28-provider-disable")
            }

            val result = runCatching { dataSource().getCurrentLocation() }
            result.exceptionOrNull()?.let { throwable ->
                record(diagnostics, "bridgeExceptionWhileDisabled=${throwable.stackTraceToString()}")
            }
            when (val value = result.getOrNull()) {
                is DeviceLocationResult.Success -> record(
                    diagnostics,
                    "bridgeWhileDisabled=Success latitude=${value.fix.coordinates.latitude} " +
                        "longitude=${value.fix.coordinates.longitude}",
                )
                is DeviceLocationResult.Unavailable -> record(
                    diagnostics,
                    "bridgeWhileDisabled=Unavailable reason=${value.reason}",
                )
                null -> record(diagnostics, "bridgeWhileDisabled=<none>")
            }
        } finally {
            setApi28LocationProvidersEnabled(true, diagnostics)
            val enabledSettled = waitForLocationEnabled(expected = true)
            record(diagnostics, "api28ProviderToggleRestoredEnabled=$enabledSettled")
            recordState(diagnostics, "after-restore")
        }

        diagnosticOnlyFailure(diagnostics)
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

    private fun probeFrameworkUpdateRegistration(diagnostics: MutableList<String>) {
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
        val listener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                record(
                    diagnostics,
                    "frameworkProbeCallback provider=${location.provider} latitude=${location.latitude} " +
                        "longitude=${location.longitude}",
                )
            }
        }

        try {
            LocationManagerCompat.requestLocationUpdates(
                locationManager,
                providerName,
                request,
                directExecutor,
                listener,
            )
            record(diagnostics, "frameworkRequestRegistration=OK")
        } catch (throwable: Throwable) {
            record(diagnostics, "frameworkRequestRegistrationException=${throwable.stackTraceToString()}")
        } finally {
            runCatching { LocationManagerCompat.removeUpdates(locationManager, listener) }
                .onSuccess { record(diagnostics, "frameworkRequestRemoval=OK") }
                .onFailure { throwable ->
                    record(diagnostics, "frameworkRequestRemovalException=${throwable.stackTraceToString()}")
                }
        }
    }

    private fun recordLastKnownLocation(diagnostics: MutableList<String>) {
        runCatching { locationManager.getLastKnownLocation(providerName) }
            .onSuccess { location ->
                if (location == null) {
                    record(diagnostics, "lastKnownLocation=null")
                } else {
                    record(
                        diagnostics,
                        "lastKnownLocation=provider=${location.provider} latitude=${location.latitude} " +
                            "longitude=${location.longitude} accuracy=${if (location.hasAccuracy()) location.accuracy else null} " +
                            "time=${location.time} elapsedRealtimeNanos=${location.elapsedRealtimeNanos}",
                    )
                }
            }
            .onFailure { throwable ->
                record(diagnostics, "lastKnownLocationException=${throwable.stackTraceToString()}")
            }
    }

    private fun recordState(diagnostics: MutableList<String>, label: String) {
        val locationEnabled = runCatching { locationManager.isLocationEnabled }
            .fold(onSuccess = { it.toString() }, onFailure = { "EX:${it.javaClass.name}:${it.message}" })
        val providerEnabled = runCatching { locationManager.isProviderEnabled(providerName) }
            .fold(onSuccess = { it.toString() }, onFailure = { "EX:${it.javaClass.name}:${it.message}" })
        val allProviders = runCatching { locationManager.allProviders.joinToString(",") }
            .fold(onSuccess = { it }, onFailure = { "EX:${it.javaClass.name}:${it.message}" })
        val enabledProviders = runCatching { locationManager.getProviders(true).joinToString(",") }
            .fold(onSuccess = { it }, onFailure = { "EX:${it.javaClass.name}:${it.message}" })
        val targetCoarse = targetContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val testCoarse = testContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val locationMode = shell("settings get secure location_mode").trim().ifEmpty { "<empty>" }
        val allowedProviders = shell("settings get secure location_providers_allowed").trim().ifEmpty { "<empty>" }
        val targetMockOp = shell("appops get ${targetContext.packageName} android:mock_location")
            .trim().replace('\n', '|').ifEmpty { "<empty>" }
        val testMockOp = shell("appops get ${testContext.packageName} android:mock_location")
            .trim().replace('\n', '|').ifEmpty { "<empty>" }

        record(
            diagnostics,
            "state[$label] locationEnabled=$locationEnabled testProviderEnabled=$providerEnabled " +
                "targetCoarse=$targetCoarse testCoarse=$testCoarse locationMode=$locationMode " +
                "allowedProviders=$allowedProviders allProviders=$allProviders enabledProviders=$enabledProviders " +
                "targetMockOp=$targetMockOp testMockOp=$testMockOp",
        )
    }

    private fun setApi28LocationProvidersEnabled(
        enabled: Boolean,
        diagnostics: MutableList<String>,
    ) {
        val operator = if (enabled) "+" else "-"
        val gpsOutput = shell("settings put secure location_providers_allowed ${operator}gps").trim()
        val networkOutput = shell("settings put secure location_providers_allowed ${operator}network").trim()
        record(
            diagnostics,
            "setApi28LocationProvidersEnabled($enabled) gpsOutput=${gpsOutput.ifEmpty { "<empty>" }} " +
                "networkOutput=${networkOutput.ifEmpty { "<empty>" }}",
        )
    }

    private suspend fun waitForLocationEnabled(expected: Boolean): Boolean {
        repeat(30) {
            if (runCatching { locationManager.isLocationEnabled }.getOrDefault(!expected) == expected) {
                return true
            }
            delay(100)
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

    private fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
        }
    }

    private companion object {
        const val DIAGNOSTIC_TAG = "ARIHNA_STEP5_DIAG"
    }
}
