from pathlib import Path


def write(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"missing patch marker in {path}: {old[:80]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/domain/AlarmModels.kt", r'''package com.archimedeprojects.arihna.feature.alarms.domain

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
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
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
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
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
''')

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/data/preferences/AlarmRulePreferencesCodec.kt", r'''package com.archimedeprojects.arihna.feature.alarms.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Base64

internal object AlarmRulePreferencesCodec {
    internal val rulesKey = stringPreferencesKey("alarm.rules.v1")

    private const val HEADER_V1 = "ARIHNA_ALARMS_V1"
    private const val HEADER_V2 = "ARIHNA_ALARMS_V2"
    private const val NONE = "-"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun decode(preferences: Preferences): List<AlarmRule> = decode(preferences[rulesKey])

    fun decode(encoded: String?): List<AlarmRule> {
        if (encoded == null) return emptyList()
        val lines = encoded.split('\n')
        val version = when (lines.firstOrNull()) {
            HEADER_V1 -> 1
            HEADER_V2 -> 2
            else -> throw AlarmRulesPersistenceException("Unsupported or malformed alarm rules version")
        }
        if (lines.size == 1) return emptyList()

        val rules = lines.drop(1).filter { it.isNotEmpty() }.map { decodeRow(it, version) }
        if (rules.map { it.alarmId }.toSet().size != rules.size) {
            throw AlarmRulesPersistenceException("Duplicate alarm id in persisted rules")
        }
        return rules.sortedBy { it.alarmId }
    }

    fun encode(rules: List<AlarmRule>): String {
        if (rules.map { it.alarmId }.toSet().size != rules.size) {
            throw AlarmRulesPersistenceException("Duplicate alarm id cannot be persisted")
        }
        val rows = rules.sortedBy { it.alarmId }.map(::encodeRowV2)
        return buildString {
            append(HEADER_V2)
            rows.forEach { row ->
                append('\n')
                append(row)
            }
        }
    }

    fun write(preferences: MutablePreferences, rules: List<AlarmRule>) {
        preferences[rulesKey] = encode(rules)
    }

    private fun encodeRowV2(rule: AlarmRule): String = when (val definition = rule.definition) {
        is AlarmDefinition.PrayerLinked -> listOf(
            "P",
            encodeText(rule.alarmId),
            rule.revision.toString(),
            encodeBoolean(rule.enabled),
            rule.soundProfile.name,
            definition.prayer.name,
            definition.offsetMinutes.toString(),
            encodeOptionalText(rule.ringtoneUri),
            encodeOptionalText(rule.ringtoneTitle),
        ).joinToString("|")

        is AlarmDefinition.Custom -> listOf(
            "C",
            encodeText(rule.alarmId),
            rule.revision.toString(),
            encodeBoolean(rule.enabled),
            rule.soundProfile.name,
            encodeText(definition.label),
            definition.localTime.toString(),
            definition.weekdays
                .sortedBy { it.value }
                .joinToString(",") { it.value.toString() }
                .ifEmpty { NONE },
            encodeOptionalText(rule.ringtoneUri),
            encodeOptionalText(rule.ringtoneTitle),
        ).joinToString("|")
    }

    private fun decodeRow(row: String, version: Int): AlarmRule {
        val fields = row.split('|')
        return try {
            when (fields.firstOrNull()) {
                "P" -> decodePrayerRow(fields, version)
                "C" -> decodeCustomRow(fields, version)
                else -> throw AlarmRulesPersistenceException("Unknown alarm rule type")
            }
        } catch (exception: AlarmRulesPersistenceException) {
            throw exception
        } catch (exception: Exception) {
            throw AlarmRulesPersistenceException("Malformed persisted alarm rule", exception)
        }
    }

