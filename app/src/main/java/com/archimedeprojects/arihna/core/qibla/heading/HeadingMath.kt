package com.archimedeprojects.arihna.core.qibla.heading

import com.archimedeprojects.arihna.core.qibla.calculation.normalizeDegrees

private val SOURCE_PRIORITY = listOf(
    HeadingSource.TRUE_HEADING_SENSOR,
    HeadingSource.ROTATION_VECTOR,
    HeadingSource.GEOMAGNETIC_ROTATION_VECTOR,
    HeadingSource.ACCELEROMETER_MAGNETIC_FIELD,
)

fun selectHeadingSource(availableSources: Set<HeadingSource>): HeadingSource? =
    SOURCE_PRIORITY.firstOrNull(availableSources::contains)

fun trueHeadingFromMagneticDegrees(
    magneticHeadingDegrees: Double,
    declinationDegrees: Double,
): Double = normalizeDegrees(magneticHeadingDegrees + declinationDegrees)

fun headingForDisplayQuarterTurns(
    naturalOrientationHeadingDegrees: Double,
    clockwiseQuarterTurns: Int,
): Double {
    require(clockwiseQuarterTurns in 0..3) { "Display rotation must be 0..3 quarter turns" }
    return normalizeDegrees(naturalOrientationHeadingDegrees - clockwiseQuarterTurns * 90.0)
}

fun headingQualityFromPlatformAccuracy(accuracy: Int): HeadingQuality = when (accuracy) {
    3 -> HeadingQuality.HIGH
    2 -> HeadingQuality.MEDIUM
    1 -> HeadingQuality.LOW
    0 -> HeadingQuality.UNRELIABLE
    else -> HeadingQuality.UNKNOWN
}

fun combineHeadingQuality(first: HeadingQuality, second: HeadingQuality): HeadingQuality = when {
    HeadingQuality.UNRELIABLE in setOf(first, second) -> HeadingQuality.UNRELIABLE
    HeadingQuality.LOW in setOf(first, second) -> HeadingQuality.LOW
    HeadingQuality.UNKNOWN in setOf(first, second) -> HeadingQuality.UNKNOWN
    HeadingQuality.MEDIUM in setOf(first, second) -> HeadingQuality.MEDIUM
    else -> HeadingQuality.HIGH
}
