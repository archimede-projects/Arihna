package com.archimedeprojects.arihna.feature.alarms

import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayerScheduleSource
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmReconciler
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRuleDraft
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmNotificationPermissionReader
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmScheduleResult
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmCapability
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun customCreationPersistsValidatedClockValueBeforeScheduling() = runTest(dispatcher) {
        val events = mutableListOf<String>()
        val repository = MutableRepository(events)
        val scheduler = RecordingScheduler(events)
        val viewModel = viewModel(repository, scheduler)

        viewModel.saveCustom(
            existing = null,
            label = "Farmaco",
            localTime = LocalTime.of(12, 0),
            weekdays = emptySet(),
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            ringtoneUri = "content://alarm/4",
            ringtoneTitle = "Morning Flower",
        )
        advanceUntilIdle()

        assertEquals(listOf("save", "schedule"), events)
        val saved = repository.items.value.single()
        val definition = saved.definition as AlarmDefinition.Custom
        assertEquals(LocalTime.NOON, definition.localTime)
        assertEquals("Morning Flower", saved.ringtoneTitle)
    }

    @Test
    fun editingPreservesAlarmIdAdvancesRevisionCancelsOldOccurrenceAndReconciles() = runTest(dispatcher) {
        val events = mutableListOf<String>()
        val existing = AlarmRule(
            alarmId = "custom-stable",
            revision = 7,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom("Lavoro", LocalTime.of(6, 30)),
        )
        val repository = MutableRepository(events, listOf(existing))
        val scheduler = RecordingScheduler(events)
        val viewModel = viewModel(repository, scheduler)

        viewModel.saveCustom(
            existing = existing,
            label = "Lavoro nuovo",
            localTime = LocalTime.of(7, 15),
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            soundProfile = AlarmSoundProfile.ADHAN,
            ringtoneUri = null,
            ringtoneTitle = null,
        )
        advanceUntilIdle()

        val updated = repository.items.value.single()
        assertEquals("custom-stable", updated.alarmId)
        assertEquals(8L, updated.revision)
        assertNotEquals(existing.revision, updated.revision)
        assertEquals(AlarmSoundProfile.ADHAN, updated.soundProfile)
        assertEquals(listOf("save", "cancel:custom-stable", "schedule"), events)
    }

    private fun viewModel(repository: MutableRepository, scheduler: RecordingScheduler): AlarmsViewModel {
        val reconciler = AlarmReconciler(
            ruleRepository = repository,
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource { null },
            clock = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("UTC") },
            deliveryReady = { true },
        )
        return AlarmsViewModel(
            repository = repository,
            reconciler = reconciler,
            scheduler = scheduler,
            notificationPermissionReader = AlarmNotificationPermissionReader { true },
        )
    }

    private class MutableRepository(
        private val events: MutableList<String>,
        initial: List<AlarmRule> = emptyList(),
    ) : AlarmRuleRepository {
        val items = MutableStateFlow(initial)
        override val rules: Flow<List<AlarmRule>> = items
        override suspend fun getAll(): List<AlarmRule> = items.value
        override suspend fun get(alarmId: String): AlarmRule? = items.value.firstOrNull { it.alarmId == alarmId }

        override suspend fun save(draft: AlarmRuleDraft): AlarmRule {
            events += "save"
            val existing = items.value.firstOrNull { it.alarmId == draft.alarmId }
            val saved = AlarmRule(
                alarmId = draft.alarmId,
                revision = (existing?.revision ?: 0L) + 1L,
                enabled = draft.enabled,
                soundProfile = draft.soundProfile,
                definition = draft.definition,
                ringtoneUri = draft.ringtoneUri,
                ringtoneTitle = draft.ringtoneTitle,
            )
            items.value = items.value.filterNot { it.alarmId == saved.alarmId } + saved
            return saved
        }

        override suspend fun setEnabled(alarmId: String, enabled: Boolean): AlarmRule? = error("not used")
        override suspend fun delete(alarmId: String): AlarmRule? = error("not used")
        override suspend fun disableOneShotAfterValidatedDelivery(alarmId: String, expectedRevision: Long): AlarmRule? = error("not used")
    }

    private class RecordingScheduler(private val events: MutableList<String>) : AlarmPlatformScheduler {
        override fun capability(): ExactAlarmCapability = ExactAlarmCapability.READY
        override fun scheduleExact(occurrence: AlarmOccurrence): AlarmScheduleResult {
            events += "schedule"
            return AlarmScheduleResult.Scheduled(occurrence)
        }
        override fun cancel(alarmId: String) { events += "cancel:$alarmId" }
    }
}
