package com.archimedeprojects.arihna.core.location.domain

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
