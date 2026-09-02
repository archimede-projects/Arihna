package com.archimedeprojects.arihna.core.qibla.heading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadingMathTest {
    @Test
    fun `source hierarchy is deterministic`() {
        assertEquals(HeadingSource.TRUE_HEADING_SENSOR, selectHeadingSource(HeadingSource.entries.toSet()))
        assertEquals(
            HeadingSource.ROTATION_VECTOR,
            selectHeadingSource(
                setOf(
                    HeadingSource.ROTATION_VECTOR,
                    HeadingSource.GEOMAGNETIC_ROTATION_VECTOR,
                    HeadingSource.ACCELEROMETER_MAGNETIC_FIELD,
                ),
            ),
        )
        assertEquals(
            HeadingSource.GEOMAGNETIC_ROTATION_VECTOR,
            selectHeadingSource(
                setOf(
                    HeadingSource.GEOMAGNETIC_ROTATION_VECTOR,
                    HeadingSource.ACCELEROMETER_MAGNETIC_FIELD,
                ),
            ),
        )
        assertEquals(
            HeadingSource.ACCELEROMETER_MAGNETIC_FIELD,
            selectHeadingSource(setOf(HeadingSource.ACCELEROMETER_MAGNETIC_FIELD)),
        )
        assertNull(selectHeadingSource(emptySet()))
    }

    @Test
    fun `magnetic declination converts east-positive to true heading with wrap`() {
        assertEquals(15.0, trueHeadingFromMagneticDegrees(10.0, 5.0), 1e-9)
        assertEquals(2.0, trueHeadingFromMagneticDegrees(358.0, 4.0), 1e-9)
        assertEquals(355.0, trueHeadingFromMagneticDegrees(2.0, -7.0), 1e-9)
    }

    @Test
    fun `direct heading follows active display top across quarter turns`() {
        assertEquals(90.0, headingForDisplayQuarterTurns(90.0, 0), 1e-9)
        assertEquals(0.0, headingForDisplayQuarterTurns(90.0, 1), 1e-9)
        assertEquals(270.0, headingForDisplayQuarterTurns(90.0, 2), 1e-9)
        assertEquals(180.0, headingForDisplayQuarterTurns(90.0, 3), 1e-9)
    }

    @Test
    fun `platform quality mapping mirrors Android status values`() {
        assertEquals(HeadingQuality.HIGH, headingQualityFromPlatformAccuracy(3))
        assertEquals(HeadingQuality.MEDIUM, headingQualityFromPlatformAccuracy(2))
        assertEquals(HeadingQuality.LOW, headingQualityFromPlatformAccuracy(1))
        assertEquals(HeadingQuality.UNRELIABLE, headingQualityFromPlatformAccuracy(0))
        assertEquals(HeadingQuality.UNKNOWN, headingQualityFromPlatformAccuracy(-1))
        assertEquals(HeadingQuality.UNKNOWN, headingQualityFromPlatformAccuracy(99))
    }

    @Test
    fun `paired fallback quality keeps the least trustworthy known status`() {
        assertEquals(
            HeadingQuality.UNRELIABLE,
            combineHeadingQuality(HeadingQuality.HIGH, HeadingQuality.UNRELIABLE),
        )
        assertEquals(
            HeadingQuality.LOW,
            combineHeadingQuality(HeadingQuality.MEDIUM, HeadingQuality.LOW),
        )
        assertEquals(
            HeadingQuality.UNKNOWN,
            combineHeadingQuality(HeadingQuality.HIGH, HeadingQuality.UNKNOWN),
        )
        assertEquals(
            HeadingQuality.MEDIUM,
            combineHeadingQuality(HeadingQuality.HIGH, HeadingQuality.MEDIUM),
        )
        assertEquals(
            HeadingQuality.HIGH,
            combineHeadingQuality(HeadingQuality.HIGH, HeadingQuality.HIGH),
        )
    }
}
