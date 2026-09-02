package com.archimedeprojects.arihna.core.qibla.calculation

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaAngleMathTest {
    @Test
    fun `normalization keeps angles in zero inclusive three sixty exclusive range`() {
        assertEquals(0.0, normalizeDegrees(0.0), 0.0)
        assertEquals(0.0, normalizeDegrees(360.0), 0.0)
        assertEquals(1.0, normalizeDegrees(721.0), 0.0)
        assertEquals(359.0, normalizeDegrees(-1.0), 0.0)
        assertEquals(350.0, normalizeDegrees(-370.0), 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `normalization rejects non finite values`() {
        normalizeDegrees(Double.NaN)
    }

    @Test
    fun `relative direction wraps across north by shortest normalized direction`() {
        assertEquals(
            10.0,
            relativeQiblaDirectionDegrees(
                qiblaBearingTrueDegrees = 5.0,
                deviceHeadingTrueDegrees = 355.0,
            ),
            0.0,
        )
        assertEquals(
            350.0,
            relativeQiblaDirectionDegrees(
                qiblaBearingTrueDegrees = 355.0,
                deviceHeadingTrueDegrees = 5.0,
            ),
            0.0,
        )
    }
}
