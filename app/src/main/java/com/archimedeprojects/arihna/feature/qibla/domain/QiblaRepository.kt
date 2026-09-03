package com.archimedeprojects.arihna.feature.qibla.domain

import kotlinx.coroutines.flow.Flow

fun interface QiblaRepository {
    fun observeQibla(): Flow<QiblaState>
}
