package com.archimedeprojects.arihna.feature.alarms.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object AlarmOccurrenceResolver {
    fun resolveCustom(
        rule: AlarmRule,
        now: Instant,
        deviceZoneId: ZoneId,
    ): AlarmOccurrence? {
        if (!rule.enabled) return null
        val definition = rule.definition as? AlarmDefinition.Custom ?: return null
        val startDate = now.atZone(deviceZoneId).toLocalDate()
        val maxDaysAhead = if (definition.weekdays.isEmpty()) 1 else 7

        for (daysAhead in 0..maxDaysAhead) {
            val date = startDate.plusDays(daysAhead.toLong())
            if (definition.weekdays.isNotEmpty() && date.dayOfWeek !in definition.weekdays) continue

            val triggerAt = resolveLocalDateTime(
                LocalDateTime.of(date, definition.localTime),
                deviceZoneId,
            )
            if (triggerAt.isAfter(now)) {
                return occurrence(rule, triggerAt)
            }
        }
        return null
    }

    fun resolvePrayer(
        rule: AlarmRule,
        prayerScheduleInstants: Iterable<Instant>,
        now: Instant,
    ): AlarmOccurrence? {
        if (!rule.enabled) return null
        val definition = rule.definition as? AlarmDefinition.PrayerLinked ?: return null
        val triggerAt = prayerScheduleInstants
            .asSequence()
            .map { it.plusSeconds(definition.offsetMinutes.toLong() * 60L) }
            .filter { it.isAfter(now) }
            .minOrNull()
            ?: return null
        return occurrence(rule, triggerAt)
    }

    internal fun resolveLocalDateTime(localDateTime: LocalDateTime, zoneId: ZoneId): Instant {
        val rules = zoneId.rules
        val validOffsets = rules.getValidOffsets(localDateTime)
        return when (validOffsets.size) {
            0 -> rules.getTransition(localDateTime)?.instant
                ?: error("Missing zone transition for local-time gap: $localDateTime $zoneId")
            1 -> localDateTime.toInstant(validOffsets.single())
            else -> localDateTime.toInstant(validOffsets.first())
        }
    }

    private fun occurrence(rule: AlarmRule, triggerAt: Instant) = AlarmOccurrence(
        alarmId = rule.alarmId,
        ruleRevision = rule.revision,
        triggerAt = triggerAt,
        occurrenceToken = AlarmOccurrenceTokens.create(rule.alarmId, rule.revision, triggerAt),
    )
}

object AlarmOccurrenceTokens {
    fun create(alarmId: String, revision: Long, triggerAt: Instant): String {
        val payload = "$alarmId|$revision|${triggerAt.toEpochMilli()}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
