package com.archimedeprojects.arihna.core.qibla.heading

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultDeviceHeadingDataSourceTest {
    private val rome = Coordinates(41.9028, 12.4964)

    @Test
    fun `true heading source bypasses declination and keeps direct accuracy`() = runTest {
        val backend = FakeBackend(
            sources = setOf(HeadingSource.TRUE_HEADING_SENSOR),
            events = listOf(
                HeadingSensorEvent.Reading(
                    headingDegrees = 361.0,
                    quality = HeadingQuality.HIGH,
                    estimatedAccuracyDegrees = 4.5,
                ),
            ),
        )
        var declinationCalls = 0
        val dataSource = DefaultDeviceHeadingDataSource(
            backend = backend,
            magneticDeclinationProvider = MagneticDeclinationProvider {
                declinationCalls++
                99.0
            },
        )

        val state = dataSource.observeHeading(rome).take(1).toList().single()
        val reading = state as DeviceHeadingState.Reading
        assertEquals(1.0, reading.trueHeadingDegrees, 1e-9)
        assertEquals(HeadingSource.TRUE_HEADING_SENSOR, reading.source)
        assertEquals(4.5, reading.estimatedAccuracyDegrees!!, 1e-9)
        assertNull(reading.magneticHeadingDegrees)
        assertNull(reading.declinationDegrees)
        assertEquals(0, declinationCalls)
    }

    @Test
    fun `magnetic fallback applies declination exactly once per observation`() = runTest {
        val backend = FakeBackend(
            sources = setOf(HeadingSource.ROTATION_VECTOR),
            events = listOf(
                HeadingSensorEvent.Reading(358.0, HeadingQuality.MEDIUM, 8.0),
                HeadingSensorEvent.Reading(10.0, HeadingQuality.MEDIUM, 7.0),
            ),
        )
        var declinationCalls = 0
        val dataSource = DefaultDeviceHeadingDataSource(
            backend = backend,
            magneticDeclinationProvider = MagneticDeclinationProvider {
                declinationCalls++
                4.0
            },
        )

        val states = dataSource.observeHeading(rome).take(2).toList()
        val first = states[0] as DeviceHeadingState.Reading
        val second = states[1] as DeviceHeadingState.Reading
        assertEquals(2.0, first.trueHeadingDegrees, 1e-9)
        assertEquals(358.0, first.magneticHeadingDegrees!!, 1e-9)
        assertEquals(4.0, first.declinationDegrees!!, 1e-9)
        assertEquals(14.0, second.trueHeadingDegrees, 1e-9)
        assertEquals(1, declinationCalls)
    }

    @Test
    fun `no supported sensor is controlled unavailable`() = runTest {
        val dataSource = DefaultDeviceHeadingDataSource(
            backend = FakeBackend(emptySet(), emptyList()),
            magneticDeclinationProvider = MagneticDeclinationProvider { 0.0 },
        )

        assertEquals(
            DeviceHeadingState.Unavailable(HeadingUnavailableReason.NO_SUPPORTED_SENSOR),
            dataSource.observeHeading(rome).take(1).toList().single(),
        )
    }

    @Test
    fun `invalid coordinates never touch backend or declination`() = runTest {
        val backend = FakeBackend(setOf(HeadingSource.ROTATION_VECTOR), emptyList())
        var declinationCalls = 0
        val dataSource = DefaultDeviceHeadingDataSource(
            backend = backend,
            magneticDeclinationProvider = MagneticDeclinationProvider {
                declinationCalls++
                0.0
            },
        )

        val state = dataSource.observeHeading(Coordinates(Double.NaN, 0.0))
            .take(1)
            .toList()
            .single()
        assertEquals(
            DeviceHeadingState.Unavailable(HeadingUnavailableReason.INVALID_COORDINATES),
            state,
        )
        assertEquals(0, backend.availableCalls)
        assertEquals(0, declinationCalls)
    }

    @Test
    fun `registration failure remains controlled`() = runTest {
        val backend = FakeBackend(
            sources = setOf(HeadingSource.GEOMAGNETIC_ROTATION_VECTOR),
            events = listOf(HeadingSensorEvent.RegistrationFailed),
        )
        val dataSource = DefaultDeviceHeadingDataSource(
            backend = backend,
            magneticDeclinationProvider = MagneticDeclinationProvider { 2.0 },
        )

        assertEquals(
            DeviceHeadingState.Unavailable(HeadingUnavailableReason.REGISTRATION_FAILED),
            dataSource.observeHeading(rome).take(1).toList().single(),
        )
    }

    @Test
    fun `collection cancellation propagates to sensor backend lifecycle`() = runTest {
        val backend = LifecycleBackend()
        val dataSource = DefaultDeviceHeadingDataSource(
            backend = backend,
            magneticDeclinationProvider = MagneticDeclinationProvider { 0.0 },
        )

        val job = launch {
            dataSource.observeHeading(rome).collect { }
        }
        runCurrent()
        assertEquals(1, backend.activeCollectors)

        job.cancelAndJoin()
        runCurrent()
        assertEquals(0, backend.activeCollectors)
        assertEquals(1, backend.starts)
        assertEquals(1, backend.stops)
    }

    @Test
    fun `invalid declination stops before backend registration`() = runTest {
        val backend = FakeBackend(setOf(HeadingSource.ROTATION_VECTOR), emptyList())
        val dataSource = DefaultDeviceHeadingDataSource(
            backend = backend,
            magneticDeclinationProvider = MagneticDeclinationProvider { Double.NaN },
        )

        assertEquals(
            DeviceHeadingState.Unavailable(HeadingUnavailableReason.INVALID_DECLINATION),
            dataSource.observeHeading(rome).take(1).toList().single(),
        )
        assertEquals(0, backend.observeCalls)
    }

    private class FakeBackend(
        private val sources: Set<HeadingSource>,
        private val events: List<HeadingSensorEvent>,
    ) : HeadingSensorBackend {
        var availableCalls = 0
        var observeCalls = 0

        override fun availableSources(): Set<HeadingSource> {
            availableCalls++
            return sources
        }

        override fun observe(source: HeadingSource): Flow<HeadingSensorEvent> = flow {
            observeCalls++
            events.forEach { emit(it) }
        }
    }

    private class LifecycleBackend : HeadingSensorBackend {
        var activeCollectors = 0
        var starts = 0
        var stops = 0

        override fun availableSources(): Set<HeadingSource> =
            setOf(HeadingSource.TRUE_HEADING_SENSOR)

        override fun observe(source: HeadingSource): Flow<HeadingSensorEvent> = flow {
            starts++
            activeCollectors++
            try {
                emit(
                    HeadingSensorEvent.Reading(
                        headingDegrees = 10.0,
                        quality = HeadingQuality.UNKNOWN,
                        estimatedAccuracyDegrees = null,
                    ),
                )
                awaitCancellation()
            } finally {
                activeCollectors--
                stops++
            }
        }
    }
}
