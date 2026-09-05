package com.archimedeprojects.arihna.feature.alarms.data.preferences

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRulePreferencesCodecTest {
    @Test
    fun emptyCollectionHasStableV2EncodingAndV1StillDecodes() {
        assertEquals("ARIHNA_ALARMS_V2", AlarmRulePreferencesCodec.encode(emptyList()))
        assertEquals(emptyList<AlarmRule>(), AlarmRulePreferencesCodec.decode("ARIHNA_ALARMS_V2"))
        assertEquals(emptyList<AlarmRule>(), AlarmRulePreferencesCodec.decode("ARIHNA_ALARMS_V1"))
        assertEquals(emptyList<AlarmRule>(), AlarmRulePreferencesCodec.decode(null))
    }

    @Test
    fun prayerAndCustomRulesRoundTripRingtoneSelectionLosslessly() {
        val rules = listOf(
            AlarmRule(
                alarmId = "prayer|fajr/à",
                revision = 19,
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.PrayerLinked(AlarmPrayer.FAJR, -35),
                ringtoneUri = "content://media/alarm/17",
                ringtoneTitle = "Morning Flower",
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
        assertTrue(encoded.startsWith("ARIHNA_ALARMS_V2\n"))
        assertEquals(rules.sortedBy { it.alarmId }, AlarmRulePreferencesCodec.decode(encoded))
    }

    @Test
    fun v1RulesMigrateWithoutInventingRingtone() {
        val oldSystem = "ARIHNA_ALARMS_V1\nP|cHJheWVyLWZhanI|1|1|SYSTEM_DEFAULT|FAJR|0"
        val decoded = AlarmRulePreferencesCodec.decode(oldSystem).single()
        assertEquals(AlarmSoundProfile.SYSTEM_DEFAULT, decoded.soundProfile)
        assertNull(decoded.ringtoneUri)
        assertNull(decoded.ringtoneTitle)
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
        AlarmRulePreferencesCodec.decode("ARIHNA_ALARMS_V3")
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
        assertTrue(encoded.contains("|1,2,7|-|-"))
    }

    private fun prayer(id: String, prayer: AlarmPrayer) = AlarmRule(
        alarmId = id,
        revision = 1,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.PrayerLinked(prayer),
    )
}
