package com.archimedeprojects.arihna.feature.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmReconciler
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRuleDraft
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmNotificationPermissionReader
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmCapability
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmsUiState(
    val rules: List<AlarmRule> = emptyList(),
    val exactAlarmReady: Boolean = false,
    val notificationReady: Boolean = false,
    val message: String? = null,
)

class AlarmsViewModel(
    private val repository: AlarmRuleRepository,
    private val reconciler: AlarmReconciler,
    private val scheduler: AlarmPlatformScheduler,
    private val notificationPermissionReader: AlarmNotificationPermissionReader,
) : ViewModel() {
    private val capabilityVersion = MutableStateFlow(0L)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AlarmsUiState> = combine(
        repository.rules,
        capabilityVersion,
        message,
    ) { rules, _, currentMessage ->
        AlarmsUiState(
            rules = rules.sortedBy { it.alarmId },
            exactAlarmReady = scheduler.capability() == ExactAlarmCapability.READY,
            notificationReady = notificationPermissionReader.isGranted(),
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlarmsUiState())

    fun refreshCapabilities() {
        capabilityVersion.update { it + 1 }
        reconcileNow()
    }

    fun setPrayerEnabled(prayer: AlarmPrayer, enabled: Boolean) {
        mutate {
            val id = prayerAlarmId(prayer)
            val existing = repository.get(id)
            if (existing == null) {
                repository.save(
                    AlarmRuleDraft(
                        alarmId = id,
                        enabled = enabled,
                        soundProfile = AlarmSoundProfile.ADHAN,
                        definition = AlarmDefinition.PrayerLinked(prayer = prayer, offsetMinutes = 0),
                    ),
                )
            } else {
                repository.setEnabled(id, enabled)
            }
        }
    }

    fun createCustom(label: String, timeText: String) {
        val parsed = runCatching { LocalTime.parse(timeText.trim()) }.getOrNull()
        if (label.isBlank() || parsed == null) {
            message.value = "Inserisci nome e ora in formato HH:mm"
            return
        }
        mutate {
            repository.save(
                AlarmRuleDraft(
                    alarmId = "custom-${UUID.randomUUID()}",
                    enabled = true,
                    soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                    definition = AlarmDefinition.Custom(
                        label = label.trim(),
                        localTime = parsed,
                    ),
                ),
            )
            message.value = "Sveglia salvata"
        }
    }

    fun toggle(rule: AlarmRule) {
        mutate { repository.setEnabled(rule.alarmId, !rule.enabled) }
    }

    fun delete(rule: AlarmRule) {
        mutate { repository.delete(rule.alarmId) }
    }

    fun setSound(rule: AlarmRule, soundProfile: AlarmSoundProfile) {
        if (rule.definition is AlarmDefinition.Custom && soundProfile == AlarmSoundProfile.ADHAN) {
            message.value = "Adhan è disponibile per le sveglie di preghiera"
            return
        }
        if (rule.soundProfile == soundProfile) return
        mutate {
            repository.save(
                AlarmRuleDraft(
                    alarmId = rule.alarmId,
                    enabled = rule.enabled,
                    soundProfile = soundProfile,
                    definition = rule.definition,
                ),
            )
            message.value = "Suono aggiornato"
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching {
                block()
                reconciler.reconcile()
            }.onFailure {
                message.value = "Impossibile aggiornare la sveglia"
            }
            capabilityVersion.update { it + 1 }
        }
    }

    private fun reconcileNow() {
        viewModelScope.launch {
            runCatching { reconciler.reconcile() }
            capabilityVersion.update { it + 1 }
        }
    }

    companion object {
        fun prayerAlarmId(prayer: AlarmPrayer): String = "prayer-${prayer.name.lowercase()}"
    }
}
