from pathlib import Path


def write(path: str, content: str) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content)

write('app/src/main/java/com/archimedeprojects/arihna/core/location/model/CachedDeviceLocation.kt', '''package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant

/** Raw real framework cache. Android does not carry the historical ZoneId with this record. */
data class CachedDeviceLocation(
    val coordinates: Coordinates,
    val capturedAt: Instant,
    val accuracyMeters: Float?,
) {
    val isValid: Boolean
        get() = coordinates.isValid && (accuracyMeters == null || (accuracyMeters.isFinite() && accuracyMeters >= 0f))
}
''')

write('app/src/main/java/com/archimedeprojects/arihna/core/location/model/DeviceLocationResult.kt', '''package com.archimedeprojects.arihna.core.location.model

sealed interface DeviceLocationResult {
    data class Success(
        val fix: DeviceLocationFix,
        val freshness: LocationFreshness = LocationFreshness.FRESH,
    ) : DeviceLocationResult

    /** Real cached coordinates/timestamp without an invented historical timezone. */
    data class Cached(
        val location: CachedDeviceLocation,
    ) : DeviceLocationResult

    data class Unavailable(val reason: LocationFailure) : DeviceLocationResult
}
''')

write('app/src/main/java/com/archimedeprojects/arihna/core/location/model/SelectedLocation.kt', '''package com.archimedeprojects.arihna.core.location.model

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.ZoneId

data class SelectedLocation(
    val source: LocationSource,
    val coordinates: Coordinates,
    val zoneId: ZoneId,
    val displayName: String,
    val freshness: LocationFreshness? = null,
) {
    val isValid: Boolean
        get() = coordinates.isValid && displayName.isNotBlank()
}
''')

write('app/src/main/java/com/archimedeprojects/arihna/core/location/model/LocationResolutionState.kt', '''package com.archimedeprojects.arihna.core.location.model

sealed interface LocationResolutionState {
    data object Unconfigured : LocationResolutionState
    data object Resolving : LocationResolutionState

    data class Ready(
        val location: SelectedLocation,
    ) : LocationResolutionState {
        constructor(location: SelectedLocation, freshness: LocationFreshness?) : this(
            location = location.copy(freshness = freshness),
        )

        val freshness: LocationFreshness?
            get() = location.freshness
    }

    data class PermissionDenied(
        val canRequestAgain: Boolean,
        val cachedLocation: SelectedLocation?,
    ) : LocationResolutionState

    data class LocationServicesDisabled(
        val cachedLocation: SelectedLocation?,
    ) : LocationResolutionState

    data class Unavailable(
        val reason: LocationFailure,
        val cachedLocation: SelectedLocation?,
    ) : LocationResolutionState
}
''')

write('app/src/main/java/com/archimedeprojects/arihna/core/location/platform/LocationManagerDeviceLocationDataSource.kt', '''package com.archimedeprojects.arihna.core.location.platform

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
''')