    private fun decodePrayerRow(fields: List<String>, version: Int): AlarmRule {
        val expected = if (version == 1) 7 else 9
        if (fields.size != expected) throw AlarmRulesPersistenceException("Malformed prayer alarm row")
        return AlarmRule(
            alarmId = decodeText(fields[1]),
            revision = fields[2].toLong(),
            enabled = decodeBoolean(fields[3]),
            soundProfile = AlarmSoundProfile.valueOf(fields[4]),
            definition = AlarmDefinition.PrayerLinked(
                prayer = AlarmPrayer.valueOf(fields[5]),
                offsetMinutes = fields[6].toInt(),
            ),
            ringtoneUri = if (version == 2) decodeOptionalText(fields[7]) else null,
            ringtoneTitle = if (version == 2) decodeOptionalText(fields[8]) else null,
        )
    }

    private fun decodeCustomRow(fields: List<String>, version: Int): AlarmRule {
        val expected = if (version == 1) 8 else 10
        if (fields.size != expected) throw AlarmRulesPersistenceException("Malformed custom alarm row")
        return AlarmRule(
            alarmId = decodeText(fields[1]),
            revision = fields[2].toLong(),
            enabled = decodeBoolean(fields[3]),
            soundProfile = AlarmSoundProfile.valueOf(fields[4]),
            definition = AlarmDefinition.Custom(
                label = decodeText(fields[5]),
                localTime = LocalTime.parse(fields[6]),
                weekdays = decodeWeekdays(fields[7]),
            ),
            ringtoneUri = if (version == 2) decodeOptionalText(fields[8]) else null,
            ringtoneTitle = if (version == 2) decodeOptionalText(fields[9]) else null,
        )
    }

    private fun encodeText(value: String): String = encoder.encodeToString(
        value.toByteArray(StandardCharsets.UTF_8),
    )

    private fun decodeText(value: String): String = String(
        decoder.decode(value),
        StandardCharsets.UTF_8,
    )

    private fun encodeOptionalText(value: String?): String = value?.let(::encodeText) ?: NONE
    private fun decodeOptionalText(value: String): String? = if (value == NONE) null else decodeText(value)

    private fun encodeBoolean(value: Boolean): String = if (value) "1" else "0"

    private fun decodeBoolean(value: String): Boolean = when (value) {
        "1" -> true
        "0" -> false
        else -> throw AlarmRulesPersistenceException("Invalid persisted boolean")
    }

    private fun decodeWeekdays(value: String): Set<DayOfWeek> {
        if (value == NONE) return emptySet()
        if (value.isBlank()) throw AlarmRulesPersistenceException("Malformed weekday set")
        return value.split(',').mapTo(linkedSetOf()) { encodedDay ->
            DayOfWeek.of(encodedDay.toInt())
        }
    }
}

