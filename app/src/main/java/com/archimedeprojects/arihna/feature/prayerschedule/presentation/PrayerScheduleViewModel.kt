package com.archimedeprojects.arihna.feature.prayerschedule.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsDefaults
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsRepository
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleRepository
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleState
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class PrayerAdjustmentTarget { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

class PrayerScheduleViewModel(
    private val repository: PrayerScheduleRepository,
    private val clock: Clock,
    private val ticker: PrayerScheduleTicker,
    private val settingsRepository: PrayerSettingsRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PrayerScheduleUiState>(PrayerScheduleUiState.Loading)
    val uiState: StateFlow<PrayerScheduleUiState> = _uiState.asStateFlow()

    private val _prayerSettings = MutableStateFlow(PrayerSettingsDefaults.CANONICAL)
    val prayerSettings: StateFlow<PrayerCalculationSettings> = _prayerSettings.asStateFlow()

    private var latestDomainState: PrayerScheduleState = PrayerScheduleState.Loading
    private var refreshedExpiredTarget: Instant? = null

    init {
        observeRepository()
        observeSettings()
        observeTicker()
    }

    fun setCalculationMethod(method: PrayerCalculationMethod) = mutateSettings { it.copy(method = method) }
    fun setAsrMethod(method: AsrMethod) = mutateSettings { it.copy(asrMethod = method) }
    fun setHighLatitudeRule(rule: HighLatitudeRule) = mutateSettings { it.copy(highLatitudeRule = rule) }

    fun adjustPrayer(target: PrayerAdjustmentTarget, deltaMinutes: Int) {
        mutateSettings { current ->
            val a = current.adjustments
            fun bounded(value: Int) = (value + deltaMinutes).coerceIn(MIN_ADJUSTMENT, MAX_ADJUSTMENT)
            current.copy(
                adjustments = when (target) {
                    PrayerAdjustmentTarget.FAJR -> a.copy(fajrMinutes = bounded(a.fajrMinutes))
                    PrayerAdjustmentTarget.SUNRISE -> a.copy(sunriseMinutes = bounded(a.sunriseMinutes))
                    PrayerAdjustmentTarget.DHUHR -> a.copy(dhuhrMinutes = bounded(a.dhuhrMinutes))
                    PrayerAdjustmentTarget.ASR -> a.copy(asrMinutes = bounded(a.asrMinutes))
                    PrayerAdjustmentTarget.MAGHRIB -> a.copy(maghribMinutes = bounded(a.maghribMinutes))
                    PrayerAdjustmentTarget.ISHA -> a.copy(ishaMinutes = bounded(a.ishaMinutes))
                },
            )
        }
    }

    fun resetPrayerSettings() {
        val target = settingsRepository ?: return
        viewModelScope.launch { target.update(PrayerSettingsDefaults.CANONICAL) }
    }

    private fun mutateSettings(transform: (PrayerCalculationSettings) -> PrayerCalculationSettings) {
        val target = settingsRepository ?: return
        viewModelScope.launch { target.update(transform(target.get())) }
    }

    private fun observeSettings() {
        val target = settingsRepository ?: return
        viewModelScope.launch { target.settings.collect { _prayerSettings.value = it } }
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.observeSchedule().collect { state ->
                latestDomainState = state
                val target = (state as? PrayerScheduleState.Ready)?.schedule?.nextPrayer?.time
                if (target != refreshedExpiredTarget) refreshedExpiredTarget = null
                _uiState.value = state.toUiState(clock.instant())
            }
        }
    }

    private fun observeTicker() {
        viewModelScope.launch { ticker.ticks().collect { onTick() } }
    }

    private suspend fun onTick() {
        val state = latestDomainState as? PrayerScheduleState.Ready ?: return
        val now = clock.instant()
        _uiState.value = state.toUiState(now)
        val target = state.schedule.nextPrayer?.time ?: return
        if (!now.isBefore(target) && refreshedExpiredTarget != target) {
            refreshedExpiredTarget = target
            repository.refresh()
        }
    }

    companion object {
        const val MIN_ADJUSTMENT = -60
        const val MAX_ADJUSTMENT = 60
    }
}
