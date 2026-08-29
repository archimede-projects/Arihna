package com.archimedeprojects.arihna.core.prayer.model

data class PrayerCalculationSettings(
    val method: PrayerCalculationMethod,
    val asrMethod: AsrMethod = AsrMethod.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.AUTOMATIC,
    val adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments(),
)
