package com.archimedeprojects.arihna.core.qibla.platform

import android.hardware.GeomagneticField
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.qibla.heading.MagneticDeclinationProvider
import java.time.Clock

class AndroidGeomagneticDeclinationProvider(
    private val clock: Clock,
) : MagneticDeclinationProvider {
    override fun declinationDegrees(origin: Coordinates): Double {
        require(origin.isValid) { "Coordinates must be valid" }
        return GeomagneticField(
            origin.latitude.toFloat(),
            origin.longitude.toFloat(),
            0f,
            clock.millis(),
        ).declination.toDouble()
    }
}
