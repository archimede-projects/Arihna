package com.archimedeprojects.arihna.core.prayer.calculation

import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimeAdjustments
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Madhab
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdhanPrayerTimeCalculatorTest {
    private val calculator = AdhanPrayerTimeCalculator()

    @Test
    fun raleighMwlGoldenValues() {
        val day = success(
            date = LocalDate.of(2015, 12, 1),
            coordinates = Coordinates(35.7750, -78.6336),
            zoneId = ZoneId.of("America/New_York"),
            settings = settings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
        )

        assertTimes(day, "05:35", "07:06", "12:05", "14:42", "17:01", "18:26")
    }

    @Test
    fun raleighIsnaHanafiGoldenValues() {
        val day = success(
            date = LocalDate.of(2015, 7, 12),
            coordinates = Coordinates(35.7750, -78.6336),
            zoneId = ZoneId.of("America/New_York"),
            settings = settings(PrayerCalculationMethod.ISNA, asrMethod = AsrMethod.HANAFI),
        )

        assertTimes(day, "04:42", "06:08", "13:21", "18:22", "20:32", "21:57")
    }

    @Test
    fun cairoEgyptianGoldenValues() {
        val day = success(
            date = LocalDate.of(2020, 1, 1),
            coordinates = Coordinates(30.028703, 31.249528),
            zoneId = ZoneId.of("Africa/Cairo"),
            settings = settings(PrayerCalculationMethod.EGYPTIAN),
        )

        assertTimes(day, "05:18", "06:51", "11:59", "14:47", "17:06", "18:29")
    }

    @Test
    fun makkahUmmAlQuraGoldenValuesOutsideRamadan() {
        val day = success(
            date = LocalDate.of(2016, 1, 5),
            coordinates = MAKKAH,
            zoneId = ZoneId.of("Asia/Riyadh"),
            settings = settings(PrayerCalculationMethod.UMM_AL_QURA),
        )

        assertTimes(day, "05:38", "07:00", "12:26", "15:31", "17:52", "19:22")
        assertEquals(90L, Duration.between(day.times.maghrib, day.times.isha).toMinutes())
    }

    @Test
    fun islamabadKarachiGoldenValues() {
        // Independent historical timetable: Masjidway G-9/4 Islamabad, 2015-09-01,
        // explicitly labelled University of Islamic Sciences, Karachi.
        val day = success(
            date = LocalDate.of(2015, 9, 1),
            coordinates = Coordinates(33.6844, 73.0479),
            zoneId = ZoneId.of("Asia/Karachi"),
            settings = settings(PrayerCalculationMethod.KARACHI, asrMethod = AsrMethod.HANAFI),
        )

        assertTimes(day, "04:15", "05:41", "12:08", "16:44", "18:34", "20:01")
    }

    @Test
    fun allArihnaCalculationMethodsMapToExpectedAdhanMethods() {
        val expected = mapOf(
            PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE to CalculationMethod.MUSLIM_WORLD_LEAGUE,
            PrayerCalculationMethod.UMM_AL_QURA to CalculationMethod.UMM_AL_QURA,
            PrayerCalculationMethod.ISNA to CalculationMethod.NORTH_AMERICA,
            PrayerCalculationMethod.EGYPTIAN to CalculationMethod.EGYPTIAN,
            PrayerCalculationMethod.KARACHI to CalculationMethod.KARACHI,
            PrayerCalculationMethod.DUBAI to CalculationMethod.DUBAI,
            PrayerCalculationMethod.KUWAIT to CalculationMethod.KUWAIT,
            PrayerCalculationMethod.QATAR to CalculationMethod.QATAR,
            PrayerCalculationMethod.MOONSIGHTING_COMMITTEE to CalculationMethod.MOON_SIGHTING_COMMITTEE,
            PrayerCalculationMethod.SINGAPORE to CalculationMethod.SINGAPORE,
            PrayerCalculationMethod.TURKEY to CalculationMethod.TURKEY,
        )

        assertEquals(PrayerCalculationMethod.entries.size, expected.size)
        expected.forEach { (arihna, adhan) ->
            assertEquals(adhan, AdhanPrayerMappings.calculationMethod(arihna))
        }
    }

    @Test
    fun asrMethodsMapAndHanafiOccursLaterWithoutChangingOtherPrayers() {
        assertEquals(Madhab.SHAFI, AdhanPrayerMappings.asrMethod(AsrMethod.STANDARD))
        assertEquals(Madhab.HANAFI, AdhanPrayerMappings.asrMethod(AsrMethod.HANAFI))

        val date = LocalDate.of(2015, 7, 12)
        val coordinates = Coordinates(35.7750, -78.6336)
        val zone = ZoneId.of("America/New_York")
        val standard = success(date, coordinates, zone, settings(PrayerCalculationMethod.ISNA))
        val hanafi = success(
            date,
            coordinates,
            zone,
            settings(PrayerCalculationMethod.ISNA, asrMethod = AsrMethod.HANAFI),
        )

        assertTrue(hanafi.times.asr.isAfter(standard.times.asr))
        assertEquals(standard.times.fajr, hanafi.times.fajr)
        assertEquals(standard.times.sunrise, hanafi.times.sunrise)
        assertEquals(standard.times.dhuhr, hanafi.times.dhuhr)
        assertEquals(standard.times.maghrib, hanafi.times.maghrib)
        assertEquals(standard.times.isha, hanafi.times.isha)
    }

    @Test
    fun manualOffsetsAreAppliedPerPrayerAndRemainExact() {
        val date = LocalDate.of(2015, 12, 1)
        val coordinates = Coordinates(35.7750, -78.6336)
        val zone = ZoneId.of("America/New_York")
        val base = success(date, coordinates, zone, settings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE))
        val adjusted = success(
            date,
            coordinates,
            zone,
            settings(
                PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE,
                adjustments = PrayerTimeAdjustments(
                    fajrMinutes = 10,
                    sunriseMinutes = -3,
                    dhuhrMinutes = 0,
                    asrMinutes = 4,
                    maghribMinutes = -2,
                    ishaMinutes = 7,
                ),
            ),
        )

        assertMinuteDelta(base.times.fajr, adjusted.times.fajr, 10)
        assertMinuteDelta(base.times.sunrise, adjusted.times.sunrise, -3)
        assertMinuteDelta(base.times.dhuhr, adjusted.times.dhuhr, 0)
        assertMinuteDelta(base.times.asr, adjusted.times.asr, 4)
        assertMinuteDelta(base.times.maghrib, adjusted.times.maghrib, -2)
        assertMinuteDelta(base.times.isha, adjusted.times.isha, 7)
    }

    @Test
    fun highLatitudeMiddleOfNightGoldenValues() {
        val day = highLatitudeDay(HighLatitudeRule.MIDDLE_OF_THE_NIGHT)
        assertTimes(day, "01:14", "04:26", "13:14", "17:46", "22:01", "01:14")
    }

    @Test
    fun highLatitudeSeventhOfNightGoldenValues() {
        val day = highLatitudeDay(HighLatitudeRule.SEVENTH_OF_THE_NIGHT)
        assertTimes(day, "03:31", "04:26", "13:14", "17:46", "22:01", "22:56")
    }

    @Test
    fun highLatitudeTwilightAngleGoldenValues() {
        val day = highLatitudeDay(HighLatitudeRule.TWILIGHT_ANGLE)
        assertTimes(day, "02:31", "04:26", "13:14", "17:46", "22:01", "23:50")
    }

    @Test
    fun automaticHighLatitudeUsesSeventhAbove48DegreesInBothHemispheres() {
        assertEquals(
            HighLatitudeRule.SEVENTH_OF_THE_NIGHT,
            AdhanPrayerMappings.resolveHighLatitudeRule(HighLatitudeRule.AUTOMATIC, 55.0),
        )
        assertEquals(
            HighLatitudeRule.SEVENTH_OF_THE_NIGHT,
            AdhanPrayerMappings.resolveHighLatitudeRule(HighLatitudeRule.AUTOMATIC, -55.0),
        )
        assertEquals(
            HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
            AdhanPrayerMappings.resolveHighLatitudeRule(HighLatitudeRule.AUTOMATIC, 48.0),
        )

        val automatic = highLatitudeDay(HighLatitudeRule.AUTOMATIC)
        val seventh = highLatitudeDay(HighLatitudeRule.SEVENTH_OF_THE_NIGHT)
        assertEquals(seventh.times, automatic.times)
    }

    @Test
    fun polarAstronomicalFailureReturnsUnavailableInsteadOfThrowing() {
        val result = calculator.calculate(
            date = LocalDate.of(2018, 1, 1),
            coordinates = Coordinates(71.275009, -156.761368),
            zoneId = ZoneId.of("America/Anchorage"),
            settings = settings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
        )

        assertEquals(
            PrayerCalculationResult.Unavailable(PrayerCalculationResult.Reason.EXTREME_LATITUDE),
            result,
        )
    }

    @Test
    fun europeRomeDstUsesZoneRulesAtBoth2026Transitions() {
        val coordinates = Coordinates(41.9028, 12.4964)
        val zone = ZoneId.of("Europe/Rome")

        val march28 = success(
            LocalDate.of(2026, 3, 28),
            coordinates,
            zone,
            settings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
        )
        val march29 = success(
            LocalDate.of(2026, 3, 29),
            coordinates,
            zone,
            settings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
        )
        val october24 = success(
            LocalDate.of(2026, 10, 24),
            coordinates,
            zone,
            settings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
        )
        val october25 = success(
            LocalDate.of(2026, 10, 25),
            coordinates,
            zone,
            settings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
        )

        assertEquals(ZoneOffset.ofHours(1), march28.times.dhuhr.atZone(zone).offset)
        assertEquals(ZoneOffset.ofHours(2), march29.times.dhuhr.atZone(zone).offset)
        assertEquals(ZoneOffset.ofHours(2), october24.times.dhuhr.atZone(zone).offset)
        assertEquals(ZoneOffset.ofHours(1), october25.times.dhuhr.atZone(zone).offset)
    }

    @Test
    fun ummAlQuraRamadanUses120MinutesAndKeepsManualOffsetSeparate() {
        val zone = ZoneId.of("Asia/Riyadh")
        val ramadanDate = LocalDate.of(2016, 6, 15)
        val normal = success(
            ramadanDate,
            MAKKAH,
            zone,
            settings(PrayerCalculationMethod.UMM_AL_QURA),
        )
        val withManualIshaOffset = success(
            ramadanDate,
            MAKKAH,
            zone,
            settings(
                PrayerCalculationMethod.UMM_AL_QURA,
                adjustments = PrayerTimeAdjustments(ishaMinutes = 5),
            ),
        )

        assertEquals(120L, Duration.between(normal.times.maghrib, normal.times.isha).toMinutes())
        assertEquals(125L, Duration.between(withManualIshaOffset.times.maghrib, withManualIshaOffset.times.isha).toMinutes())
    }

    @Test
    fun invalidCoordinatesReturnControlledDomainError() {
        listOf(
            Coordinates(91.0, 0.0),
            Coordinates(-91.0, 0.0),
            Coordinates(0.0, 181.0),
            Coordinates(0.0, -181.0),
            Coordinates(Double.NaN, 0.0),
            Coordinates(0.0, Double.POSITIVE_INFINITY),
        ).forEach { coordinates ->
            val result = calculator.calculate(
                date = LocalDate.of(2026, 1, 1),
                coordinates = coordinates,
                zoneId = ZoneId.of("UTC"),
                settings = settings(PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE),
            )
            assertEquals(
                PrayerCalculationResult.Unavailable(PrayerCalculationResult.Reason.INVALID_COORDINATES),
                result,
            )
        }
    }

    private fun highLatitudeDay(rule: HighLatitudeRule): PrayerDay = success(
        date = LocalDate.of(2020, 6, 15),
        coordinates = Coordinates(55.983226, -3.216649),
        zoneId = ZoneId.of("Europe/London"),
        settings = settings(
            PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = rule,
        ),
    )

    private fun settings(
        method: PrayerCalculationMethod,
        asrMethod: AsrMethod = AsrMethod.STANDARD,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.AUTOMATIC,
        adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments(),
    ) = PrayerCalculationSettings(
        method = method,
        asrMethod = asrMethod,
        highLatitudeRule = highLatitudeRule,
        adjustments = adjustments,
    )

    private fun success(
        date: LocalDate,
        coordinates: Coordinates,
        zoneId: ZoneId,
        settings: PrayerCalculationSettings,
    ): PrayerDay {
        val result = calculator.calculate(date, coordinates, zoneId, settings)
        assertTrue("Expected Success but was $result", result is PrayerCalculationResult.Success)
        return (result as PrayerCalculationResult.Success).prayerDay
    }

    private fun assertTimes(
        day: PrayerDay,
        fajr: String,
        sunrise: String,
        dhuhr: String,
        asr: String,
        maghrib: String,
        isha: String,
    ) {
        assertLocalTime(fajr, day.times.fajr, day.zoneId, "Fajr")
        assertLocalTime(sunrise, day.times.sunrise, day.zoneId, "Sunrise")
        assertLocalTime(dhuhr, day.times.dhuhr, day.zoneId, "Dhuhr")
        assertLocalTime(asr, day.times.asr, day.zoneId, "Asr")
        assertLocalTime(maghrib, day.times.maghrib, day.zoneId, "Maghrib")
        assertLocalTime(isha, day.times.isha, day.zoneId, "Isha")
    }

    private fun assertLocalTime(
        expected: String,
        instant: Instant,
        zoneId: ZoneId,
        label: String,
    ) {
        val expectedTime = LocalTime.parse(expected)
        val actualTime = instant.atZone(zoneId).toLocalTime()
        val expectedMinute = expectedTime.hour * 60 + expectedTime.minute
        val actualMinute = actualTime.hour * 60 + actualTime.minute
        val directDifference = abs(expectedMinute - actualMinute)
        val circularDifference = min(directDifference, MINUTES_PER_DAY - directDifference)
        assertTrue(
            "$label expected $expected ±1 minute but was ${actualTime.withSecond(0).withNano(0)} in $zoneId",
            circularDifference <= GOLDEN_TOLERANCE_MINUTES,
        )
    }

    private fun assertMinuteDelta(base: Instant, adjusted: Instant, expectedMinutes: Int) {
        assertEquals(expectedMinutes.toLong(), Duration.between(base, adjusted).toMinutes())
    }

    private companion object {
        val MAKKAH = Coordinates(21.427009, 39.828685)
        const val GOLDEN_TOLERANCE_MINUTES = 1
        const val MINUTES_PER_DAY = 24 * 60
    }
}