class AlarmRulesPersistenceException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
''')

replace_once(
    "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/data/preferences/PreferencesDataStoreAlarmRuleRepository.kt",
    '''                it.enabled == draft.enabled &&\n                    it.soundProfile == draft.soundProfile &&\n                    it.definition == draft.definition\n''',
    '''                it.enabled == draft.enabled &&\n                    it.soundProfile == draft.soundProfile &&\n                    it.definition == draft.definition &&\n                    it.ringtoneUri == draft.ringtoneUri &&\n                    it.ringtoneTitle == draft.ringtoneTitle\n''',
)
replace_once(
    "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/data/preferences/PreferencesDataStoreAlarmRuleRepository.kt",
    '''                soundProfile = draft.soundProfile,\n                definition = draft.definition,\n            )\n''',
    '''                soundProfile = draft.soundProfile,\n                definition = draft.definition,\n                ringtoneUri = draft.ringtoneUri,\n                ringtoneTitle = draft.ringtoneTitle,\n            )\n''',
)

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/AlarmsViewModel.kt", r'''package com.archimedeprojects.arihna.feature.alarms

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
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    fun saveCustom(
        existing: AlarmRule?,
        label: String,
        localTime: LocalTime,
        weekdays: Set<DayOfWeek>,
        soundProfile: AlarmSoundProfile,
        ringtoneUri: String?,
        ringtoneTitle: String?,
    ) {
        if (label.isBlank()) {
            message.value = "Inserisci un nome per la sveglia"
            return
        }
        if (existing != null && existing.definition !is AlarmDefinition.Custom) {
            message.value = "La regola selezionata non è una sveglia personale"
            return
        }
        mutate {
            val id = existing?.alarmId ?: "custom-${UUID.randomUUID()}"
            repository.save(
                AlarmRuleDraft(
                    alarmId = id,
                    enabled = existing?.enabled ?: true,
                    soundProfile = soundProfile,
                    definition = AlarmDefinition.Custom(
                        label = label.trim(),
                        localTime = localTime.withSecond(0).withNano(0),
                        weekdays = weekdays,
                    ),
                    ringtoneUri = if (soundProfile == AlarmSoundProfile.SYSTEM_DEFAULT) ringtoneUri else null,
                    ringtoneTitle = if (soundProfile == AlarmSoundProfile.SYSTEM_DEFAULT) ringtoneTitle else null,
                ),
            )
            if (existing != null) {
                scheduler.cancel(id)
                message.value = "Sveglia aggiornata"
            } else {
                message.value = "Sveglia salvata"
            }
        }
    }

    fun toggle(rule: AlarmRule) {
        mutate { repository.setEnabled(rule.alarmId, !rule.enabled) }
    }

    fun delete(rule: AlarmRule) {
        mutate {
            repository.delete(rule.alarmId)
            scheduler.cancel(rule.alarmId)
        }
    }

    fun setSound(
        rule: AlarmRule,
        soundProfile: AlarmSoundProfile,
        ringtoneUri: String? = null,
        ringtoneTitle: String? = null,
    ) {
        val targetUri = if (soundProfile == AlarmSoundProfile.SYSTEM_DEFAULT) ringtoneUri else null
        val targetTitle = if (soundProfile == AlarmSoundProfile.SYSTEM_DEFAULT) ringtoneTitle else null
        if (
            rule.soundProfile == soundProfile &&
            rule.ringtoneUri == targetUri &&
            rule.ringtoneTitle == targetTitle
        ) return
        mutate {
            repository.save(
                AlarmRuleDraft(
                    alarmId = rule.alarmId,
                    enabled = rule.enabled,
                    soundProfile = soundProfile,
                    definition = rule.definition,
                    ringtoneUri = targetUri,
                    ringtoneTitle = targetTitle,
                ),
            )
            scheduler.cancel(rule.alarmId)
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
''')

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmRingtonePicker.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri

object AlarmRingtonePicker {
    fun createIntent(existingUri: String? = null): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            existingUri?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
            }
        }

    @Suppress("DEPRECATION")
    fun pickedUri(data: Intent?): Uri? =
        data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

    fun title(context: Context, uri: Uri): String? = runCatching {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
''')

write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmDiagnosticTestScheduler.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

enum class AlarmDiagnosticKind {
    SYSTEM_ALARM,
    ADHAN,
}

enum class AlarmDiagnosticScheduleResult {
    SCHEDULED,
    NEEDS_NOTIFICATION_PERMISSION,
    NEEDS_EXACT_ALARM_ACCESS,
    NEEDS_FULL_SCREEN_ACCESS,
}

class AlarmDiagnosticTestScheduler(
    context: Context,
    private val notificationPermissionReader: AlarmNotificationPermissionReader,
    private val fullScreenAccess: AlarmFullScreenAccess,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleOneMinute(kind: AlarmDiagnosticKind): AlarmDiagnosticScheduleResult {
        if (!notificationPermissionReader.isGranted()) {
            return AlarmDiagnosticScheduleResult.NEEDS_NOTIFICATION_PERMISSION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return AlarmDiagnosticScheduleResult.NEEDS_EXACT_ALARM_ACCESS
        }
        if (!fullScreenAccess.isGranted()) {
            return AlarmDiagnosticScheduleResult.NEEDS_FULL_SCREEN_ACCESS
        }
        cancel()
        val triggerAt = System.currentTimeMillis() + TEST_DELAY_MILLIS
        val token = "diagnostic-${UUID.randomUUID()}"
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(kind, triggerAt, token),
        )
        return AlarmDiagnosticScheduleResult.SCHEDULED
    }

    fun cancel() {
        AlarmDiagnosticKind.entries.forEach { kind ->
            val operation = pendingIntent(kind, 0L, "cancel")
            alarmManager.cancel(operation)
            operation.cancel()
        }
    }

    private fun pendingIntent(
        kind: AlarmDiagnosticKind,
        triggerAt: Long,
        token: String,
    ): PendingIntent {
        val intent = Intent(appContext, AlarmDiagnosticReceiver::class.java).apply {
            data = Uri.Builder()
                .scheme("arihna")
                .authority("alarm-diagnostic")
                .appendPath(kind.name.lowercase())
                .build()
            putExtra(EXTRA_KIND, kind.name)
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
            putExtra(EXTRA_TOKEN, token)
        }
        return PendingIntent.getBroadcast(
            appContext,
            kind.ordinal + 7000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val TEST_DELAY_MILLIS = 60_000L
        internal const val EXTRA_KIND = "arihna.diagnostic.kind"
        internal const val EXTRA_TRIGGER_AT = "arihna.diagnostic.trigger_at"
        internal const val EXTRA_TOKEN = "arihna.diagnostic.token"
    }
}

class AlarmDiagnosticReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return
        val kind = runCatching {
            AlarmDiagnosticKind.valueOf(intent.getStringExtra(AlarmDiagnosticTestScheduler.EXTRA_KIND).orEmpty())
        }.getOrNull() ?: return
        val triggerAt = intent.getLongExtra(AlarmDiagnosticTestScheduler.EXTRA_TRIGGER_AT, 0L)
        val token = intent.getStringExtra(AlarmDiagnosticTestScheduler.EXTRA_TOKEN)?.takeIf { it.isNotBlank() } ?: return
        if (triggerAt <= 0L) return

        val profile = when (kind) {
            AlarmDiagnosticKind.SYSTEM_ALARM -> AlarmSoundProfile.SYSTEM_DEFAULT
            AlarmDiagnosticKind.ADHAN -> AlarmSoundProfile.ADHAN
        }
        val id = when (kind) {
            AlarmDiagnosticKind.SYSTEM_ALARM -> "diagnostic-system-alarm"
            AlarmDiagnosticKind.ADHAN -> "diagnostic-adhan"
        }
        val title = when (kind) {
            AlarmDiagnosticKind.SYSTEM_ALARM -> "Test sveglia"
            AlarmDiagnosticKind.ADHAN -> "Test Adhan"
        }
        val rule = AlarmRule(
            alarmId = id,
            revision = 1L,
            enabled = true,
            soundProfile = profile,
            definition = AlarmDefinition.Custom(
                label = title,
                localTime = LocalTime.now().withSecond(0).withNano(0),
            ),
        )
        val occurrence = AlarmOccurrence(
            alarmId = id,
            ruleRevision = 1L,
            triggerAt = Instant.ofEpochMilli(triggerAt),
            occurrenceToken = token,
        )
        AndroidAlarmNotificationDelivery(
            context = context.applicationContext,
            permissionReader = AndroidAlarmNotificationPermissionReader(context.applicationContext),
        ).deliver(rule, occurrence)
    }
}
''')

# Make notification posting explicit before foreground ringing so Android can process the fullScreenIntent.
write("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmNotifications.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule

fun interface AlarmNotificationPermissionReader {
    fun isGranted(): Boolean
}

class AndroidAlarmNotificationPermissionReader(
    context: Context,
) : AlarmNotificationPermissionReader {
    private val appContext = context.applicationContext

    override fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}

enum class AlarmNotificationDeliveryResult {
    DELIVERED,
    NEEDS_NOTIFICATION_PERMISSION,
}

interface AlarmNotificationDelivery {
    fun ensureChannels()
    fun deliver(rule: AlarmRule, occurrence: AlarmOccurrence): AlarmNotificationDeliveryResult
}

fun interface AlarmRingingStarter {
    fun start(rule: AlarmRule, occurrence: AlarmOccurrence)
}

fun interface AlarmNotificationPoster {
    fun post(rule: AlarmRule, occurrence: AlarmOccurrence)
}

class AndroidAlarmRingingStarter(context: Context) : AlarmRingingStarter {
    private val appContext = context.applicationContext

    override fun start(rule: AlarmRule, occurrence: AlarmOccurrence) {
        ContextCompat.startForegroundService(
            appContext,
            AlarmRingingService.ringIntent(appContext, rule, occurrence),
        )
    }
}

class AndroidAlarmNotificationPoster(context: Context) : AlarmNotificationPoster {
    private val appContext = context.applicationContext

    override fun post(rule: AlarmRule, occurrence: AlarmOccurrence) {
        val payload = createAlarmRingingPayload(rule, occurrence)
        val notification = AlarmRingingNotificationFactory.build(appContext, payload)
        NotificationManagerCompat.from(appContext).notify(
            AlarmRingingNotificationFactory.notificationId(rule.alarmId),
            notification,
        )
    }
}

class AndroidAlarmNotificationDelivery(
    context: Context,
    private val permissionReader: AlarmNotificationPermissionReader,
    private val ringingStarter: AlarmRingingStarter = AndroidAlarmRingingStarter(context),
    private val notificationPoster: AlarmNotificationPoster = AndroidAlarmNotificationPoster(context),
) : AlarmNotificationDelivery {
    private val appContext = context.applicationContext

    override fun ensureChannels() {
        AlarmRingingNotificationFactory.ensureChannels(appContext)
    }

    override fun deliver(
        rule: AlarmRule,
        occurrence: AlarmOccurrence,
    ): AlarmNotificationDeliveryResult {
        if (!permissionReader.isGranted()) {
            return AlarmNotificationDeliveryResult.NEEDS_NOTIFICATION_PERMISSION
        }
        ensureChannels()
        notificationPoster.post(rule, occurrence)
        ringingStarter.start(rule, occurrence)
        return AlarmNotificationDeliveryResult.DELIVERED
    }

    companion object {
        const val CHANNEL_PRAYER = AlarmRingingNotificationFactory.CHANNEL_PRAYER
        const val CHANNEL_CUSTOM = AlarmRingingNotificationFactory.CHANNEL_CUSTOM

        fun notificationId(alarmId: String): Int =
            AlarmRingingNotificationFactory.notificationId(alarmId)
    }
}
''')

