package com.archimedeprojects.arihna.core.location.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.location.model.ManualCitySnapshot
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId

internal object LocationPreferencesCodec {
    internal val modeKey = stringPreferencesKey("location.mode")

    internal val manualIdKey = longPreferencesKey("location.manual.id")
    internal val manualNameKey = stringPreferencesKey("location.manual.name")
    internal val manualRegionKey = stringPreferencesKey("location.manual.region")
    internal val manualCountryNameKey = stringPreferencesKey("location.manual.country_name")
    internal val manualCountryCodeKey = stringPreferencesKey("location.manual.country_code")
    internal val manualLatitudeKey = doublePreferencesKey("location.manual.latitude")
    internal val manualLongitudeKey = doublePreferencesKey("location.manual.longitude")
    internal val manualTimeZoneIdKey = stringPreferencesKey("location.manual.timezone_id")

    internal val deviceLatitudeKey = doublePreferencesKey("location.device.latitude")
    internal val deviceLongitudeKey = doublePreferencesKey("location.device.longitude")
    internal val deviceTimeZoneIdKey = stringPreferencesKey("location.device.timezone_id")
    internal val deviceCapturedAtEpochMillisKey = longPreferencesKey("location.device.captured_at_epoch_millis")
    internal val deviceAccuracyMetersKey = floatPreferencesKey("location.device.accuracy_meters")

    private const val MODE_UNSET = "UNSET"
    private const val MODE_DEVICE = "DEVICE"
    private const val MODE_MANUAL = "MANUAL"

    private val manualRequiredKeys = setOf<Preferences.Key<*>>(
        manualIdKey,
        manualNameKey,
        manualCountryNameKey,
        manualCountryCodeKey,
        manualLatitudeKey,
        manualLongitudeKey,
        manualTimeZoneIdKey,
    )

    private val manualAllKeys = manualRequiredKeys + manualRegionKey

    private val deviceRequiredKeys = setOf<Preferences.Key<*>>(
        deviceLatitudeKey,
        deviceLongitudeKey,
        deviceTimeZoneIdKey,
        deviceCapturedAtEpochMillisKey,
    )

    private val deviceAllKeys = deviceRequiredKeys + deviceAccuracyMetersKey

    fun decodePreference(preferences: Preferences): LocationPreference {
        return when (val mode = preferences[modeKey]) {
            null, MODE_UNSET -> {
                if (containsAny(preferences, manualAllKeys)) {
                    throw LocationPersistenceException("Manual location fields exist without MANUAL mode")
                }
                LocationPreference.Unset
            }

            MODE_DEVICE -> {
                if (containsAny(preferences, manualAllKeys)) {
                    throw LocationPersistenceException("Manual location fields must be cleared in DEVICE mode")
                }
                LocationPreference.Device
            }

            MODE_MANUAL -> LocationPreference.Manual(decodeManualSnapshot(preferences))
            else -> throw LocationPersistenceException("Unsupported location mode: $mode")
        }
    }

    fun decodeCachedDeviceFix(preferences: Preferences): DeviceLocationFix? {
        if (!containsAny(preferences, deviceAllKeys)) {
            return null
        }

        requireAll(preferences, deviceRequiredKeys, "cached device fix")

        val coordinates = Coordinates(
            latitude = required(preferences, deviceLatitudeKey, "device latitude"),
            longitude = required(preferences, deviceLongitudeKey, "device longitude"),
        )
        val zoneId = parseZoneId(required(preferences, deviceTimeZoneIdKey, "device timezone"))
        val capturedAt = try {
            Instant.ofEpochMilli(required(preferences, deviceCapturedAtEpochMillisKey, "device capturedAt"))
        } catch (error: DateTimeException) {
            throw LocationPersistenceException("Invalid device capturedAt", error)
        }
        val fix = DeviceLocationFix(
            coordinates = coordinates,
            zoneId = zoneId,
            capturedAt = capturedAt,
            accuracyMeters = preferences[deviceAccuracyMetersKey],
        )
        if (!fix.isValid) {
            throw LocationPersistenceException("Cached device fix is invalid")
        }
        return fix
    }

