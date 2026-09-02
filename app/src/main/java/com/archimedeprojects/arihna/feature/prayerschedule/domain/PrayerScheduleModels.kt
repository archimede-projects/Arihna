package com.archimedeprojects.arihna.feature.prayerschedule.domain

import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PrayerScheduleInput(
    val coordinates: Coordinates,
    val zoneId: ZoneId,
    val settings: PrayerCalculationSettings,
    val localDate: LocalDate,
)

enum class PrayerName {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

data class NextPrayer(
    val prayer: PrayerName,
    val time: Instant,
)

data class PrayerSchedule(
    val localDate: LocalDate,
    val selectedLocation: SelectedLocation,
    val settings: PrayerCalculationSettings,
    val today: PrayerDay,
    val nextPrayer: NextPrayer?,
    val generatedAt: Instant,
    val locationFreshness: LocationFreshness? = null,
)

sealed interface PrayerScheduleState {
    data object Loading : PrayerScheduleState

    data class NoLocation(
        val locationState: LocationResolutionState,
    ) : PrayerScheduleState

    data class Ready(
        val schedule: PrayerSchedule,
    ) : PrayerScheduleState

    data class CalculationUnavailable(
        val reason: PrayerCalculationResult.Reason,
        val selectedLocation: SelectedLocation,
    ) : PrayerScheduleState
}
