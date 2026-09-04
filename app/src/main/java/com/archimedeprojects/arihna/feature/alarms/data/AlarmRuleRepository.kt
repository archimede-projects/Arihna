package com.archimedeprojects.arihna.feature.alarms.data

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRuleDraft
import kotlinx.coroutines.flow.Flow

interface AlarmRuleRepository {
    val rules: Flow<List<AlarmRule>>

    suspend fun getAll(): List<AlarmRule>

    suspend fun get(alarmId: String): AlarmRule?

    /**
     * Creates or replaces a rule. The repository owns the persisted revision and increments it
     * monotonically whenever the rule actually changes.
     */
    suspend fun save(draft: AlarmRuleDraft): AlarmRule

    suspend fun setEnabled(alarmId: String, enabled: Boolean): AlarmRule?

    suspend fun delete(alarmId: String): AlarmRule?

    /**
     * Auto-disables a one-shot custom alarm only when the delivered occurrence belongs to the
     * current persisted revision. A stale occurrence therefore cannot disable a newer rule.
     */
    suspend fun disableOneShotAfterValidatedDelivery(
        alarmId: String,
        expectedRevision: Long,
    ): AlarmRule?
}
