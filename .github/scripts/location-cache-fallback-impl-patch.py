from pathlib import Path


def write(path: str, content: str) -> None:
    Path(path).write_text(content)


def replace(path: str, old: str, new: str, count: int = 1) -> None:
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f'missing replacement in {path}: {old[:80]!r}')
    p.write_text(text.replace(old, new, count))

write(
    'app/src/main/java/com/archimedeprojects/arihna/core/location/data/DeviceLocationDataSource.kt',
    '''package com.archimedeprojects.arihna.core.location.data

import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.DeviceLocationResult
import kotlinx.coroutines.flow.Flow

interface DeviceLocationDataSource {
    suspend fun getCurrentLocation(): DeviceLocationResult
    suspend fun getLastKnownLocation(): DeviceLocationResult
    fun observeSignificantUpdates(): Flow<DeviceLocationFix>
}
''',
)

write(
    'app/src/main/java/com/archimedeprojects/arihna/core/location/model/DeviceLocationResult.kt',
    '''package com.archimedeprojects.arihna.core.location.model

sealed interface DeviceLocationResult {
    data class Success(
        val fix: DeviceLocationFix,
        val freshness: LocationFreshness = LocationFreshness.FRESH,
    ) : DeviceLocationResult

    data class Unavailable(val reason: LocationFailure) : DeviceLocationResult
}
''',
)

replace(
    'app/src/main/java/com/archimedeprojects/arihna/core/location/domain/LocationUpdatePolicy.kt',
    'val currentFixTimeout: Duration = Duration.ofSeconds(20),',
    'val currentFixTimeout: Duration = Duration.ofSeconds(30),',
)

write(
    'app/src/main/java/com/archimedeprojects/arihna/core/location/platform/LocationManagerDeviceLocationDataSource.kt',
    '''package com.archimedeprojects.arihna.core.location.platform

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
 * This bridge obtains a current fix when possible and exposes real framework cache as CACHED fallback.
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

            // A cache fallback must retain a real capture time; never replace a missing cache timestamp with now.
            if (location.time <= 0L) return@mapNotNull null
            val fix = location.toDeviceFix()
            if (!fix.isValid) return@mapNotNull null
            LastKnownDeviceCandidate(provider = provider, fix = fix)
        }

        val selected = selectNewestLastKnownCandidate(candidates)
            ?: return DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)
        return DeviceLocationResult.Success(selected.fix, LocationFreshness.CACHED)
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

internal data class LastKnownDeviceCandidate(
    val provider: String,
    val fix: DeviceLocationFix,
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
    compareBy<LastKnownDeviceCandidate> { it.fix.capturedAt }
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
''',
)

