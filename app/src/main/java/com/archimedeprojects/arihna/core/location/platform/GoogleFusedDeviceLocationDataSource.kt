package com.archimedeprojects.arihna.core.location.platform

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.domain.LocationUpdatePolicy
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

/** Production device-location source backed by Google Play Services fused location. */
class GoogleFusedDeviceLocationDataSource(
    context: Context,
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
    private val locationManager: LocationManager =
        context.applicationContext.getSystemService(LocationManager::class.java),
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
    private val clock: Clock = Clock.systemUTC(),
    private val updatePolicy: LocationUpdatePolicy = LocationUpdatePolicy(),
) : DeviceLocationDataSource {
    private val appContext = context.applicationContext

    override suspend fun getCurrentLocation(): DeviceLocationResult {
        if (!hasForegroundLocationPermission(appContext) ||
            !LocationManagerCompat.isLocationEnabled(locationManager)
        ) {
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        val current = fetchCurrentRaw()
        if (current != null) {
            val fix = current.toDeviceFix()
            return if (fix.isValid) {
                DeviceLocationResult.Success(fix, LocationFreshness.FRESH)
            } else {
                DeviceLocationResult.Unavailable(LocationFailure.INVALID_FIX)
            }
        }

        return getLastKnownLocation()
    }

    override suspend fun getLastKnownLocation(): DeviceLocationResult {
        if (!hasForegroundLocationPermission(appContext)) {
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }
        val raw = fetchLastRaw() ?: return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        val cached = raw.toCachedDeviceLocationOrNull()
            ?: return DeviceLocationResult.Unavailable(LocationFailure.INVALID_FIX)
        return DeviceLocationResult.Cached(cached)
    }

    override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = callbackFlow {
        if (!hasForegroundLocationPermission(appContext) ||
            !LocationManagerCompat.isLocationEnabled(locationManager)
        ) {
            close()
            return@callbackFlow
        }

        val spec = fusedForegroundRequestSpec(updatePolicy)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, spec.intervalMillis)
            .setMinUpdateIntervalMillis(spec.intervalMillis)
            .setMinUpdateDistanceMeters(spec.minDistanceMeters)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.toDeviceFix()?.takeIf { it.isValid }?.let(::trySend)
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close(it) }
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }

        awaitClose {
            runCatching { fusedClient.removeLocationUpdates(callback) }
        }
    }

    private suspend fun fetchCurrentRaw(): Location? = suspendCancellableCoroutine { continuation ->
        val tokenSource = CancellationTokenSource()
        continuation.invokeOnCancellation { tokenSource.cancel() }
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
                .addOnCanceledListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        } catch (_: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    private suspend fun fetchLastRaw(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
                .addOnCanceledListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        } catch (_: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        }
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

    private fun Location.toCachedDeviceLocationOrNull(): CachedDeviceLocation? {
        if (time <= 0L) return null
        val capturedAt = runCatching { Instant.ofEpochMilli(time) }.getOrNull() ?: return null
        val cached = CachedDeviceLocation(
            coordinates = Coordinates(latitude = latitude, longitude = longitude),
            capturedAt = capturedAt,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
        )
        return cached.takeIf { it.isValid }
    }
}

internal data class FusedForegroundRequestSpec(
    val intervalMillis: Long,
    val minDistanceMeters: Float,
)

internal fun fusedForegroundRequestSpec(policy: LocationUpdatePolicy): FusedForegroundRequestSpec =
    FusedForegroundRequestSpec(
        intervalMillis = policy.minimumForegroundUpdateInterval.toMillis(),
        minDistanceMeters = 0f,
    )
