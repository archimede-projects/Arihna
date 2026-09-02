package com.archimedeprojects.arihna.core.qibla.calculation

import com.archimedeprojects.arihna.core.prayer.model.Coordinates

object QiblaTarget {
    val KAABA: Coordinates = Coordinates(
        latitude = 21.42251267,
        longitude = 39.82619741,
    )
}

fun normalizeDegrees(value: Double): Double {
    require(value.isFinite()) { "Angle must be finite" }
    val normalized = value % 360.0
    return if (normalized < 0.0) normalized + 360.0 else normalized
}

fun relativeQiblaDirectionDegrees(
    qiblaBearingTrueDegrees: Double,
    deviceHeadingTrueDegrees: Double,
): Double = normalizeDegrees(qiblaBearingTrueDegrees - deviceHeadingTrueDegrees)