write(
    'app/src/main/java/com/archimedeprojects/arihna/core/location/domain/LocationCoordinator.kt',
    '''package com.archimedeprojects.arihna.core.location.domain

import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.data.UnsupportedCityTimeZoneException
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
        return LocationResolutionState.Ready(city.toSelectedLocation(), freshness = null)
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
        val cachedLocation = cachedFix?.let { toSelectedDeviceLocation(it) }

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
        val result = if (timeoutOccurred) {
            readLastKnownResult()
        } else {
            currentResult
        }

        return when (result) {
            is DeviceLocationResult.Success -> handleDeviceSuccess(result, cachedFix)
            is DeviceLocationResult.Unavailable -> {
                val failure = if (timeoutOccurred) LocationFailure.TIMEOUT else result.reason
                if ((failure == LocationFailure.TIMEOUT || failure == LocationFailure.NO_PROVIDER) && cachedFix != null) {
                    LocationResolutionState.Ready(
                        location = toSelectedDeviceLocation(cachedFix),
                        freshness = LocationFreshness.CACHED,
                    )
                } else {
                    LocationResolutionState.Unavailable(
                        reason = failure,
                        cachedLocation = cachedLocation,
                    )
                }
            }
        }
    }

    suspend fun acceptDeviceUpdate(candidate: DeviceLocationFix): LocationResolutionState {
        if (!candidate.isValid) {
            return LocationResolutionState.Unavailable(
                reason = LocationFailure.INVALID_FIX,
                cachedLocation = readCachedDeviceFix()?.takeIf { it.isValid }?.let { toSelectedDeviceLocation(it) },
            )
        }

        val previous = readCachedDeviceFix()?.takeIf { it.isValid }
        return handleDeviceFix(candidate, previous, LocationFreshness.FRESH)
    }

    private suspend fun handleDeviceSuccess(
        result: DeviceLocationResult.Success,
        previous: DeviceLocationFix?,
    ): LocationResolutionState {
        val candidate = if (result.freshness == LocationFreshness.CACHED && previous != null) {
            if (result.fix.capturedAt.isAfter(previous.capturedAt)) result.fix else previous
        } else {
            result.fix
        }
        return handleDeviceFix(candidate, previous, result.freshness)
    }

    private suspend fun handleDeviceFix(
        candidate: DeviceLocationFix,
        previous: DeviceLocationFix?,
        freshness: LocationFreshness,
    ): LocationResolutionState {
        if (!candidate.isValid) {
            return LocationResolutionState.Unavailable(
                reason = LocationFailure.INVALID_FIX,
                cachedLocation = previous?.let { toSelectedDeviceLocation(it) },
            )
        }

        if (!updatePolicy.shouldAccept(previous, candidate) && previous != null) {
            return LocationResolutionState.Ready(
                location = toSelectedDeviceLocation(previous),
                freshness = LocationFreshness.CACHED,
            )
        }

        if (!persist { preferencesRepository.saveDeviceFix(candidate) }) {
            return LocationResolutionState.Unavailable(
                reason = LocationFailure.PERSISTENCE_ERROR,
                cachedLocation = previous?.let { toSelectedDeviceLocation(it) },
            )
        }

        return LocationResolutionState.Ready(
            location = toSelectedDeviceLocation(candidate),
            freshness = freshness,
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
        return LocationResolutionState.Ready(city.toSelectedLocation(), freshness = null)
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

    private suspend fun toSelectedDeviceLocation(fix: DeviceLocationFix): SelectedLocation {
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
        )
    }

    private fun ManualCity.toSelectedLocation(): SelectedLocation = SelectedLocation(
        source = LocationSource.Manual(cityId = id),
        coordinates = coordinates,
        zoneId = zoneId,
        displayName = displayName,
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
''',
)

replace(
    'app/src/main/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsPresentation.kt',
    '"Nessuna posizione è arrivata entro 20 secondi. Puoi riprovare o scegliere una città manuale."',
    '"Nessuna posizione corrente è arrivata entro 30 secondi e non è disponibile alcuna posizione reale salvata. Puoi riprovare o scegliere una città manuale."',
)

replace(
    'app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/domain/PrayerScheduleModels.kt',
    'import com.archimedeprojects.arihna.core.location.model.LocationResolutionState\n',
    'import com.archimedeprojects.arihna.core.location.model.LocationFreshness\nimport com.archimedeprojects.arihna.core.location.model.LocationResolutionState\n',
)
replace(
    'app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/domain/PrayerScheduleModels.kt',
    '    val generatedAt: Instant,\n)',
    '    val generatedAt: Instant,\n    val locationFreshness: LocationFreshness? = null,\n)',
)
replace(
    'app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/domain/DefaultPrayerScheduleRepository.kt',
    '                        generatedAt = now,\n',
    '                        generatedAt = now,\n                        locationFreshness = ready.freshness,\n',
)

