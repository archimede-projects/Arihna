package com.archimedeprojects.arihna.core.qibla.heading

import kotlinx.coroutines.flow.Flow

interface HeadingSensorBackend {
    fun availableSources(): Set<HeadingSource>
    fun observe(source: HeadingSource): Flow<HeadingSensorEvent>
}

sealed interface HeadingSensorEvent {
    data class Reading(
        val headingDegrees: Double,
        val quality: HeadingQuality,
        val estimatedAccuracyDegrees: Double?,
    ) : HeadingSensorEvent

    data object RegistrationFailed : HeadingSensorEvent
}
