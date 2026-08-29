package com.archimedeprojects.arihna.core.prayer.calculation

import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult.Reason
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimeAdjustments
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import com.batoulapps.adhan2.Coordinates as AdhanCoordinates
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan2.PrayerTimes as AdhanPrayerTimes

class AdhanPrayerTimeCalculator : PrayerTimeCalculator {
    override fun calculate(
        date: LocalDate,
        coordinates: Coordinates,
        zoneId: ZoneId,
        settings: PrayerCalculationSettings,
    ): PrayerCalculationResult {
        if (!coordinates.isValid) {
            return PrayerCalculationResult.Unavailable(Reason.INVALID_COORDINATES)
        }

        return try {
            val resolvedHighLatitudeRule = settings.highLatitudeRule.resolve(coordinates.latitude)
            val baseParameters = settings.method.toAdhan().parameters
            val parameters = baseParameters.copy(
                madhab = settings.asrMethod.toAdhan(),
                highLatitudeRule = resolvedHighLatitudeRule.toAdhan(),
                prayerAdjustments = settings.adjustments.toAdhan(),
            )
            val calculated = AdhanPrayerTimes(
                coordinates = AdhanCoordinates(coordinates.latitude, coordinates.longitude),
                dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth),
                calculationParameters = parameters,
            )
            val times = PrayerTimes(
                fajr = calculated.fajr.toJavaInstant(),
                sunrise = calculated.sunrise.toJavaInstant(),
                dhuhr = calculated.dhuhr.toJavaInstant(),
                asr = calculated.asr.toJavaInstant(),
                maghrib = calculated.maghrib.toJavaInstant(),
                isha = calculated.isha.toJavaInstant(),
            )
            PrayerCalculationResult.Success(
                PrayerDay(
                    date = date,
                    zoneId = zoneId,
                    coordinates = coordinates,
                    settings = settings,
                    times = times,
                ),
            )
        } catch (_: IllegalStateException) {
            PrayerCalculationResult.Unavailable(
                if (abs(coordinates.latitude) >= POLAR_CIRCLE_LATITUDE) {
                    Reason.EXTREME_LATITUDE
                } else {
                    Reason.ASTRONOMICAL_EVENT_UNAVAILABLE
                },
            )
        } catch (_: IllegalArgumentException) {
            PrayerCalculationResult.Unavailable(Reason.CALCULATION_ERROR)
        } catch (_: ArithmeticException) {
            PrayerCalculationResult.Unavailable(Reason.CALCULATION_ERROR)
        }
    }

    private fun PrayerCalculationMethod.toAdhan(): CalculationMethod = when (this) {
        PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        PrayerCalculationMethod.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        PrayerCalculationMethod.ISNA -> CalculationMethod.NORTH_AMERICA
        PrayerCalculationMethod.EGYPTIAN -> CalculationMethod.EGYPTIAN
        PrayerCalculationMethod.KARACHI -> CalculationMethod.KARACHI
        PrayerCalculationMethod.DUBAI -> CalculationMethod.DUBAI
        PrayerCalculationMethod.KUWAIT -> CalculationMethod.KUWAIT
        PrayerCalculationMethod.QATAR -> CalculationMethod.QATAR
        PrayerCalculationMethod.MOONSIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE
        PrayerCalculationMethod.SINGAPORE -> CalculationMethod.SINGAPORE
        PrayerCalculationMethod.TURKEY -> CalculationMethod.TURKEY
    }

    private fun AsrMethod.toAdhan(): Madhab = when (this) {
        AsrMethod.STANDARD -> Madhab.SHAFI
        AsrMethod.HANAFI -> Madhab.HANAFI
    }

    private fun HighLatitudeRule.resolve(latitude: Double): HighLatitudeRule = when (this) {
        HighLatitudeRule.AUTOMATIC -> if (abs(latitude) > AUTO_HIGH_LATITUDE_THRESHOLD) {
            HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        } else {
            HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        }
        else -> this
    }

    private fun HighLatitudeRule.toAdhan(): AdhanHighLatitudeRule = when (this) {
        HighLatitudeRule.AUTOMATIC -> error("AUTOMATIC must be resolved before mapping to Adhan")
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
        HighLatitudeRule.TWILIGHT_ANGLE -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
    }

    private fun PrayerTimeAdjustments.toAdhan(): PrayerAdjustments = PrayerAdjustments(
        fajr = fajrMinutes,
        sunrise = sunriseMinutes,
        dhuhr = dhuhrMinutes,
        asr = asrMinutes,
        maghrib = maghribMinutes,
        isha = ishaMinutes,
    )

    private fun kotlin.time.Instant.toJavaInstant(): Instant = Instant.ofEpochMilli(toEpochMilliseconds())

    private companion object {
        const val AUTO_HIGH_LATITUDE_THRESHOLD = 48.0
        const val POLAR_CIRCLE_LATITUDE = 66.0
    }
}
