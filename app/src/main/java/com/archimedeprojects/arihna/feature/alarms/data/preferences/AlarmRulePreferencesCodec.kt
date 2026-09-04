package com.archimedeprojects.arihna.feature.alarms.data.preferences

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

    private const val HEADER = "ARIHNA_ALARMS_V1"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun decode(preferences: Preferences): List<AlarmRule> = decode(preferences[rulesKey])

    fun decode(encoded: String?): List<AlarmRule> {
        if (encoded == null) return emptyList()
        val lines = encoded.split('\n')
        if (lines.firstOrNull() != HEADER) {
            throw AlarmRulesPersistenceException("Unsupported or malformed alarm rules version")
        }
        if (lines.size == 1) return emptyList()

        val rules = lines.drop(1).filter { it.isNotEmpty() }.map(::decodeRow)
        if (rules.map { it.alarmId }.toSet().size != rules.size) {
            throw AlarmRulesPersistenceException("Duplicate alarm id in persisted rules")
        }
        return rules.sortedBy { it.alarmId }
    }

    fun encode(rules: List<AlarmRule>): String {
        if (rules.map { it.alarmId }.toSet().size != rules.size) {
            throw AlarmRulesPersistenceException("Duplicate alarm id cannot be persisted")
        }
        val rows = rules.sortedBy { it.alarmId }.map(::encodeRow)
        return buildString {
            append(HEADER)
            rows.forEach { row ->
                append('\n')
                append(row)
            }
        }
    }

    fun write(preferences: MutablePreferences, rules: List<AlarmRule>) {
        preferences[rulesKey] = encode(rules)
    }

    private fun encodeRow(rule: AlarmRule): String = when (val definition = rule.definition) {
        is AlarmDefinition.PrayerLinked -> listOf(
            "P",
            encodeText(rule.alarmId),
            rule.revision.toString(),
            encodeBoolean(rule.enabled),
            rule.soundProfile.name,
            definition.prayer.name,
            definition.offsetMinutes.toString(),
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
                .ifEmpty { "-" },
        ).joinToString("|")
    }

    private fun decodeRow(row: String): AlarmRule {
        val fields = row.split('|')
        return try {
            when (fields.firstOrNull()) {
                "P" -> {
                    if (fields.size != 7) throw AlarmRulesPersistenceException("Malformed prayer alarm row")
                    AlarmRule(
                        alarmId = decodeText(fields[1]),
                        revision = fields[2].toLong(),
                        enabled = decodeBoolean(fields[3]),
                        soundProfile = AlarmSoundProfile.valueOf(fields[4]),
                        definition = AlarmDefinition.PrayerLinked(
                            prayer = AlarmPrayer.valueOf(fields[5]),
                            offsetMinutes = fields[6].toInt(),
                        ),
                    )
                }

                "C" -> {
                    if (fields.size != 8) throw AlarmRulesPersistenceException("Malformed custom alarm row")
                    AlarmRule(
                        alarmId = decodeText(fields[1]),
                        revision = fields[2].toLong(),
                        enabled = decodeBoolean(fields[3]),
                        soundProfile = AlarmSoundProfile.valueOf(fields[4]),
                        definition = AlarmDefinition.Custom(
                            label = decodeText(fields[5]),
                            localTime = LocalTime.parse(fields[6]),
                            weekdays = decodeWeekdays(fields[7]),
                        ),
                    )
                }

                else -> throw AlarmRulesPersistenceException("Unknown alarm rule type")
            }
        } catch (exception: AlarmRulesPersistenceException) {
            throw exception
        } catch (exception: Exception) {
            throw AlarmRulesPersistenceException("Malformed persisted alarm rule", exception)
        }
    }

    private fun encodeText(value: String): String = encoder.encodeToString(
        value.toByteArray(StandardCharsets.UTF_8),
    )

    private fun decodeText(value: String): String = String(
        decoder.decode(value),
        StandardCharsets.UTF_8,
    )

    private fun encodeBoolean(value: Boolean): String = if (value) "1" else "0"

    private fun decodeBoolean(value: String): Boolean = when (value) {
        "1" -> true
        "0" -> false
        else -> throw AlarmRulesPersistenceException("Invalid persisted boolean")
    }

    private fun decodeWeekdays(value: String): Set<DayOfWeek> {
        if (value == "-") return emptySet()
        if (value.isBlank()) throw AlarmRulesPersistenceException("Malformed weekday set")
        return value.split(',').mapTo(linkedSetOf()) { encodedDay ->
            DayOfWeek.of(encodedDay.toInt())
        }
    }
}

class AlarmRulesPersistenceException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
