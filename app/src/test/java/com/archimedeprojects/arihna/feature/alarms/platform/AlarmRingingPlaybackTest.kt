package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.domain.AdhanVariant
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun alarmStreamVolumeIndexMapsRequestedPercentIntoAndroidRange() {
        assertEquals(1, alarmStreamVolumeIndex(0, 1, 15))
        assertEquals(7, alarmStreamVolumeIndex(40, 1, 15))
        assertEquals(15, alarmStreamVolumeIndex(100, 1, 15))
        assertEquals(15, alarmStreamVolumeIndex(140, 1, 15))
    }

    @Test
    fun effectiveGainUsesFullPlayerGainAfterStreamControlAndSoftwareFallbackOtherwise() {
        assertEquals(1f, effectivePlaybackGain(40, streamVolumeApplied = true), 0f)
        assertEquals(0f, effectivePlaybackGain(0, streamVolumeApplied = true), 0f)
        assertEquals(0.4f, effectivePlaybackGain(40, streamVolumeApplied = false), 0.0001f)
    }

    @Test
    fun streamVolumeSessionTemporarilyAppliesAndRestoresOriginalVolume() {
        val backend = FakeAlarmStreamVolumeBackend(current = 4)
        val session = AlarmStreamVolumeSession(backend)

        assertTrue(session.apply(40))
        assertEquals(7, backend.current)
        session.restore()
        assertEquals(4, backend.current)
    }

    @Test
    fun streamVolumeSessionDoesNotOverwriteUserChangeMadeWhileRinging() {
        val backend = FakeAlarmStreamVolumeBackend(current = 4)
        val session = AlarmStreamVolumeSession(backend)

        assertTrue(session.apply(50))
        backend.current = 11
        session.restore()
        assertEquals(11, backend.current)
    }

    @Test
    fun streamVolumeSessionFallsBackWhenAndroidVolumeIsFixedOrRejected() {
        val fixed = FakeAlarmStreamVolumeBackend(current = 5, fixed = true)
        assertFalse(AlarmStreamVolumeSession(fixed).apply(80))
        assertEquals(5, fixed.current)

        val rejected = FakeAlarmStreamVolumeBackend(current = 5, failSet = true)
        assertFalse(AlarmStreamVolumeSession(rejected).apply(80))
        assertEquals(5, rejected.current)
    }

    @Test
    fun onlyAdhanAutoEndsAfterNaturalAudioCompletion() {
        assertTrue(shouldAutoEndRingingOnNaturalCompletion(AlarmSoundProfile.ADHAN))
        assertFalse(shouldAutoEndRingingOnNaturalCompletion(AlarmSoundProfile.SYSTEM_DEFAULT))
        assertFalse(shouldAutoEndRingingOnNaturalCompletion(AlarmSoundProfile.SILENT))
    }

    @Test
    fun ringingActivityDismissalIsScopedToMatchingAlarmId() {
        assertTrue(shouldDismissRingingActivity("fajr", "fajr"))
        assertFalse(shouldDismissRingingActivity("fajr", "isha"))
        assertFalse(shouldDismissRingingActivity("fajr", null))
        assertFalse(shouldDismissRingingActivity(null, "fajr"))
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
    @Test
    fun approvedVintageAlarmUsesBundledResourceAndStaysLoopingProfile() {
        assertTrue(isArihnaClassicAlarm(ARIHNA_CLASSIC_ALARM_URI))
        assertEquals(ARIHNA_CLASSIC_ALARM_TITLE, "Sveglia classica Arihna")
        assertEquals(
            com.archimedeprojects.arihna.R.raw.arihna_classic_alarm,
            arihnaClassicAlarmRawResource(),
        )
        assertFalse(shouldAutoEndRingingOnNaturalCompletion(AlarmSoundProfile.SYSTEM_DEFAULT))
    }


    private class FakeAlarmStreamVolumeBackend(
        var current: Int,
        private val min: Int = 1,
        private val max: Int = 15,
        private val fixed: Boolean = false,
        private val failSet: Boolean = false,
    ) : AlarmStreamVolumeBackend {
        override val isVolumeFixed: Boolean
            get() = fixed

        override fun minVolume(): Int = min

        override fun maxVolume(): Int = max

        override fun currentVolume(): Int = current

        override fun setVolume(index: Int) {
            if (failSet) throw RuntimeException("rejected")
            current = index.coerceIn(min, max)
        }
    }
}
