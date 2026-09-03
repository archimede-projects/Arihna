package com.archimedeprojects.arihna.feature.qibla.domain

import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.SelectedLocation
import com.archimedeprojects.arihna.core.qibla.heading.HeadingQuality
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSource
import com.archimedeprojects.arihna.core.qibla.heading.HeadingUnavailableReason

enum class QiblaBearingUnavailableReason {
    INVALID_COORDINATES,
    AT_KAABA_OR_COINCIDENT,
}

sealed interface QiblaState {
    data class NoLocation(
        val locationState: LocationResolutionState,
    ) : QiblaState

    data class BearingUnavailable(
        val location: SelectedLocation,
        val reason: QiblaBearingUnavailableReason,
    ) : QiblaState

    data class StaticBearing(
        val location: SelectedLocation,
        val bearingTrueDegrees: Double,
    ) : QiblaState

    data class LiveCompassStarting(
        val location: SelectedLocation,
        val bearingTrueDegrees: Double,
    ) : QiblaState

    data class LiveCompass(
        val location: SelectedLocation,
        val bearingTrueDegrees: Double,
        val deviceHeadingTrueDegrees: Double,
        val relativeQiblaDirectionDegrees: Double,
        val quality: HeadingQuality,
        val estimatedAccuracyDegrees: Double?,
        val headingSource: HeadingSource,
    ) : QiblaState

    data class SensorUnavailable(
        val location: SelectedLocation,
        val bearingTrueDegrees: Double,
        val reason: HeadingUnavailableReason,
    ) : QiblaState
}
