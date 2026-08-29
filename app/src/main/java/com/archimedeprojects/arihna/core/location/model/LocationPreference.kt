package com.archimedeprojects.arihna.core.location.model

sealed interface LocationPreference {
    data object Unset : LocationPreference
    data object Device : LocationPreference
    data class Manual(val city: ManualCitySnapshot) : LocationPreference
}
