package com.archimedeprojects.arihna.feature.prayerschedule.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.core.location.data.preferences.PreferencesDataStoreLocationPreferencesRepository
import com.archimedeprojects.arihna.core.location.model.DeviceLocationFix
import com.archimedeprojects.arihna.core.location.model.ManualCity
import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimeAdjustments
import com.archimedeprojects.arihna.feature.prayerschedule.data.PrayerSettingsDefaults
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesDataStorePrayerSettingsRepositoryAndroidTest {
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prayerRepository: PreferencesDataStorePrayerSettingsRepository
    private lateinit var locationRepository: PreferencesDataStoreLocationPreferencesRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.filesDir, "datastore/shared-prayer-location-${UUID.randomUUID()}.preferences_pb")
        file.parentFile?.mkdirs()
        file.delete()
        openDataStore()
    }

    @After
    fun tearDown() = runBlocking {
        closeDataStore()
        file.delete()
    }

    @Test
    fun firstUseMaterializesAndPersistsCanonicalDefault() = runBlocking {
        assertTrue(dataStore.data.first().asMap().isEmpty())

        assertEquals(PrayerSettingsDefaults.CANONICAL, prayerRepository.get())

        assertPersistedCanonical()
    }

    @Test
    fun restartRestoresSameMaterializedDefault() = runBlocking {
        assertEquals(PrayerSettingsDefaults.CANONICAL, prayerRepository.get())
        assertPersistedCanonical()

        restartDataStore()

        assertEquals(PrayerSettingsDefaults.CANONICAL, prayerRepository.get())
        assertPersistedCanonical()
    }

    @Test
    fun customSettingsAndEveryOffsetRoundTrip() = runBlocking {
        val custom = customSettings()

        prayerRepository.update(custom)

        assertEquals(custom, prayerRepository.get())
        assertEquals(custom, PrayerSettingsPreferencesCodec.decodeOrNull(dataStore.data.first()))
    }

    @Test
    fun partialRecordRecoversAtomicallyToCanonicalDefault() = runBlocking {
        dataStore.edit { preferences ->
            PrayerSettingsPreferencesCodec.write(preferences, customSettings())
            preferences.remove(PrayerSettingsPreferencesCodec.ishaOffsetKey)
        }

        assertRecoveryToCanonical()
    }

    @Test
    fun invalidMethodRecoversAtomicallyToCanonicalDefault() = runBlocking {
        dataStore.edit { preferences ->
            PrayerSettingsPreferencesCodec.write(preferences, customSettings())
            preferences[PrayerSettingsPreferencesCodec.methodKey] = "INVALID_METHOD"
        }

        assertRecoveryToCanonical()
    }

    @Test
    fun invalidAsrRecoversAtomicallyToCanonicalDefault() = runBlocking {
        dataStore.edit { preferences ->
            PrayerSettingsPreferencesCodec.write(preferences, customSettings())
            preferences[PrayerSettingsPreferencesCodec.asrKey] = "INVALID_ASR"
        }

        assertRecoveryToCanonical()
    }

    @Test
    fun invalidHighLatitudeRuleRecoversAtomicallyToCanonicalDefault() = runBlocking {
        dataStore.edit { preferences ->
            PrayerSettingsPreferencesCodec.write(preferences, customSettings())
            preferences[PrayerSettingsPreferencesCodec.highLatitudeRuleKey] = "INVALID_HIGH_LATITUDE_RULE"
        }

        assertRecoveryToCanonical()
    }

    @Test
    fun wrongTypedEnumKeyRecoversAtomicallyToCanonicalDefault() = runBlocking {
        dataStore.edit { preferences ->
            PrayerSettingsPreferencesCodec.write(preferences, customSettings())
            preferences.remove(PrayerSettingsPreferencesCodec.methodKey)
            preferences[intPreferencesKey(PrayerSettingsPreferencesCodec.methodKey.name)] = 7
        }

        assertRecoveryToCanonical()
    }

    @Test
    fun wrongTypedOffsetKeyRecoversAtomicallyToCanonicalDefault() = runBlocking {
        dataStore.edit { preferences ->
            PrayerSettingsPreferencesCodec.write(preferences, customSettings())
            preferences.remove(PrayerSettingsPreferencesCodec.fajrOffsetKey)
            preferences[stringPreferencesKey(PrayerSettingsPreferencesCodec.fajrOffsetKey.name)] = "seven"
        }

        assertRecoveryToCanonical()
    }

    @Test
    fun prayerAndLocationEntriesRemainIsolatedInBothDirections() = runBlocking {
        locationRepository.selectManual(makkah())
        val locationBeforePrayer = locationEntries(dataStore.data.first())

        prayerRepository.get()
        prayerRepository.update(customSettings())

        assertEquals(locationBeforePrayer, locationEntries(dataStore.data.first()))
        val prayerBeforeLocation = prayerEntries(dataStore.data.first())
        assertEquals(customSettings(), prayerRepository.get())

        locationRepository.selectDevice()
        locationRepository.saveDeviceFix(deviceFix())
        locationRepository.preference.first()
        locationRepository.cachedDeviceFix.first()

        assertEquals(prayerBeforeLocation, prayerEntries(dataStore.data.first()))
        assertEquals(customSettings(), prayerRepository.get())
    }

    private suspend fun assertRecoveryToCanonical() {
        assertEquals(PrayerSettingsDefaults.CANONICAL, prayerRepository.get())
        assertPersistedCanonical()
    }

    private suspend fun assertPersistedCanonical() {
        val persisted = dataStore.data.first()
        assertEquals(PrayerSettingsDefaults.CANONICAL, PrayerSettingsPreferencesCodec.decodeOrNull(persisted))
        assertEquals(
            PrayerSettingsPreferencesCodec.requiredKeyNames,
            prayerEntries(persisted).keys.mapTo(mutableSetOf()) { it.name },
        )
    }

    private fun prayerEntries(preferences: Preferences): Map<Preferences.Key<*>, Any> =
        preferences.asMap().filterKeys { it.name.startsWith("prayer.") }

    private fun locationEntries(preferences: Preferences): Map<Preferences.Key<*>, Any> =
        preferences.asMap().filterKeys { it.name.startsWith("location.") }

    private fun customSettings() = PrayerCalculationSettings(
        method = PrayerCalculationMethod.UMM_AL_QURA,
        asrMethod = AsrMethod.HANAFI,
        highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE,
        adjustments = PrayerTimeAdjustments(
            fajrMinutes = 8,
            sunriseMinutes = -5,
            dhuhrMinutes = 0,
            asrMinutes = 12,
            maghribMinutes = -3,
            ishaMinutes = 6,
        ),
    )

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
        capturedAt = Instant.parse("2026-08-30T20:00:00Z"),
        accuracyMeters = 1500f,
    )

    private fun openDataStore() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        prayerRepository = PreferencesDataStorePrayerSettingsRepository(dataStore)
        locationRepository = PreferencesDataStoreLocationPreferencesRepository(dataStore)
    }

    private suspend fun closeDataStore() {
        scope.coroutineContext[Job]?.cancelAndJoin()
    }

    private suspend fun restartDataStore() {
        closeDataStore()
        openDataStore()
    }
}
