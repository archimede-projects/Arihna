package com.archimedeprojects.arihna.core.location.domain

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
