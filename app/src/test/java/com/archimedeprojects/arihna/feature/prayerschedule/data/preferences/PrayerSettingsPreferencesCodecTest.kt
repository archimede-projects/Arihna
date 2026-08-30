package com.archimedeprojects.arihna.feature.prayerschedule.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimeAdjustments
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerSettingsPreferencesCodecTest {
    @Test
    fun emptyPreferencesHaveNoImplicitSettings() {
        assertNull(PrayerSettingsPreferencesCodec.decodeOrNull(mutablePreferencesOf()))
    }

    @Test
    fun canonicalSettingsRoundTripAsCompleteRecord() {
        val preferences = mutablePreferencesOf()

        PrayerSettingsPreferencesCodec.write(preferences, PrayerSettingsDefaults.CANONICAL)

        assertEquals(PrayerSettingsDefaults.CANONICAL, PrayerSettingsPreferencesCodec.decodeOrNull(preferences))
        assertEquals(PrayerSettingsPreferencesCodec.requiredKeyNames, prayerKeyNames(preferences))
    }

    @Test
    fun customSettingsRoundTripEveryPositiveNegativeAndZeroOffset() {
        val preferences = mutablePreferencesOf()
        val settings = customSettings()

        PrayerSettingsPreferencesCodec.write(preferences, settings)

        assertEquals(settings, PrayerSettingsPreferencesCodec.decodeOrNull(preferences))
    }

    @Test
    fun partialRecordIsRejected() {
        val preferences = mutablePreferencesOf(
            PrayerSettingsPreferencesCodec.methodKey to PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE.name,
        )

        assertNull(PrayerSettingsPreferencesCodec.decodeOrNull(preferences))
    }

    @Test
    fun unknownMethodIsRejected() {
        val preferences = completePreferences()
        preferences[PrayerSettingsPreferencesCodec.methodKey] = "NOT_A_METHOD"

        assertNull(PrayerSettingsPreferencesCodec.decodeOrNull(preferences))
    }

    @Test
    fun unknownAsrMethodIsRejected() {
        val preferences = completePreferences()
        preferences[PrayerSettingsPreferencesCodec.asrKey] = "NOT_AN_ASR_METHOD"

        assertNull(PrayerSettingsPreferencesCodec.decodeOrNull(preferences))
    }

    @Test
    fun unknownHighLatitudeRuleIsRejected() {
        val preferences = completePreferences()
        preferences[PrayerSettingsPreferencesCodec.highLatitudeRuleKey] = "NOT_A_HIGH_LATITUDE_RULE"

        assertNull(PrayerSettingsPreferencesCodec.decodeOrNull(preferences))
    }

    @Test
    fun wrongTypeForEnumKeyIsRejected() {
        val preferences = completePreferences()
        preferences.remove(PrayerSettingsPreferencesCodec.methodKey)
        preferences[intPreferencesKey(PrayerSettingsPreferencesCodec.methodKey.name)] = 42

        assertNull(PrayerSettingsPreferencesCodec.decodeOrNull(preferences))
    }

    @Test
    fun wrongTypeForOffsetKeyIsRejected() {
        val preferences = completePreferences()
        preferences.remove(PrayerSettingsPreferencesCodec.fajrOffsetKey)
        preferences[stringPreferencesKey(PrayerSettingsPreferencesCodec.fajrOffsetKey.name)] = "five"

        assertNull(PrayerSettingsPreferencesCodec.decodeOrNull(preferences))
    }

    private fun completePreferences() = mutablePreferencesOf().also {
        PrayerSettingsPreferencesCodec.write(it, customSettings())
    }

    private fun customSettings() = PrayerCalculationSettings(
        method = PrayerCalculationMethod.UMM_AL_QURA,
        asrMethod = AsrMethod.HANAFI,
        highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE,
        adjustments = PrayerTimeAdjustments(
            fajrMinutes = 7,
            sunriseMinutes = -4,
            dhuhrMinutes = 0,
            asrMinutes = 11,
            maghribMinutes = -2,
            ishaMinutes = 5,
        ),
    )

    private fun prayerKeyNames(preferences: androidx.datastore.preferences.core.Preferences): Set<String> =
        preferences.asMap().keys.mapTo(mutableSetOf()) { it.name }
            .filterTo(mutableSetOf()) { it.startsWith("prayer.") }
}
