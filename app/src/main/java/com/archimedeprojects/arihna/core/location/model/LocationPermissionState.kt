package com.archimedeprojects.arihna.core.location.model

sealed interface LocationPermissionState {
    data object NotRequested : LocationPermissionState
    data object Granted : LocationPermissionState
    data class Denied(val canRequestAgain: Boolean) : LocationPermissionState
}