write('app/src/main/java/com/archimedeprojects/arihna/core/location/domain/LocationCoordinator.kt', '''package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.data.UnsupportedCityTimeZoneException
import com.archimedeprojects.arihna.core.location.model.CachedDeviceLocation
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class LocationCoordinator(
    private val deviceLocationDataSource: DeviceLocationDataSource,
    private val cityRepository: CityRepository,
    private val preferencesRepository: LocationPreferencesRepository,
    private val updatePolicy: LocationUpdatePolicy = LocationUpdatePolicy(),
) {
    suspend fun restore(
        permissionState: LocationPermissionState,
        locationServicesEnabled: Boolean,
    ): LocationResolutionState {
        val preference = readPreference()
            ?: return LocationResolutionState.Unavailable(LocationFailure.PERSISTENCE_ERROR, null)

        return when (preference) {
            LocationPreference.Unset -> LocationResolutionState.Unconfigured
            LocationPreference.Device -> resolveDevice(permissionState, locationServicesEnabled)
            is LocationPreference.Manual -> restoreManual(preference)
        }
    }

    suspend fun selectDevice(
        permissionState: LocationPermissionState,
        locationServicesEnabled: Boolean,
    ): LocationResolutionState {
        if (!persist { preferencesRepository.selectDevice() }) {
            return LocationResolutionState.Unavailable(LocationFailure.PERSISTENCE_ERROR, null)
        }
        return resolveDevice(permissionState, locationServicesEnabled)
    }

    suspend fun selectManual(city: ManualCity): LocationResolutionState {
        if (!city.isValid) {
            return LocationResolutionState.Unavailable(LocationFailure.INVALID_FIX, null)
        }
        if (!persist { preferencesRepository.selectManual(city) }) {
            return LocationResolutionState.Unavailable(LocationFailure.PERSISTENCE_ERROR, null)
        }
        return LocationResolutionState.Ready(city.toSelectedLocation())
    }

    suspend fun selectManual(cityId: Long): LocationResolutionState {
        val city = try {
            cityRepository.findById(cityId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: UnsupportedCityTimeZoneException) {
            return LocationResolutionState.Unavailable(LocationFailure.UNSUPPORTED_TIME_ZONE, null)
        } catch (_: Exception) {
            return LocationResolutionState.Unavailable(LocationFailure.CITY_DATASET_UNAVAILABLE, null)
        } ?: return LocationResolutionState.Unavailable(LocationFailure.CITY_NOT_FOUND, null)

        return selectManual(city)
    }

    suspend fun resolveDevice(
        permissionState: LocationPermissionState,
        locationServicesEnabled: Boolean,
    ): LocationResolutionState {
        val cachedFix = readCachedDeviceFix()?.takeIf { it.isValid }
        val cachedLocation = cachedFix?.let { toSelectedDeviceLocation(it, LocationFreshness.CACHED) }

        when (permissionState) {
            LocationPermissionState.NotRequested -> {
                return LocationResolutionState.PermissionDenied(
                    canRequestAgain = true,
                    cachedLocation = cachedLocation,
                )
            }

            is LocationPermissionState.Denied -> {
                return LocationResolutionState.PermissionDenied(
                    canRequestAgain = permissionState.canRequestAgain,
                    cachedLocation = cachedLocation,
                )
            }

            LocationPermissionState.Granted -> Unit
        }

        if (!locationServicesEnabled) {
            return LocationResolutionState.LocationServicesDisabled(cachedLocation)
        }

        val currentResult = try {
            withTimeoutOrNull(updatePolicy.currentFixTimeout.toMillis()) {
                deviceLocationDataSource.getCurrentLocation()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        }

        val timeoutOccurred = currentResult == null
        val result = currentResult ?: readLastKnownResult()

        return when (result) {
            is DeviceLocationResult.Success -> handleDeviceSuccess(
                result = result,
                previous = cachedFix,
                forceCached = timeoutOccurred,
            )
            is DeviceLocationResult.Cached -> handleRawCachedFallback(
                raw = result.location,
                persisted = cachedFix,
                failureWithoutCompleteCache = if (timeoutOccurred) LocationFailure.TIMEOUT else LocationFailure.NO_PROVIDER,
            )
            is DeviceLocationResult.Unavailable -> fallbackToPersistedOrUnavailable(
                persisted = cachedFix,
                reason = if (timeoutOccurred) LocationFailure.TIMEOUT else result.reason,
            )
        }
    }

    suspend fun acceptDeviceUpdate(candidate: DeviceLocationFix): LocationResolutionState {
        if (!candidate.isValid) {
            return LocationResolutionState.Unavailable(
                reason = LocationFailure.INVALID_FIX,
                cachedLocation = readCachedDeviceFix()?.takeIf { it.isValid }?.let {
                    toSelectedDeviceLocation(it, LocationFreshness.CACHED)
                },
            )
        }

        val previous = readCachedDeviceFix()?.takeIf { it.isValid }
        return handleDeviceFix(candidate, previous, LocationFreshness.FRESH)
    }

    private suspend fun handleDeviceSuccess(
        result: DeviceLocationResult.Success,
        previous: DeviceLocationFix?,
        forceCached: Boolean,
    ): LocationResolutionState {
        val freshness = if (forceCached) LocationFreshness.CACHED else result.freshness
        val candidate = if (freshness == LocationFreshness.CACHED && previous != null) {
            if (result.fix.capturedAt.isAfter(previous.capturedAt)) result.fix else previous
        } else {
            result.fix
        }
        return handleDeviceFix(candidate, previous, freshness)
    }

    private suspend fun handleRawCachedFallback(
        raw: CachedDeviceLocation,
        persisted: DeviceLocationFix?,
        failureWithoutCompleteCache: LocationFailure,
    ): LocationResolutionState {
        if (!raw.isValid) {
            return fallbackToPersistedOrUnavailable(persisted, LocationFailure.INVALID_FIX)
        }

        if (persisted == null) {
            // Android Location has no historical ZoneId. A raw cache alone cannot safely drive prayer calculations.
            return LocationResolutionState.Unavailable(
                reason = failureWithoutCompleteCache,
                cachedLocation = null,
            )
        }

        // A provenance match proves that the raw framework cache is the same real fix Arihna persisted.
        // If it does not match, the persisted fix is still the newest complete cache Arihna can safely use.
        cachedFrameworkLocationMatchesPersistedFix(raw, persisted)
        return LocationResolutionState.Ready(
            toSelectedDeviceLocation(persisted, LocationFreshness.CACHED),
        )
    }

    private suspend fun fallbackToPersistedOrUnavailable(
        persisted: DeviceLocationFix?,
        reason: LocationFailure,
    ): LocationResolutionState = if (persisted != null) {
        LocationResolutionState.Ready(
            toSelectedDeviceLocation(persisted, LocationFreshness.CACHED),
        )
    } else {
        LocationResolutionState.Unavailable(reason = reason, cachedLocation = null)
    }

    private suspend fun handleDeviceFix(
        candidate: DeviceLocationFix,
        previous: DeviceLocationFix?,
        freshness: LocationFreshness,
    ): LocationResolutionState {
        if (!candidate.isValid) {
            return LocationResolutionState.Unavailable(
                reason = LocationFailure.INVALID_FIX,
                cachedLocation = previous?.let { toSelectedDeviceLocation(it, LocationFreshness.CACHED) },
            )
        }

        if (!updatePolicy.shouldAccept(previous, candidate) && previous != null) {
            return LocationResolutionState.Ready(
                toSelectedDeviceLocation(previous, LocationFreshness.CACHED),
            )
        }

        if (!persist { preferencesRepository.saveDeviceFix(candidate) }) {
            return LocationResolutionState.Unavailable(
                reason = LocationFailure.PERSISTENCE_ERROR,
                cachedLocation = previous?.let { toSelectedDeviceLocation(it, LocationFreshness.CACHED) },
            )
        }

        return LocationResolutionState.Ready(
            toSelectedDeviceLocation(candidate, freshness),
        )
    }

    private suspend fun readLastKnownResult(): DeviceLocationResult = try {
        deviceLocationDataSource.getLastKnownLocation()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
    }

    private suspend fun restoreManual(preference: LocationPreference.Manual): LocationResolutionState {
        val city = preference.city.toManualCityOrNull()
            ?: return LocationResolutionState.Unavailable(LocationFailure.PERSISTENCE_ERROR, null)
        return LocationResolutionState.Ready(city.toSelectedLocation())
    }

    private suspend fun readPreference(): LocationPreference? = try {
        preferencesRepository.preference.first()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    private suspend fun readCachedDeviceFix(): DeviceLocationFix? = try {
        preferencesRepository.cachedDeviceFix.first()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    private suspend fun toSelectedDeviceLocation(
        fix: DeviceLocationFix,
        freshness: LocationFreshness,
    ): SelectedLocation {
        val nearestCity = try {
            cityRepository.nearest(fix.coordinates)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }

        val displayName = nearestCity?.displayName
            ?: "${fix.coordinates.latitude}, ${fix.coordinates.longitude}"

        return SelectedLocation(
            source = LocationSource.Device(
                capturedAt = fix.capturedAt,
                accuracyMeters = fix.accuracyMeters,
            ),
            coordinates = fix.coordinates,
            zoneId = fix.zoneId,
            displayName = displayName,
            freshness = freshness,
        )
    }

    private fun ManualCity.toSelectedLocation(): SelectedLocation = SelectedLocation(
        source = LocationSource.Manual(cityId = id),
        coordinates = coordinates,
        zoneId = zoneId,
        displayName = displayName,
        freshness = null,
    )

    private suspend fun persist(block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        false
    }
}

internal fun cachedFrameworkLocationMatchesPersistedFix(
    raw: CachedDeviceLocation,
    persisted: DeviceLocationFix,
): Boolean = raw.capturedAt == persisted.capturedAt && raw.coordinates == persisted.coordinates
''')

