package com.archimedeprojects.arihna.feature.alarms.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRuleDraft
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesDataStoreAlarmRuleRepositoryAndroidTest {
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesDataStoreAlarmRuleRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.filesDir, "datastore/shared-alarm-${UUID.randomUUID()}.preferences_pb")
        file.parentFile?.mkdirs()
        file.delete()
        openDataStore()
    }

    @After
    fun tearDown() {
        runBlocking { closeDataStore() }
        file.delete()
    }

    @Test
    fun missingAlarmNamespaceReadsAsEmptyWithoutWritingOtherNamespaces() = runBlocking {
        val locationSentinel = stringPreferencesKey("location.alarm_test_sentinel")
        val prayerSentinel = stringPreferencesKey("prayer.alarm_test_sentinel")
        dataStore.edit {
            it[locationSentinel] = "location"
            it[prayerSentinel] = "prayer"
        }

        assertEquals(emptyList<Any>(), repository.getAll())
        val persisted = dataStore.data.first()
        assertEquals("location", persisted[locationSentinel])
        assertEquals("prayer", persisted[prayerSentinel])
        assertNull(persisted[AlarmRulePreferencesCodec.rulesKey])
    }

    @Test
    fun saveRoundTripUsesStableIdAndMonotonicRevision() = runBlocking {
        val first = repository.save(
            AlarmRuleDraft(
                alarmId = "custom-1",
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.Custom(
                    label = "Lavoro",
                    localTime = LocalTime.of(7, 15),
                    weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                ),
            ),
        )
        val same = repository.save(
            AlarmRuleDraft(
                alarmId = "custom-1",
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = first.definition,
            ),
        )
        val changed = repository.setEnabled("custom-1", false)

        assertEquals(1L, first.revision)
        assertEquals(first, same)
        assertEquals(2L, changed?.revision)
        assertFalse(requireNotNull(changed).enabled)
        assertEquals("custom-1", changed.alarmId)
    }

    @Test
    fun prayerAndCustomRulesSurviveDataStoreRestartLosslessly() = runBlocking {
        repository.save(
            AlarmRuleDraft(
                alarmId = "fajr",
                enabled = true,
                soundProfile = AlarmSoundProfile.SILENT,
                definition = AlarmDefinition.PrayerLinked(AlarmPrayer.FAJR, -20),
            ),
        )
        repository.save(
            AlarmRuleDraft(
                alarmId = "work",
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.Custom("Work", LocalTime.of(6, 45)),
            ),
        )
        val before = repository.getAll()

        restartDataStore()

        assertEquals(before, repository.getAll())
        assertEquals(before, repository.rules.first())
    }

    @Test
    fun alarmWritesNeverRewriteLocationOrPrayerEntries() = runBlocking {
        val locationSentinel = stringPreferencesKey("location.shared_sentinel")
        val prayerSentinel = stringPreferencesKey("prayer.shared_sentinel")
        dataStore.edit {
            it[locationSentinel] = "keep-location"
            it[prayerSentinel] = "keep-prayer"
        }

        repository.save(
            AlarmRuleDraft(
                alarmId = "isha",
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.PrayerLinked(AlarmPrayer.ISHA, 5),
            ),
        )
        repository.setEnabled("isha", false)
        repository.delete("isha")

        val persisted = dataStore.data.first()
        assertEquals("keep-location", persisted[locationSentinel])
        assertEquals("keep-prayer", persisted[prayerSentinel])
        assertTrue(persisted.asMap().keys.none { it.name.startsWith("alarm.") && it.name != "alarm.rules.v1" })
    }

    @Test
    fun validatedOneShotDeliveryDisablesOnlyMatchingRevision() = runBlocking {
        val oneShot = repository.save(
            AlarmRuleDraft(
                alarmId = "one-shot",
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.Custom("Una volta", LocalTime.of(9, 0)),
            ),
        )

        assertNull(repository.disableOneShotAfterValidatedDelivery("one-shot", oneShot.revision + 1L))
        assertTrue(requireNotNull(repository.get("one-shot")).enabled)

        val disabled = repository.disableOneShotAfterValidatedDelivery("one-shot", oneShot.revision)
        assertEquals(oneShot.revision + 1L, disabled?.revision)
        assertFalse(requireNotNull(disabled).enabled)
    }

    @Test
    fun recurringCustomAndPrayerRulesAreNeverAutoDisabledByDeliveryHelper() = runBlocking {
        val recurring = repository.save(
            AlarmRuleDraft(
                alarmId = "recurring",
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.Custom(
                    "Recurring",
                    LocalTime.of(8, 0),
                    setOf(DayOfWeek.MONDAY),
                ),
            ),
        )
        val prayer = repository.save(
            AlarmRuleDraft(
                alarmId = "prayer",
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.PrayerLinked(AlarmPrayer.DHUHR),
            ),
        )

        assertNull(repository.disableOneShotAfterValidatedDelivery(recurring.alarmId, recurring.revision))
        assertNull(repository.disableOneShotAfterValidatedDelivery(prayer.alarmId, prayer.revision))
        assertTrue(requireNotNull(repository.get(recurring.alarmId)).enabled)
        assertTrue(requireNotNull(repository.get(prayer.alarmId)).enabled)
    }

    private fun openDataStore() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        repository = PreferencesDataStoreAlarmRuleRepository(dataStore)
    }

    private suspend fun closeDataStore() {
        scope.coroutineContext[Job]?.cancelAndJoin()
    }

    private suspend fun restartDataStore() {
        closeDataStore()
        openDataStore()
    }
}
