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
}
