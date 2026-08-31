package com.archimedeprojects.arihna.core.location.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class NetworkLocationUpdatesProbe(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val callbackExecutor: Executor = ContextCompat.getMainExecutor(appContext)

    suspend fun run() {
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val providerEnabled = runCatching {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)

        LocationDiagnosticTrace.record(
            "RU_PROBE_START",
            buildString {
                append("provider=${LocationManager.NETWORK_PROVIDER}")
                append(" sdk=${Build.VERSION.SDK_INT}")
                append(" coarse=$coarseGranted")
                append(" providerEnabled=$providerEnabled")
                append(" intervalMs=$REQUEST_INTERVAL_MILLIS")
                append(" minUpdateIntervalMs=$MIN_UPDATE_INTERVAL_MILLIS")
                append(" minDistanceM=$MIN_UPDATE_DISTANCE_METERS")
                append(" maxUpdateDelayMs=$MAX_UPDATE_DELAY_MILLIS")
                append(" quality=BALANCED")
                append(" durationMs=$PROBE_TIMEOUT_MILLIS")
                append(" diagnosticRecencyBoundMs=$DIAGNOSTIC_RECENCY_BOUND_MILLIS")
            },
        )

        if (!coarseGranted) {
            LocationDiagnosticTrace.record("RU_TERMINAL", "outcome=UNAVAILABLE_PERMISSION")
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            LocationDiagnosticTrace.record(
                "RU_TERMINAL",
                "outcome=UNSUPPORTED_API sdk=${Build.VERSION.SDK_INT} required=${Build.VERSION_CODES.S}",
            )
            return
        }
        if (!providerEnabled) {
            LocationDiagnosticTrace.record(
                "RU_TERMINAL",
                "outcome=UNAVAILABLE_PROVIDER provider=${LocationManager.NETWORK_PROVIDER}",
            )
            return
        }

        val requestElapsed = SystemClock.elapsedRealtime()
        val requestWall = System.currentTimeMillis()
        LocationDiagnosticTrace.record(
            "RU_SUBSCRIBE",
            "provider=${LocationManager.NETWORK_PROVIDER} elapsedMs=$requestElapsed wallMs=$requestWall",
        )

        val terminal = try {
            withTimeoutOrNull(PROBE_TIMEOUT_MILLIS) {
                awaitAcceptedLocation(requestElapsed)
            }
        } catch (error: CancellationException) {
            LocationDiagnosticTrace.record(
                "RU_TERMINAL",
                "outcome=CANCELLED latencyMs=${SystemClock.elapsedRealtime() - requestElapsed} ${error.javaClass.simpleName}",
            )
            throw error
        }

        if (terminal == null) {
            LocationDiagnosticTrace.record(
                "RU_TERMINAL",
                "outcome=TIMEOUT latencyMs=${SystemClock.elapsedRealtime() - requestElapsed}",
            )
        } else {
            LocationDiagnosticTrace.record(
                "RU_TERMINAL",
                "outcome=${terminal.name} latencyMs=${SystemClock.elapsedRealtime() - requestElapsed}",
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun awaitAcceptedLocation(requestElapsed: Long): ProbeTerminal =
        suspendCancellableCoroutine { continuation ->
            val registered = AtomicBoolean(false)
            val removed = AtomicBoolean(false)
            lateinit var listener: LocationListener

            fun removeListener(reason: String) {
                if (!registered.get() || !removed.compareAndSet(false, true)) return
                val removal = runCatching { locationManager.removeUpdates(listener) }
                LocationDiagnosticTrace.record(
                    "RU_LISTENER_REMOVED",
                    if (removal.isSuccess) {
                        "reason=$reason"
                    } else {
                        val error = removal.exceptionOrNull()
                        "reason=$reason removalError=${error?.javaClass?.simpleName}: ${error?.message}"
                    },
                )
            }

            listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val callbackLatency = SystemClock.elapsedRealtime() - requestElapsed
                    val elapsedAge = elapsedAgeMillis(location)
                    val decision = if (elapsedAge <= DIAGNOSTIC_RECENCY_BOUND_MILLIS) {
                        "ACCEPTED"
                    } else {
                        "REJECTED_TOO_OLD"
                    }
                    LocationDiagnosticTrace.record(
                        "RU_CALLBACK",
                        locationDetail(
                            location = location,
                            callbackLatencyMillis = callbackLatency,
                            elapsedAgeMillis = elapsedAge,
                            decision = decision,
                        ),
                    )
                    if (decision == "ACCEPTED" && continuation.isActive) {
                        removeListener("accepted")
                        continuation.resume(ProbeTerminal.ACCEPTED)
                    }
                }

                @Deprecated("Deprecated in Android framework")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit
            }

            continuation.invokeOnCancellation { cause ->
                removeListener("cancelled:${cause?.javaClass?.simpleName ?: "unknown"}")
            }

            val request = LocationRequest.Builder(REQUEST_INTERVAL_MILLIS)
                .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS)
                .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
                .setMaxUpdateDelayMillis(MAX_UPDATE_DELAY_MILLIS)
                .setQuality(LocationRequest.QUALITY_BALANCED_POWER_ACCURACY)
                .setDurationMillis(PROBE_TIMEOUT_MILLIS)
                .build()

            registered.set(true)
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    request,
                    callbackExecutor,
                    listener,
                )
                LocationDiagnosticTrace.record(
                    "RU_REGISTERED",
                    "provider=${LocationManager.NETWORK_PROVIDER} request=$request",
                )
            } catch (error: Throwable) {
                LocationDiagnosticTrace.record(
                    "RU_ERROR",
                    "stage=register ${error.javaClass.simpleName}: ${error.message}",
                )
                removeListener("registration-error")
                if (continuation.isActive) {
                    continuation.resume(ProbeTerminal.ERROR)
                }
            }
        }

    private fun elapsedAgeMillis(location: Location): Long =
        ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L)
            .coerceAtLeast(0L)

    private fun locationDetail(
        location: Location,
        callbackLatencyMillis: Long,
        elapsedAgeMillis: Long,
        decision: String,
    ): String {
        val wallAgeMillis = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        val accuracy = if (location.hasAccuracy()) location.accuracy.toString() else "<none>"
        return buildString {
            append("requestedProvider=${LocationManager.NETWORK_PROVIDER}")
            append(" callbackProvider=${location.provider}")
            append(" latencyMs=$callbackLatencyMillis")
            append(" elapsedAgeMs=$elapsedAgeMillis")
            append(" wallAgeMs=$wallAgeMillis")
            append(" accuracyM=$accuracy")
            append(" timeMs=${location.time}")
            append(" elapsedRealtimeNanos=${location.elapsedRealtimeNanos}")
            append(" decision=$decision")
            append(" boundMs=$DIAGNOSTIC_RECENCY_BOUND_MILLIS")
        }
    }

    private enum class ProbeTerminal {
        ACCEPTED,
        ERROR,
    }

    private companion object {
        const val REQUEST_INTERVAL_MILLIS = 10_000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 0L
        const val MIN_UPDATE_DISTANCE_METERS = 0f
        const val MAX_UPDATE_DELAY_MILLIS = 0L
        const val PROBE_TIMEOUT_MILLIS = 35_000L
        const val DIAGNOSTIC_RECENCY_BOUND_MILLIS = REQUEST_INTERVAL_MILLIS
    }
}
