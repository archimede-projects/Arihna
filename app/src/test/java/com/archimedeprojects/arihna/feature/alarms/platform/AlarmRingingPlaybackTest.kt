package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.domain.AdhanVariant
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmRingingPlaybackTest {
    @Test
    fun playbackGainClampsZeroToHundredPercent() {
        assertEquals(0f, playbackGain(-10), 0f)
        assertEquals(0.4f, playbackGain(40), 0.0001f)
        assertEquals(1f, playbackGain(100), 0f)
        assertEquals(1f, playbackGain(140), 0f)
    }

    @Test
    fun onlyDedicatedTakbirVariantRepeatsExactlyTwice() {
        AdhanVariant.entries.forEach { variant ->
            val expected = if (variant == AdhanVariant.TAKBIR_X2) 2 else 1
            assertEquals(expected, adhanRepeatCount(variant))
        }
    }
}
