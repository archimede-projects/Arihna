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
import com.archimedeprojects.arihna.core.location.model.CachedDeviceLocation
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
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
 * Current callbacks are complete Device fixes; framework last-known is exposed as raw CACHED
 * coordinates/timestamp because Android does not retain the historical ZoneId with Location.
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
            ?: return lastKnownLocationResult()

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
                        continuation.resume(lastKnownLocationResult())
                        return@getCurrentLocation
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
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(lastKnownLocationResult())
            } catch (_: IllegalArgumentException) {
                if (continuation.isActive) continuation.resume(lastKnownLocationResult())
            }
        }
    }

    override suspend fun getLastKnownLocation(): DeviceLocationResult = lastKnownLocationResult()

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

    private fun lastKnownLocationResult(): DeviceLocationResult {
        if (!hasCoarsePermission()) {
            return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        val availableProviders = runCatching { locationManager.allProviders.toSet() }
            .getOrDefault(emptySet())
        val candidates = LAST_KNOWN_PROVIDER_ORDER.mapNotNull { provider ->
            if (provider !in availableProviders) return@mapNotNull null
            val location = try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } ?: return@mapNotNull null

            val cached = location.toCachedDeviceLocationOrNull() ?: return@mapNotNull null
            LastKnownDeviceCandidate(provider = provider, location = cached)
        }

        val selected = selectNewestLastKnownCandidate(candidates)
            ?: return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        return DeviceLocationResult.Cached(selected.location)
    }

    private fun hasCoarsePermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

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

internal data class LastKnownDeviceCandidate(
    val provider: String,
    val location: CachedDeviceLocation,
)

internal fun foregroundLocationRequestSpec(policy: LocationUpdatePolicy): ForegroundLocationRequestSpec =
    ForegroundLocationRequestSpec(
        intervalMillis = policy.minimumForegroundUpdateInterval.toMillis(),
        // The domain layer must see candidates below 5 km so a ZoneId change can still be significant.
        minDistanceMeters = 0f,
    )

internal fun selectNewestLastKnownCandidate(
    candidates: List<LastKnownDeviceCandidate>,
): LastKnownDeviceCandidate? = candidates.maxWithOrNull(
    compareBy<LastKnownDeviceCandidate> { it.location.capturedAt }
        .thenBy { if (it.provider == FRAMEWORK_FUSED_PROVIDER) 1 else 0 },
)

internal fun selectCurrentLocationProvider(locationManager: LocationManager): String? {
    val enabled = runCatching { locationManager.getProviders(true).toSet() }.getOrDefault(emptySet())
    return selectCurrentLocationProviderFromEnabledProviders(enabled)
}

internal fun selectCurrentLocationProviderFromEnabledProviders(enabledProviders: Set<String>): String? =
    FRAMEWORK_FUSED_PROVIDER.takeIf { it in enabledProviders }

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

private val LAST_KNOWN_PROVIDER_ORDER = listOf(FRAMEWORK_FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
private const val FRAMEWORK_FUSED_PROVIDER = "fused"
