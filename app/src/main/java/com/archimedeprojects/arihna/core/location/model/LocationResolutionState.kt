package com.archimedeprojects.arihna.core.location.model

sealed interface LocationResolutionState {
    data object Unconfigured : LocationResolutionState
    data object Resolving : LocationResolutionState

    data class Ready(
        val location: SelectedLocation,
        val freshness: LocationFreshness?,
    ) : LocationResolutionState

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