# Patch ringing payload/channel/audio without changing the approved ringing screen behavior.
service = Path("app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmRingingService.kt")
text = service.read_text(encoding="utf-8")
text = text.replace(
'''internal data class AlarmRingingPayload(\n    val alarmId: String,\n    val title: String,\n    val soundProfile: AlarmSoundProfile,\n    val isPrayer: Boolean,\n    val occurrenceToken: String,\n)\n''',
'''internal data class AlarmRingingPayload(\n    val alarmId: String,\n    val title: String,\n    val soundProfile: AlarmSoundProfile,\n    val isPrayer: Boolean,\n    val occurrenceToken: String,\n    val ringtoneUri: String? = null,\n    val ringtoneTitle: String? = null,\n)\n\ninternal fun createAlarmRingingPayload(\n    rule: AlarmRule,\n    occurrence: AlarmOccurrence,\n): AlarmRingingPayload = AlarmRingingPayload(\n    alarmId = rule.alarmId,\n    title = when (val definition = rule.definition) {\n        is AlarmDefinition.PrayerLinked -> definition.prayer.displayName()\n        is AlarmDefinition.Custom -> definition.label\n    },\n    soundProfile = rule.soundProfile,\n    isPrayer = rule.definition is AlarmDefinition.PrayerLinked,\n    occurrenceToken = occurrence.occurrenceToken,\n    ringtoneUri = rule.ringtoneUri,\n    ringtoneTitle = rule.ringtoneTitle,\n)\n''')
text = text.replace(
'''    const val EXTRA_OCCURRENCE_TOKEN = "arihna.ringing.occurrence_token"\n''',
'''    const val EXTRA_OCCURRENCE_TOKEN = "arihna.ringing.occurrence_token"\n    const val EXTRA_RINGTONE_URI = "arihna.ringing.ringtone_uri"\n    const val EXTRA_RINGTONE_TITLE = "arihna.ringing.ringtone_title"\n''')
text = text.replace(
'''        putExtra(EXTRA_OCCURRENCE_TOKEN, payload.occurrenceToken)\n''',
'''        putExtra(EXTRA_OCCURRENCE_TOKEN, payload.occurrenceToken)\n        putExtra(EXTRA_RINGTONE_URI, payload.ringtoneUri)\n        putExtra(EXTRA_RINGTONE_TITLE, payload.ringtoneTitle)\n''')
text = text.replace(
'''            isPrayer = intent.getBooleanExtra(EXTRA_IS_PRAYER, false),\n            occurrenceToken = token,\n''',
'''            isPrayer = intent.getBooleanExtra(EXTRA_IS_PRAYER, false),\n            occurrenceToken = token,\n            ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI),\n            ringtoneTitle = intent.getStringExtra(EXTRA_RINGTONE_TITLE),\n''')
text = text.replace('const val CHANNEL_PRAYER = "arihna_prayer_alarm_v2"', 'const val CHANNEL_PRAYER = "arihna_prayer_alarm_v3"')
text = text.replace('const val CHANNEL_CUSTOM = "arihna_custom_alarm_v2"', 'const val CHANNEL_CUSTOM = "arihna_custom_alarm_v3"')
text = text.replace(
'''            AlarmSoundProfile.SYSTEM_DEFAULT -> "Sveglia in corso • tocca Stop per interrompere"\n''',
'''            AlarmSoundProfile.SYSTEM_DEFAULT -> payload.ringtoneTitle\n                ?.let { "$it • tocca Stop per interrompere" }\n                ?: "Sveglia in corso • tocca Stop per interrompere"\n''')
text = text.replace('        startAudio(payload.soundProfile)\n', '        startAudio(payload.soundProfile, payload.ringtoneUri)\n')
old_audio = '''    private fun startAudio(profile: AlarmSoundProfile) {\n        if (profile == AlarmSoundProfile.SILENT) return\n        val attributes = AudioAttributes.Builder()\n            .setUsage(AudioAttributes.USAGE_ALARM)\n            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)\n            .build()\n        val player = MediaPlayer()\n        try {\n            player.setAudioAttributes(attributes)\n            when (profile) {\n                AlarmSoundProfile.SYSTEM_DEFAULT -> {\n                    val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(\n                        this,\n                        RingtoneManager.TYPE_ALARM,\n                    ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)\n                    player.setDataSource(this, alarmUri)\n                    player.isLooping = true\n                }\n                AlarmSoundProfile.ADHAN -> {\n                    resources.openRawResourceFd(R.raw.adhan_cc0).use { descriptor ->\n                        player.setDataSource(\n                            descriptor.fileDescriptor,\n                            descriptor.startOffset,\n                            descriptor.length,\n                        )\n                    }\n                    player.isLooping = false\n                }\n                AlarmSoundProfile.SILENT -> Unit\n            }\n            player.prepare()\n            player.start()\n            if (profile == AlarmSoundProfile.ADHAN) {\n                player.setOnCompletionListener { completed ->\n                    if (mediaPlayer === completed) {\n                        mediaPlayer = null\n                    }\n                    completed.release()\n                }\n            }\n            mediaPlayer = player\n        } catch (throwable: Throwable) {\n            player.release()\n            mediaPlayer = null\n        }\n    }\n'''
new_audio = '''    private fun startAudio(profile: AlarmSoundProfile, selectedRingtoneUri: String?) {\n        if (profile == AlarmSoundProfile.SILENT) return\n        val attributes = AudioAttributes.Builder()\n            .setUsage(AudioAttributes.USAGE_ALARM)\n            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)\n            .build()\n\n        if (profile == AlarmSoundProfile.ADHAN) {\n            val player = MediaPlayer()\n            try {\n                player.setAudioAttributes(attributes)\n                resources.openRawResourceFd(R.raw.adhan_cc0).use { descriptor ->\n                    player.setDataSource(\n                        descriptor.fileDescriptor,\n                        descriptor.startOffset,\n                        descriptor.length,\n                    )\n                }\n                player.isLooping = false\n                player.prepare()\n                player.start()\n                player.setOnCompletionListener { completed ->\n                    if (mediaPlayer === completed) mediaPlayer = null\n                    completed.release()\n                }\n                mediaPlayer = player\n            } catch (throwable: Throwable) {\n                player.release()\n                mediaPlayer = null\n            }\n            return\n        }\n\n        val candidates = buildList<Uri> {\n            selectedRingtoneUri?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull()?.let(::add) }\n            RingtoneManager.getActualDefaultRingtoneUri(this@AlarmRingingService, RingtoneManager.TYPE_ALARM)?.let(::add)\n            add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))\n        }.distinct()\n        for (uri in candidates) {\n            val player = MediaPlayer()\n            val started = runCatching {\n                player.setAudioAttributes(attributes)\n                player.setDataSource(this, uri)\n                player.isLooping = true\n                player.prepare()\n                player.start()\n            }.isSuccess\n            if (started) {\n                mediaPlayer = player\n                return\n            }\n            player.release()\n        }\n        mediaPlayer = null\n    }\n'''
if old_audio not in text:
    raise SystemExit("missing old startAudio block")
