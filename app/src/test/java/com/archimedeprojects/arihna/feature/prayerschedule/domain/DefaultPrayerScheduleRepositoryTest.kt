package com.archimedeprojects.arihna.feature.prayerschedule.domain

import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.calculation.PrayerTimeCalculator
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsDefaults
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPrayerScheduleRepositoryTest {
    @Test
    fun nonReadyStatesNeverInvokeCalculator() = runTest {
        val states = listOf(
            LocationResolutionState.Unconfigured,
            LocationResolutionState.PermissionDenied(canRequestAgain = true, cachedLocation = null),
            LocationResolutionState.LocationServicesDisabled(cachedLocation = null),
            LocationResolutionState.Unavailable(LocationFailure.NO_PROVIDER, cachedLocation = null),
        )

        states.forEach { locationState ->
            val calculator = RecordingCalculator()
            val repository = repository(
                locationStates = MutableStateFlow(locationState),
                calculator = calculator,
                clock = fixedClock("2026-08-31T01:00:00Z"),
                dispatcher = StandardTestDispatcher(testScheduler),
            )

            val state = repository.observeSchedule().first { it !is PrayerScheduleState.Loading }

            assertTrue(state is PrayerScheduleState.NoLocation)
            assertEquals(locationState, (state as PrayerScheduleState.NoLocation).locationState)
            assertTrue(calculator.calls.isEmpty())
        }
    }

    @Test
    fun resolvingIsLoadingAndNeverInvokesCalculator() = runTest {
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = MutableStateFlow(LocationResolutionState.Resolving),
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val state = repository.observeSchedule().first()

        assertEquals(PrayerScheduleState.Loading, state)
        assertTrue(calculator.calls.isEmpty())
    }

    @Test
    fun readyManualForwardsExactCoordinatesAndZone() = runTest {
        val coordinates = Coordinates(45.4642, 9.1900)
        val zoneId = ZoneId.of("Europe/Rome")
        val selected = manualLocation(10L, "Milano", coordinates, zoneId)
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = MutableStateFlow(ready(selected)),
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val state = repository.observeSchedule().first { it is PrayerScheduleState.Ready }
        val call = calculator.calls.single()

        assertEquals(coordinates, call.coordinates)
        assertEquals(zoneId, call.zoneId)
        assertEquals(LocalDate.of(2026, 8, 31), call.date)
        assertEquals(selected, (state as PrayerScheduleState.Ready).schedule.selectedLocation)
    }

    @Test
    fun readyDeviceForwardsExactAcceptedFixCoordinatesAndCapturedZone() = runTest {
        val coordinates = Coordinates(41.9028, 12.4964)
        val zoneId = ZoneId.of("Europe/Rome")
        val selected = deviceLocation("Roma", coordinates, zoneId)
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = MutableStateFlow(ready(selected)),
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        repository.observeSchedule().first { it is PrayerScheduleState.Ready }
        val call = calculator.calls.single()

        assertEquals(coordinates, call.coordinates)
        assertEquals(zoneId, call.zoneId)
    }

    @Test
    fun acceptedLocationTransitionsProduceDistinctCalculationInputs() = runTest {
        val roma = deviceLocation("Roma", Coordinates(41.9028, 12.4964), ZoneId.of("Europe/Rome"))
        val milano = manualLocation(1L, "Milano", Coordinates(45.4642, 9.1900), ZoneId.of("Europe/Rome"))
        val torino = manualLocation(2L, "Torino", Coordinates(45.0703, 7.6869), ZoneId.of("Europe/Rome"))
        val movedDevice = deviceLocation("Nord Italia", Coordinates(46.1000, 10.5000), ZoneId.of("Europe/Rome"))
        val changedZone = deviceLocation("Confine", Coordinates(46.1000, 10.5000), ZoneId.of("Europe/Paris"))
        val locationStates = MutableStateFlow<LocationResolutionState>(ready(roma))
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = locationStates,
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSchedule().collect()
        }
        runCurrent()
        assertEquals(1, calculator.calls.size)

        locationStates.value = ready(milano)
        runCurrent()
        locationStates.value = ready(torino)
        runCurrent()
        locationStates.value = ready(movedDevice)
        runCurrent()
        locationStates.value = ready(changedZone)
        runCurrent()

        assertEquals(5, calculator.calls.size)
        assertEquals(roma.coordinates, calculator.calls[0].coordinates)
        assertEquals(milano.coordinates, calculator.calls[1].coordinates)
        assertEquals(torino.coordinates, calculator.calls[2].coordinates)
        assertEquals(movedDevice.coordinates, calculator.calls[3].coordinates)
        assertEquals(ZoneId.of("Europe/Paris"), calculator.calls[4].zoneId)
    }

    @Test
    fun identicalMathematicalLocationInputIsDeduplicatedEvenIfFreshnessChanges() = runTest {
        val selected = deviceLocation("Roma", Coordinates(41.9028, 12.4964), ZoneId.of("Europe/Rome"))
        val locationStates = MutableStateFlow<LocationResolutionState>(
            LocationResolutionState.Ready(selected, LocationFreshness.FRESH),
        )
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = locationStates,
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSchedule().collect()
        }
        runCurrent()
        assertEquals(1, calculator.calls.size)

        locationStates.value = LocationResolutionState.Ready(selected, LocationFreshness.CACHED)
        runCurrent()

        assertEquals(1, calculator.calls.size)
    }

    @Test
    fun rawNonAcceptedLocationUpdateCannotRecalculateWithoutNewSelectedLocation() = runTest {
        val selected = deviceLocation("Roma", Coordinates(41.9028, 12.4964), ZoneId.of("Europe/Rome"))
        val locationStates = MutableStateFlow<LocationResolutionState>(ready(selected))
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = locationStates,
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSchedule().collect()
        }
        runCurrent()
        assertEquals(1, calculator.calls.size)

        val ignoredRawFix = Coordinates(41.9030, 12.4966)
        assertTrue(ignoredRawFix.isValid)
        runCurrent()

        assertEquals(1, calculator.calls.size)
        assertEquals(selected.coordinates, calculator.calls.single().coordinates)
    }

    @Test
    fun settingsChangeRecalculatesButIdenticalSettingsDoNot() = runTest {
        val settingsRepository = FakePrayerSettingsRepository(PrayerSettingsDefaults.CANONICAL)
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = MutableStateFlow(
                ready(manualLocation(1L, "Roma", Coordinates(41.9028, 12.4964), ZoneId.of("Europe/Rome"))),
            ),
            settingsRepository = settingsRepository,
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSchedule().collect()
        }
        runCurrent()
        assertEquals(1, calculator.calls.size)

        val changed = PrayerCalculationSettings(method = PrayerCalculationMethod.UMM_AL_QURA)
        settingsRepository.update(changed)
        runCurrent()
        assertEquals(2, calculator.calls.size)
        assertEquals(changed, calculator.calls.last().settings)

        settingsRepository.update(changed)
        runCurrent()
        assertEquals(2, calculator.calls.size)
    }

    @Test
    fun bootstrapReadyCalculatesOnceAndSameInputRefreshUsesCache() = runTest {
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = MutableStateFlow(
                ready(manualLocation(1L, "Roma", Coordinates(41.9028, 12.4964), ZoneId.of("Europe/Rome"))),
            ),
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSchedule().collect()
        }
        runCurrent()
        assertEquals(1, calculator.calls.size)

        repository.refresh()
        runCurrent()

        assertEquals(1, calculator.calls.size)
    }

    @Test
    fun selectedZoneMidnightUsesDstAwareBoundaryAndChangesDateExactlyOnce() = runTest {
        val zoneId = ZoneId.of("Europe/Rome")
        val initial = Instant.parse("2026-03-28T23:30:00Z")
        val boundary = LocalDate.of(2026, 3, 30).atStartOfDay(zoneId).toInstant()
        val clock = MutableClock(initial)
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = MutableStateFlow(
                ready(manualLocation(1L, "Roma", Coordinates(41.9028, 12.4964), zoneId)),
            ),
            calculator = calculator,
            clock = clock,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSchedule().collect()
        }
        runCurrent()
        assertEquals(LocalDate.of(2026, 3, 29), calculator.calls.single().date)

        val untilBoundary = Duration.between(initial, boundary).toMillis()
        assertTrue(untilBoundary < Duration.ofHours(24).toMillis())

        clock.setInstant(boundary.minusMillis(1))
        advanceTimeBy(untilBoundary - 1)
        runCurrent()
        assertEquals(1, calculator.calls.size)

        clock.setInstant(boundary)
        advanceTimeBy(1)
        runCurrent()

        assertEquals(2, calculator.calls.size)
        assertEquals(LocalDate.of(2026, 3, 30), calculator.calls.last().date)
    }

    @Test
    fun zoneChangeCancelsOldMidnightAndSchedulesNewBoundary() = runTest {
        val initial = Instant.parse("2026-01-01T18:00:00Z")
        val clock = MutableClock(initial)
        val locationStates = MutableStateFlow<LocationResolutionState>(
            ready(manualLocation(1L, "Roma", Coordinates(41.9028, 12.4964), ZoneId.of("Europe/Rome"))),
        )
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = locationStates,
            calculator = calculator,
            clock = clock,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeSchedule().collect()
        }
        runCurrent()
        assertEquals(1, calculator.calls.size)

        locationStates.value = ready(
            manualLocation(2L, "New York", Coordinates(40.7128, -74.0060), ZoneId.of("America/New_York")),
        )
        runCurrent()
        assertEquals(2, calculator.calls.size)
        assertEquals(ZoneId.of("America/New_York"), calculator.calls.last().zoneId)

        clock.setInstant(Instant.parse("2026-01-01T23:00:00Z"))
        advanceTimeBy(Duration.ofHours(5).toMillis())
        runCurrent()
        assertEquals(2, calculator.calls.size)

        clock.setInstant(Instant.parse("2026-01-02T05:00:00Z"))
        advanceTimeBy(Duration.ofHours(6).toMillis())
        runCurrent()

        assertEquals(3, calculator.calls.size)
        assertEquals(ZoneId.of("America/New_York"), calculator.calls.last().zoneId)
        assertEquals(LocalDate.of(2026, 1, 2), calculator.calls.last().date)
    }

    @Test
    fun beforeFirstPrayerNextPrayerIsTodaysFajr() = runTest {
        val zoneId = ZoneId.of("Europe/Rome")
        val clock = Clock.fixed(
            LocalDate.of(2026, 8, 31).atTime(4, 0).atZone(zoneId).toInstant(),
            ZoneOffset.UTC,
        )
        val repository = repository(
            locationStates = MutableStateFlow(
                ready(manualLocation(1L, "Roma", Coordinates(41.9028, 12.4964), zoneId)),
            ),
            calculator = RecordingCalculator(),
            clock = clock,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val state = repository.observeSchedule().first { it is PrayerScheduleState.Ready }
        val nextPrayer = (state as PrayerScheduleState.Ready).schedule.nextPrayer

        assertEquals(PrayerName.FAJR, nextPrayer?.prayer)
        assertEquals(
            LocalDate.of(2026, 8, 31).atTime(5, 0).atZone(zoneId).toInstant(),
            nextPrayer?.time,
        )
    }

    @Test
    fun afterLastPrayerUsesTomorrowFajrWhenAvailable() = runTest {
        val zoneId = ZoneId.of("Europe/Rome")
        val clock = Clock.fixed(
            LocalDate.of(2026, 8, 31).atTime(22, 0).atZone(zoneId).toInstant(),
            ZoneOffset.UTC,
        )
        val calculator = RecordingCalculator()
        val repository = repository(
            locationStates = MutableStateFlow(
                ready(manualLocation(1L, "Roma", Coordinates(41.9028, 12.4964), zoneId)),
            ),
            calculator = calculator,
            clock = clock,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val state = repository.observeSchedule().first { it is PrayerScheduleState.Ready }
        val schedule = (state as PrayerScheduleState.Ready).schedule

        assertEquals(2, calculator.calls.size)
        assertEquals(LocalDate.of(2026, 8, 31), calculator.calls[0].date)
        assertEquals(LocalDate.of(2026, 9, 1), calculator.calls[1].date)
        assertEquals(PrayerName.FAJR, schedule.nextPrayer?.prayer)
        assertEquals(
            LocalDate.of(2026, 9, 1).atTime(5, 0).atZone(zoneId).toInstant(),
            schedule.nextPrayer?.time,
        )
    }

    @Test
    fun calculationUnavailableIsControlledAndHasNoFallback() = runTest {
        val selected = manualLocation(1L, "Polar", Coordinates(80.0, 20.0), ZoneId.of("Arctic/Longyearbyen"))
        val calculator = RecordingCalculator { call ->
            PrayerCalculationResult.Unavailable(PrayerCalculationResult.Reason.EXTREME_LATITUDE)
        }
        val repository = repository(
            locationStates = MutableStateFlow(ready(selected)),
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val state = repository.observeSchedule().first { it !is PrayerScheduleState.Loading }

        assertTrue(state is PrayerScheduleState.CalculationUnavailable)
        val unavailable = state as PrayerScheduleState.CalculationUnavailable
        assertEquals(PrayerCalculationResult.Reason.EXTREME_LATITUDE, unavailable.reason)
        assertEquals(selected, unavailable.selectedLocation)
        assertEquals(1, calculator.calls.size)
    }

    @Test
    fun todayValidTomorrowUnavailableKeepsTodayAndLeavesNextPrayerNull() = runTest {
        val zoneId = ZoneId.of("Europe/Rome")
        val today = LocalDate.of(2026, 8, 31)
        val clock = Clock.fixed(today.atTime(22, 0).atZone(zoneId).toInstant(), ZoneOffset.UTC)
        val calculator = RecordingCalculator { call ->
            if (call.date == today.plusDays(1)) {
                PrayerCalculationResult.Unavailable(
                    PrayerCalculationResult.Reason.ASTRONOMICAL_EVENT_UNAVAILABLE,
                )
            } else {
                success(call)
            }
        }
        val repository = repository(
            locationStates = MutableStateFlow(
                ready(manualLocation(1L, "Roma", Coordinates(41.9028, 12.4964), zoneId)),
            ),
            calculator = calculator,
            clock = clock,
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val state = repository.observeSchedule().first { it is PrayerScheduleState.Ready }
        val schedule = (state as PrayerScheduleState.Ready).schedule

        assertEquals(today, schedule.today.date)
        assertNull(schedule.nextPrayer)
        assertEquals(2, calculator.calls.size)
    }

    @Test
    fun staleRomaCalculationCannotOverwriteNewerMilanoSelection() = runTest {
        val zoneId = ZoneId.of("Europe/Rome")
        val roma = manualLocation(1L, "Roma", Coordinates(41.9028, 12.4964), zoneId)
        val milano = manualLocation(2L, "Milano", Coordinates(45.4642, 9.1900), zoneId)
        val locationStates = MutableStateFlow<LocationResolutionState>(ready(roma))
        val calculator = BlockingFirstCalculator(roma.coordinates)
        val repository = repository(
            locationStates = locationStates,
            calculator = calculator,
            clock = fixedClock("2026-08-31T01:00:00Z"),
            dispatcher = Dispatchers.Default,
        )
        val romaReady = CountDownLatch(1)
        val milanoReady = CountDownLatch(1)

        val collector = backgroundScope.launch(Dispatchers.Default) {
            repository.observeSchedule().collect { state ->
                if (state is PrayerScheduleState.Ready) {
                    when (state.schedule.selectedLocation.displayName) {
                        "Roma" -> romaReady.countDown()
                        "Milano" -> milanoReady.countDown()
                    }
                }
            }
        }

        assertTrue(calculator.firstStarted.await(2, TimeUnit.SECONDS))
        locationStates.value = ready(milano)
        Thread.sleep(100)
        calculator.releaseFirst.countDown()

        assertTrue(milanoReady.await(3, TimeUnit.SECONDS))
        assertEquals(1L, romaReady.count)
        assertTrue(calculator.calls.any { it.coordinates == milano.coordinates })

        collector.cancel()
    }

    private fun repository(
        locationStates: Flow<LocationResolutionState>,
        settingsRepository: PrayerSettingsRepository = FakePrayerSettingsRepository(PrayerSettingsDefaults.CANONICAL),
        calculator: PrayerTimeCalculator,
        clock: Clock,
        dispatcher: CoroutineDispatcher,
    ): DefaultPrayerScheduleRepository = DefaultPrayerScheduleRepository(
        locationStates = locationStates,
        prayerSettingsRepository = settingsRepository,
        prayerTimeCalculator = calculator,
        clock = clock,
        calculationDispatcher = dispatcher,
    )

    private fun ready(location: SelectedLocation): LocationResolutionState.Ready =
        LocationResolutionState.Ready(location, freshness = null)

    private fun manualLocation(
        id: Long,
        name: String,
        coordinates: Coordinates,
        zoneId: ZoneId,
    ): SelectedLocation = SelectedLocation(
        source = LocationSource.Manual(id),
        coordinates = coordinates,
        zoneId = zoneId,
        displayName = name,
    )

    private fun deviceLocation(
        name: String,
        coordinates: Coordinates,
        zoneId: ZoneId,
    ): SelectedLocation = SelectedLocation(
        source = LocationSource.Device(
            capturedAt = Instant.parse("2026-08-31T00:00:00Z"),
            accuracyMeters = 2_000f,
        ),
        coordinates = coordinates,
        zoneId = zoneId,
        displayName = name,
    )

    private fun fixedClock(instant: String): Clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)

    private data class Call(
        val date: LocalDate,
        val coordinates: Coordinates,
        val zoneId: ZoneId,
        val settings: PrayerCalculationSettings,
    )

    private class RecordingCalculator(
        private val result: (Call) -> PrayerCalculationResult = ::success,
    ) : PrayerTimeCalculator {
        val calls = CopyOnWriteArrayList<Call>()

        override fun calculate(
            date: LocalDate,
            coordinates: Coordinates,
            zoneId: ZoneId,
            settings: PrayerCalculationSettings,
        ): PrayerCalculationResult {
            val call = Call(date, coordinates, zoneId, settings)
            calls += call
            return result(call)
        }
    }

    private class BlockingFirstCalculator(
        private val blockedCoordinates: Coordinates,
    ) : PrayerTimeCalculator {
        val calls = CopyOnWriteArrayList<Call>()
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)

        override fun calculate(
            date: LocalDate,
            coordinates: Coordinates,
            zoneId: ZoneId,
            settings: PrayerCalculationSettings,
        ): PrayerCalculationResult {
            val call = Call(date, coordinates, zoneId, settings)
            calls += call
            if (coordinates == blockedCoordinates && firstStarted.count > 0L) {
                firstStarted.countDown()
                releaseFirst.await(3, TimeUnit.SECONDS)
            }
            return success(call)
        }
    }

    private class FakePrayerSettingsRepository(initial: PrayerCalculationSettings) : PrayerSettingsRepository {
        private val mutableSettings = MutableStateFlow(initial)

        override val settings: Flow<PrayerCalculationSettings> = mutableSettings

        override suspend fun get(): PrayerCalculationSettings = mutableSettings.value

        override suspend fun update(settings: PrayerCalculationSettings) {
            mutableSettings.value = settings
        }
    }

    private class MutableClock(
        private var current: Instant,
        private val currentZone: ZoneId = ZoneOffset.UTC,
    ) : Clock() {
        override fun getZone(): ZoneId = currentZone

        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)

        override fun instant(): Instant = current

        fun setInstant(instant: Instant) {
            current = instant
        }
    }

    private companion object {
        fun success(call: Call): PrayerCalculationResult.Success = PrayerCalculationResult.Success(
            PrayerDay(
                date = call.date,
                zoneId = call.zoneId,
                coordinates = call.coordinates,
                settings = call.settings,
                times = PrayerTimes(
                    fajr = instant(call.date, call.zoneId, 5, 0),
                    sunrise = instant(call.date, call.zoneId, 6, 30),
                    dhuhr = instant(call.date, call.zoneId, 12, 0),
                    asr = instant(call.date, call.zoneId, 15, 30),
                    maghrib = instant(call.date, call.zoneId, 18, 30),
                    isha = instant(call.date, call.zoneId, 20, 0),
                ),
            ),
        )

        fun instant(date: LocalDate, zoneId: ZoneId, hour: Int, minute: Int): Instant =
            date.atTime(LocalTime.of(hour, minute)).atZone(zoneId).toInstant()
    }
}
