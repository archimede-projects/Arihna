package com.archimedeprojects.arihna.feature.prayerschedule.presentation

import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleState
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

enum class PrayerScheduleLocationSourceUi {
    DEVICE,
    MANUAL,
}

data class PrayerScheduleLocationUi(
    val displayName: String,
    val source: PrayerScheduleLocationSourceUi,
)

data class NextPrayerUiState(
    val prayer: PrayerName,
    val time: Instant,
    val remaining: Duration,
)

sealed interface PrayerScheduleUiState {
    data object Loading : PrayerScheduleUiState

    data class NoLocation(
        val message: String,
        val locationState: LocationResolutionState,
    ) : PrayerScheduleUiState

    data class Ready(
        val localDate: LocalDate,
        val location: PrayerScheduleLocationUi,
        val selectedLocation: SelectedLocation,
        val settings: PrayerCalculationSettings,
        val today: PrayerDay,
        val nextPrayer: NextPrayerUiState?,
        val locationFreshness: LocationFreshness? = null,
        val locationAge: Duration? = null,
    ) : PrayerScheduleUiState

    data class CalculationUnavailable(
        val message: String,
        val reason: PrayerCalculationResult.Reason,
        val selectedLocation: SelectedLocation,
    ) : PrayerScheduleUiState
}

internal fun PrayerScheduleState.toUiState(now: Instant): PrayerScheduleUiState = when (this) {
    PrayerScheduleState.Loading -> PrayerScheduleUiState.Loading

    is PrayerScheduleState.NoLocation -> PrayerScheduleUiState.NoLocation(
        message = locationMessage(locationState),
        locationState = locationState,
    )

    is PrayerScheduleState.Ready -> PrayerScheduleUiState.Ready(
        localDate = schedule.localDate,
        location = PrayerScheduleLocationUi(
            displayName = schedule.selectedLocation.displayName,
            source = schedule.selectedLocation.source.toUiSource(),
        ),
        selectedLocation = schedule.selectedLocation,
        settings = schedule.settings,
        today = schedule.today,
        nextPrayer = schedule.nextPrayer?.let { nextPrayer ->
            NextPrayerUiState(
                prayer = nextPrayer.prayer,
                time = nextPrayer.time,
                remaining = remainingUntil(nextPrayer.time, now),
            )
        },
        locationFreshness = schedule.selectedLocation.freshness,
        locationAge = cachedDeviceLocationAge(
            selectedLocation = schedule.selectedLocation,
            now = now,
        ),
    )

    is PrayerScheduleState.CalculationUnavailable -> PrayerScheduleUiState.CalculationUnavailable(
        message = "Orari di preghiera non disponibili per la posizione e le impostazioni selezionate.",
        reason = reason,
        selectedLocation = selectedLocation,
    )
}

internal fun remainingUntil(target: Instant, now: Instant): Duration {
    val remaining = Duration.between(now, target)
    return if (remaining.isNegative) Duration.ZERO else remaining
}

private fun cachedDeviceLocationAge(
    selectedLocation: SelectedLocation,
    now: Instant,
): Duration? {
    if (selectedLocation.freshness != LocationFreshness.CACHED) return null
    val source = selectedLocation.source as? LocationSource.Device ?: return null
    val age = Duration.between(source.capturedAt, now)
    return if (age.isNegative) Duration.ZERO else age
}

private fun LocationSource.toUiSource(): PrayerScheduleLocationSourceUi = when (this) {
    is LocationSource.Device -> PrayerScheduleLocationSourceUi.DEVICE
    is LocationSource.Manual -> PrayerScheduleLocationSourceUi.MANUAL
}

private fun locationMessage(state: LocationResolutionState): String = when (state) {
    LocationResolutionState.Unconfigured ->
        "Imposta una posizione per calcolare gli orari di preghiera."

    LocationResolutionState.Resolving ->
        "Posizione in aggiornamento. Gli orari di preghiera saranno disponibili appena pronta."

    is LocationResolutionState.PermissionDenied ->
        "Permesso posizione non disponibile. Puoi autorizzarlo oppure scegliere una città manualmente."

    is LocationResolutionState.LocationServicesDisabled ->
        "Servizi di localizzazione disattivati. Attivali oppure scegli una città manualmente."

    is LocationResolutionState.Unavailable ->
        "Posizione non disponibile. Riprova oppure scegli una città manualmente."

    is LocationResolutionState.Ready ->
        "Posizione pronta."
}
