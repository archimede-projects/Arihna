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
    fun bundledMakkahTakbirPairPlaysExactlyOnce() {
        AdhanVariant.entries.forEach { variant ->
            assertEquals(1, adhanRepeatCount(variant))
        }
        assertEquals(
            com.archimedeprojects.arihna.R.raw.takbir_makkah_x2_cc_by,
            adhanRawResource(AdhanVariant.TAKBIR_X2),
        )
    }
}
