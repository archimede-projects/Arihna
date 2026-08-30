package com.archimedeprojects.arihna.feature.prayerschedule.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimeAdjustments

internal object PrayerSettingsPreferencesCodec {
    internal val methodKey = stringPreferencesKey("prayer.method")
    internal val asrKey = stringPreferencesKey("prayer.asr")
    internal val highLatitudeRuleKey = stringPreferencesKey("prayer.high_latitude_rule")
    internal val fajrOffsetKey = intPreferencesKey("prayer.offset.fajr")
    internal val sunriseOffsetKey = intPreferencesKey("prayer.offset.sunrise")
    internal val dhuhrOffsetKey = intPreferencesKey("prayer.offset.dhuhr")
    internal val asrOffsetKey = intPreferencesKey("prayer.offset.asr")
    internal val maghribOffsetKey = intPreferencesKey("prayer.offset.maghrib")
    internal val ishaOffsetKey = intPreferencesKey("prayer.offset.isha")

    internal val requiredKeyNames = setOf(
        methodKey.name,
        asrKey.name,
        highLatitudeRuleKey.name,
        fajrOffsetKey.name,
        sunriseOffsetKey.name,
        dhuhrOffsetKey.name,
        asrOffsetKey.name,
        maghribOffsetKey.name,
        ishaOffsetKey.name,
    )

    fun decodeOrNull(preferences: Preferences): PrayerCalculationSettings? {
        val presentNames = preferences.asMap().keys.mapTo(mutableSetOf()) { it.name }
        if (requiredKeyNames.none(presentNames::contains)) {
            return null
        }
        if (!requiredKeyNames.all(presentNames::contains)) {
            return null
        }

        return try {
            PrayerCalculationSettings(
                method = PrayerCalculationMethod.valueOf(preferences[methodKey] ?: return null),
                asrMethod = AsrMethod.valueOf(preferences[asrKey] ?: return null),
                highLatitudeRule = HighLatitudeRule.valueOf(preferences[highLatitudeRuleKey] ?: return null),
                adjustments = PrayerTimeAdjustments(
                    fajrMinutes = preferences[fajrOffsetKey] ?: return null,
                    sunriseMinutes = preferences[sunriseOffsetKey] ?: return null,
                    dhuhrMinutes = preferences[dhuhrOffsetKey] ?: return null,
                    asrMinutes = preferences[asrOffsetKey] ?: return null,
                    maghribMinutes = preferences[maghribOffsetKey] ?: return null,
                    ishaMinutes = preferences[ishaOffsetKey] ?: return null,
                ),
            )
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: ClassCastException) {
            null
        }
    }

    fun write(preferences: MutablePreferences, settings: PrayerCalculationSettings) {
        removeKnownPrayerEntries(preferences)
        preferences[methodKey] = settings.method.name
        preferences[asrKey] = settings.asrMethod.name
        preferences[highLatitudeRuleKey] = settings.highLatitudeRule.name
        preferences[fajrOffsetKey] = settings.adjustments.fajrMinutes
        preferences[sunriseOffsetKey] = settings.adjustments.sunriseMinutes
        preferences[dhuhrOffsetKey] = settings.adjustments.dhuhrMinutes
        preferences[asrOffsetKey] = settings.adjustments.asrMinutes
        preferences[maghribOffsetKey] = settings.adjustments.maghribMinutes
        preferences[ishaOffsetKey] = settings.adjustments.ishaMinutes
    }

    private fun removeKnownPrayerEntries(preferences: MutablePreferences) {
        val matchingKeys = preferences.asMap().keys.filter { it.name in requiredKeyNames }
        matchingKeys.forEach { key ->
            @Suppress("UNCHECKED_CAST")
            preferences.remove(key as Preferences.Key<Any>)
        }
    }
}