replace(
    'app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/presentation/PrayerSchedulePresentation.kt',
    'import com.archimedeprojects.arihna.core.location.model.LocationResolutionState\n',
    'import com.archimedeprojects.arihna.core.location.model.LocationFreshness\nimport com.archimedeprojects.arihna.core.location.model.LocationResolutionState\n',
)
replace(
    'app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/presentation/PrayerSchedulePresentation.kt',
    '        val nextPrayer: NextPrayerUiState?,\n    ) : PrayerScheduleUiState',
    '        val nextPrayer: NextPrayerUiState?,\n        val locationFreshness: LocationFreshness? = null,\n        val locationAge: Duration? = null,\n    ) : PrayerScheduleUiState',
)
replace(
    'app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/presentation/PrayerSchedulePresentation.kt',
    '        nextPrayer = schedule.nextPrayer?.let { nextPrayer ->\n            NextPrayerUiState(\n                prayer = nextPrayer.prayer,\n                time = nextPrayer.time,\n                remaining = remainingUntil(nextPrayer.time, now),\n            )\n        },\n    )',
    '        nextPrayer = schedule.nextPrayer?.let { nextPrayer ->\n            NextPrayerUiState(\n                prayer = nextPrayer.prayer,\n                time = nextPrayer.time,\n                remaining = remainingUntil(nextPrayer.time, now),\n            )\n        },\n        locationFreshness = schedule.locationFreshness,\n        locationAge = cachedDeviceLocationAge(\n            selectedLocation = schedule.selectedLocation,\n            freshness = schedule.locationFreshness,\n            now = now,\n        ),\n    )',
)
replace(
    'app/src/main/java/com/archimedeprojects/arihna/feature/prayerschedule/presentation/PrayerSchedulePresentation.kt',
    'private fun LocationSource.toUiSource(): PrayerScheduleLocationSourceUi = when (this) {',
    '''private fun cachedDeviceLocationAge(
    selectedLocation: SelectedLocation,
    freshness: LocationFreshness?,
    now: Instant,
): Duration? {
    if (freshness != LocationFreshness.CACHED) return null
    val source = selectedLocation.source as? LocationSource.Device ?: return null
    val age = Duration.between(source.capturedAt, now)
    return if (age.isNegative) Duration.ZERO else age
}

private fun LocationSource.toUiSource(): PrayerScheduleLocationSourceUi = when (this) {''',
)

