package com.archimedeprojects.arihna.feature.planning

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class PlanningType { TASK, REMINDER, ALARM }

data class PlanningItem(
    val id: String,
    val revision: Long,
    val type: PlanningType,
    val title: String,
    val note: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val complete: Boolean = false,
    val delivered: Boolean = false,
    val sound: AlarmSoundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
    val volume: Int = 100,
) {
    val pending: Boolean get() = !complete && !delivered
}

object PlanningTime {
    /** Wall-clock date/time follows the phone's current timezone, including after timezone changes. */
    fun instant(date: LocalDate, time: LocalTime, zone: ZoneId): Instant {
        val local = LocalDateTime.of(date, time)
        val rules = zone.rules
        val offsets = rules.getValidOffsets(local)
        return when (offsets.size) {
            0 -> requireNotNull(rules.getTransition(local)).instant
            1 -> local.toInstant(offsets.single())
            else -> local.toInstant(offsets.first())
        }
    }

    fun validFuture(date: LocalDate, time: LocalTime, now: Instant, zone: ZoneId): Boolean =
        instant(date, time, zone).isAfter(now)
}

/** Versioned, strict, independent codec; legacy alarm V1/V2/V3 is never touched. */
object PlanningCodec {
    private const val VERSION = "1"
    private fun enc(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun dec(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    fun encode(items: List<PlanningItem>): String = items.joinToString("\n") { item ->
        listOf(
            VERSION, enc(item.id), item.revision.toString(), item.type.name, enc(item.title),
            enc(item.note), item.date?.toString().orEmpty(), item.time?.toString().orEmpty(),
            item.complete.toString(), item.delivered.toString(), item.sound.name,
            enc(item.ringtoneUri.orEmpty()), enc(item.ringtoneTitle.orEmpty()), item.volume.toString(),
        ).joinToString("|")
    }

    fun decode(data: String): List<PlanningItem> = data.lineSequence().filter { it.isNotBlank() }
        .mapNotNull { line ->
            runCatching {
                val a = line.split("|")
                require(a.size == 14 && a[0] == VERSION)
                val id = dec(a[1])
                val title = dec(a[4])
                val revision = a[2].toLong()
                val volume = a[13].toInt()
                require(id.isNotBlank() && title.isNotBlank() && revision > 0 && volume in 0..100)
                val type = PlanningType.valueOf(a[3])
                val date = a[6].takeIf(String::isNotBlank)?.let(LocalDate::parse)
                val time = a[7].takeIf(String::isNotBlank)?.let(LocalTime::parse)
                require(type == PlanningType.TASK || date != null && time != null)
                PlanningItem(
                    id = id, revision = revision, type = type, title = title,
                    note = dec(a[5]), date = date, time = time,
                    complete = a[8].toBooleanStrict(), delivered = a[9].toBooleanStrict(),
                    sound = AlarmSoundProfile.valueOf(a[10]),
                    ringtoneUri = dec(a[11]).ifBlank { null },
                    ringtoneTitle = dec(a[12]).ifBlank { null },
                    volume = volume,
                )
            }.getOrNull()
        }.toList()
}

class PlanningRepository(private val store: DataStore<Preferences>) {
    companion object { private val KEY = stringPreferencesKey("arihna_planning_items_v1") }
    val entries: Flow<List<PlanningItem>> = store.data.map { PlanningCodec.decode(it[KEY].orEmpty()) }
    suspend fun all(): List<PlanningItem> = entries.first()
    suspend fun find(id: String): PlanningItem? = all().firstOrNull { it.id == id }

    suspend fun save(
        type: PlanningType, title: String, note: String, date: LocalDate?, time: LocalTime?,
        sound: AlarmSoundProfile, ringtoneUri: String?, ringtoneTitle: String?, volume: Int,
        id: String? = null,
    ): PlanningItem {
        require(title.isNotBlank())
        require(type == PlanningType.TASK || date != null && time != null)
        require(volume in 0..100)
        val itemId = id ?: "planning-${UUID.randomUUID()}"
        var result: PlanningItem? = null
        store.edit { prefs ->
            val old = PlanningCodec.decode(prefs[KEY].orEmpty())
            val existing = old.firstOrNull { it.id == itemId }
            val updated = PlanningItem(
                id = itemId, revision = (existing?.revision ?: 0) + 1,
                type = type, title = title.trim(), note = note.trim(),
                date = date, time = time, sound = sound, ringtoneUri = ringtoneUri,
                ringtoneTitle = ringtoneTitle, volume = volume,
            )
            prefs[KEY] = PlanningCodec.encode(old.filterNot { it.id == itemId } + updated)
            result = updated
        }
        return requireNotNull(result)
    }

    suspend fun markDelivered(id: String, revision: Long): Boolean = change(id) {
        if (it.revision != revision || !it.pending) null else it.copy(delivered = true)
    }

    suspend fun setCompleted(id: String, completed: Boolean): Boolean = change(id) {
        if (it.type != PlanningType.TASK && !completed) null
        else it.copy(complete = completed, revision = it.revision + 1)
    }

    suspend fun delete(id: String) {
        store.edit { prefs ->
            val existing = PlanningCodec.decode(prefs[KEY].orEmpty())
            prefs[KEY] = PlanningCodec.encode(existing.filterNot { it.id == id })
        }
    }

    private suspend fun change(id: String, transform: (PlanningItem) -> PlanningItem?): Boolean {
        var changed = false
        store.edit { prefs ->
            val items = PlanningCodec.decode(prefs[KEY].orEmpty())
            val current = items.firstOrNull { it.id == id } ?: return@edit
            val next = transform(current) ?: return@edit
            prefs[KEY] = PlanningCodec.encode(items.map { if (it.id == id) next else it })
            changed = true
        }
        return changed
    }
}
