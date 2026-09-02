package com.archimedeprojects.arihna.core.location.model

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
