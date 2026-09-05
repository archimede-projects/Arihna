package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence

enum class AlarmOccurrenceHandlingResult {
    IGNORED_STALE_OR_INVALID,
    NEEDS_NOTIFICATION_PERMISSION,
    DELIVERED,
}

class AlarmOccurrenceHandler(
    private val ruleRepository: AlarmRuleRepository,
    private val notificationDelivery: AlarmNotificationDelivery,
    private val reconcileNow: suspend () -> Unit,
) {
    suspend fun handle(envelope: AlarmOccurrenceEnvelope): AlarmOccurrenceHandlingResult {
        val rule = ruleRepository.get(envelope.alarmId)
        if (!AlarmOccurrenceEnvelopeValidator.isCurrent(envelope, rule)) {
            return AlarmOccurrenceHandlingResult.IGNORED_STALE_OR_INVALID
        }
        rule ?: return AlarmOccurrenceHandlingResult.IGNORED_STALE_OR_INVALID

        val occurrence = AlarmOccurrence(
            alarmId = envelope.alarmId,
            ruleRevision = envelope.ruleRevision,
            triggerAt = envelope.triggerAt,
            occurrenceToken = envelope.occurrenceToken,
        )
        return when (notificationDelivery.deliver(rule, occurrence)) {
            AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION -> {
                reconcileNow()
                AlarmOccurrenceHandlingResult.NEEDS_NOTIFICATION_PERMISSION
            }

            AlarmNotificationDeliveryResult.DELIVERED -> {
                if (rule.isOneShotCustom) {
                    ruleRepository.disableOneShotAfterValidatedDelivery(
                        alarmId = rule.alarmId,
                        expectedRevision = rule.revision,
                    )
                }
                reconcileNow()
                AlarmOccurrenceHandlingResult.DELIVERED
            }
        }
    }
}
