package com.archimedeprojects.arihna.core.prayer.calculation

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationResult
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import java.time.LocalDate
import java.time.ZoneId

fun interface PrayerTimeCalculator {
    fun calculate(
        date: LocalDate,
        coordinates: Coordinates,
        zoneId: ZoneId,
        settings: PrayerCalculationSettings,
    ): PrayerCalculationResult
}
