package com.archimedeprojects.arihna.feature.alarms.platform

import android.content.Intent
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrenceTokens
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import java.time.Instant

data class AlarmOccurrenceEnvelope(
    val alarmId: String,
    val ruleRevision: Long,
    val triggerAt: Instant,
    val occurrenceToken: String,
)

object AlarmIntentContract {
    const val EXTRA_ALARM_ID = "com.archimedeprojects.arihna.alarm.ALARM_ID"
    const val EXTRA_RULE_REVISION = "com.archimedeprojects.arihna.alarm.RULE_REVISION"
    const val EXTRA_TRIGGER_AT_EPOCH_MILLIS = "com.archimedeprojects.arihna.alarm.TRIGGER_AT_EPOCH_MILLIS"
    const val EXTRA_OCCURRENCE_TOKEN = "com.archimedeprojects.arihna.alarm.OCCURRENCE_TOKEN"

    fun decode(intent: Intent): AlarmOccurrenceEnvelope? {
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)?.takeIf { it.isNotBlank() } ?: return null
        val revision = intent.getLongExtra(EXTRA_RULE_REVISION, Long.MIN_VALUE)
        if (revision <= 0L) return null
        val triggerAtMillis = intent.getLongExtra(EXTRA_TRIGGER_AT_EPOCH_MILLIS, Long.MIN_VALUE)
        if (triggerAtMillis == Long.MIN_VALUE) return null
        val token = intent.getStringExtra(EXTRA_OCCURRENCE_TOKEN)?.takeIf { it.isNotBlank() } ?: return null
        return AlarmOccurrenceEnvelope(
            alarmId = alarmId,
            ruleRevision = revision,
            triggerAt = Instant.ofEpochMilli(triggerAtMillis),
            occurrenceToken = token,
        )
    }
}

object AlarmOccurrenceEnvelopeValidator {
    fun isCurrent(envelope: AlarmOccurrenceEnvelope, rule: AlarmRule?): Boolean {
        if (rule == null || !rule.enabled) return false
        if (rule.alarmId != envelope.alarmId || rule.revision != envelope.ruleRevision) return false
        val expected = AlarmOccurrenceTokens.create(
            envelope.alarmId,
            envelope.ruleRevision,
            envelope.triggerAt,
        )
        return expected == envelope.occurrenceToken
    }
}
