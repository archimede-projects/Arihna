package com.archimedeprojects.arihna.feature.qibla

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaCompassMotionTest {
    @Test
    fun northWrapClockwiseUsesTwoDegreeShortestPath() {
        assertEquals(361f, shortestUnwrappedCompassTarget(359f, 1f), 0.0001f)
    }

    @Test
    fun northWrapCounterClockwiseUsesTwoDegreeShortestPath() {
        assertEquals(-1f, shortestUnwrappedCompassTarget(1f, 359f), 0.0001f)
    }

    @Test
    fun longWrappedTargetUsesShortestDirection() {
        assertEquals(-160f, shortestUnwrappedCompassTarget(10f, 200f), 0.0001f)
    }

    @Test
    fun accumulatedTurnsPreserveContinuity() {
        assertEquals(362f, shortestUnwrappedCompassTarget(361f, 2f), 0.0001f)
    }

    @Test
    fun cardinalRoseKeepsTrueNorthOppositeDeviceHeading() {
        assertEquals(0f, cardinalRoseWrappedTarget(0f), 0.0001f)
        assertEquals(270f, cardinalRoseWrappedTarget(90f), 0.0001f)
        assertEquals(180f, cardinalRoseWrappedTarget(180f), 0.0001f)
        assertEquals(90f, cardinalRoseWrappedTarget(270f), 0.0001f)
    }

    @Test
    fun cardinalRoseCrossesNorthByShortestPath() {
        val before = cardinalRoseWrappedTarget(359f)
        val after = cardinalRoseWrappedTarget(1f)
        assertEquals(1f, before, 0.0001f)
        assertEquals(359f, after, 0.0001f)
        assertEquals(-1f, shortestUnwrappedCompassTarget(before, after), 0.0001f)
    }
}
