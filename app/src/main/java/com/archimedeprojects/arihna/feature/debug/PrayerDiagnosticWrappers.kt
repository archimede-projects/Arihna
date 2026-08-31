package com.archimedeprojects.arihna.feature.debug

import android.os.SystemClock
import com.archimedeprojects.arihna.core.location.diagnostics.LocationDiagnosticTrace
import com.archimedeprojects.arihna.core.prayer.calculation.PrayerTimeCalculator
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsRepository
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleRepository
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleState
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class TracingPrayerTimeCalculator(
    private val delegate: PrayerTimeCalculator,
) : PrayerTimeCalculator {
    override fun calculate(
        date: LocalDate,
        coordinates: Coordinates,
        zoneId: ZoneId,
        settings: PrayerCalculationSettings,
    ): PrayerCalculationResult {
        val start = SystemClock.elapsedRealtime()
        LocationDiagnosticTrace.record(
            "PRAYER_CALC_START",
            "date=$date lat=${coordinates.latitude} lon=${coordinates.longitude} zone=${zoneId.id} method=${settings.method}",
        )
        return try {
            delegate.calculate(date, coordinates, zoneId, settings).also { result ->
                LocationDiagnosticTrace.record(
                    "PRAYER_CALC_END",
                    "durationMs=${SystemClock.elapsedRealtime() - start} result=${result.javaClass.simpleName}",
                )
            }
        } catch (error: Throwable) {
            LocationDiagnosticTrace.record(
                "PRAYER_CALC_THROW",
                "durationMs=${SystemClock.elapsedRealtime() - start} ${error.javaClass.simpleName}: ${error.message}",
            )
            throw error
        }
    }
}

class TracingPrayerSettingsRepository(
    private val delegate: PrayerSettingsRepository,
) : PrayerSettingsRepository {
    override val settings: Flow<PrayerCalculationSettings> = delegate.settings
        .onStart { LocationDiagnosticTrace.record("PRAYER_SETTINGS_SUBSCRIBE") }
        .onEach { settings ->
            LocationDiagnosticTrace.record(
                "PRAYER_SETTINGS_EMIT",
                "method=${settings.method} asr=${settings.asrMethod} highLat=${settings.highLatitudeRule}",
            )
        }

    override suspend fun get(): PrayerCalculationSettings = delegate.get()

    override suspend fun update(settings: PrayerCalculationSettings) = delegate.update(settings)
}

class TracingPrayerScheduleRepository(
    private val delegate: PrayerScheduleRepository,
) : PrayerScheduleRepository {
    override fun observeSchedule(): Flow<PrayerScheduleState> = delegate.observeSchedule()
        .onStart { LocationDiagnosticTrace.record("SCHEDULE_OBSERVE_START") }
        .onEach { state -> LocationDiagnosticTrace.record("SCHEDULE_STATE", state.diagnosticLabel()) }

    override suspend fun refresh() {
        LocationDiagnosticTrace.record("SCHEDULE_REFRESH")
        delegate.refresh()
    }
}

fun PrayerScheduleState.diagnosticLabel(): String = when (this) {
    PrayerScheduleState.Loading -> "Loading"
    is PrayerScheduleState.NoLocation -> "NoLocation(${locationState.javaClass.simpleName})"
    is PrayerScheduleState.CalculationUnavailable ->
        "CalculationUnavailable(reason=$reason location=${selectedLocation.displayName})"
    is PrayerScheduleState.Ready ->
        "Ready(location=${schedule.selectedLocation.displayName} date=${schedule.localDate} next=${schedule.nextPrayer?.prayer}@${schedule.nextPrayer?.time})"
}
