package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
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
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Google Play Services implementation of Arihna's foreground device-location contract.
 * Fresh fixes capture the current device ZoneId. Play Services lastLocation remains raw cached
 * coordinates/timestamp so Arihna never invents a historical timezone for old coordinates.
 */
class GoogleFusedDeviceLocationDataSource(
    context: Context,
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
    private val clock: Clock = Clock.systemUTC(),
    private val updatePolicy: LocationUpdatePolicy = LocationUpdatePolicy(),
) : DeviceLocationDataSource {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    override suspend fun getCurrentLocation(): DeviceLocationResult {
        if (!hasForegroundLocationPermission() || !isLocationEnabled()) {
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        val location = try {
            awaitCurrentLocation()
        } catch (_: SecurityException) {
            null
        }

        if (location == null) return getLastKnownLocation()
        val fix = location.toDeviceFix()
        return if (fix.isValid) {
            DeviceLocationResult.Success(fix = fix, freshness = LocationFreshness.FRESH)
        } else {
            DeviceLocationResult.Unavailable(LocationFailure.INVALID_FIX)
        }
    }

    override suspend fun getLastKnownLocation(): DeviceLocationResult {
        if (!hasForegroundLocationPermission()) {
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        val location = try {
            fusedClient.lastLocation.awaitNullable()
        } catch (_: SecurityException) {
            null
        }
        val cached = location?.toCachedDeviceLocationOrNull()
            ?: return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        return DeviceLocationResult.Cached(cached)
    }

    override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = callbackFlow {
        if (!hasForegroundLocationPermission() || !isLocationEnabled()) {
            close()
            return@callbackFlow
        }

        val interval = updatePolicy.minimumForegroundUpdateInterval.toMillis()
        val request = LocationRequest.Builder(interval)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMinUpdateIntervalMillis(interval)
            .setMinUpdateDistanceMeters(0f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.toDeviceFix()?.let { fix ->
                    if (fix.isValid) trySend(fix)
                }
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

    private suspend fun awaitCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellation.cancel() }
        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0L)
                .build()
            fusedClient.getCurrentLocation(request, cancellation.token)
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

    private fun hasForegroundLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean =
        runCatching { LocationManagerCompat.isLocationEnabled(locationManager) }.getOrDefault(false)

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

private suspend fun <T> Task<T>.awaitNullable(): T? = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener {
        if (continuation.isActive) continuation.resume(null)
    }
    addOnCanceledListener {
        if (continuation.isActive) continuation.resume(null)
    }
}
