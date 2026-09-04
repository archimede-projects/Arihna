package com.archimedeprojects.arihna.feature.alarms.domain

import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmScheduleResult
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmCapability
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmStep5ReconcilerTest {
    @Test
    fun missingNotificationPermissionCancelsIdentityAndNeverSchedules() = runBlocking {
        val rule = customRule("notification-blocked")
        val scheduler = RecordingScheduler(ExactAlarmCapability.READY)
        val reconciler = AlarmReconciler(
            ruleRepository = FakeRuleRepository(listOf(rule)),
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource { null },
            clock = Clock.fixed(java.time.Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("UTC") },
            deliveryReady = { false },
        )

        val report = reconciler.reconcile()

        assertTrue(report.outcomes.single() is AlarmReconcileOutcome.NeedsNotificationPermission)
        assertEquals(listOf(rule.alarmId), scheduler.cancelled)
        assertEquals(0, scheduler.scheduleAttempts)
    }

    @Test
    fun missingExactAlarmAccessCancelsIdentityWithoutInexactFallback() = runBlocking {
        val rule = customRule("exact-blocked")
        val scheduler = RecordingScheduler(ExactAlarmCapability.NEEDS_EXACT_ALARM_ACCESS)
        val reconciler = AlarmReconciler(
            ruleRepository = FakeRuleRepository(listOf(rule)),
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource { null },
            clock = Clock.fixed(java.time.Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("UTC") },
            deliveryReady = { true },
        )

        val report = reconciler.reconcile()

        assertTrue(report.outcomes.single() is AlarmReconcileOutcome.NeedsExactAlarmAccess)
        assertEquals(listOf(rule.alarmId), scheduler.cancelled)
        assertEquals(1, scheduler.scheduleAttempts)
    }

    private fun customRule(id: String) = AlarmRule(
        alarmId = id,
        revision = 1,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.Custom(
            label = "Test",
            localTime = LocalTime.of(12, 0),
        ),
    )

    private class RecordingScheduler(
        private val exactCapability: ExactAlarmCapability,
    ) : AlarmPlatformScheduler {
        val cancelled = mutableListOf<String>()
        var scheduleAttempts = 0

        override fun capability(): ExactAlarmCapability = exactCapability

        override fun scheduleExact(occurrence: AlarmOccurrence): AlarmScheduleResult {
            scheduleAttempts += 1
            return if (exactCapability == ExactAlarmCapability.READY) {
                AlarmScheduleResult.Scheduled(occurrence)
            } else {
                AlarmScheduleResult.NeedsExactAlarmAccess
            }
        }

        override fun cancel(alarmId: String) {
            cancelled += alarmId
        }
    }

    private class FakeRuleRepository(
        private val items: List<AlarmRule>,
    ) : AlarmRuleRepository {
        override val rules: Flow<List<AlarmRule>> = flowOf(items)
        override suspend fun getAll(): List<AlarmRule> = items
        override suspend fun get(alarmId: String): AlarmRule? = items.firstOrNull { it.alarmId == alarmId }
        override suspend fun save(draft: AlarmRuleDraft): AlarmRule = error("not used")
        override suspend fun setEnabled(alarmId: String, enabled: Boolean): AlarmRule? = error("not used")
        override suspend fun delete(alarmId: String): AlarmRule? = error("not used")
        override suspend fun disableOneShotAfterValidatedDelivery(
            alarmId: String,
            expectedRevision: Long,
        ): AlarmRule? = error("not used")
    }
}
