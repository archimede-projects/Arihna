package com.archimedeprojects.arihna.feature.qibla

internal fun shortestUnwrappedCompassTarget(
    previousUnwrappedDegrees: Float,
    nextWrappedDegrees: Float,
): Float {
    require(previousUnwrappedDegrees.isFinite())
    require(nextWrappedDegrees.isFinite())

    val previousWrapped = normalizeCompassDegrees(previousUnwrappedDegrees)
    val nextWrapped = normalizeCompassDegrees(nextWrappedDegrees)
    val delta = normalizeCompassDegrees(nextWrapped - previousWrapped + 180f) - 180f
    return previousUnwrappedDegrees + delta
}

internal fun cardinalRoseWrappedTarget(deviceHeadingTrueDegrees: Float): Float {
    require(deviceHeadingTrueDegrees.isFinite())
    return normalizeCompassDegrees(-deviceHeadingTrueDegrees)
}

private fun normalizeCompassDegrees(value: Float): Float {
    val normalized = value % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}
