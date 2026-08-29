package com.archimedeprojects.arihna.core.prayer.model

import java.time.Instant

data class PrayerTimes(
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val maghrib: Instant,
    val isha: Instant,
)
