package com.archimedeprojects.arihna.feature.prayerschedule.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleRepository
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleState
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PrayerScheduleViewModel(
    private val repository: PrayerScheduleRepository,
    private val clock: Clock,
    private val ticker: PrayerScheduleTicker,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PrayerScheduleUiState>(PrayerScheduleUiState.Loading)
    val uiState: StateFlow<PrayerScheduleUiState> = _uiState.asStateFlow()

    private var latestDomainState: PrayerScheduleState = PrayerScheduleState.Loading
    private var refreshedExpiredTarget: Instant? = null

    init {
        observeRepository()
        observeTicker()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.observeSchedule().collect { state ->
                latestDomainState = state

                val target = (state as? PrayerScheduleState.Ready)
                    ?.schedule
                    ?.nextPrayer
                    ?.time
                if (target != refreshedExpiredTarget) {
                    refreshedExpiredTarget = null
                }

                _uiState.value = state.toUiState(clock.instant())
            }
        }
    }

    private fun observeTicker() {
        viewModelScope.launch {
            ticker.ticks().collect {
                onTick()
            }
        }
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
}
