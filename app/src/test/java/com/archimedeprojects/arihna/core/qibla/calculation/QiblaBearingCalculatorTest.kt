package com.archimedeprojects.arihna.core.qibla.calculation

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.qibla.model.QiblaBearingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class QiblaBearingCalculatorTest {
    @Test
    fun `kaaba target is frozen to approved coordinates`() {
        assertEquals(21.42251267, QiblaTarget.KAABA.latitude, 0.0)
        assertEquals(39.82619741, QiblaTarget.KAABA.longitude, 0.0)
    }

    @Test
    fun `golden bearings match approved fixtures`() {
        assertBearing(41.9028, 12.4964, 123.2758758)
        assertBearing(40.7128, -74.0060, 58.4816940)
        assertBearing(-33.8688, 151.2093, 277.4996006)
        assertBearing(51.5074, -0.1278, 118.9872115)
        assertBearing(-6.2088, 106.8456, 295.1517485)
    }

    @Test
    fun `invalid coordinates return controlled result`() {
        val invalid = listOf(
            Coordinates(90.0001, 0.0),
            Coordinates(-90.0001, 0.0),
            Coordinates(0.0, 180.0001),
            Coordinates(0.0, -180.0001),
            Coordinates(Double.NaN, 0.0),
            Coordinates(0.0, Double.POSITIVE_INFINITY),
        )

        invalid.forEach { coordinates ->
            assertSame(
                QiblaBearingResult.InvalidCoordinates,
                GreatCircleQiblaBearingCalculator.calculate(coordinates),
            )
        }
    }

    @Test
    fun `kaaba coordinates return controlled coincident result`() {
        assertSame(
            QiblaBearingResult.AtKaabaOrCoincident,
            GreatCircleQiblaBearingCalculator.calculate(QiblaTarget.KAABA),
        )
    }

    private fun assertBearing(latitude: Double, longitude: Double, expected: Double) {
        val result = GreatCircleQiblaBearingCalculator.calculate(Coordinates(latitude, longitude))
        val success = result as QiblaBearingResult.Success
        assertEquals(expected, success.bearingTrueDegrees, 0.0001)
    }
}
