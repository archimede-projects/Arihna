package com.archimedeprojects.arihna.feature.alarms.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRuleDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesDataStoreAlarmRuleRepository(
    private val dataStore: DataStore<Preferences>,
) : AlarmRuleRepository {
    override val rules: Flow<List<AlarmRule>> = dataStore.data
        .map { preferences -> AlarmRulePreferencesCodec.decode(preferences) }
        .distinctUntilChanged()

    override suspend fun getAll(): List<AlarmRule> = rules.first()

    override suspend fun get(alarmId: String): AlarmRule? = getAll().firstOrNull { it.alarmId == alarmId }

    override suspend fun save(draft: AlarmRuleDraft): AlarmRule {
        var persistedRule: AlarmRule? = null
        dataStore.edit { preferences ->
            val current = AlarmRulePreferencesCodec.decode(preferences)
            val existing = current.firstOrNull { it.alarmId == draft.alarmId }
            val unchanged = existing?.let {
                it.enabled == draft.enabled &&
                    it.soundProfile == draft.soundProfile &&
                    it.definition == draft.definition
            } == true
            if (unchanged) {
                persistedRule = existing
                return@edit
            }

            val updated = AlarmRule(
                alarmId = draft.alarmId,
                revision = (existing?.revision ?: 0L) + 1L,
                enabled = draft.enabled,
                soundProfile = draft.soundProfile,
                definition = draft.definition,
            )
            persistedRule = updated
            AlarmRulePreferencesCodec.write(
                preferences,
                current.filterNot { it.alarmId == draft.alarmId } + updated,
            )
        }
        return requireNotNull(persistedRule)
    }

    override suspend fun setEnabled(alarmId: String, enabled: Boolean): AlarmRule? {
        var persistedRule: AlarmRule? = null
        dataStore.edit { preferences ->
            val current = AlarmRulePreferencesCodec.decode(preferences)
            val existing = current.firstOrNull { it.alarmId == alarmId } ?: return@edit
            if (existing.enabled == enabled) {
                persistedRule = existing
                return@edit
            }
            val updated = existing.copy(
                revision = existing.revision + 1L,
                enabled = enabled,
            )
            persistedRule = updated
            AlarmRulePreferencesCodec.write(
                preferences,
                current.map { rule -> if (rule.alarmId == alarmId) updated else rule },
            )
        }
        return persistedRule
    }

    override suspend fun delete(alarmId: String): AlarmRule? {
        var deleted: AlarmRule? = null
        dataStore.edit { preferences ->
            val current = AlarmRulePreferencesCodec.decode(preferences)
            val existing = current.firstOrNull { it.alarmId == alarmId } ?: return@edit
            deleted = existing
            AlarmRulePreferencesCodec.write(preferences, current.filterNot { it.alarmId == alarmId })
        }
        return deleted
    }

    override suspend fun disableOneShotAfterValidatedDelivery(
        alarmId: String,
        expectedRevision: Long,
    ): AlarmRule? {
        var persistedRule: AlarmRule? = null
        dataStore.edit { preferences ->
            val current = AlarmRulePreferencesCodec.decode(preferences)
            val existing = current.firstOrNull { it.alarmId == alarmId } ?: return@edit
            if (
                existing.revision != expectedRevision ||
                !existing.enabled ||
                !existing.isOneShotCustom
            ) {
                return@edit
            }
            val updated = existing.copy(
                revision = existing.revision + 1L,
                enabled = false,
            )
            persistedRule = updated
            AlarmRulePreferencesCodec.write(
                preferences,
                current.map { rule -> if (rule.alarmId == alarmId) updated else rule },
            )
        }
        return persistedRule
    }
}