    fun writeDevicePreference(preferences: MutablePreferences) {
        preferences[modeKey] = MODE_DEVICE
        clearManual(preferences)
    }

    fun writeManualPreference(preferences: MutablePreferences, city: ManualCity) {
        if (!city.isValid) {
            throw LocationPersistenceException("Manual city is invalid")
        }
        val snapshot = ManualCitySnapshot.from(city)
        preferences[modeKey] = MODE_MANUAL
        preferences[manualIdKey] = snapshot.id
        preferences[manualNameKey] = snapshot.name
        snapshot.regionName?.let { preferences[manualRegionKey] = it } ?: preferences.remove(manualRegionKey)
        preferences[manualCountryNameKey] = snapshot.countryName
        preferences[manualCountryCodeKey] = snapshot.countryCode
        preferences[manualLatitudeKey] = snapshot.latitude
        preferences[manualLongitudeKey] = snapshot.longitude
        preferences[manualTimeZoneIdKey] = snapshot.timeZoneId
    }

    fun writeCachedDeviceFix(preferences: MutablePreferences, fix: DeviceLocationFix) {
        if (!fix.isValid) {
            throw LocationPersistenceException("Device fix is invalid")
        }
        preferences[deviceLatitudeKey] = fix.coordinates.latitude
        preferences[deviceLongitudeKey] = fix.coordinates.longitude
        preferences[deviceTimeZoneIdKey] = fix.zoneId.id
        preferences[deviceCapturedAtEpochMillisKey] = fix.capturedAt.toEpochMilli()
        fix.accuracyMeters?.let { preferences[deviceAccuracyMetersKey] = it } ?: preferences.remove(deviceAccuracyMetersKey)
    }

    private fun decodeManualSnapshot(preferences: Preferences): ManualCitySnapshot {
        requireAll(preferences, manualRequiredKeys, "manual location")
        val snapshot = ManualCitySnapshot(
            id = required(preferences, manualIdKey, "manual city id"),
            name = required(preferences, manualNameKey, "manual city name"),
            regionName = preferences[manualRegionKey],
            countryName = required(preferences, manualCountryNameKey, "manual country name"),
            countryCode = required(preferences, manualCountryCodeKey, "manual country code"),
            latitude = required(preferences, manualLatitudeKey, "manual latitude"),
            longitude = required(preferences, manualLongitudeKey, "manual longitude"),
            timeZoneId = required(preferences, manualTimeZoneIdKey, "manual timezone"),
        )
        if (snapshot.toManualCityOrNull() == null) {
            throw LocationPersistenceException("Persisted manual location is invalid")
        }
        return snapshot
    }

    private fun parseZoneId(id: String): ZoneId = try {
        ZoneId.of(id)
    } catch (error: DateTimeException) {
        throw LocationPersistenceException("Unsupported timezone id: $id", error)
    }

    private fun containsAny(preferences: Preferences, keys: Set<Preferences.Key<*>>): Boolean {
        val present = preferences.asMap().keys
        return keys.any(present::contains)
    }

    private fun requireAll(preferences: Preferences, keys: Set<Preferences.Key<*>>, label: String) {
        val present = preferences.asMap().keys
        if (!keys.all(present::contains)) {
            throw LocationPersistenceException("Incomplete $label record")
        }
    }

    private fun clearManual(preferences: MutablePreferences) {
        preferences.remove(manualIdKey)
        preferences.remove(manualNameKey)
        preferences.remove(manualRegionKey)
        preferences.remove(manualCountryNameKey)
        preferences.remove(manualCountryCodeKey)
        preferences.remove(manualLatitudeKey)
        preferences.remove(manualLongitudeKey)
        preferences.remove(manualTimeZoneIdKey)
    }

    private fun <T> required(preferences: Preferences, key: Preferences.Key<T>, label: String): T {
        return preferences[key] ?: throw LocationPersistenceException("Missing $label")
    }
}
