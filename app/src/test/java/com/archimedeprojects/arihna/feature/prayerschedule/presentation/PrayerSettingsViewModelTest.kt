package com.archimedeprojects.arihna.feature.prayerschedule.presentation

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
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun updatesPersistAndAdjustmentIsBoundedAndResetRestoresCanonical() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = PrayerScheduleViewModel(
            repository = FakeScheduleRepository(),
            clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
            ticker = object : PrayerScheduleTicker { override fun ticks(): Flow<Unit> = MutableStateFlow(Unit) },
            settingsRepository = settings,
        )
        advanceUntilIdle()
        vm.setCalculationMethod(PrayerCalculationMethod.UMM_AL_QURA)
        vm.setAsrMethod(AsrMethod.HANAFI)
        vm.setHighLatitudeRule(HighLatitudeRule.TWILIGHT_ANGLE)
        repeat(80) { vm.adjustPrayer(PrayerAdjustmentTarget.FAJR, 1) }
        advanceUntilIdle()
        assertEquals(PrayerCalculationMethod.UMM_AL_QURA, settings.current.method)
        assertEquals(AsrMethod.HANAFI, settings.current.asrMethod)
        assertEquals(HighLatitudeRule.TWILIGHT_ANGLE, settings.current.highLatitudeRule)
        assertEquals(60, settings.current.adjustments.fajrMinutes)
        vm.resetPrayerSettings()
        advanceUntilIdle()
        assertEquals(PrayerSettingsDefaults.CANONICAL, settings.current)
    }

    private class FakeSettingsRepository : PrayerSettingsRepository {
        private val state = MutableStateFlow(PrayerSettingsDefaults.CANONICAL)
        var current: PrayerCalculationSettings = PrayerSettingsDefaults.CANONICAL
            private set
        override val settings: Flow<PrayerCalculationSettings> = state
        override suspend fun get(): PrayerCalculationSettings = current
        override suspend fun update(settings: PrayerCalculationSettings) {
            current = settings
            state.value = settings
        }
    }

    private class FakeScheduleRepository : PrayerScheduleRepository {
        override fun observeSchedule(): Flow<PrayerScheduleState> = MutableStateFlow(PrayerScheduleState.Loading)
        override suspend fun refresh() = Unit
    }
}