# Downstream code consumes SelectedLocation freshness as the source of truth.
repo = Path('app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/domain/DefaultPrayerScheduleRepository.kt')
text = repo.read_text()
text = text.replace('locationFreshness = ready.freshness,', 'locationFreshness = selectedLocation.freshness,')
repo.write_text(text)

presentation = Path('app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/presentation/PrayerSchedulePresentation.kt')
text = presentation.read_text()
text = text.replace('locationFreshness = schedule.locationFreshness,', 'locationFreshness = schedule.selectedLocation.freshness,')
text = text.replace('''        locationAge = cachedDeviceLocationAge(\n            selectedLocation = schedule.selectedLocation,\n            freshness = schedule.locationFreshness,\n            now = now,\n        ),''', '''        locationAge = cachedDeviceLocationAge(\n            selectedLocation = schedule.selectedLocation,\n            now = now,\n        ),''')
text = text.replace('''private fun cachedDeviceLocationAge(\n    selectedLocation: SelectedLocation,\n    freshness: LocationFreshness?,\n    now: Instant,\n): Duration? {\n    if (freshness != LocationFreshness.CACHED) return null''', '''private fun cachedDeviceLocationAge(\n    selectedLocation: SelectedLocation,\n    now: Instant,\n): Duration? {\n    if (selectedLocation.freshness != LocationFreshness.CACHED) return null''')
presentation.write_text(text)

