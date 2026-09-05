package com.archimedeprojects.arihna.feature.alarms.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

enum class AlarmPrayer {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
}

enum class AlarmSoundProfile {
    ADHAN,
    SYSTEM_DEFAULT,
    SILENT,
}

sealed interface AlarmDefinition {
    data class PrayerLinked(
        val prayer: AlarmPrayer,
        val offsetMinutes: Int = 0,
    ) : AlarmDefinition

    data class Custom(
        val label: String,
        val localTime: LocalTime,
        val weekdays: Set<DayOfWeek> = emptySet(),
    ) : AlarmDefinition {
        init {
            require(label.isNotBlank()) { "Custom alarm label must not be blank" }
        }
    }
}

data class AlarmRule(
    val alarmId: String,
    val revision: Long,
    val enabled: Boolean,
    val soundProfile: AlarmSoundProfile,
    val definition: AlarmDefinition,
) {
    init {
        require(alarmId.isNotBlank()) { "alarmId must not be blank" }
        require(revision > 0L) { "revision must be positive" }
    }

    val isOneShotCustom: Boolean
        get() = definition is AlarmDefinition.Custom && definition.weekdays.isEmpty()
}

data class AlarmRuleDraft(
    val alarmId: String,
    val enabled: Boolean,
    val soundProfile: AlarmSoundProfile,
    val definition: AlarmDefinition,
) {
    init {
        require(alarmId.isNotBlank()) { "alarmId must not be blank" }
    }
}

data class AlarmOccurrence(
    val alarmId: String,
    val ruleRevision: Long,
    val triggerAt: Instant,
    val occurrenceToken: String,
)
