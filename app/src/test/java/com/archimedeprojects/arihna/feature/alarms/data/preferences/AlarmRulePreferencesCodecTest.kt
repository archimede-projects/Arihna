package com.archimedeprojects.arihna.feature.alarms.data.preferences

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRulePreferencesCodecTest {
    @Test
    fun emptyCollectionHasStableVersionedEncoding() {
        assertEquals("ARIHNA_ALARMS_V1", AlarmRulePreferencesCodec.encode(emptyList()))
        assertEquals(emptyList<AlarmRule>(), AlarmRulePreferencesCodec.decode("ARIHNA_ALARMS_V1"))
        assertEquals(emptyList<AlarmRule>(), AlarmRulePreferencesCodec.decode(null))
    }

    @Test
    fun prayerAndCustomRulesRoundTripLosslessly() {
        val rules = listOf(
            AlarmRule(
                alarmId = "prayer|fajr/à",
                revision = 19,
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.PrayerLinked(AlarmPrayer.FAJR, -35),
            ),
            AlarmRule(
                alarmId = "custom:work",
                revision = 3,
                enabled = false,
                soundProfile = AlarmSoundProfile.SILENT,
                definition = AlarmDefinition.Custom(
                    label = "Lavoro | الرياض",
                    localTime = LocalTime.of(6, 45, 12),
                    weekdays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                ),
            ),
        )

        val encoded = AlarmRulePreferencesCodec.encode(rules)
        val decoded = AlarmRulePreferencesCodec.decode(encoded)

        assertEquals(rules.sortedBy { it.alarmId }, decoded)
    }

    @Test
    fun adhanProfileRoundTripsWithoutChangingVersionAndOldProfilesStillDecode() {
        val adhan = AlarmRule(
            alarmId = "prayer-adhan",
            revision = 4,
            enabled = true,
            soundProfile = AlarmSoundProfile.ADHAN,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.MAGHRIB),
        )
        val encoded = AlarmRulePreferencesCodec.encode(listOf(adhan))
        assertTrue(encoded.startsWith("ARIHNA_ALARMS_V1\n"))
        assertEquals(listOf(adhan), AlarmRulePreferencesCodec.decode(encoded))

        val oldSystem = "ARIHNA_ALARMS_V1\nP|cHJheWVyLWZhanI|1|1|SYSTEM_DEFAULT|FAJR|0"
        val oldSilent = "ARIHNA_ALARMS_V1\nP|cHJheWVyLWlzaGE|2|0|SILENT|ISHA|0"
        assertEquals(AlarmSoundProfile.SYSTEM_DEFAULT, AlarmRulePreferencesCodec.decode(oldSystem).single().soundProfile)
        assertEquals(AlarmSoundProfile.SILENT, AlarmRulePreferencesCodec.decode(oldSilent).single().soundProfile)
    }

    @Test
    fun collectionEncodingIsDeterministicRegardlessOfInputOrder() {
        val first = prayer("z", AlarmPrayer.ISHA)
        val second = prayer("a", AlarmPrayer.DHUHR)

        assertEquals(
            AlarmRulePreferencesCodec.encode(listOf(first, second)),
            AlarmRulePreferencesCodec.encode(listOf(second, first)),
        )
    }

    @Test(expected = AlarmRulesPersistenceException::class)
    fun rejectsUnknownVersionRatherThanSilentlyDroppingRules() {
        AlarmRulePreferencesCodec.decode("ARIHNA_ALARMS_V2")
    }

    @Test(expected = AlarmRulesPersistenceException::class)
    fun rejectsDuplicateIds() {
        AlarmRulePreferencesCodec.encode(listOf(prayer("same", AlarmPrayer.FAJR), prayer("same", AlarmPrayer.ISHA)))
    }

    @Test
    fun weekdayEncodingUsesStableIsoOrdering() {
        val encoded = AlarmRulePreferencesCodec.encode(
            listOf(
                AlarmRule(
                    alarmId = "weekdays",
                    revision = 1,
                    enabled = true,
                    soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                    definition = AlarmDefinition.Custom(
                        label = "Weekdays",
                        localTime = LocalTime.of(8, 0),
                        weekdays = setOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY, DayOfWeek.MONDAY),
                    ),
                ),
            ),
        )

        assertTrue(encoded.endsWith("|1,2,7"))
    }

    private fun prayer(id: String, prayer: AlarmPrayer) = AlarmRule(
        alarmId = id,
        revision = 1,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.PrayerLinked(prayer),
    )
}