write(
    'app/src/main/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreen.kt',
    '''package com.archimedeprojects.arihna.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleLocationSourceUi
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleUiState
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomePrayerScheduleRoute(
    contentPadding: PaddingValues,
    viewModel: PrayerScheduleViewModel,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    HomePrayerScheduleScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onOpenLocationSettings = onOpenLocationSettings,
        onRefreshLocation = onRefreshLocation,
    )
}

@Composable
fun HomePrayerScheduleScreen(
    contentPadding: PaddingValues,
    uiState: PrayerScheduleUiState,
    onOpenLocationSettings: () -> Unit,
    onRefreshLocation: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Orari di preghiera",
            style = MaterialTheme.typography.headlineSmall,
        )

        when (uiState) {
            PrayerScheduleUiState.Loading -> LoadingContent()
            is PrayerScheduleUiState.NoLocation -> NoLocationContent(
                state = uiState,
                onOpenLocationSettings = onOpenLocationSettings,
            )
            is PrayerScheduleUiState.CalculationUnavailable -> CalculationUnavailableContent(uiState)
            is PrayerScheduleUiState.Ready -> ReadyContent(uiState, onRefreshLocation)
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text(
            text = "Calcolo degli orari in corso…",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NoLocationContent(
    state: PrayerScheduleUiState.NoLocation,
    onOpenLocationSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onOpenLocationSettings) {
            Text("Configura posizione")
        }
    }
}

@Composable
private fun CalculationUnavailableContent(state: PrayerScheduleUiState.CalculationUnavailable) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Orari non disponibili",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Nessun orario viene mostrato finché il calcolo non torna disponibile.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ReadyContent(
    state: PrayerScheduleUiState.Ready,
    onRefreshLocation: () -> Unit,
) {
    val zoneId = state.today.zoneId

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.location.source == PrayerScheduleLocationSourceUi.DEVICE) {
            Text(
                text = "Posizione dispositivo",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = state.location.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.locationFreshness == LocationFreshness.CACHED) {
                val age = state.locationAge ?: Duration.ZERO
                Text(
                    text = "Basato su posizione di ${formatLocationAge(age)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Button(onClick = onRefreshLocation) {
                    Text("Aggiorna posizione")
                }
            }
        } else {
            Text(
                text = state.location.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Posizione manuale",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Prossima preghiera",
                style = MaterialTheme.typography.labelLarge,
            )
            val nextPrayer = state.nextPrayer
            if (nextPrayer == null) {
                Text(
                    text = "Nessuna prossima preghiera disponibile.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = "${prayerLabel(nextPrayer.prayer)} · ${formatTime(nextPrayer.time, zoneId)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Tra ${formatCountdown(nextPrayer.remaining)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    Text(
        text = "Metodo: ${methodLabel(state.settings.method)}",
        style = MaterialTheme.typography.bodySmall,
    )

    Text(
        text = "Oggi",
        style = MaterialTheme.typography.titleMedium,
    )

    PrayerTimeRow("Fajr", state.today.times.fajr, zoneId)
    PrayerTimeRow("Alba", state.today.times.sunrise, zoneId)
    PrayerTimeRow("Dhuhr", state.today.times.dhuhr, zoneId)
    PrayerTimeRow("Asr", state.today.times.asr, zoneId)
    PrayerTimeRow("Maghrib", state.today.times.maghrib, zoneId)
    PrayerTimeRow("Isha", state.today.times.isha, zoneId)
}

@Composable
private fun PrayerTimeRow(
    label: String,
    time: Instant,
    zoneId: ZoneId,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(formatTime(time, zoneId), style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider()
    }
}

private fun prayerLabel(prayer: PrayerName): String = when (prayer) {
    PrayerName.FAJR -> "Fajr"
    PrayerName.DHUHR -> "Dhuhr"
    PrayerName.ASR -> "Asr"
    PrayerName.MAGHRIB -> "Maghrib"
    PrayerName.ISHA -> "Isha"
}

private fun methodLabel(method: PrayerCalculationMethod): String = when (method) {
    PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League (MWL)"
    PrayerCalculationMethod.UMM_AL_QURA -> "Umm al-Qura"
    PrayerCalculationMethod.ISNA -> "ISNA"
    PrayerCalculationMethod.EGYPTIAN -> "Egyptian General Authority"
    PrayerCalculationMethod.KARACHI -> "University of Islamic Sciences, Karachi"
    PrayerCalculationMethod.DUBAI -> "Dubai"
    PrayerCalculationMethod.KUWAIT -> "Kuwait"
    PrayerCalculationMethod.QATAR -> "Qatar"
    PrayerCalculationMethod.MOONSIGHTING_COMMITTEE -> "Moonsighting Committee"
    PrayerCalculationMethod.SINGAPORE -> "Singapore"
    PrayerCalculationMethod.TURKEY -> "Turkey"
}

private fun formatLocationAge(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    return when {
        totalSeconds < 60L -> if (totalSeconds == 1L) "1 secondo fa" else "$totalSeconds secondi fa"
        totalSeconds < 3_600L -> {
            val minutes = totalSeconds / 60L
            if (minutes == 1L) "1 minuto fa" else "$minutes minuti fa"
        }
        totalSeconds < 86_400L -> {
            val hours = totalSeconds / 3_600L
            if (hours == 1L) "1 ora fa" else "$hours ore fa"
        }
        else -> {
            val days = totalSeconds / 86_400L
            if (days == 1L) "1 giorno fa" else "$days giorni fa"
        }
    }
}

private fun formatTime(time: Instant, zoneId: ZoneId): String =
    TIME_FORMATTER.withZone(zoneId).format(time)

private fun formatCountdown(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
}

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
''',
)

replace(
    'app/src/main/java/com/archimedeprojects/arihna/app/ArihnaNavHost.kt',
    '''                    onOpenLocationSettings = {
                        navController.navigate(Destination.Settings.route) {
                            launchSingleTop = true
                        }
                    },
''',
    '''                    onOpenLocationSettings = {
                        navController.navigate(Destination.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                    onRefreshLocation = {
                        val permissionState = locationPermissionStateResolver.resolve(
                            activity = activity,
                            hasRequestedBefore = locationSettingsViewModel.hasRequestedPermissionBefore(),
                        )
                        if (
                            permissionState == com.archimedeprojects.arihna.core.location.model.LocationPermissionState.Granted &&
                            locationEnvironment.isLocationServicesEnabled()
                        ) {
                            locationSettingsViewModel.selectDevice(
                                permissionState = permissionState,
                                locationServicesEnabled = true,
                            )
                        } else {
                            navController.navigate(Destination.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    },
''',
)

