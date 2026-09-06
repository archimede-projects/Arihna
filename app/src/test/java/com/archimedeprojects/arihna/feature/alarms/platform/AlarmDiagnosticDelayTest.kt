package com.archimedeprojects.arihna.feature.alarms.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmDiagnosticDelayTest {
    @Test
    fun diagnosticDelayIsTenSeconds() {
        assertEquals(10_000L, AlarmDiagnosticTestScheduler.TEST_DELAY_MILLIS)
    }
}
