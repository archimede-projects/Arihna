package com.archimedeprojects.arihna.core.location.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.archimedeprojects.arihna.core.location.data.LocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.ManualCity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesDataStoreLocationPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : LocationPreferencesRepository {
    override val preference: Flow<LocationPreference> = dataStore.data.map(LocationPreferencesCodec::decodePreference)

    override val cachedDeviceFix: Flow<DeviceLocationFix?> = dataStore.data.map(LocationPreferencesCodec::decodeCachedDeviceFix)

    override suspend fun selectDevice() {
        dataStore.edit { preferences ->
            LocationPreferencesCodec.writeDevicePreference(preferences)
        }
    }

    override suspend fun selectManual(city: ManualCity) {
        require(city.isValid) { "Cannot persist an invalid manual city" }
        dataStore.edit { preferences ->
            LocationPreferencesCodec.writeManualPreference(preferences, city)
        }
    }

    override suspend fun saveDeviceFix(fix: DeviceLocationFix) {
        require(fix.isValid) { "Cannot persist an invalid device location fix" }
        dataStore.edit { preferences ->
            LocationPreferencesCodec.writeCachedDeviceFix(preferences, fix)
        }
    }
}

class LocationPersistenceException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
