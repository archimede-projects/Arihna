package com.archimedeprojects.arihna.feature.prayerschedule.domain

import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.prayer.calculation.PrayerTimeCalculator
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPrayerScheduleRepository(
    private val locationStates: Flow<LocationResolutionState>,
    private val prayerSettingsRepository: PrayerSettingsRepository,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val clock: Clock,
    private val calculationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PrayerScheduleRepository {
    private val refreshVersion = MutableStateFlow(0L)
    private val cacheMutex = Mutex()
    private val calculationCache = mutableMapOf<PrayerScheduleInput, PrayerCalculationResult>()

    override fun observeSchedule(): Flow<PrayerScheduleState> = flow {
        val latestTriggerVersion = AtomicLong(0L)

        combine(
            locationStates,
            prayerSettingsRepository.settings,
            refreshVersion,
        ) { locationState, settings, refresh ->
            ObservationContext(
                locationState = locationState,
                settings = settings,
                refresh = refresh,
            )
        }
            .distinctUntilChanged()
            .map { context ->
                VersionedObservationContext(
                    context = context,
                    version = latestTriggerVersion.incrementAndGet(),
                )
            }
            .flatMapLatest { context -> triggerFlow(context, latestTriggerVersion) }
            .mapLatest { trigger ->
                val state = deriveState(trigger.locationState, trigger.settings)
                state.takeIf { trigger.version == latestTriggerVersion.get() }
            }
            .filterNotNull()
            .onStart { emit(PrayerScheduleState.Loading) }
            .distinctUntilChanged()
            .collect { emit(it) }
    }

    override suspend fun refresh() {
        refreshVersion.update { it + 1L }
    }

    private fun triggerFlow(
        versionedContext: VersionedObservationContext,
        latestTriggerVersion: AtomicLong,
    ): Flow<ScheduleTrigger> = flow {
        val context = versionedContext.context
        val ready = context.locationState as? LocationResolutionState.Ready
        if (ready == null || !ready.location.isValid) {
            emit(
                ScheduleTrigger(
                    locationState = context.locationState,
                    settings = context.settings,
                    version = versionedContext.version,
                ),
            )
            return@flow
        }

        var version = versionedContext.version
        while (currentCoroutineContext().isActive) {
            emit(
                ScheduleTrigger(
                    locationState = context.locationState,
                    settings = context.settings,
                    version = version,
                ),
            )

            val now = clock.instant()
            val zoneId = ready.location.zoneId
            val currentDate = LocalDate.ofInstant(now, zoneId)
            val nextMidnight = currentDate
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
            val delayMillis = Duration.between(now, nextMidnight)
                .toMillis()
                .coerceAtLeast(1L)

            delay(delayMillis)
            version = latestTriggerVersion.incrementAndGet()
        }
    }

    private suspend fun deriveState(
        locationState: LocationResolutionState,
        settings: PrayerCalculationSettings,
    ): PrayerScheduleState {
        if (locationState is LocationResolutionState.Resolving) {
            return PrayerScheduleState.Loading
        }

        val ready = locationState as? LocationResolutionState.Ready
            ?: return PrayerScheduleState.NoLocation(locationState)
        val selectedLocation = ready.location
        if (!selectedLocation.isValid) {
            return PrayerScheduleState.NoLocation(locationState)
        }

        val now = clock.instant()
        val input = PrayerScheduleInput(
            coordinates = selectedLocation.coordinates,
            zoneId = selectedLocation.zoneId,
            settings = settings,
            localDate = LocalDate.ofInstant(now, selectedLocation.zoneId),
        )

        return when (val todayResult = calculateCached(input)) {
            is PrayerCalculationResult.Unavailable -> PrayerScheduleState.CalculationUnavailable(
                reason = todayResult.reason,
                selectedLocation = selectedLocation,
            )

            is PrayerCalculationResult.Success -> {
                val nextPrayer = nextPrayer(
                    input = input,
                    todayTimes = todayResult.prayerDay.times,
                    now = now,
                )

                PrayerScheduleState.Ready(
                    PrayerSchedule(
                        localDate = input.localDate,
                        selectedLocation = selectedLocation,
                        settings = settings,
                        today = todayResult.prayerDay,
                        nextPrayer = nextPrayer,
                        generatedAt = now,
                        locationFreshness = ready.freshness,
                    ),
                )
            }
        }
    }

    private suspend fun nextPrayer(
        input: PrayerScheduleInput,
        todayTimes: PrayerTimes,
        now: Instant,
    ): NextPrayer? {
        nextPrayerIn(todayTimes, now)?.let { return it }

        val tomorrowInput = input.copy(localDate = input.localDate.plusDays(1))
        return when (val tomorrowResult = calculateCached(tomorrowInput)) {
            is PrayerCalculationResult.Unavailable -> null
            is PrayerCalculationResult.Success -> nextPrayerIn(
                times = tomorrowResult.prayerDay.times,
                now = now,
            )
        }
    }

    private suspend fun calculateCached(input: PrayerScheduleInput): PrayerCalculationResult {
        cacheMutex.withLock {
            calculationCache[input]?.let { return it }
        }

        val calculated = withContext(calculationDispatcher) {
            prayerTimeCalculator.calculate(
                date = input.localDate,
                coordinates = input.coordinates,
                zoneId = input.zoneId,
                settings = input.settings,
            )
        }

        return cacheMutex.withLock {
            calculationCache.getOrPut(input) { calculated }
        }
    }

    private fun nextPrayerIn(times: PrayerTimes, now: Instant): NextPrayer? =
        listOf(
            NextPrayer(PrayerName.FAJR, times.fajr),
            NextPrayer(PrayerName.DHUHR, times.dhuhr),
            NextPrayer(PrayerName.ASR, times.asr),
            NextPrayer(PrayerName.MAGHRIB, times.maghrib),
            NextPrayer(PrayerName.ISHA, times.isha),
        ).firstOrNull { it.time.isAfter(now) }

    private data class ObservationContext(
        val locationState: LocationResolutionState,
        val settings: PrayerCalculationSettings,
        val refresh: Long,
    )

    private data class VersionedObservationContext(
        val context: ObservationContext,
        val version: Long,
    )

    private data class ScheduleTrigger(
        val locationState: LocationResolutionState,
        val settings: PrayerCalculationSettings,
        val version: Long,
    )
}
