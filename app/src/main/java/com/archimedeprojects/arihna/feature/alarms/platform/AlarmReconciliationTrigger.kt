package com.archimedeprojects.arihna.feature.alarms.platform

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmReconciler

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

class DefaultAlarmReconciliationTrigger(
    private val reconciler: AlarmReconciler,
) : AlarmReconciliationTrigger {
    override suspend fun request(reason: AlarmReconciliationReason) {
        reconciler.reconcile()
    }
}

object NoOpAlarmReconciliationTrigger : AlarmReconciliationTrigger {
    override suspend fun request(reason: AlarmReconciliationReason) = Unit
}
