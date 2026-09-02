package com.archimedeprojects.arihna.core.qibla.model

sealed interface QiblaBearingResult {
    data class Success(
        val bearingTrueDegrees: Double,
    ) : QiblaBearingResult

    data object InvalidCoordinates : QiblaBearingResult

    data object AtKaabaOrCoincident : QiblaBearingResult
}
