package com.archimedeprojects.arihna.core.prayer.calculation

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult.Reason
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerDay
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField
import kotlin.math.abs
import com.batoulapps.adhan2.Coordinates as AdhanCoordinates
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
            val resolvedHighLatitudeRule = AdhanPrayerMappings.resolveHighLatitudeRule(
                settings.highLatitudeRule,
                coordinates.latitude,
            )
            val baseParameters = AdhanPrayerMappings.calculationMethod(settings.method).parameters
            val ramadanIshaAdjustment = if (
                settings.method == PrayerCalculationMethod.UMM_AL_QURA && isRamadan(date)
            ) {
                RAMADAN_UMM_AL_QURA_EXTRA_ISHA_MINUTES
            } else {
                0
            }
            val parameters = baseParameters.copy(
                madhab = AdhanPrayerMappings.asrMethod(settings.asrMethod),
                highLatitudeRule = AdhanPrayerMappings.highLatitudeRule(resolvedHighLatitudeRule),
                prayerAdjustments = AdhanPrayerMappings.adjustments(settings.adjustments),
                methodAdjustments = baseParameters.methodAdjustments.copy(
                    isha = baseParameters.methodAdjustments.isha + ramadanIshaAdjustment,
                ),
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
        } catch (_: DateTimeException) {
            PrayerCalculationResult.Unavailable(Reason.CALCULATION_ERROR)
        }
    }

    private fun isRamadan(date: LocalDate): Boolean =
        HijrahChronology.INSTANCE.date(date).get(ChronoField.MONTH_OF_YEAR) == RAMADAN_MONTH

    private fun kotlin.time.Instant.toJavaInstant(): Instant =
        Instant.ofEpochMilli(toEpochMilliseconds())

    private companion object {
        const val RAMADAN_MONTH = 9
        const val RAMADAN_UMM_AL_QURA_EXTRA_ISHA_MINUTES = 30
        const val POLAR_CIRCLE_LATITUDE = 66.0
    }
}
