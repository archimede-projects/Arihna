package com.archimedeprojects.arihna.feature.alarms.platform

enum class AlarmReconciliationReason {
    BOOT_COMPLETED,
    WALL_CLOCK_CHANGED,
    TIMEZONE_CHANGED,
    EXACT_ALARM_ACCESS_CHANGED,
    APP_REPLACED,
}

fun interface AlarmReconciliationTrigger {
    suspend fun request(reason: AlarmReconciliationReason)
}

object NoOpAlarmReconciliationTrigger : AlarmReconciliationTrigger {
    override suspend fun request(reason: AlarmReconciliationReason) = Unit
}