# Tests and expected copy.
replace(
    'app/src/test/java/com/archimedeprojects/arihna/core/location/domain/LocationUpdatePolicyTest.kt',
    'Duration.ofSeconds(20)',
    'Duration.ofSeconds(30)',
)
replace(
    'app/src/test/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsPresentationTest.kt',
    'assertTrue(timeout.message.contains("20 secondi"))',
    'assertTrue(timeout.message.contains("30 secondi"))',
)
replace(
    'app/src/androidTest/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreenAndroidTest.kt',
    '"Nessuna posizione è arrivata entro 20 secondi. Puoi riprovare o scegliere una città manuale."',
    '"Nessuna posizione corrente è arrivata entro 30 secondi e non è disponibile alcuna posizione reale salvata. Puoi riprovare o scegliere una città manuale."',
)
replace(
    'app/src/androidTest/java/com/archimedeprojects/arihna/core/location/platform/LocationManagerDeviceLocationDataSourceAndroidTest.kt',
    'fun oneShotProviderSelectionIsNetworkFirstWithFrameworkFusedFallback() {',
    'fun oneShotProviderSelectionUsesFrameworkFusedOnly() {',
)
replace(
    'app/src/androidTest/java/com/archimedeprojects/arihna/core/location/platform/LocationManagerDeviceLocationDataSourceAndroidTest.kt',
    '''        assertEquals(
            LocationManager.NETWORK_PROVIDER,
            selectCurrentLocationProviderFromEnabledProviders(
                setOf("fused", LocationManager.NETWORK_PROVIDER),
            ),
        )
        assertEquals(
            "fused",
            selectCurrentLocationProviderFromEnabledProviders(setOf("fused")),
        )
        assertNull(
            selectCurrentLocationProviderFromEnabledProviders(
                setOf(LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER),
            ),
        )
''',
    '''        assertEquals(
            "fused",
            selectCurrentLocationProviderFromEnabledProviders(
                setOf("fused", LocationManager.NETWORK_PROVIDER),
            ),
        )
        assertEquals(
            "fused",
            selectCurrentLocationProviderFromEnabledProviders(setOf("fused")),
        )
        assertNull(
            selectCurrentLocationProviderFromEnabledProviders(
                setOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER),
            ),
        )
''',
)

# Add new interface method to test fakes.
for test_path in [
    'app/src/test/java/com/archimedeprojects/arihna/core/location/domain/LocationCoordinatorTest.kt',
    'app/src/test/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsViewModelTest.kt',
]:
    replace(
        test_path,
        '        override suspend fun getCurrentLocation(): DeviceLocationResult {\n',
        '        override suspend fun getLastKnownLocation(): DeviceLocationResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)\n\n        override suspend fun getCurrentLocation(): DeviceLocationResult {\n',
    ) if 'LocationCoordinatorTest.kt' in test_path else replace(
        test_path,
        '        override suspend fun getCurrentLocation(): DeviceLocationResult = currentResult\n',
        '        override suspend fun getCurrentLocation(): DeviceLocationResult = currentResult\n        override suspend fun getLastKnownLocation(): DeviceLocationResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)\n',
    )

