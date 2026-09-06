package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.model.CachedDeviceLocation
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/** Google Play Services foreground device-location source. */
class FusedDeviceLocationDataSource(
    context: Context,
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
    private val clock: Clock = Clock.systemUTC(),
) : DeviceLocationDataSource {
    private val appContext = context.applicationContext

    override suspend fun getCurrentLocation(): DeviceLocationResult {
        if (!hasForegroundLocationPermission()) {
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }
            try {
                fusedClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                    .addOnSuccessListener { location ->
                        if (!continuation.isActive) return@addOnSuccessListener
                        if (location == null) {
                            continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                            return@addOnSuccessListener
                        }
                        val fix = location.toDeviceFix()
                        continuation.resume(
                            if (fix.isValid) {
                                DeviceLocationResult.Success(fix, LocationFreshness.FRESH)
                            } else {
                                DeviceLocationResult.Unavailable(LocationFailure.INVALID_FIX)
                            },
                        )
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                        }
                    }
            } catch (_: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                }
            }
        }
    }

    override suspend fun getLastKnownLocation(): DeviceLocationResult {
        if (!hasForegroundLocationPermission()) {
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (!continuation.isActive) return@addOnSuccessListener
                        if (location == null) {
                            continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                            return@addOnSuccessListener
                        }
                        val cached = location.toCachedLocationOrNull()
                        continuation.resume(
                            if (cached != null) {
                                DeviceLocationResult.Cached(cached)
                            } else {
                                DeviceLocationResult.Unavailable(LocationFailure.INVALID_FIX)
                            },
                        )
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                        }
                    }
            } catch (_: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                }
            }
        }
    }

    override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = callbackFlow {
        if (!hasForegroundLocationPermission()) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            FOREGROUND_UPDATE_INTERVAL_MILLIS,
        )
            .setMinUpdateIntervalMillis(FOREGROUND_FASTEST_INTERVAL_MILLIS)
            .setMinUpdateDistanceMeters(FOREGROUND_MIN_DISTANCE_METERS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    val fix = location.toDeviceFix()
                    if (fix.isValid) trySend(fix)
                }
            }
        }

        try {
            fusedClient
                .requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close(it) }
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }

        awaitClose {
            runCatching { fusedClient.removeLocationUpdates(callback) }
        }
    }

    private fun hasForegroundLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun Location.toDeviceFix(): DeviceLocationFix {
        val timestampMillis = time.takeIf { it > 0L } ?: clock.millis()
        val capturedAt = runCatching { Instant.ofEpochMilli(timestampMillis) }.getOrElse { clock.instant() }
        return DeviceLocationFix(
            coordinates = Coordinates(latitude = latitude, longitude = longitude),
            zoneId = zoneIdProvider(),
            capturedAt = capturedAt,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
        )
    }

    private fun Location.toCachedLocationOrNull(): CachedDeviceLocation? {
        if (time <= 0L) return null
        val capturedAt = runCatching { Instant.ofEpochMilli(time) }.getOrNull() ?: return null
        return CachedDeviceLocation(
            coordinates = Coordinates(latitude = latitude, longitude = longitude),
            capturedAt = capturedAt,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
        ).takeIf { it.isValid }
    }

    private companion object {
        const val FOREGROUND_UPDATE_INTERVAL_MILLIS = 20_000L
        const val FOREGROUND_FASTEST_INTERVAL_MILLIS = 7_000L
        const val FOREGROUND_MIN_DISTANCE_METERS = 25f
    }
}
