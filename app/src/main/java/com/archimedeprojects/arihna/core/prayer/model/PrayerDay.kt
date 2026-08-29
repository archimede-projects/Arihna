package com.archimedeprojects.arihna.core.prayer.model

import java.time.LocalDate
import java.time.ZoneId

data class PrayerDay(
    val date: LocalDate,
    val zoneId: ZoneId,
    val coordinates: Coordinates,
    val settings: PrayerCalculationSettings,
    val times: PrayerTimes,
)
