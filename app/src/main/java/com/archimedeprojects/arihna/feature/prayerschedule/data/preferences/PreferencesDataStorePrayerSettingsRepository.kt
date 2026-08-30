package com.archimedeprojects.arihna.feature.prayerschedule.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsDefaults
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesDataStorePrayerSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : PrayerSettingsRepository {
    override val settings: Flow<PrayerCalculationSettings> = dataStore.data
        .map { preferences ->
            PrayerSettingsPreferencesCodec.decodeOrNull(preferences)
                ?: materializeCanonicalSettingsIfNeeded()
        }
        .distinctUntilChanged()

    override suspend fun get(): PrayerCalculationSettings = settings.first()

    override suspend fun update(settings: PrayerCalculationSettings) {
        dataStore.edit { preferences ->
            PrayerSettingsPreferencesCodec.write(preferences, settings)
        }
    }

    private suspend fun materializeCanonicalSettingsIfNeeded(): PrayerCalculationSettings {
        val persisted = dataStore.edit { preferences ->
            if (PrayerSettingsPreferencesCodec.decodeOrNull(preferences) == null) {
                PrayerSettingsPreferencesCodec.write(preferences, PrayerSettingsDefaults.CANONICAL)
            }
        }

        return PrayerSettingsPreferencesCodec.decodeOrNull(persisted)
            ?: throw PrayerSettingsPersistenceException("Unable to materialize canonical Prayer settings")
    }
}

class PrayerSettingsPersistenceException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
