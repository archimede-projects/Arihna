package com.archimedeprojects.arihna.feature.qibla

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaCompassMotionTest {
    @Test fun northWrapClockwiseUsesTwoDegreeShortestPath() = assertEquals(361f, shortestUnwrappedCompassTarget(359f, 1f), 0.0001f)
    @Test fun northWrapCounterClockwiseUsesTwoDegreeShortestPath() = assertEquals(-1f, shortestUnwrappedCompassTarget(1f, 359f), 0.0001f)
    @Test fun cardinalRoseKeepsTrueNorthOppositeDeviceHeading() {
        assertEquals(0f, cardinalRoseWrappedTarget(0f), 0.0001f)
        assertEquals(270f, cardinalRoseWrappedTarget(90f), 0.0001f)
        assertEquals(180f, cardinalRoseWrappedTarget(180f), 0.0001f)
        assertEquals(90f, cardinalRoseWrappedTarget(270f), 0.0001f)
    }
    @Test fun cardinalRoseCrossesNorthByShortestPath() {
        val before = cardinalRoseWrappedTarget(359f)
        val after = cardinalRoseWrappedTarget(1f)
        assertEquals(-1f, shortestUnwrappedCompassTarget(before, after), 0.0001f)
    }
    @Test fun headingCardinalLabelsUseItalianWest() {
        assertEquals("N", headingCardinalLabel(359.0))
        assertEquals("NE", headingCardinalLabel(45.0))
        assertEquals("E", headingCardinalLabel(90.0))
        assertEquals("SO", headingCardinalLabel(225.0))
        assertEquals("O", headingCardinalLabel(270.0))
    }
    @Test fun magneticFieldDiagnosticClassifiesAmbientAndInterference() {
        assertEquals(MagneticFieldStatus.NORMAL, classifyMagneticField(47.0))
        assertEquals(MagneticFieldStatus.INTERFERENCE, classifyMagneticField(120.0))
        assertEquals(MagneticFieldStatus.INTERFERENCE, classifyMagneticField(5.0))
        assertEquals(MagneticFieldStatus.UNAVAILABLE, classifyMagneticField(null))
    }
}
