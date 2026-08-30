package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoarseMockProviderCrossApiAndroidTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext: Context = instrumentation.targetContext
    private val locationManager: LocationManager =
        targetContext.getSystemService(LocationManager::class.java)
    private val providerName = "arihna_coarse_crossapi_${UUID.randomUUID()}"

    @Before
    fun setUp() {
        shell("pm grant ${targetContext.packageName} ${Manifest.permission.ACCESS_COARSE_LOCATION}")
        shell("appops set ${targetContext.packageName} android:mock_location allow")
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
        assertTrue("cross-api diagnostic provider must be enabled", locationManager.isProviderEnabled(providerName))
    }

    @After
    fun tearDown() {
        runCatching { locationManager.setTestProviderEnabled(providerName, false) }
        runCatching { locationManager.removeTestProvider(providerName) }
    }

    @Test
    fun minimalMockFixIsVisibleToCoarseOnlyCaller(): Unit {
        val diagnostics = mutableListOf("case=E coarse-only mock-provider cross-api control")
        record(diagnostics, "sdk=${Build.VERSION.SDK_INT} release=${Build.VERSION.RELEASE}")
        record(diagnostics, "targetPackage=${targetContext.packageName}")
        record(
            diagnostics,
            "coarseGranted=${targetContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED} " +
                "fineGranted=${targetContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED}",
        )
        record(
            diagnostics,
            "mockAppOp=${compact(shell("appops get ${targetContext.packageName} android:mock_location"))}",
        )

        val location = Location(providerName).apply {
            latitude = 41.9028
            longitude = 12.4964
            accuracy = 120f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        var injectionFailure: Throwable? = null
        try {
            locationManager.setTestProviderLocation(providerName, location)
        } catch (throwable: Throwable) {
            injectionFailure = throwable
        }
        record(
            diagnostics,
            "setTestProviderLocationFailure=${injectionFailure?.let { "${it.javaClass.name}: ${it.message}" } ?: "<none>"}",
        )

        val lastKnown = runCatching { locationManager.getLastKnownLocation(providerName) }
            .onFailure { record(diagnostics, "getLastKnownException=${it.javaClass.name}: ${it.message}") }
            .getOrNull()
        record(
            diagnostics,
            if (lastKnown == null) {
                "coarseLastKnown=null"
            } else {
                "coarseLastKnown provider=${lastKnown.provider} lat=${lastKnown.latitude} lon=${lastKnown.longitude} " +
                    "accuracy=${lastKnown.accuracy} fromMock=${lastKnown.isFromMockProvider}"
            },
        )

        val dump = shell("dumpsys location").lineSequence()
            .filter {
                providerName in it ||
                    "Last Known Locations" in it ||
                    "Mock Providers:" in it ||
                    "mHasLocation" in it
            }
            .joinToString(" | ") { it.trim() }
        record(diagnostics, "dump=${dump.ifBlank { "<none>" }}")

        throw AssertionError(
            "STEP5_CROSS_API_DIAGNOSTIC_ONLY\n" + diagnostics.joinToString("\n"),
        )
    }

    private fun record(diagnostics: MutableList<String>, message: String) {
        diagnostics += message
        System.err.println("ARIHNA_STEP5_CROSS_API $message")
    }

    private fun compact(value: String): String =
        value.lineSequence().map(String::trim).filter(String::isNotEmpty).joinToString(" | ")

    private fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
        }
    }
}
