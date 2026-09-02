package com.archimedeprojects.arihna.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archimedeprojects.arihna.core.location.data.CityRepository
import com.archimedeprojects.arihna.core.location.data.DeviceLocationDataSource
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.domain.LocationCoordinator
import com.archimedeprojects.arihna.core.location.model.CitySearchResult
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationPermissionState
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LocationModeUi {
    Unconfigured,
    Device,
    Manual,
}

data class LocationSettingsUiState(
    val resolutionState: LocationResolutionState = LocationResolutionState.Unconfigured,
    val activeMode: LocationModeUi = LocationModeUi.Unconfigured,
    val rationaleVisible: Boolean = false,
    val hasRequestedPermissionBefore: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<CitySearchResult> = emptyList(),
    val searchInProgress: Boolean = false,
    val searchMessage: String? = null,
)

class LocationSettingsViewModel(
    private val coordinator: LocationCoordinator,
    private val cityRepository: CityRepository,
    private val deviceLocationDataSource: DeviceLocationDataSource,
    private val preferencesRepository: LocationPreferencesRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        LocationSettingsUiState(
            hasRequestedPermissionBefore = savedStateHandle[KEY_PERMISSION_REQUESTED] ?: false,
        ),
    )
    val uiState: StateFlow<LocationSettingsUiState> = _uiState.asStateFlow()

    private var foreground = false
    private var resolutionJob: Job? = null
    private var updateJob: Job? = null
    private var searchJob: Job? = null

    fun hasRequestedPermissionBefore(): Boolean =
        savedStateHandle[KEY_PERMISSION_REQUESTED] ?: false

    fun onUseDeviceClick() {
        _uiState.update { it.copy(rationaleVisible = true) }
    }

    fun dismissRationale() {
        _uiState.update { it.copy(rationaleVisible = false) }
    }

    fun markPermissionRequestStarted() {
        savedStateHandle[KEY_PERMISSION_REQUESTED] = true
        _uiState.update {
            it.copy(
                hasRequestedPermissionBefore = true,
                rationaleVisible = false,
            )
        }
    }

    fun onForeground(
        permissionState: LocationPermissionState,
        locationServicesEnabled: Boolean,
    ) {
        foreground = true
        restoreForeground(
            permissionState = permissionState,
            locationServicesEnabled = locationServicesEnabled,
        )
    }

    fun onBackground() {
        foreground = false
        resolutionJob?.cancel()
        resolutionJob = null
        updateJob?.cancel()
        updateJob = null
    }

    fun selectDevice(
        permissionState: LocationPermissionState,
        locationServicesEnabled: Boolean,
    ) {
        _uiState.update { it.copy(rationaleVisible = false) }
        resolveSelectedDevice(
            permissionState = permissionState,
            locationServicesEnabled = locationServicesEnabled,
        )
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchMessage = null,
            )
        }
        searchJob?.cancel()

        val normalized = query.trim()
        if (normalized.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchInProgress = false,
                    searchMessage = null,
                )
            }
            return
        }

        if (normalized.length < 2) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    searchInProgress = false,
                    searchMessage = "Inserisci almeno 2 caratteri.",
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(searchInProgress = true) }
            delay(SEARCH_DEBOUNCE_MILLIS)
            val result = try {
                cityRepository.search(normalized)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }

            if (result == null) {
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        searchInProgress = false,
                        searchMessage = "Archivio città non disponibile.",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        searchResults = result,
                        searchInProgress = false,
                        searchMessage = if (result.isEmpty()) "Nessuna città trovata." else null,
                    )
                }
            }
        }
    }

    fun selectManual(cityId: Long) {
        resolutionJob?.cancel()
        stopUpdates()
        resolutionJob = viewModelScope.launch {
            _uiState.update { it.copy(resolutionState = LocationResolutionState.Resolving) }
            val state = coordinator.selectManual(cityId)
            val mode = readMode()
            _uiState.update {
                it.copy(
                    resolutionState = state,
                    activeMode = mode,
                    searchQuery = if (state is LocationResolutionState.Ready) "" else it.searchQuery,
                    searchResults = if (state is LocationResolutionState.Ready) emptyList() else it.searchResults,
                    searchMessage = null,
                )
            }
        }
    }

    private fun restoreForeground(
        permissionState: LocationPermissionState,
        locationServicesEnabled: Boolean,
    ) {
        resolutionJob?.cancel()
        resolutionJob = viewModelScope.launch {
            val persistedState = coordinator.restorePersistedState(
                permissionState = permissionState,
                locationServicesEnabled = locationServicesEnabled,
            )
            val mode = readMode()
            _uiState.update {
                it.copy(
                    resolutionState = persistedState,
                    activeMode = mode,
                )
            }

            val shouldRevalidate =
                mode == LocationModeUi.Device &&
                    permissionState == LocationPermissionState.Granted &&
                    locationServicesEnabled &&
                    (persistedState is LocationResolutionState.Ready ||
                        persistedState == LocationResolutionState.Resolving)

            if (shouldRevalidate) {
                val revalidatedState = coordinator.resolveDevice(
                    permissionState = permissionState,
                    locationServicesEnabled = locationServicesEnabled,
                )
                if (!foreground || readMode() != LocationModeUi.Device) return@launch

                val visibleState = preserveCachedReadyDuringRevalidation(
                    persistedState = persistedState,
                    revalidatedState = revalidatedState,
                )
                _uiState.update {
                    it.copy(
                        resolutionState = visibleState,
                        activeMode = LocationModeUi.Device,
                    )
                }
            }

            configureForegroundUpdates(
                mode = mode,
                permissionState = permissionState,
                locationServicesEnabled = locationServicesEnabled,
            )
        }
    }

    private fun resolveSelectedDevice(
        permissionState: LocationPermissionState,
        locationServicesEnabled: Boolean,
    ) {
        resolutionJob?.cancel()
        resolutionJob = viewModelScope.launch {
            _uiState.update { it.copy(resolutionState = LocationResolutionState.Resolving) }

            val state = coordinator.selectDevice(permissionState, locationServicesEnabled)
            val mode = readMode()
            _uiState.update {
                it.copy(
                    resolutionState = state,
                    activeMode = mode,
                )
            }

            configureForegroundUpdates(
                mode = mode,
                permissionState = permissionState,
                locationServicesEnabled = locationServicesEnabled,
            )
        }
    }

    private fun preserveCachedReadyDuringRevalidation(
        persistedState: LocationResolutionState,
        revalidatedState: LocationResolutionState,
    ): LocationResolutionState = if (
        persistedState is LocationResolutionState.Ready &&
        persistedState.freshness == LocationFreshness.CACHED &&
        revalidatedState !is LocationResolutionState.Ready
    ) {
        persistedState
    } else {
        revalidatedState
    }

    private fun configureForegroundUpdates(
        mode: LocationModeUi,
        permissionState: LocationPermissionState,
        locationServicesEnabled: Boolean,
    ) {
        if (
            !foreground ||
            mode != LocationModeUi.Device ||
            permissionState != LocationPermissionState.Granted ||
            !locationServicesEnabled
        ) {
            stopUpdates()
            return
        }

        if (updateJob?.isActive == true) return
        updateJob = viewModelScope.launch {
            try {
                deviceLocationDataSource.observeSignificantUpdates().collect { candidate ->
                    val state = coordinator.acceptDeviceUpdate(candidate)
                    if (readMode() == LocationModeUi.Device) {
                        _uiState.update {
                            it.copy(
                                resolutionState = state,
                                activeMode = LocationModeUi.Device,
                            )
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The foreground fresh-fix resolution owns visible provider/timeout errors.
                // Update-stream failures are retried on the next foreground transition.
            }
        }
    }

    private fun stopUpdates() {
        updateJob?.cancel()
        updateJob = null
    }

    private suspend fun readMode(): LocationModeUi = try {
        when (preferencesRepository.preference.first()) {
            LocationPreference.Unset -> LocationModeUi.Unconfigured
            LocationPreference.Device -> LocationModeUi.Device
            is LocationPreference.Manual -> LocationModeUi.Manual
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        _uiState.value.activeMode
    }

    override fun onCleared() {
        stopUpdates()
        super.onCleared()
    }

    private companion object {
        const val KEY_PERMISSION_REQUESTED = "location_permission_requested_before"
        const val SEARCH_DEBOUNCE_MILLIS = 250L
    }
}
