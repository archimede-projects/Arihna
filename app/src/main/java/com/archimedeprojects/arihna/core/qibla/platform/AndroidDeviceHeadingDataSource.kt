package com.archimedeprojects.arihna.core.qibla.platform

import android.content.Context
import com.archimedeprojects.arihna.core.prayer.model.Coordinates
import com.archimedeprojects.arihna.core.qibla.heading.DefaultDeviceHeadingDataSource
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingDataSource
import com.archimedeprojects.arihna.core.qibla.heading.DeviceHeadingState
import java.time.Clock
import kotlinx.coroutines.flow.Flow

class AndroidDeviceHeadingDataSource(
    context: Context,
    clock: Clock = Clock.systemUTC(),
) : DeviceHeadingDataSource {
    private val delegate = DefaultDeviceHeadingDataSource(
        backend = AndroidHeadingSensorBackend(context),
        magneticDeclinationProvider = AndroidGeomagneticDeclinationProvider(clock),
    )

    override fun observeHeading(origin: Coordinates): Flow<DeviceHeadingState> =
        delegate.observeHeading(origin)
}
