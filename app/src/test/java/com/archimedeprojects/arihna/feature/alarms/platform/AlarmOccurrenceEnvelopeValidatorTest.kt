package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrenceTokens
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmOccurrenceEnvelopeValidatorTest {
    private val triggerAt = Instant.parse("2026-09-05T06:00:00Z")
    private val rule = AlarmRule(
        alarmId = "custom-1",
        revision = 7,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.Custom("Sveglia", LocalTime.of(8, 0)),
    )

    @Test
    fun matchingCurrentRevisionAndTokenIsValid() {
        assertTrue(AlarmOccurrenceEnvelopeValidator.isCurrent(envelope(rule.revision), rule))
    }

    @Test
    fun staleRevisionCannotValidateNewerRule() {
        assertFalse(AlarmOccurrenceEnvelopeValidator.isCurrent(envelope(6), rule))
    }

    @Test
    fun disabledRuleCannotValidate() {
        assertFalse(AlarmOccurrenceEnvelopeValidator.isCurrent(envelope(rule.revision), rule.copy(enabled = false)))
    }

    @Test
    fun tamperedTokenCannotValidate() {
        assertFalse(
            AlarmOccurrenceEnvelopeValidator.isCurrent(
                envelope(rule.revision).copy(occurrenceToken = "tampered"),
                rule,
            ),
        )
    }

    private fun envelope(revision: Long) = AlarmOccurrenceEnvelope(
        alarmId = rule.alarmId,
        ruleRevision = revision,
        triggerAt = triggerAt,
        occurrenceToken = AlarmOccurrenceTokens.create(rule.alarmId, revision, triggerAt),
    )
}
