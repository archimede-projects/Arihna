package com.archimedeprojects.arihna.feature.prayerschedule.presentation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface PrayerScheduleTicker {
    fun ticks(): Flow<Unit>
}

class OneSecondPrayerScheduleTicker : PrayerScheduleTicker {
    override fun ticks(): Flow<Unit> = flow {
        while (true) {
            delay(TICK_INTERVAL_MILLIS)
            emit(Unit)
        }
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 1_000L
    }
}
