package com.archimedeprojects.arihna.core.location.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.os.CancellationSignal
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.domain.LocationUpdatePolicy
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Android framework implementation of [DeviceLocationDataSource].
 *
 * Timeout and significant-change acceptance intentionally remain in the pure domain layer.
 * This bridge only obtains current fixes and forwards foreground candidates.
 */
class LocationManagerDeviceLocationDataSource(
    context: Context,
    private val locationManager: LocationManager = context.applicationContext.getSystemService(LocationManager::class.java),
    private val callbackExecutor: Executor = ContextCompat.getMainExecutor(context.applicationContext),
    private val zoneIdProvider: () -> ZoneId = ZoneId::systemDefault,
    private val clock: Clock = Clock.systemUTC(),
    private val currentLocationProviderSelector: (LocationManager) -> String? = ::selectCurrentLocationProvider,
    private val providerSelector: (LocationManager) -> String? = ::selectCoarseProvider,
    private val updatePolicy: LocationUpdatePolicy = LocationUpdatePolicy(),
) : DeviceLocationDataSource {
    private val appContext = context.applicationContext

    override suspend fun getCurrentLocation(): DeviceLocationResult {
        if (!hasCoarsePermission() || !LocationManagerCompat.isLocationEnabled(locationManager)) {
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }
        val provider = currentLocationProviderSelector(locationManager)
            ?: return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)

        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    cancellationSignal,
                    callbackExecutor,
                ) { location ->
                    if (!continuation.isActive) return@getCurrentLocation
                    if (location == null) {
                        continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                        return@getCurrentLocation
                    }
                    val fix = location.toDeviceFix()
                    continuation.resume(
                        if (fix.isValid) {
                            DeviceLocationResult.Success(fix)
                        } else {
                            DeviceLocationResult.Unavailable(LocationFailure.INVALID_FIX)
                        },
                    )
                }
            } catch (_: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                }
            } catch (_: IllegalArgumentException) {
                if (continuation.isActive) {
                    continuation.resume(DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER))
                }
            }
        }
    }

    override fun observeSignificantUpdates(): Flow<DeviceLocationFix> = callbackFlow {
        if (!hasCoarsePermission() || !LocationManagerCompat.isLocationEnabled(locationManager)) {
            close()
            return@callbackFlow
        }
        val provider = providerSelector(locationManager)
        if (provider == null) {
            close()
            return@callbackFlow
        }

        val requestSpec = foregroundLocationRequestSpec(updatePolicy)
        val request = LocationRequestCompat.Builder(requestSpec.intervalMillis)
            .setMinUpdateIntervalMillis(requestSpec.intervalMillis)
            .setMinUpdateDistanceMeters(requestSpec.minDistanceMeters)
            .build()
        val listener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                trySend(location.toDeviceFix())
            }

            override fun onProviderDisabled(disabledProvider: String) {
                if (disabledProvider == provider) close()
            }
        }

        try {
            LocationManagerCompat.requestLocationUpdates(
                locationManager,
                provider,
                request,
                callbackExecutor,
                listener,
            )
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        } catch (_: IllegalArgumentException) {
            close()
            return@callbackFlow
        }

        awaitClose {
            runCatching { LocationManagerCompat.removeUpdates(locationManager, listener) }
        }
    }

    private fun hasCoarsePermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

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
}

internal data class ForegroundLocationRequestSpec(
    val intervalMillis: Long,
    val minDistanceMeters: Float,
)

internal fun foregroundLocationRequestSpec(policy: LocationUpdatePolicy): ForegroundLocationRequestSpec =
    ForegroundLocationRequestSpec(
        intervalMillis = policy.minimumForegroundUpdateInterval.toMillis(),
        // The domain layer must see candidates below 5 km so a ZoneId change can still be significant.
        minDistanceMeters = 0f,
    )

internal fun selectCurrentLocationProvider(locationManager: LocationManager): String? {
    val enabled = runCatching { locationManager.getProviders(true).toSet() }.getOrDefault(emptySet())
    return selectCurrentLocationProviderFromEnabledProviders(enabled)
}

internal fun selectCurrentLocationProviderFromEnabledProviders(enabledProviders: Set<String>): String? =
    listOf(LocationManager.NETWORK_PROVIDER, FRAMEWORK_FUSED_PROVIDER)
        .firstOrNull { it in enabledProviders }

@Suppress("DEPRECATION")
internal fun selectCoarseProvider(locationManager: LocationManager): String? {
    val criteria = Criteria().apply {
        accuracy = Criteria.ACCURACY_COARSE
        powerRequirement = Criteria.POWER_LOW
    }
    val best = runCatching { locationManager.getBestProvider(criteria, true) }.getOrNull()
    if (best != null && best != LocationManager.PASSIVE_PROVIDER) return best

    val enabled = runCatching { locationManager.getProviders(true).toSet() }.getOrDefault(emptySet())
    return listOf(LocationManager.NETWORK_PROVIDER, FRAMEWORK_FUSED_PROVIDER)
        .firstOrNull { it in enabled }
}

private const val FRAMEWORK_FUSED_PROVIDER = "fused"
