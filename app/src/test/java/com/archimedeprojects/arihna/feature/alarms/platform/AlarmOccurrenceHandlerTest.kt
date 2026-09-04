package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrenceTokens
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRuleDraft
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmOccurrenceHandlerTest {
    @Test
    fun staleEnvelopeIsIgnoredBeforeUserVisibleDelivery() = runBlocking {
        val repository = MutableRuleRepository(oneShotRule())
        val delivery = FakeDelivery(AlarmNotificationDeliveryResult.DELIVERED)
        var reconciles = 0
        val handler = AlarmOccurrenceHandler(repository, delivery) { reconciles += 1 }
        val stale = envelope(repository.current!!.copy(revision = 2))

        val result = handler.handle(stale)

        assertEquals(AlarmOccurrenceHandlingResult.IGNORED_STALE_OR_INVALID, result)
        assertEquals(0, delivery.calls)
        assertEquals(0, repository.disableCalls)
        assertEquals(0, reconciles)
    }

    @Test
    fun successfulValidatedOneShotDeliveryDisablesCurrentRevisionThenReconciles() = runBlocking {
        val repository = MutableRuleRepository(oneShotRule())
        val delivery = FakeDelivery(AlarmNotificationDeliveryResult.DELIVERED)
        var reconciles = 0
        val handler = AlarmOccurrenceHandler(repository, delivery) { reconciles += 1 }

        val result = handler.handle(envelope(repository.current!!))

        assertEquals(AlarmOccurrenceHandlingResult.DELIVERED, result)
        assertEquals(1, delivery.calls)
        assertEquals(1, repository.disableCalls)
        assertFalse(repository.current!!.enabled)
        assertEquals(1, reconciles)
    }

    @Test
    fun blockedNotificationDoesNotMarkOneShotAsDelivered() = runBlocking {
        val repository = MutableRuleRepository(oneShotRule())
        val delivery = FakeDelivery(AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION)
        var reconciles = 0
        val handler = AlarmOccurrenceHandler(repository, delivery) { reconciles += 1 }

        val result = handler.handle(envelope(repository.current!!))

        assertEquals(AlarmOccurrenceHandlingResult.NEEDS_NOTIFICATION_PERMISSION, result)
        assertEquals(1, delivery.calls)
        assertEquals(0, repository.disableCalls)
        assertTrue(repository.current!!.enabled)
        assertEquals(1, reconciles)
    }

    @Test
    fun recurringDeliveryStaysEnabledAndRequestsNextReconciliation() = runBlocking {
        val recurring = oneShotRule().copy(
            definition = AlarmDefinition.Custom(
                label = "Ricorrente",
                localTime = LocalTime.of(7, 30),
                weekdays = setOf(DayOfWeek.MONDAY),
            ),
        )
        val repository = MutableRuleRepository(recurring)
        val delivery = FakeDelivery(AlarmNotificationDeliveryResult.DELIVERED)
        var reconciles = 0
        val handler = AlarmOccurrenceHandler(repository, delivery) { reconciles += 1 }

        val result = handler.handle(envelope(repository.current!!))

        assertEquals(AlarmOccurrenceHandlingResult.DELIVERED, result)
        assertEquals(0, repository.disableCalls)
        assertTrue(repository.current!!.enabled)
        assertEquals(1, reconciles)
    }

    private fun oneShotRule() = AlarmRule(
        alarmId = "custom-test",
        revision = 1,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.Custom(
            label = "Test",
            localTime = LocalTime.of(7, 30),
        ),
    )

    private fun envelope(rule: AlarmRule): AlarmOccurrenceEnvelope {
        val triggerAt = Instant.parse("2026-09-05T05:30:00Z")
        return AlarmOccurrenceEnvelope(
            alarmId = rule.alarmId,
            ruleRevision = rule.revision,
            triggerAt = triggerAt,
            occurrenceToken = AlarmOccurrenceTokens.create(rule.alarmId, rule.revision, triggerAt),
        )
    }

    private class FakeDelivery(
        private val result: AlarmNotificationDeliveryResult,
    ) : AlarmNotificationDelivery {
        var calls = 0
        override fun ensureChannels() = Unit
        override fun deliver(rule: AlarmRule, occurrence: AlarmOccurrence): AlarmNotificationDeliveryResult {
            calls += 1
            return result
        }
    }

    private class MutableRuleRepository(initial: AlarmRule) : AlarmRuleRepository {
        private val state = MutableStateFlow(listOf(initial))
        var current: AlarmRule? = initial
        var disableCalls = 0

        override val rules: Flow<List<AlarmRule>> = state
        override suspend fun getAll(): List<AlarmRule> = current?.let(::listOf).orEmpty()
        override suspend fun get(alarmId: String): AlarmRule? = current?.takeIf { it.alarmId == alarmId }
        override suspend fun save(draft: AlarmRuleDraft): AlarmRule = error("not used")
        override suspend fun setEnabled(alarmId: String, enabled: Boolean): AlarmRule? = error("not used")
        override suspend fun delete(alarmId: String): AlarmRule? = error("not used")

        override suspend fun disableOneShotAfterValidatedDelivery(
            alarmId: String,
            expectedRevision: Long,
        ): AlarmRule? {
            val rule = current ?: return null
            if (rule.alarmId != alarmId || rule.revision != expectedRevision || !rule.isOneShotCustom) return rule
            disableCalls += 1
            val updated = rule.copy(enabled = false, revision = rule.revision + 1)
            current = updated
            state.value = listOf(updated)
            return updated
        }
    }
}
