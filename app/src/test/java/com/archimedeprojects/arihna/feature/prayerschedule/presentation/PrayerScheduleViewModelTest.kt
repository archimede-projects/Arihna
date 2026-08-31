package com.archimedeprojects.arihna.feature.prayerschedule.presentation

import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.archimedeprojects.arihna.feature.prayerschedule.domain.NextPrayer
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerSchedule
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleRepository
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleState
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerScheduleViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noLocationMapsToClearLocationRequiredMessage() = runTest(dispatcher) {
        val repository = FakePrayerScheduleRepository(
            PrayerScheduleState.NoLocation(LocationResolutionState.Unconfigured),
        )
        val viewModel = PrayerScheduleViewModel(repository, MutableClock(NOW), ManualTicker())

        advanceUntilIdle()

        val state = viewModel.uiState.value as PrayerScheduleUiState.NoLocation
        assertEquals("Imposta una posizione per calcolare gli orari di preghiera.", state.message)
        assertEquals(LocationResolutionState.Unconfigured, state.locationState)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun readyMapsLocationNextPrayerAndInitialCountdown() = runTest(dispatcher) {
        val target = NOW.plusSeconds(90)
        val repository = FakePrayerScheduleRepository(readyState(PrayerName.DHUHR, target))
        val viewModel = PrayerScheduleViewModel(repository, MutableClock(NOW), ManualTicker())

        advanceUntilIdle()

        val state = viewModel.uiState.value as PrayerScheduleUiState.Ready
        assertEquals("Roma, Lazio, Italia", state.location.displayName)
        assertEquals(PrayerScheduleLocationSourceUi.MANUAL, state.location.source)
        assertEquals(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE, state.settings.method)
        assertEquals(PrayerName.DHUHR, state.nextPrayer?.prayer)
        assertEquals(target, state.nextPrayer?.time)
        assertEquals(Duration.ofSeconds(90), state.nextPrayer?.remaining)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun ordinaryOneSecondTicksOnlyUpdateCountdownAndNeverRefresh() = runTest(dispatcher) {
        val clock = MutableClock(NOW)
        val ticker = ManualTicker()
        val repository = FakePrayerScheduleRepository(
            readyState(PrayerName.DHUHR, NOW.plusSeconds(10)),
        )
        val viewModel = PrayerScheduleViewModel(repository, clock, ticker)
        advanceUntilIdle()

        repeat(3) {
            clock.advance(Duration.ofSeconds(1))
            ticker.tick()
            advanceUntilIdle()
        }

        val state = viewModel.uiState.value as PrayerScheduleUiState.Ready
        assertEquals(Duration.ofSeconds(7), state.nextPrayer?.remaining)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun countdownExpiryRefreshesExactlyOnceForSameTarget() = runTest(dispatcher) {
        val clock = MutableClock(NOW)
        val ticker = ManualTicker()
        val target = NOW.plusSeconds(2)
        val repository = FakePrayerScheduleRepository(readyState(PrayerName.DHUHR, target))
        val viewModel = PrayerScheduleViewModel(repository, clock, ticker)
        advanceUntilIdle()

        clock.advance(Duration.ofSeconds(1))
        ticker.tick()
        advanceUntilIdle()
        assertEquals(0, repository.refreshCalls)

        clock.advance(Duration.ofSeconds(1))
        ticker.tick()
        advanceUntilIdle()
        assertEquals(1, repository.refreshCalls)
        val expired = viewModel.uiState.value as PrayerScheduleUiState.Ready
        assertEquals(Duration.ZERO, expired.nextPrayer?.remaining)

        repeat(3) {
            clock.advance(Duration.ofSeconds(1))
            ticker.tick()
            advanceUntilIdle()
        }
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun expiryAdvancesToRepositoryProvidedCachedNextPrayer() = runTest(dispatcher) {
        val clock = MutableClock(NOW)
        val ticker = ManualTicker()
        val firstTarget = NOW.plusSeconds(1)
        val secondTarget = NOW.plusSeconds(3_601)
        val repository = FakePrayerScheduleRepository(readyState(PrayerName.DHUHR, firstTarget))
        repository.onRefresh = {
            emit(readyState(PrayerName.ASR, secondTarget))
        }
        val viewModel = PrayerScheduleViewModel(repository, clock, ticker)
        advanceUntilIdle()

        clock.advance(Duration.ofSeconds(1))
        ticker.tick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as PrayerScheduleUiState.Ready
        assertEquals(1, repository.refreshCalls)
        assertEquals(PrayerName.ASR, state.nextPrayer?.prayer)
        assertEquals(secondTarget, state.nextPrayer?.time)
        assertEquals(Duration.ofSeconds(3_600), state.nextPrayer?.remaining)
    }

    @Test
    fun calculationUnavailableMapsControlledMessageWithoutInventedSchedule() = runTest(dispatcher) {
        val repository = FakePrayerScheduleRepository(
            PrayerScheduleState.CalculationUnavailable(
                reason = PrayerCalculationResult.Reason.EXTREME_LATITUDE,
                selectedLocation = selectedLocation(),
            ),
        )
        val ticker = ManualTicker()
        val viewModel = PrayerScheduleViewModel(repository, MutableClock(NOW), ticker)
        advanceUntilIdle()

        ticker.tick()
        advanceUntilIdle()

        val state = viewModel.uiState.value as PrayerScheduleUiState.CalculationUnavailable
        assertEquals(PrayerCalculationResult.Reason.EXTREME_LATITUDE, state.reason)
        assertEquals(
            "Orari di preghiera non disponibili per la posizione e le impostazioni selezionate.",
            state.message,
        )
        assertEquals("Roma, Lazio, Italia", state.selectedLocation.displayName)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun readyWithoutNextPrayerNeverRefreshesFromTicker() = runTest(dispatcher) {
        val clock = MutableClock(NOW)
        val ticker = ManualTicker()
        val repository = FakePrayerScheduleRepository(readyState(nextPrayer = null))
        val viewModel = PrayerScheduleViewModel(repository, clock, ticker)
        advanceUntilIdle()

        repeat(5) {
            clock.advance(Duration.ofSeconds(1))
            ticker.tick()
            advanceUntilIdle()
        }

        val state = viewModel.uiState.value as PrayerScheduleUiState.Ready
        assertNull(state.nextPrayer)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun loadingMapsWithoutRepositoryRefresh() = runTest(dispatcher) {
        val repository = FakePrayerScheduleRepository(PrayerScheduleState.Loading)
        val ticker = ManualTicker()
        val viewModel = PrayerScheduleViewModel(repository, MutableClock(NOW), ticker)
        advanceUntilIdle()

        ticker.tick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PrayerScheduleUiState.Loading)
        assertEquals(0, repository.refreshCalls)
    }

    private class FakePrayerScheduleRepository(initial: PrayerScheduleState) : PrayerScheduleRepository {
        private val states = MutableStateFlow(initial)
        var refreshCalls: Int = 0
        var onRefresh: (suspend FakePrayerScheduleRepository.() -> Unit)? = null

        override fun observeSchedule(): Flow<PrayerScheduleState> = states

        override suspend fun refresh() {
            refreshCalls += 1
            onRefresh?.invoke(this)
        }

        fun emit(state: PrayerScheduleState) {
            states.value = state
        }
    }

    private class ManualTicker : PrayerScheduleTicker {
        private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

        override fun ticks(): Flow<Unit> = flow

        fun tick() {
            check(flow.tryEmit(Unit))
        }
    }

    private class MutableClock(
        private var current: Instant,
        private val currentZone: ZoneId = ZoneId.of("UTC"),
    ) : Clock() {
        override fun getZone(): ZoneId = currentZone

        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)

        override fun instant(): Instant = current

        fun advance(duration: Duration) {
            current = current.plus(duration)
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-31T10:00:00Z")
        val DATE: LocalDate = LocalDate.of(2026, 8, 31)
        val ZONE: ZoneId = ZoneId.of("Europe/Rome")
    }
}

private fun readyState(
    prayer: PrayerName,
    target: Instant,
): PrayerScheduleState.Ready = readyState(NextPrayer(prayer, target))

private fun readyState(nextPrayer: NextPrayer?): PrayerScheduleState.Ready {
    val settings = PrayerCalculationSettings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE)
    val day = PrayerDay(
        date = LocalDate.of(2026, 8, 31),
        zoneId = ZoneId.of("Europe/Rome"),
        coordinates = Coordinates(41.9028, 12.4964),
        settings = settings,
        times = PrayerTimes(
            fajr = Instant.parse("2026-08-31T03:00:00Z"),
            sunrise = Instant.parse("2026-08-31T04:30:00Z"),
            dhuhr = Instant.parse("2026-08-31T10:01:00Z"),
            asr = Instant.parse("2026-08-31T14:00:00Z"),
            maghrib = Instant.parse("2026-08-31T18:00:00Z"),
            isha = Instant.parse("2026-08-31T19:30:00Z"),
        ),
    )
    return PrayerScheduleState.Ready(
        PrayerSchedule(
            localDate = day.date,
            selectedLocation = selectedLocation(),
            settings = settings,
            today = day,
            nextPrayer = nextPrayer,
            generatedAt = Instant.parse("2026-08-31T09:59:59Z"),
        ),
    )
}

private fun selectedLocation(): SelectedLocation = SelectedLocation(
    source = LocationSource.Manual(cityId = 3169070L),
    coordinates = Coordinates(41.9028, 12.4964),
    zoneId = ZoneId.of("Europe/Rome"),
    displayName = "Roma, Lazio, Italia",
)