# Coordinator fake gets configurable last-known plus focused timeout fallback test.
replace(
    'app/src/test/java/com/archimedeprojects/arihna/core/location/domain/LocationCoordinatorTest.kt',
    '        var currentCalls: Int = 0\n',
    '        var currentCalls: Int = 0\n        var lastKnownResult: DeviceLocationResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)\n        var lastKnownCalls: Int = 0\n',
)
replace(
    'app/src/test/java/com/archimedeprojects/arihna/core/location/domain/LocationCoordinatorTest.kt',
    '        override suspend fun getLastKnownLocation(): DeviceLocationResult = DeviceLocationResult.Unavailable(LocationFailure.NO_PROVIDER)\n',
    '''        override suspend fun getLastKnownLocation(): DeviceLocationResult {
            lastKnownCalls += 1
            return lastKnownResult
        }
''',
)
replace(
    'app/src/test/java/com/archimedeprojects/arihna/core/location/domain/LocationCoordinatorTest.kt',
    '    @Test\n    fun timeoutWithoutCacheNeverInventsLocation() = runBlocking {',
    '''    @Test
    fun timeoutUsesRealLastKnownAsCachedReady() = runBlocking {
        val lastKnown = romeFix().copy(capturedAt = Instant.parse("2026-08-29T09:30:00Z"))
        val device = FakeDeviceLocationDataSource().apply {
            delayMillis = 100
            currentResult = DeviceLocationResult.Success(romeFix())
            lastKnownResult = DeviceLocationResult.Success(lastKnown, LocationFreshness.CACHED)
        }
        val coordinator = coordinator(
            device = device,
            preferences = FakeLocationPreferencesRepository(LocationPreference.Device),
            policy = LocationUpdatePolicy(currentFixTimeout = Duration.ofMillis(10)),
        )

        val state = coordinator.resolveDevice(LocationPermissionState.Granted, true)

        val ready = state as LocationResolutionState.Ready
        assertEquals(LocationFreshness.CACHED, ready.freshness)
        assertEquals(lastKnown.coordinates, ready.location.coordinates)
        assertEquals(1, device.lastKnownCalls)
    }

    @Test
    fun timeoutWithoutCacheNeverInventsLocation() = runBlocking {''',
)
replace(
    'app/src/test/java/com/archimedeprojects/arihna/core/location/domain/LocationCoordinatorTest.kt',
    'import com.archimedeprojects.arihna.core.location.model.LocationFailure\n',
    'import com.archimedeprojects.arihna.core.location.model.LocationFailure\nimport com.archimedeprojects.arihna.core.location.model.LocationFreshness\n',
)

# Home instrumentation: cached disclosure + retry.
replace(
    'app/src/androidTest/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreenAndroidTest.kt',
    'import com.archimedeprojects.arihna.core.location.model.LocationResolutionState\n',
    'import com.archimedeprojects.arihna.core.location.model.LocationFreshness\nimport com.archimedeprojects.arihna.core.location.model.LocationResolutionState\n',
)
replace(
    'app/src/androidTest/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreenAndroidTest.kt',
    '    @Test\n    fun calculationUnavailableShowsControlledErrorWithoutInventedTimes() {',
    '''    @Test
    fun cachedDeviceLocationShowsAgeAndRefreshAction() {
        var refreshCalls = 0
        val state = readyState().copy(
            location = PrayerScheduleLocationUi(
                displayName = "Pegognaga, Italia",
                source = PrayerScheduleLocationSourceUi.DEVICE,
            ),
            selectedLocation = SelectedLocation(
                source = LocationSource.Device(
                    capturedAt = Instant.parse("2026-08-31T09:00:00Z"),
                    accuracyMeters = 2_000f,
                ),
                coordinates = Coordinates(44.99, 10.85),
                zoneId = ZoneId.of("Europe/Rome"),
                displayName = "Pegognaga, Italia",
            ),
            locationFreshness = LocationFreshness.CACHED,
            locationAge = Duration.ofHours(2),
        )
        setScreen(uiState = state, onRefreshLocation = { refreshCalls += 1 })

        composeRule.onNodeWithText("Basato su posizione di 2 ore fa").assertIsDisplayed()
        composeRule.onNodeWithText("Aggiorna posizione").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, refreshCalls) }
    }

    @Test
    fun calculationUnavailableShowsControlledErrorWithoutInventedTimes() {''',
)
replace(
    'app/src/androidTest/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreenAndroidTest.kt',
    '        onOpenLocationSettings: () -> Unit = {},\n',
    '        onOpenLocationSettings: () -> Unit = {},\n        onRefreshLocation: () -> Unit = {},\n',
)
replace(
    'app/src/androidTest/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreenAndroidTest.kt',
    '                    onOpenLocationSettings = onOpenLocationSettings,\n',
    '                    onOpenLocationSettings = onOpenLocationSettings,\n                    onRefreshLocation = onRefreshLocation,\n',
)
