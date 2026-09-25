package com.archimedeprojects.arihna.feature.alarms.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrayerAlertPreferencesAndroidTest {
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesDataStorePrayerAlertPreferencesRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.filesDir, "datastore/prayer-alert-${UUID.randomUUID()}.preferences_pb")
        file.parentFile?.mkdirs()
        file.delete()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        repository = PreferencesDataStorePrayerAlertPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
        file.delete()
    }

    @Test
    fun missingValuesDefaultToHundredAndPrayersRoundTripIndependently() = runBlocking {
        AlarmPrayer.entries.forEach { prayer ->
            assertEquals(100, repository.volumeFor(prayer))
        }

        repository.setVolume(AlarmPrayer.FAJR, 100)
        repository.setVolume(AlarmPrayer.DHUHR, 40)
        repository.setVolume(AlarmPrayer.ASR, 35)
        repository.setVolume(AlarmPrayer.ISHA, 90)

        assertEquals(100, repository.volumeFor(AlarmPrayer.FAJR))
        assertEquals(40, repository.volumeFor(AlarmPrayer.DHUHR))
        assertEquals(35, repository.volumeFor(AlarmPrayer.ASR))
        assertEquals(100, repository.volumeFor(AlarmPrayer.MAGHRIB))
        assertEquals(90, repository.volumeFor(AlarmPrayer.ISHA))
    }

    @Test
    fun persistedValuesClampToZeroThroughHundred() = runBlocking {
        repository.setVolume(AlarmPrayer.DHUHR, -10)
        repository.setVolume(AlarmPrayer.ISHA, 140)
        assertEquals(0, repository.volumeFor(AlarmPrayer.DHUHR))
        assertEquals(100, repository.volumeFor(AlarmPrayer.ISHA))
    }
}
