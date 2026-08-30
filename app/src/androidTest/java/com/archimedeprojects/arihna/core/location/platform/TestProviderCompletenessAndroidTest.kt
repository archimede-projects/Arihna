package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
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
class TestProviderCompletenessAndroidTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext: Context = instrumentation.targetContext
    private val testContext: Context = instrumentation.context
    private val locationManager: LocationManager =
        targetContext.getSystemService(LocationManager::class.java)
    private val providerName = "arihna_complete_${UUID.randomUUID()}"

    @Before
    fun setUp() {
        grantCoarseAndMockLocationAppOp(targetContext.packageName)
        grantCoarseAndMockLocationAppOp(testContext.packageName)
        createProvider()
    }

    @After
    fun tearDown() {
        runCatching { locationManager.setTestProviderEnabled(providerName, false) }
        runCatching { locationManager.removeTestProvider(providerName) }
    }

    @Test
    fun testProviderLocationCompletenessAndMockProviderState(): Unit {
        val diagnostics = mutableListOf("case=C test-provider completeness and framework acceptance")
        record(diagnostics, "targetPackage=${targetContext.packageName} testPackage=${testContext.packageName}")
        record(diagnostics, "mockAppOpTarget=${compact(shell("appops get ${targetContext.packageName} android:mock_location"))}")
        record(diagnostics, "mockAppOpTest=${compact(shell("appops get ${testContext.packageName} android:mock_location"))}")
        recordProviderVisibility(diagnostics, "initial")

        val baseline = baseLocation(
            latitude = 41.9028,
            longitude = 12.4964,
            accuracyMeters = 120f,
        )
        recordLocationShape(diagnostics, "baseline-before", baseline)
        recordReflectionCompleteness(diagnostics, "baseline-before", baseline)
        runInjectionProbe(diagnostics, "baseline", baseline)

        resetProvider()

        val fullyPopulated = baseLocation(
            latitude = 41.9031,
            longitude = 12.4966,
            accuracyMeters = 140f,
        ).apply {
            altitude = 12.0
            speed = 0f
            bearing = 0f
            verticalAccuracyMeters = 3f
            speedAccuracyMetersPerSecond = 1f
            bearingAccuracyDegrees = 1f
        }
        recordLocationShape(diagnostics, "full-before-makeComplete", fullyPopulated)
        recordReflectionCompleteness(diagnostics, "full-before-makeComplete", fullyPopulated)
        attemptMakeComplete(diagnostics, fullyPopulated)
        recordLocationShape(diagnostics, "full-after-makeComplete", fullyPopulated)
        recordReflectionCompleteness(diagnostics, "full-after-makeComplete", fullyPopulated)
        runInjectionProbe(diagnostics, "fully-populated", fullyPopulated)

        throw AssertionError(
            "STEP5_COMPLETENESS_DIAGNOSTIC_ONLY\n" + diagnostics.joinToString("\n"),
        )
    }

    @Suppress("DEPRECATION")
    private fun createProvider() {
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
        assertTrue("diagnostic provider must be enabled", locationManager.isProviderEnabled(providerName))
        assertTrue(
            "diagnostic provider must be visible with coarse permission",
            providerName in locationManager.getProviders(true),
        )
    }

    private fun resetProvider() {
        runCatching { locationManager.setTestProviderEnabled(providerName, false) }
        runCatching { locationManager.removeTestProvider(providerName) }
        createProvider()
    }

    private fun baseLocation(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
    ): Location = Location(providerName).apply {
        this.latitude = latitude
        this.longitude = longitude
        accuracy = accuracyMeters
        time = System.currentTimeMillis()
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    }

    @Suppress("DEPRECATION")
    private fun runInjectionProbe(
        diagnostics: MutableList<String>,
        label: String,
        location: Location,
    ) {
        shell("logcat -c")
        val startedNanos = SystemClock.elapsedRealtimeNanos()
        var returnedNormally = false
        var returnValue = "<not-returned>"
        try {
            val result = locationManager.setTestProviderLocation(providerName, location)
            returnedNormally = true
            returnValue = result.toString()
        } catch (throwable: Throwable) {
            record(
                diagnostics,
                "$label setTestProviderLocationException=${throwable.javaClass.name}: ${throwable.message}",
            )
        }
        val durationMicros = (SystemClock.elapsedRealtimeNanos() - startedNanos) / 1_000L
        record(
            diagnostics,
            "$label setTestProviderLocation returnedNormally=$returnedNormally returnValue=$returnValue durationMicros=$durationMicros",
        )

        val immediate = runCatching { locationManager.getLastKnownLocation(providerName) }
            .onFailure {
                record(diagnostics, "$label immediateLastKnownException=${it.javaClass.name}: ${it.message}")
            }
            .getOrNull()
        recordLocation(diagnostics, "$label immediateLastKnown", immediate)
        recordMockProviderDump(diagnostics, "$label immediateDump")
        recordRelevantLogcat(diagnostics, "$label immediateLogcat")

        Thread.sleep(500)

        val delayed = runCatching { locationManager.getLastKnownLocation(providerName) }
            .onFailure {
                record(diagnostics, "$label delayedLastKnownException=${it.javaClass.name}: ${it.message}")
            }
            .getOrNull()
        recordLocation(diagnostics, "$label delayedLastKnown500ms", delayed)
        recordMockProviderDump(diagnostics, "$label delayedDump500ms")
        recordRelevantLogcat(diagnostics, "$label delayedLogcat500ms")
    }

    private fun recordLocationShape(
        diagnostics: MutableList<String>,
        label: String,
        location: Location,
    ) {
        val derivedApi28Complete =
            location.provider != null &&
                location.hasAccuracy() &&
                location.time != 0L &&
                location.elapsedRealtimeNanos != 0L
        record(
            diagnostics,
            "$label derivedApi28Complete=$derivedApi28Complete provider=${location.provider} " +
                "hasAccuracy=${location.hasAccuracy()} accuracy=${location.accuracy} time=${location.time} " +
                "elapsedRealtimeNanos=${location.elapsedRealtimeNanos} hasAltitude=${location.hasAltitude()} " +
                "hasSpeed=${location.hasSpeed()} hasBearing=${location.hasBearing()} " +
                "hasVerticalAccuracy=${location.hasVerticalAccuracy()} " +
                "hasSpeedAccuracy=${location.hasSpeedAccuracy()} " +
                "hasBearingAccuracy=${location.hasBearingAccuracy()}",
        )
    }

    private fun recordReflectionCompleteness(
        diagnostics: MutableList<String>,
        label: String,
        location: Location,
    ) {
        val reflected = runCatching {
            val method = Location::class.java.getDeclaredMethod("isComplete")
            method.isAccessible = true
            method.invoke(location)
        }
        reflected.fold(
            onSuccess = { record(diagnostics, "$label reflectedIsComplete=$it") },
            onFailure = {
                record(
                    diagnostics,
                    "$label reflectedIsCompleteUnavailable=${it.javaClass.name}: ${it.message}",
                )
            },
        )
    }

    private fun attemptMakeComplete(
        diagnostics: MutableList<String>,
        location: Location,
    ) {
        val result = runCatching {
            val method = Location::class.java.getDeclaredMethod("makeComplete")
            method.isAccessible = true
            method.invoke(location)
        }
        result.fold(
            onSuccess = { record(diagnostics, "makeCompleteReflection=success returnValue=$it") },
            onFailure = {
                record(
                    diagnostics,
                    "makeCompleteReflection=unavailable ${it.javaClass.name}: ${it.message}",
                )
            },
        )
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
        val elapsedAgeMillis = if (location.elapsedRealtimeNanos != 0L) {
            (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        } else {
            null
        }
        record(
            diagnostics,
            "$label provider=${location.provider} lat=${location.latitude} lon=${location.longitude} " +
                "accuracy=${if (location.hasAccuracy()) location.accuracy else null} " +
                "wallAgeMillis=$wallAgeMillis elapsedAgeMillis=$elapsedAgeMillis " +
                "fromMock=${location.isFromMockProvider}",
        )
    }

    private fun recordProviderVisibility(
        diagnostics: MutableList<String>,
        label: String,
    ) {
        record(
            diagnostics,
            "$label locationEnabled=${locationManager.isLocationEnabled} " +
                "providerEnabled=${locationManager.isProviderEnabled(providerName)} " +
                "providerVisible=${providerName in locationManager.getProviders(true)}",
        )
    }

    private fun recordMockProviderDump(
        diagnostics: MutableList<String>,
        label: String,
    ) {
        val lines = shell("dumpsys location").lineSequence().toList()
        val hitIndices = lines.indices.filter { providerName in lines[it] }
        if (hitIndices.isEmpty()) {
            record(diagnostics, "$label providerSnippet=<provider-not-found>")
            return
        }
        val selected = linkedSetOf<Int>()
        hitIndices.forEach { hit ->
            val start = (hit - 2).coerceAtLeast(0)
            val end = (hit + 12).coerceAtMost(lines.lastIndex)
            for (index in start..end) selected += index
        }
        val snippet = selected
            .sorted()
            .joinToString(" | ") { index -> lines[index].trim() }
        record(diagnostics, "$label providerSnippet=${snippet.ifBlank { "<blank>" }}")
    }

    private fun recordRelevantLogcat(
        diagnostics: MutableList<String>,
        label: String,
    ) {
        val output = shell(
            "logcat -d -v brief -s LocationManager:V LocationManagerService:V MockProvider:V '*:S'",
        )
        record(diagnostics, "$label=${compact(output).ifBlank { "<none>" }}")
    }

    private fun grantCoarseAndMockLocationAppOp(packageName: String) {
        shell("pm grant $packageName ${Manifest.permission.ACCESS_COARSE_LOCATION}")
        shell("appops set $packageName android:mock_location allow")
    }

    private fun record(diagnostics: MutableList<String>, message: String) {
        diagnostics += message
        System.err.println("$DIAGNOSTIC_TAG $message")
    }

    private fun compact(value: String): String =
        value.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString(" | ")

    private fun shell(command: String): String {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
        }
    }

    private companion object {
        const val DIAGNOSTIC_TAG = "ARIHNA_STEP5_COMPLETE"
    }
}
