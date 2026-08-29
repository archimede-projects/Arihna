package com.archimedeprojects.arihna.core.location.model

enum class LocationFailure {
    TIMEOUT,
    NO_PROVIDER,
    INVALID_FIX,
    CITY_NOT_FOUND,
    CITY_DATASET_UNAVAILABLE,
    UNSUPPORTED_TIME_ZONE,
    PERSISTENCE_ERROR,
}
