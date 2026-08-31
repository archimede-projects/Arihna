package com.archimedeprojects.arihna.core.location.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class ProviderCurrentLocationProbe(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val callbackExecutor: Executor = ContextCompat.getMainExecutor(appContext)

    suspend fun runParallel() = coroutineScope {
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val enabledProviders = runCatching {
            locationManager.getProviders(true).joinToString(prefix = "[", postfix = "]")
        }.getOrDefault("<provider-query-failed>")
        LocationDiagnosticTrace.record(
            "AB_PROBE_START",
            "coarse=$coarseGranted enabledProviders=$enabledProviders timeoutMs=$PROBE_TIMEOUT_MILLIS",
        )

        logLastKnown(LocationManager.NETWORK_PROVIDER)
        logLastKnown(FRAMEWORK_FUSED_PROVIDER)

        val network = async { probeCurrent(LocationManager.NETWORK_PROVIDER) }
        val fused = async { probeCurrent(FRAMEWORK_FUSED_PROVIDER) }
        network.await()
        fused.await()
        LocationDiagnosticTrace.record("AB_PROBE_COMPLETE")
    }

    private suspend fun probeCurrent(provider: String) {
        val enabled = runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        if (!enabled) {
            LocationDiagnosticTrace.record("AB_PROVIDER_DISABLED", "provider=$provider")
            return
        }

        val requestElapsed = SystemClock.elapsedRealtime()
        LocationDiagnosticTrace.record("AB_CURRENT_REQUEST", "provider=$provider")
        val callback = withTimeoutOrNull(PROBE_TIMEOUT_MILLIS) {
            awaitCurrentLocation(provider)
        }
        val totalLatency = SystemClock.elapsedRealtime() - requestElapsed

        if (callback == null) {
            LocationDiagnosticTrace.record(
                "AB_CURRENT_TIMEOUT",
                "provider=$provider latencyMs=$totalLatency",
            )
            return
        }

        val location = callback.location
        if (location == null) {
            LocationDiagnosticTrace.record(
                "AB_CURRENT_NULL",
                "provider=$provider latencyMs=${callback.latencyMillis}",
            )
        } else {
            LocationDiagnosticTrace.record(
                "AB_CURRENT_SUCCESS",
                locationDetail(provider, location, callback.latencyMillis),
            )
        }
    }

    private suspend fun awaitCurrentLocation(provider: String): CallbackResult =
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            val requestElapsed = SystemClock.elapsedRealtime()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    cancellationSignal,
                    callbackExecutor,
                ) { location ->
                    val latency = SystemClock.elapsedRealtime() - requestElapsed
                    LocationDiagnosticTrace.record(
                        "AB_CURRENT_CALLBACK",
                        if (location == null) {
                            "provider=$provider latencyMs=$latency location=null"
                        } else {
                            locationDetail(provider, location, latency)
                        },
                    )
                    if (continuation.isActive) {
                        continuation.resume(CallbackResult(location, latency))
                    }
                }
            } catch (error: Throwable) {
                LocationDiagnosticTrace.record(
                    "AB_CURRENT_THROW",
                    "provider=$provider ${error.javaClass.simpleName}: ${error.message}",
                )
                if (continuation.isActive) {
                    continuation.resume(CallbackResult(null, SystemClock.elapsedRealtime() - requestElapsed))
                }
            }
        }

    private fun logLastKnown(provider: String) {
        val enabled = runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        if (!enabled) {
            LocationDiagnosticTrace.record("AB_LAST_KNOWN", "provider=$provider enabled=false")
            return
        }
        val location = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        if (location == null) {
            LocationDiagnosticTrace.record("AB_LAST_KNOWN", "provider=$provider location=null")
        } else {
            LocationDiagnosticTrace.record(
                "AB_LAST_KNOWN",
                locationDetail(provider, location, callbackLatencyMillis = null),
            )
        }
    }

    private fun locationDetail(
        requestedProvider: String,
        location: Location,
        callbackLatencyMillis: Long?,
    ): String {
        val elapsedAgeMs = ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L)
            .coerceAtLeast(0L)
        val wallAgeMs = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        val accuracy = if (location.hasAccuracy()) location.accuracy.toString() else "<none>"
        return buildString {
            append("requestedProvider=$requestedProvider")
            append(" callbackProvider=${location.provider}")
            callbackLatencyMillis?.let { append(" latencyMs=$it") }
            append(" elapsedAgeMs=$elapsedAgeMs")
            append(" wallAgeMs=$wallAgeMs")
            append(" accuracyM=$accuracy")
            append(" timeMs=${location.time}")
            append(" elapsedRealtimeNanos=${location.elapsedRealtimeNanos}")
        }
    }

    private data class CallbackResult(
        val location: Location?,
        val latencyMillis: Long,
    )

    private companion object {
        const val FRAMEWORK_FUSED_PROVIDER = "fused"
        const val PROBE_TIMEOUT_MILLIS = 35_000L
    }
}
