package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence

enum class ExactAlarmCapability {
    READY,
    NEEDS_EXACT_ALARM_ACCESS,
}

sealed interface AlarmScheduleResult {
    data class Scheduled(val occurrence: AlarmOccurrence) : AlarmScheduleResult
    data object NeedsExactAlarmAccess : AlarmScheduleResult
}

interface AlarmPlatformScheduler {
    fun capability(): ExactAlarmCapability
    fun scheduleExact(occurrence: AlarmOccurrence): AlarmScheduleResult
    fun cancel(alarmId: String)
}

interface ExactAlarmBackend {
    fun hasExactAlarmAccess(): Boolean
    fun scheduleRtcWakeupExact(occurrence: AlarmOccurrence)
    fun cancel(alarmId: String)
}

class DefaultAlarmPlatformScheduler(
    private val backend: ExactAlarmBackend,
) : AlarmPlatformScheduler {
    override fun capability(): ExactAlarmCapability =
        if (backend.hasExactAlarmAccess()) {
            ExactAlarmCapability.READY
        } else {
            ExactAlarmCapability.NEEDS_EXACT_ALARM_ACCESS
        }

    override fun scheduleExact(occurrence: AlarmOccurrence): AlarmScheduleResult {
        if (!backend.hasExactAlarmAccess()) {
            return AlarmScheduleResult.NeedsExactAlarmAccess
        }
        backend.scheduleRtcWakeupExact(occurrence)
        return AlarmScheduleResult.Scheduled(occurrence)
    }

    override fun cancel(alarmId: String) {
        backend.cancel(alarmId)
    }
}
