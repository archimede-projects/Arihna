package com.archimedeprojects.arihna.feature.alarms.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PrayerAlertVolumePreferences(
    val byPrayer: Map<AlarmPrayer, Int> = AlarmPrayer.entries.associateWith { DEFAULT_PRAYER_VOLUME_PERCENT },
) {
    fun volumeFor(prayer: AlarmPrayer): Int =
        byPrayer[prayer]?.coerceIn(MIN_VOLUME_PERCENT, MAX_VOLUME_PERCENT)
            ?: DEFAULT_PRAYER_VOLUME_PERCENT

    companion object {
        const val MIN_VOLUME_PERCENT = 0
        const val MAX_VOLUME_PERCENT = 100
        const val DEFAULT_PRAYER_VOLUME_PERCENT = 100
    }
}

interface PrayerAlertPreferencesRepository {
    val volumes: Flow<PrayerAlertVolumePreferences>

    suspend fun current(): PrayerAlertVolumePreferences

    suspend fun volumeFor(prayer: AlarmPrayer): Int

    suspend fun setVolume(prayer: AlarmPrayer, percent: Int)
}

class PreferencesDataStorePrayerAlertPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : PrayerAlertPreferencesRepository {
    override val volumes: Flow<PrayerAlertVolumePreferences> = dataStore.data
        .map(::decodePrayerAlertVolumePreferences)
        .distinctUntilChanged()

    override suspend fun current(): PrayerAlertVolumePreferences = volumes.first()

    override suspend fun volumeFor(prayer: AlarmPrayer): Int = current().volumeFor(prayer)

    override suspend fun setVolume(prayer: AlarmPrayer, percent: Int) {
        val clamped = percent.coerceIn(
            PrayerAlertVolumePreferences.MIN_VOLUME_PERCENT,
            PrayerAlertVolumePreferences.MAX_VOLUME_PERCENT,
        )
        dataStore.edit { preferences ->
            preferences[volumeKey(prayer)] = clamped
        }
    }
}

internal fun decodePrayerAlertVolumePreferences(
    preferences: Preferences,
): PrayerAlertVolumePreferences = PrayerAlertVolumePreferences(
    byPrayer = AlarmPrayer.entries.associateWith { prayer ->
        preferences[volumeKey(prayer)]
            ?.coerceIn(
                PrayerAlertVolumePreferences.MIN_VOLUME_PERCENT,
                PrayerAlertVolumePreferences.MAX_VOLUME_PERCENT,
            )
            ?: PrayerAlertVolumePreferences.DEFAULT_PRAYER_VOLUME_PERCENT
    },
)

private fun volumeKey(prayer: AlarmPrayer) =
    intPreferencesKey("alarm.prayer.volume.${prayer.name.lowercase()}.v1")
