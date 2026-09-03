from pathlib import Path

files = {
'app/src/main/java/com/archimedeprojects/arihna/feature/qibla/domain/QiblaModels.kt': r'''package com.archimedeprojects.arihna.feature.qibla.domain

import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.qibla.heading.HeadingQuality
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSource
import com.archimedeprojects.arihna.core.qibla.heading.HeadingUnavailableReason

enum class QiblaBearingUnavailableReason {
    INVALID_COORDINATES,
    AT_KAABA_OR_COINCIDENT,
}

sealed interface QiblaState {
    data class NoLocation(
        val locationState: LocationResolutionState,
    ) : QiblaState

    data class BearingUnavailable(
        val location: SelectedLocation,
        val reason: QiblaBearingUnavailableReason,
    ) : QiblaState

    data class StaticBearing(
        val location: SelectedLocation,
        val bearingTrueDegrees: Double,
    ) : QiblaState

    data class LiveCompassStarting(
        val location: SelectedLocation,
        val bearingTrueDegrees: Double,
    ) : QiblaState

    data class LiveCompass(
        val location: SelectedLocation,
        val bearingTrueDegrees: Double,
        val deviceHeadingTrueDegrees: Double,
        val relativeQiblaDirectionDegrees: Double,
        val quality: HeadingQuality,
        val estimatedAccuracyDegrees: Double?,
        val headingSource: HeadingSource,
    ) : QiblaState

    data class SensorUnavailable(
        val location: SelectedLocation,
        val bearingTrueDegrees: Double,
        val reason: HeadingUnavailableReason,
    ) : QiblaState
}
''',
'app/src/main/java/com/archimedeprojects/arihna/feature/qibla/domain/QiblaRepository.kt': r'''package com.archimedeprojects.arihna.feature.qibla.domain

import kotlinx.coroutines.flow.Flow

fun interface QiblaRepository {
    fun observeQibla(): Flow<QiblaState>
}
''',
'app/src/main/java/com/archimedeprojects/arihna/feature/qibla/domain/DefaultQiblaRepository.kt': r'''package com.archimedeprojects.arihna.feature.qibla.domain

import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.qibla.calculation.QiblaBearingCalculator
import com.archimedeprojects.arihna.core.qibla.calculation.relativeQiblaDirectionDegrees
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingDataSource
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingState
import com.archimedeprojects.arihna.core.qibla.model.QiblaBearingResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultQiblaRepository(
    private val locationStates: Flow<LocationResolutionState>,
    private val bearingCalculator: QiblaBearingCalculator,
    private val headingDataSource: DeviceHeadingDataSource,
) : QiblaRepository {
    override fun observeQibla(): Flow<QiblaState> =
        locationStates
            .distinctUntilChanged()
            .flatMapLatest(::stateForLocation)
            .distinctUntilChanged()

    private fun stateForLocation(locationState: LocationResolutionState): Flow<QiblaState> {
        val ready = locationState as? LocationResolutionState.Ready
            ?: return flowOf(QiblaState.NoLocation(locationState))
        val location = ready.location
        if (!location.isValid) {
            return flowOf(QiblaState.NoLocation(locationState))
        }

        return when (val bearing = bearingCalculator.calculate(location.coordinates)) {
            QiblaBearingResult.InvalidCoordinates -> flowOf(
                QiblaState.BearingUnavailable(
                    location = location,
                    reason = QiblaBearingUnavailableReason.INVALID_COORDINATES,
                ),
            )

            QiblaBearingResult.AtKaabaOrCoincident -> flowOf(
                QiblaState.BearingUnavailable(
                    location = location,
                    reason = QiblaBearingUnavailableReason.AT_KAABA_OR_COINCIDENT,
                ),
            )

            is QiblaBearingResult.Success -> stateForBearing(location, bearing.bearingTrueDegrees)
        }
    }

    private fun stateForBearing(
        location: SelectedLocation,
        bearingTrueDegrees: Double,
    ): Flow<QiblaState> = when (location.source) {
        is LocationSource.Manual -> flowOf(
            QiblaState.StaticBearing(
                location = location,
                bearingTrueDegrees = bearingTrueDegrees,
            ),
        )

        is LocationSource.Device -> headingDataSource
            .observeHeading(location.coordinates)
            .map { headingState ->
                when (headingState) {
                    is DeviceHeadingState.Unavailable -> QiblaState.SensorUnavailable(
                        location = location,
                        bearingTrueDegrees = bearingTrueDegrees,
                        reason = headingState.reason,
                    )

                    is DeviceHeadingState.Reading -> QiblaState.LiveCompass(
                        location = location,
                        bearingTrueDegrees = bearingTrueDegrees,
                        deviceHeadingTrueDegrees = headingState.trueHeadingDegrees,
                        relativeQiblaDirectionDegrees = relativeQiblaDirectionDegrees(
                            qiblaBearingTrueDegrees = bearingTrueDegrees,
                            deviceHeadingTrueDegrees = headingState.trueHeadingDegrees,
                        ),
                        quality = headingState.quality,
                        estimatedAccuracyDegrees = headingState.estimatedAccuracyDegrees,
                        headingSource = headingState.source,
                    )
                }
            }
            .onStart {
                emit(
                    QiblaState.LiveCompassStarting(
                        location = location,
                        bearingTrueDegrees = bearingTrueDegrees,
                    ),
                )
            }
    }
}
''',
'app/src/test/java/com/archimedeprojects/arihna/feature/qibla/domain/DefaultQiblaRepositoryTest.kt': r'''package com.archimedeprojects.arihna.feature.qibla.domain

import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.qibla.calculation.QiblaBearingCalculator
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingDataSource
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingState
import com.archimedeprojects.arihna.core.qibla.heading.HeadingQuality
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSource
import com.archimedeprojects.arihna.core.qibla.heading.HeadingUnavailableReason
import com.archimedeprojects.arihna.core.qibla.model.QiblaBearingResult
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultQiblaRepositoryTest {
    private val rome = Coordinates(41.9028, 12.4964)
    private val milan = Coordinates(45.4642, 9.1900)

    @Test
    fun `every non-ready location state produces no-location without calculator or heading`() = runTest {
        val calculator = RecordingCalculator(QiblaBearingResult.Success(123.0))
        val heading = RecordingHeadingDataSource()
        val states = listOf<LocationResolutionState>(
            LocationResolutionState.Unconfigured,
            LocationResolutionState.Resolving,
            LocationResolutionState.PermissionDenied(canRequestAgain = true, cachedLocation = deviceLocation()),
            LocationResolutionState.LocationServicesDisabled(cachedLocation = deviceLocation()),
            LocationResolutionState.Unavailable(LocationFailure.NO_LOCATION_FIX, cachedLocation = deviceLocation()),
        )

        states.forEach { locationState ->
            val repository = DefaultQiblaRepository(flowOf(locationState), calculator, heading)
            assertEquals(QiblaState.NoLocation(locationState), repository.observeQibla().first())
        }
        assertTrue(calculator.origins.isEmpty())
        assertTrue(heading.origins.isEmpty())
    }

    @Test
    fun `invalid ready location stays controlled and does not start heading`() = runTest {
        val invalid = deviceLocation(coordinates = Coordinates(Double.NaN, 12.0))
        val calculator = RecordingCalculator(QiblaBearingResult.Success(1.0))
        val heading = RecordingHeadingDataSource()
        val locationState = LocationResolutionState.Ready(invalid)
        val repository = DefaultQiblaRepository(flowOf(locationState), calculator, heading)

        assertEquals(QiblaState.NoLocation(locationState), repository.observeQibla().first())
        assertTrue(calculator.origins.isEmpty())
        assertTrue(heading.origins.isEmpty())
    }

    @Test
    fun `manual location forwards exact coordinates and never subscribes to heading`() = runTest {
        val manual = manualLocation(milan)
        val calculator = RecordingCalculator(QiblaBearingResult.Success(124.5))
        val heading = RecordingHeadingDataSource()
        val repository = DefaultQiblaRepository(
            flowOf(LocationResolutionState.Ready(manual)),
            calculator,
            heading,
        )

        assertEquals(
            QiblaState.StaticBearing(manual, 124.5),
            repository.observeQibla().first(),
        )
        assertEquals(listOf(milan), calculator.origins)
        assertTrue(heading.origins.isEmpty())
    }

    @Test
    fun `cached device location is immediately usable without any location acquisition dependency`() = runTest {
        val cached = deviceLocation(freshness = LocationFreshness.CACHED)
        val calculator = RecordingCalculator(QiblaBearingResult.Success(123.276))
        val heading = RecordingHeadingDataSource(
            events = { flowOf(reading(heading = 100.0)) },
        )
        val repository = DefaultQiblaRepository(
            flowOf(LocationResolutionState.Ready(cached)),
            calculator,
            heading,
        )

        val states = repository.observeQibla().take(2).toList()
        assertEquals(QiblaState.LiveCompassStarting(cached, 123.276), states[0])
        assertTrue(states[1] is QiblaState.LiveCompass)
        assertEquals(listOf(rome), calculator.origins)
        assertEquals(listOf(rome), heading.origins)
    }

    @Test
    fun `device reading preserves heading metadata and relative direction wraps north`() = runTest {
        val device = deviceLocation()
        val calculator = RecordingCalculator(QiblaBearingResult.Success(5.0))
        val heading = RecordingHeadingDataSource(
            events = {
                flowOf(
                    DeviceHeadingState.Reading(
                        trueHeadingDegrees = 355.0,
                        quality = HeadingQuality.MEDIUM,
                        estimatedAccuracyDegrees = 7.5,
                        source = HeadingSource.ROTATION_VECTOR,
                        magneticHeadingDegrees = 350.0,
                        declinationDegrees = 5.0,
                    ),
                )
            },
        )
        val repository = DefaultQiblaRepository(flowOf(LocationResolutionState.Ready(device)), calculator, heading)

        val live = repository.observeQibla().take(2).toList()[1] as QiblaState.LiveCompass
        assertEquals(5.0, live.bearingTrueDegrees, 0.0)
        assertEquals(355.0, live.deviceHeadingTrueDegrees, 0.0)
        assertEquals(10.0, live.relativeQiblaDirectionDegrees, 0.0)
        assertEquals(HeadingQuality.MEDIUM, live.quality)
        assertEquals(7.5, live.estimatedAccuracyDegrees!!, 0.0)
        assertEquals(HeadingSource.ROTATION_VECTOR, live.headingSource)
    }

    @Test
    fun `device sensor unavailable retains valid static bearing`() = runTest {
        val device = deviceLocation()
        val repository = DefaultQiblaRepository(
            flowOf(LocationResolutionState.Ready(device)),
            RecordingCalculator(QiblaBearingResult.Success(123.0)),
            RecordingHeadingDataSource(
                events = { flowOf(DeviceHeadingState.Unavailable(HeadingUnavailableReason.NO_SUPPORTED_SENSOR)) },
            ),
        )

        val unavailable = repository.observeQibla().take(2).toList()[1]
        assertEquals(
            QiblaState.SensorUnavailable(
                location = device,
                bearingTrueDegrees = 123.0,
                reason = HeadingUnavailableReason.NO_SUPPORTED_SENSOR,
            ),
            unavailable,
        )
    }

    @Test
    fun `invalid and coincident bearing results remain explicit and do not start heading`() = runTest {
        val device = deviceLocation()
        listOf(
            QiblaBearingResult.InvalidCoordinates to QiblaBearingUnavailableReason.INVALID_COORDINATES,
            QiblaBearingResult.AtKaabaOrCoincident to QiblaBearingUnavailableReason.AT_KAABA_OR_COINCIDENT,
        ).forEach { (result, expectedReason) ->
            val heading = RecordingHeadingDataSource()
            val repository = DefaultQiblaRepository(
                flowOf(LocationResolutionState.Ready(device)),
                RecordingCalculator(result),
                heading,
            )
            assertEquals(
                QiblaState.BearingUnavailable(device, expectedReason),
                repository.observeQibla().first(),
            )
            assertTrue(heading.origins.isEmpty())
        }
    }

    @Test
    fun `device to manual change cancels old heading and stale readings cannot leak`() = runTest {
        val locations = MutableStateFlow<LocationResolutionState>(LocationResolutionState.Ready(deviceLocation()))
        val oldHeadingEvents = MutableSharedFlow<DeviceHeadingState>(extraBufferCapacity = 4)
        var oldCancelled = false
        val heading = RecordingHeadingDataSource { origin ->
            if (origin == rome) {
                flow {
                    try {
                        oldHeadingEvents.collect { emit(it) }
                    } finally {
                        oldCancelled = true
                    }
                }
            } else {
                flowOf(reading(30.0))
            }
        }
        val repository = DefaultQiblaRepository(
            locations,
            RecordingCalculator(QiblaBearingResult.Success(123.0)),
            heading,
        )
        val collected = mutableListOf<QiblaState>()
        val job = launch { repository.observeQibla().collect(collected::add) }
        runCurrent()
        assertTrue(collected.last() is QiblaState.LiveCompassStarting)

        val manual = manualLocation(milan)
        locations.value = LocationResolutionState.Ready(manual)
        runCurrent()
        assertTrue(oldCancelled)
        assertEquals(QiblaState.StaticBearing(manual, 123.0), collected.last())

        oldHeadingEvents.tryEmit(reading(250.0))
        runCurrent()
        assertEquals(QiblaState.StaticBearing(manual, 123.0), collected.last())
        job.cancelAndJoin()
    }

    @Test
    fun `device location replacement cancels prior heading and latest coordinates own output`() = runTest {
        val firstLocation = deviceLocation(rome)
        val secondLocation = deviceLocation(milan)
        val locations = MutableStateFlow<LocationResolutionState>(LocationResolutionState.Ready(firstLocation))
        val channels = mutableMapOf<Coordinates, MutableSharedFlow<DeviceHeadingState>>()
        val cancelled = mutableSetOf<Coordinates>()
        val heading = RecordingHeadingDataSource { origin ->
            val channel = channels.getOrPut(origin) { MutableSharedFlow(extraBufferCapacity = 2) }
            flow {
                try {
                    channel.collect { emit(it) }
                } finally {
                    cancelled += origin
                }
            }
        }
        val repository = DefaultQiblaRepository(
            locations,
            RecordingCalculator(QiblaBearingResult.Success(120.0)),
            heading,
        )
        val collected = mutableListOf<QiblaState>()
        val job = launch { repository.observeQibla().collect(collected::add) }
        runCurrent()

        locations.value = LocationResolutionState.Ready(secondLocation)
        runCurrent()
        assertTrue(rome in cancelled)
        channels.getValue(milan).tryEmit(reading(100.0))
        runCurrent()
        val latest = collected.last() as QiblaState.LiveCompass
        assertEquals(milan, latest.location.coordinates)
        assertEquals(20.0, latest.relativeQiblaDirectionDegrees, 0.0)

        channels.getValue(rome).tryEmit(reading(0.0))
        runCurrent()
        assertEquals(milan, (collected.last() as QiblaState.LiveCompass).location.coordinates)
        job.cancelAndJoin()
    }

    private fun deviceLocation(
        coordinates: Coordinates = rome,
        freshness: LocationFreshness = LocationFreshness.FRESH,
    ) = SelectedLocation(
        source = LocationSource.Device(Instant.parse("2026-09-03T00:00:00Z"), 100f),
        coordinates = coordinates,
        zoneId = ZoneId.of("Europe/Rome"),
        displayName = "Roma",
        freshness = freshness,
    )

    private fun manualLocation(coordinates: Coordinates) = SelectedLocation(
        source = LocationSource.Manual(cityId = 3173435L),
        coordinates = coordinates,
        zoneId = ZoneId.of("Europe/Rome"),
        displayName = "Milano",
    )

    private fun reading(heading: Double) = DeviceHeadingState.Reading(
        trueHeadingDegrees = heading,
        quality = HeadingQuality.HIGH,
        estimatedAccuracyDegrees = 3.0,
        source = HeadingSource.TRUE_HEADING_SENSOR,
        magneticHeadingDegrees = null,
        declinationDegrees = null,
    )

    private class RecordingCalculator(
        private val result: QiblaBearingResult,
    ) : QiblaBearingCalculator {
        val origins = mutableListOf<Coordinates>()
        override fun calculate(origin: Coordinates): QiblaBearingResult {
            origins += origin
            return result
        }
    }

    private class RecordingHeadingDataSource(
        private val events: (Coordinates) -> Flow<DeviceHeadingState> = { flowOf() },
    ) : DeviceHeadingDataSource {
        val origins = mutableListOf<Coordinates>()
        override fun observeHeading(origin: Coordinates): Flow<DeviceHeadingState> {
            origins += origin
            return events(origin)
        }
    }
}
''',
}

for name, content in files.items():
    path = Path(name)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