text = text.replace(old_audio, new_audio, 1)
old_ring = '''        fun ringIntent(context: Context, rule: AlarmRule, occurrence: AlarmOccurrence): Intent {\n            val payload = AlarmRingingPayload(\n                alarmId = rule.alarmId,\n                title = titleFor(rule),\n                soundProfile = rule.soundProfile,\n                isPrayer = rule.definition is AlarmDefinition.PrayerLinked,\n                occurrenceToken = occurrence.occurrenceToken,\n            )\n            return AlarmRingingIntentContract.put(\n'''
new_ring = '''        fun ringIntent(context: Context, rule: AlarmRule, occurrence: AlarmOccurrence): Intent {\n            val payload = createAlarmRingingPayload(rule, occurrence)\n            return AlarmRingingIntentContract.put(\n'''
if old_ring not in text:
    raise SystemExit("missing ringIntent payload block")
text = text.replace(old_ring, new_ring, 1)
# titleFor is no longer needed.
text = text.replace('''\n        private fun titleFor(rule: AlarmRule): String = when (val definition = rule.definition) {\n            is AlarmDefinition.PrayerLinked -> definition.prayer.displayName()\n            is AlarmDefinition.Custom -> definition.label\n        }\n''', '\n')
service.write_text(text, encoding="utf-8")

# Add the isolated diagnostic receiver and declarative lock-screen flags. No new permission.
manifest = Path("app/src/main/AndroidManifest.xml")
text = manifest.read_text(encoding="utf-8")
text = text.replace(
'''            android:exported="false"\n            android:launchMode="singleTop"\n            android:theme="@style/Theme.Arihna" />\n''',
'''            android:exported="false"\n            android:launchMode="singleTop"\n            android:showWhenLocked="true"\n            android:turnScreenOn="true"\n            android:theme="@style/Theme.Arihna" />\n''',
1,
)
text = text.replace(
'''        <receiver\n            android:name=".feature.alarms.platform.AlarmOccurrenceReceiver"\n            android:exported="false" />\n''',
'''        <receiver\n            android:name=".feature.alarms.platform.AlarmOccurrenceReceiver"\n            android:exported="false" />\n\n        <receiver\n            android:name=".feature.alarms.platform.AlarmDiagnosticReceiver"\n            android:exported="false" />\n''',
1,
)
manifest.write_text(text, encoding="utf-8")