settings = Path('app/src/main/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsPresentation.kt')
text = settings.read_text()
text = text.replace('''            freshness = when (freshness) {\n                LocationFreshness.FRESH -> "FRESH"\n                LocationFreshness.CACHED -> "CACHED"\n                null -> null\n            },''', '''            freshness = when (location.freshness) {\n                LocationFreshness.FRESH -> "FRESH"\n                LocationFreshness.CACHED -> "CACHED"\n                null -> null\n            },''')
settings.write_text(text)

# Add focused host regression for provenance safety and SelectedLocation metadata.
write('app/src/test/java/com/archimedeprojects/arihna/core/location/domain/LocationCacheFallbackProvenanceTest.kt', '''package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.model.CachedDeviceLocation
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCacheFallbackProvenanceTest {
    private val capturedAt = Instant.parse("2026-08-31T15:16:15.258Z")
    private val coordinates = Coordinates(44.993, 10.86)

    @Test
    fun rawFrameworkCacheMatchesOnlySamePersistedCaptureAndCoordinates() {
        val persisted = persistedFix()
        val same = CachedDeviceLocation(coordinates, capturedAt, 2_000f)
        val differentTime = same.copy(capturedAt = capturedAt.plusSeconds(1))
        val differentCoordinates = same.copy(coordinates = Coordinates(45.0, 10.86))

        assertTrue(cachedFrameworkLocationMatchesPersistedFix(same, persisted))
        assertFalse(cachedFrameworkLocationMatchesPersistedFix(differentTime, persisted))
        assertFalse(cachedFrameworkLocationMatchesPersistedFix(differentCoordinates, persisted))
    }

    @Test
    fun readyFreshnessIsOwnedBySelectedLocation() {
        val selected = SelectedLocation(
            source = LocationSource.Device(capturedAt, 2_000f),
            coordinates = coordinates,
            zoneId = ZoneId.of("Europe/Rome"),
            displayName = "Pegognaga, Lombardia, Italy",
            freshness = LocationFreshness.CACHED,
        )
        val ready = LocationResolutionState.Ready(selected)

        assertEquals(LocationFreshness.CACHED, selected.freshness)
        assertEquals(LocationFreshness.CACHED, ready.freshness)
    }

    @Test
    fun manualSelectedLocationHasNoDeviceFreshness() {
        val selected = SelectedLocation(
            source = LocationSource.Manual(1L),
            coordinates = coordinates,
            zoneId = ZoneId.of("Europe/Rome"),
            displayName = "Pegognaga",
        )

        assertNull(selected.freshness)
    }

    private fun persistedFix() = DeviceLocationFix(
        coordinates = coordinates,
        zoneId = ZoneId.of("Europe/Rome"),
        capturedAt = capturedAt,
        accuracyMeters = 2_000f,
    )
}
''')

# Candidate tests that model a fully zoned CACHED datasource result remain valid; platform last-known itself
# now returns DeviceLocationResult.Cached and never manufactures ZoneId.systemDefault() for historical data.
