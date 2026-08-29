package com.archimedeprojects.arihna.core.location.data.preferences

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.LocationPreference
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.location.model.ManualCitySnapshot
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LocationPreferencesCodecTest {
    @Test
    fun emptyPreferencesAreUnsetWithoutGeographicDefaults() {
        val preferences = mutablePreferencesOf()

        assertEquals(LocationPreference.Unset, LocationPreferencesCodec.decodePreference(preferences))
        assertNull(LocationPreferencesCodec.decodeCachedDeviceFix(preferences))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualLatitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualLongitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.deviceLatitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.deviceLongitudeKey))
    }

    @Test
    fun manualCityRoundTripsThroughPreferencesCodec() {
        val preferences = mutablePreferencesOf()
        val city = makkah()

        LocationPreferencesCodec.writeManualPreference(preferences, city)

        assertEquals(
            LocationPreference.Manual(ManualCitySnapshot.from(city)),
            LocationPreferencesCodec.decodePreference(preferences),
        )
    }

    @Test
    fun devicePreferenceClearsManualSnapshotAtomically() {
        val preferences = mutablePreferencesOf()
        LocationPreferencesCodec.writeManualPreference(preferences, makkah())

        LocationPreferencesCodec.writeDevicePreference(preferences)

        assertEquals(LocationPreference.Device, LocationPreferencesCodec.decodePreference(preferences))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualIdKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualLatitudeKey))
        assertFalse(preferences.asMap().keys.contains(LocationPreferencesCodec.manualTimeZoneIdKey))
    }

    @Test
    fun cachedDeviceFixRoundTripsThroughPreferencesCodec() {
        val preferences = mutablePreferencesOf()
        val fix = deviceFix()

        LocationPreferencesCodec.writeCachedDeviceFix(preferences, fix)

        assertEquals(fix, LocationPreferencesCodec.decodeCachedDeviceFix(preferences))
    }

    @Test
    fun incompleteManualRecordIsRejected() {
        val preferences = mutablePreferencesOf(
            LocationPreferencesCodec.modeKey to "MANUAL",
            LocationPreferencesCodec.manualIdKey to 104515,
        )

        assertThrows(LocationPersistenceException::class.java) {
            LocationPreferencesCodec.decodePreference(preferences)
        }
    }

    @Test
    fun incompleteCachedDeviceFixIsRejected() {
        val preferences = mutablePreferencesOf(
            LocationPreferencesCodec.deviceLatitudeKey to 41.9028,
        )

        assertThrows(LocationPersistenceException::class.java) {
            LocationPreferencesCodec.decodeCachedDeviceFix(preferences)
        }
    }

    @Test
    fun invalidTimezoneIsRejectedInsteadOfUsingDeviceTimezone() {
        val preferences = mutablePreferencesOf()
        LocationPreferencesCodec.writeManualPreference(preferences, makkah())
        preferences[LocationPreferencesCodec.manualTimeZoneIdKey] = "Mars/Olympus"

        assertThrows(LocationPersistenceException::class.java) {
            LocationPreferencesCodec.decodePreference(preferences)
        }
    }

    private fun makkah() = ManualCity(
        id = 104515,
        name = "Makkah",
        regionName = "Makkah Province",
        countryName = "Saudi Arabia",
        countryCode = "SA",
        coordinates = Coordinates(21.427009, 39.828685),
        zoneId = ZoneId.of("Asia/Riyadh"),
    )

    private fun deviceFix() = DeviceLocationFix(
        coordinates = Coordinates(41.9028, 12.4964),
        zoneId = ZoneId.of("Europe/Rome"),
        capturedAt = Instant.parse("2026-08-29T10:00:00Z"),
        accuracyMeters = 1500f,
    )
}
