package com.archimedeprojects.arihna.feature.prayerschedule.data

import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.prayer.model.PrayerTimeAdjustments
import kotlinx.coroutines.flow.Flow

interface PrayerSettingsRepository {
    val settings: Flow<PrayerCalculationSettings>

    suspend fun get(): PrayerCalculationSettings

    suspend fun update(settings: PrayerCalculationSettings)
}

object PrayerSettingsDefaults {
    val CANONICAL: PrayerCalculationSettings = PrayerCalculationSettings(
        method = PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrMethod = AsrMethod.STANDARD,
        highLatitudeRule = HighLatitudeRule.AUTOMATIC,
        adjustments = PrayerTimeAdjustments(),
    )
}
