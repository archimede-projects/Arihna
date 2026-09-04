package com.archimedeprojects.arihna.feature.alarms.domain

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmScheduleResult
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmCapability
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmReconcilerTest {
    @Test
    fun prayerNameMapsToCorrespondingPrayerInstant() {
        val times = prayerTimes(Instant.parse("2026-09-04T03:00:00Z"))
        assertEquals(times.fajr, times.instantFor(AlarmPrayer.FAJR))
        assertEquals(times.dhuhr, times.instantFor(AlarmPrayer.DHUHR))
        assertEquals(times.asr, times.instantFor(AlarmPrayer.ASR))
        assertEquals(times.maghrib, times.instantFor(AlarmPrayer.MAGHRIB))
        assertEquals(times.isha, times.instantFor(AlarmPrayer.ISHA))
    }

    @Test
    fun mixedSnapshotSchedulesOneNextOccurrencePerEnabledRuleAndCancelsDisabled() = runBlocking {
        val now = Instant.parse("2026-09-04T10:00:00Z")
        val rules = listOf(
            prayerRule("prayer", AlarmPrayer.DHUHR, enabled = true, offsetMinutes = -5),
            customRule("custom", LocalTime.of(12, 30), enabled = true),
            customRule("disabled", LocalTime.of(13, 0), enabled = false),
        )
        val scheduler = RecordingScheduler()
        val reconciler = AlarmReconciler(
            ruleRepository = FakeRuleRepository(rules),
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource {
                AlarmPrayerScheduleSnapshot(
                    today = day(LocalDate.of(2026, 9, 4), Instant.parse("2026-09-04T03:00:00Z")),
                    tomorrow = day(LocalDate.of(2026, 9, 5), Instant.parse("2026-09-05T03:00:00Z")),
                )
            },
            clock = Clock.fixed(now, ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("UTC") },
        )

        val report = reconciler.reconcile()

        assertEquals(listOf("custom", "prayer"), scheduler.scheduled.map { it.alarmId }.sorted())
        assertEquals(listOf("disabled"), scheduler.cancelled)
        val prayerOccurrence = scheduler.scheduled.single { it.alarmId == "prayer" }
        assertEquals(Instant.parse("2026-09-04T11:55:00Z"), prayerOccurrence.triggerAt)
        assertEquals(3, report.outcomes.size)
    }

    @Test
    fun prayerThatAlreadyPassedUsesTomorrowAndKeepsOffset() = runBlocking {
        val now = Instant.parse("2026-09-04T22:00:00Z")
        val scheduler = RecordingScheduler()
        val reconciler = AlarmReconciler(
            ruleRepository = FakeRuleRepository(listOf(prayerRule("fajr", AlarmPrayer.FAJR, true, 10))),
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource {
                AlarmPrayerScheduleSnapshot(
                    today = day(LocalDate.of(2026, 9, 4), Instant.parse("2026-09-04T03:00:00Z")),
                    tomorrow = day(LocalDate.of(2026, 9, 5), Instant.parse("2026-09-05T03:00:00Z")),
                )
            },
            clock = Clock.fixed(now, ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("UTC") },
        )

        reconciler.reconcile()

        assertEquals(Instant.parse("2026-09-05T03:10:00Z"), scheduler.scheduled.single().triggerAt)
    }

    @Test
    fun customAlarmUsesInjectedDeviceZoneNotPrayerZone() = runBlocking {
        val now = Instant.parse("2026-09-04T10:00:00Z")
        val scheduler = RecordingScheduler()
        val reconciler = AlarmReconciler(
            ruleRepository = FakeRuleRepository(listOf(customRule("local", LocalTime.of(13, 0), true))),
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource { null },
            clock = Clock.fixed(now, ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("Europe/Rome") },
        )

        reconciler.reconcile()

        assertEquals(Instant.parse("2026-09-04T11:00:00Z"), scheduler.scheduled.single().triggerAt)
    }

    @Test
    fun deniedExactAccessIsReportedWithoutInexactFallback() = runBlocking {
        val scheduler = RecordingScheduler(allowExact = false)
        val reconciler = AlarmReconciler(
            ruleRepository = FakeRuleRepository(listOf(customRule("denied", LocalTime.of(13, 0), true))),
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource { null },
            clock = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("UTC") },
        )

        val report = reconciler.reconcile()

        assertTrue(report.outcomes.single() is AlarmReconcileOutcome.NeedsExactAlarmAccess)
        assertEquals(1, scheduler.scheduleAttempts)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun unresolvablePrayerCancelsExistingPlatformIdentity() = runBlocking {
        val scheduler = RecordingScheduler()
        val reconciler = AlarmReconciler(
            ruleRepository = FakeRuleRepository(listOf(prayerRule("missing", AlarmPrayer.ISHA, true, 0))),
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource { null },
            clock = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("UTC") },
        )

        reconciler.reconcile()

        assertEquals(listOf("missing"), scheduler.cancelled)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    private fun prayerRule(
        id: String,
        prayer: AlarmPrayer,
        enabled: Boolean,
        offsetMinutes: Int,
    ) = AlarmRule(
        alarmId = id,
        revision = 1,
        enabled = enabled,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.PrayerLinked(prayer, offsetMinutes),
    )

    private fun customRule(id: String, time: LocalTime, enabled: Boolean) = AlarmRule(
        alarmId = id,
        revision = 1,
        enabled = enabled,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.Custom(
            label = id,
            localTime = time,
            weekdays = setOf(DayOfWeek.FRIDAY),
        ),
    )

    private fun day(date: LocalDate, fajr: Instant) = PrayerDay(
        date = date,
        zoneId = ZoneId.of("UTC"),
        coordinates = Coordinates(45.0, 10.0),
        settings = PrayerCalculationSettings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
        times = prayerTimes(fajr),
    )

    private fun prayerTimes(fajr: Instant) = PrayerTimes(
        fajr = fajr,
        sunrise = fajr.plusSeconds(3_600),
        dhuhr = fajr.plusSeconds(9 * 3_600L),
        asr = fajr.plusSeconds(12 * 3_600L),
        maghrib = fajr.plusSeconds(15 * 3_600L),
        isha = fajr.plusSeconds(17 * 3_600L),
    )

    private class RecordingScheduler(
        private val allowExact: Boolean = true,
    ) : AlarmPlatformScheduler {
        val scheduled = mutableListOf<AlarmOccurrence>()
        val cancelled = mutableListOf<String>()
        var scheduleAttempts: Int = 0

        override fun capability(): ExactAlarmCapability =
            if (allowExact) ExactAlarmCapability.READY else ExactAlarmCapability.NEEDS_EXACT_ALARM_ACCESS

        override fun scheduleExact(occurrence: AlarmOccurrence): AlarmScheduleResult {
            scheduleAttempts += 1
            if (!allowExact) return AlarmScheduleResult.NeedsExactAlarmAccess
            scheduled.removeAll { it.alarmId == occurrence.alarmId }
            scheduled += occurrence
            return AlarmScheduleResult.Scheduled(occurrence)
        }

        override fun cancel(alarmId: String) {
            cancelled += alarmId
            scheduled.removeAll { it.alarmId == alarmId }
        }
    }

    private class FakeRuleRepository(
        private val initial: List<AlarmRule>,
    ) : AlarmRuleRepository {
        override val rules: Flow<List<AlarmRule>> = flowOf(initial)
        override suspend fun getAll(): List<AlarmRule> = initial
        override suspend fun get(alarmId: String): AlarmRule? = initial.firstOrNull { it.alarmId == alarmId }
        override suspend fun save(draft: AlarmRuleDraft): AlarmRule = error("not used")
        override suspend fun setEnabled(alarmId: String, enabled: Boolean): AlarmRule? = error("not used")
        override suspend fun delete(alarmId: String): AlarmRule? = error("not used")
        override suspend fun disableOneShotAfterValidatedDelivery(
            alarmId: String,
            expectedRevision: Long,
        ): AlarmRule? = error("not used")
    }
}
