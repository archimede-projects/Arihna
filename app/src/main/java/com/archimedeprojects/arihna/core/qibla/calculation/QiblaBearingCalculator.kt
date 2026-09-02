package com.archimedeprojects.arihna.core.qibla.calculation

import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.qibla.model.QiblaBearingResult
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun interface QiblaBearingCalculator {
    fun calculate(origin: Coordinates): QiblaBearingResult
}

object GreatCircleQiblaBearingCalculator : QiblaBearingCalculator {
    override fun calculate(origin: Coordinates): QiblaBearingResult {
        if (!origin.isValid) {
            return QiblaBearingResult.InvalidCoordinates
        }

        val target = QiblaTarget.KAABA
        if (origin.latitude == target.latitude && origin.longitude == target.longitude) {
            return QiblaBearingResult.AtKaabaOrCoincident
        }

        val latitude1 = Math.toRadians(origin.latitude)
        val latitude2 = Math.toRadians(target.latitude)
        val deltaLongitude = Math.toRadians(target.longitude - origin.longitude)

        val y = sin(deltaLongitude) * cos(latitude2)
        val x = cos(latitude1) * sin(latitude2) -
            sin(latitude1) * cos(latitude2) * cos(deltaLongitude)

        val bearingDegrees = normalizeDegrees(Math.toDegrees(atan2(y, x)))
        return QiblaBearingResult.Success(bearingDegrees)
    }
}
