package com.archimedeprojects.arihna.feature.prayerschedule.domain

import kotlinx.coroutines.flow.Flow

interface PrayerScheduleRepository {
    fun observeSchedule(): Flow<PrayerScheduleState>

    suspend fun refresh()
}
