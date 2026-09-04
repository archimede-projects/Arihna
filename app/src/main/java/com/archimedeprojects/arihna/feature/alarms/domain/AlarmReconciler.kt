package com.archimedeprojects.arihna.feature.alarms.domain

import com.archimedeprojects.arihna.core.prayer.calculation.PrayerTimeCalculator
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmScheduleResult
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleRepository
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerScheduleState
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first

data class AlarmPrayerScheduleSnapshot(
    val today: PrayerDay,
    val tomorrow: PrayerDay?,
) {
    fun instantsFor(prayer: AlarmPrayer): List<Instant> = buildList {
        add(today.times.instantFor(prayer))
        tomorrow?.let { add(it.times.instantFor(prayer)) }
    }
}

fun PrayerTimes.instantFor(prayer: AlarmPrayer): Instant = when (prayer) {
    AlarmPrayer.FAJR -> fajr
    AlarmPrayer.DHUHR -> dhuhr
    AlarmPrayer.ASR -> asr
    AlarmPrayer.MAGHRIB -> maghrib
    AlarmPrayer.ISHA -> isha
}

fun interface AlarmPrayerScheduleSource {
    suspend fun snapshot(): AlarmPrayerScheduleSnapshot?
}

class RepositoryAlarmPrayerScheduleSource(
    private val repository: PrayerScheduleRepository,
    private val prayerTimeCalculator: PrayerTimeCalculator,
) : AlarmPrayerScheduleSource {
    override suspend fun snapshot(): AlarmPrayerScheduleSnapshot? {
        val state = repository.observeSchedule().first { it !is PrayerScheduleState.Loading }
        val ready = state as? PrayerScheduleState.Ready ?: return null
        val schedule = ready.schedule
        val tomorrowResult = prayerTimeCalculator.calculate(
            date = schedule.localDate.plusDays(1),
            coordinates = schedule.selectedLocation.coordinates,
            zoneId = schedule.selectedLocation.zoneId,
            settings = schedule.settings,
        )
        val tomorrow = (tomorrowResult as? PrayerCalculationResult.Success)?.prayerDay
        return AlarmPrayerScheduleSnapshot(
            today = schedule.today,
            tomorrow = tomorrow,
        )
    }
}

sealed interface AlarmReconcileOutcome {
    val alarmId: String

    data class Scheduled(
        override val alarmId: String,
        val occurrence: AlarmOccurrence,
    ) : AlarmReconcileOutcome

    data class NeedsExactAlarmAccess(
        override val alarmId: String,
    ) : AlarmReconcileOutcome

    data class Cancelled(
        override val alarmId: String,
        val reason: Reason,
    ) : AlarmReconcileOutcome {
        enum class Reason { DISABLED, UNRESOLVABLE }
    }
}

data class AlarmReconciliationReport(
    val outcomes: List<AlarmReconcileOutcome>,
)

class AlarmReconciler(
    private val ruleRepository: AlarmRuleRepository,
    private val platformScheduler: AlarmPlatformScheduler,
    private val prayerScheduleSource: AlarmPrayerScheduleSource,
    private val clock: Clock,
    private val deviceZoneId: () -> ZoneId,
) {
    suspend fun reconcile(): AlarmReconciliationReport {
        val now = clock.instant()
        var prayerSnapshotLoaded = false
        var prayerSnapshot: AlarmPrayerScheduleSnapshot? = null

        val outcomes = ruleRepository.getAll()
            .sortedBy { it.alarmId }
            .map { rule ->
                if (!rule.enabled) {
                    platformScheduler.cancel(rule.alarmId)
                    return@map AlarmReconcileOutcome.Cancelled(
                        alarmId = rule.alarmId,
                        reason = AlarmReconcileOutcome.Cancelled.Reason.DISABLED,
                    )
                }

                val occurrence = when (val definition = rule.definition) {
                    is AlarmDefinition.Custom -> AlarmOccurrenceResolver.resolveCustom(
                        rule = rule,
                        now = now,
                        deviceZoneId = deviceZoneId(),
                    )

                    is AlarmDefinition.PrayerLinked -> {
                        if (!prayerSnapshotLoaded) {
                            prayerSnapshot = prayerScheduleSource.snapshot()
                            prayerSnapshotLoaded = true
                        }
                        val instants = prayerSnapshot?.instantsFor(definition.prayer).orEmpty()
                        AlarmOccurrenceResolver.resolvePrayer(rule, instants, now)
                    }
                }

                if (occurrence == null) {
                    platformScheduler.cancel(rule.alarmId)
                    AlarmReconcileOutcome.Cancelled(
                        alarmId = rule.alarmId,
                        reason = AlarmReconcileOutcome.Cancelled.Reason.UNRESOLVABLE,
                    )
                } else {
                    when (platformScheduler.scheduleExact(occurrence)) {
                        is AlarmScheduleResult.Scheduled -> AlarmReconcileOutcome.Scheduled(
                            alarmId = rule.alarmId,
                            occurrence = occurrence,
                        )

                        AlarmScheduleResult.NeedsExactAlarmAccess ->
                            AlarmReconcileOutcome.NeedsExactAlarmAccess(rule.alarmId)
                    }
                }
            }

        return AlarmReconciliationReport(outcomes)
    }
}
