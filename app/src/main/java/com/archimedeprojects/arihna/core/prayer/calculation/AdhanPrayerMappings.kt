package com.archimedeprojects.arihna.core.prayer.calculation

import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimeAdjustments
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import kotlin.math.abs
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule

internal object AdhanPrayerMappings {
    const val AUTO_HIGH_LATITUDE_THRESHOLD = 48.0

    fun calculationMethod(method: PrayerCalculationMethod): CalculationMethod = when (method) {
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

    fun asrMethod(method: AsrMethod): Madhab = when (method) {
        AsrMethod.STANDARD -> Madhab.SHAFI
        AsrMethod.HANAFI -> Madhab.HANAFI
    }

    fun resolveHighLatitudeRule(rule: HighLatitudeRule, latitude: Double): HighLatitudeRule = when (rule) {
        HighLatitudeRule.AUTOMATIC -> if (abs(latitude) > AUTO_HIGH_LATITUDE_THRESHOLD) {
            HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        } else {
            HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        }
        else -> rule
    }

    fun highLatitudeRule(rule: HighLatitudeRule): AdhanHighLatitudeRule = when (rule) {
        HighLatitudeRule.AUTOMATIC -> error("AUTOMATIC must be resolved before mapping to Adhan")
        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
        HighLatitudeRule.TWILIGHT_ANGLE -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
    }

    fun adjustments(adjustments: PrayerTimeAdjustments): PrayerAdjustments = PrayerAdjustments(
        fajr = adjustments.fajrMinutes,
        sunrise = adjustments.sunriseMinutes,
        dhuhr = adjustments.dhuhrMinutes,
        asr = adjustments.asrMinutes,
        maghrib = adjustments.maghribMinutes,
        isha = adjustments.ishaMinutes,
    )
}
