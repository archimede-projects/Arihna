package com.archimedeprojects.arihna.core.prayer.model

sealed interface PrayerCalculationResult {
    data class Success(
        val prayerDay: PrayerDay,
    ) : PrayerCalculationResult

    data class Unavailable(
        val reason: Reason,
    ) : PrayerCalculationResult

    enum class Reason {
        INVALID_COORDINATES,
        EXTREME_LATITUDE,
        ASTRONOMICAL_EVENT_UNAVAILABLE,
        CALCULATION_ERROR,
    }
}
